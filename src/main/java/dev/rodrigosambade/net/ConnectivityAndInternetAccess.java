/*
 * SPDX-License-Identifier: MIT
 *
 * Java SE connectivity helper adapted for these exercises from Rodrigo Sambade's
 * ConnectivityAndInternetAccess gist:
 * https://gist.github.com/rodrigosambadesaa/6f8e895de21c76b946bf70a1deffe05b
 *
 * The source Gist attributes the original connectivity logic to Emil, str4d and
 * Rodrigo Sambade. This adaptation keeps its DNS-first philosophy and adds a
 * conservative TCP fallback for networks that block UDP/53.
 */
package dev.rodrigosambade.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class ConnectivityAndInternetAccess {

    private static final InetSocketAddress[] DNS_RESOLVERS = {
            new InetSocketAddress("1.1.1.1", 53),
            new InetSocketAddress("8.8.8.8", 53)
    };

    private static final InetSocketAddress[] TCP_FALLBACKS = {
            new InetSocketAddress("1.1.1.1", 443),
            new InetSocketAddress("8.8.8.8", 443)
    };

    private static final String PROBE_HOSTNAME = "example.com";
    private static final int DNS_BUFFER_SIZE = 512;

    private ConnectivityAndInternetAccess() {
    }

    public record Result(
            boolean networkInterfaceAvailable,
            boolean internetReachable,
            String method,
            String endpoint) {
    }

    public static boolean isLocalHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        if (host.equalsIgnoreCase("localhost")) {
            return true;
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isLoopbackAddress() || address.isAnyLocalAddress();
        } catch (IOException ignored) {
            return false;
        }
    }

    public static boolean hasUsableNetworkInterface() throws SocketException {
        var interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) {
            return false;
        }

        for (NetworkInterface networkInterface : Collections.list(interfaces)) {
            if (!isUsable(networkInterface)) {
                continue;
            }

            boolean hasUsableAddress = Collections
                    .list(networkInterface.getInetAddresses())
                    .stream()
                    .anyMatch(address -> !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress());

            if (hasUsableAddress) {
                return true;
            }
        }

        return false;
    }

    public static Result check(Duration timeout) {
        int timeoutMs = timeoutMillis(timeout);
        boolean interfaceAvailable;

        try {
            interfaceAvailable = hasUsableNetworkInterface();
        } catch (SocketException exception) {
            return new Result(false, false, "interface", "enumeration-failed");
        }

        if (!interfaceAvailable) {
            return new Result(false, false, "interface", "none");
        }

        for (InetSocketAddress resolver : DNS_RESOLVERS) {
            if (dnsProbe(resolver, timeoutMs)) {
                return new Result(true, true, "dns", resolver.toString());
            }
        }

        for (InetSocketAddress endpoint : TCP_FALLBACKS) {
            if (tcpProbe(endpoint, timeoutMs)) {
                return new Result(true, true, "tcp", endpoint.toString());
            }
        }

        return new Result(true, false, "none", "unreachable");
    }

    public static void requireInternetForRemoteHost(String host, Duration timeout)
            throws IOException {
        if (isLocalHost(host)) {
            return;
        }

        Result result = check(timeout);
        if (!result.internetReachable()) {
            throw new IOException(
                    "No usable Internet path detected for remote host "
                            + host
                            + " (networkInterfaceAvailable="
                            + result.networkInterfaceAvailable()
                            + ")");
        }
    }

    private static boolean isUsable(NetworkInterface networkInterface)
            throws SocketException {
        return networkInterface.isUp()
                && !networkInterface.isLoopback()
                && !networkInterface.isVirtual();
    }

    private static int timeoutMillis(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        return Math.toIntExact(Math.max(1, timeout.toMillis()));
    }

    private static boolean dnsProbe(InetSocketAddress resolver, int timeoutMs) {
        int requestId = ThreadLocalRandom.current().nextInt(0x10000);
        byte[] query;

        try {
            query = dnsQuery(requestId, PROBE_HOSTNAME);
        } catch (IOException impossible) {
            return false;
        }

        byte[] buffer = new byte[DNS_BUFFER_SIZE];
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(resolver);
            socket.setSoTimeout(timeoutMs);
            socket.send(new DatagramPacket(query, query.length));

            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            return isSuccessfulDnsResponse(buffer, response.getLength(), requestId);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isSuccessfulDnsResponse(
            byte[] response,
            int responseLength,
            int requestId) {
        if (responseLength < 12) {
            return false;
        }

        int responseId = ((response[0] & 0xff) << 8) | (response[1] & 0xff);
        boolean responseBit = (response[2] & 0x80) != 0;
        int responseCode = response[3] & 0x0f;

        return responseId == requestId && responseBit && responseCode == 0;
    }

    private static byte[] dnsQuery(int requestId, String hostname) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);

        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(requestId);
            output.writeShort(0x0100);
            output.writeShort(1);
            output.writeShort(0);
            output.writeShort(0);
            output.writeShort(0);

            for (String label : hostname.split("\\.")) {
                writeDnsLabel(output, label);
            }

            output.writeByte(0);
            output.writeShort(1);
            output.writeShort(1);
        }

        return bytes.toByteArray();
    }

    private static void writeDnsLabel(DataOutputStream output, String label)
            throws IOException {
        byte[] encoded = label.getBytes(StandardCharsets.US_ASCII);
        if (encoded.length == 0 || encoded.length > 63) {
            throw new IOException("Invalid DNS label");
        }

        output.writeByte(encoded.length);
        output.write(encoded);
    }

    private static boolean tcpProbe(InetSocketAddress endpoint, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(endpoint, timeoutMs);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
