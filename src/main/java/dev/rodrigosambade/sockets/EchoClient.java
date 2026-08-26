package dev.rodrigosambade.sockets;

import dev.rodrigosambade.net.ConnectivityAndInternetAccess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class EchoClient {

    private static final Duration CONNECTIVITY_TIMEOUT = Duration.ofSeconds(2);
    private static final int SOCKET_TIMEOUT_MS = 3_000;

    private EchoClient() {
    }

    public static String echo(String host, int port, String message) throws IOException {
        ConnectivityAndInternetAccess.requireInternetForRemoteHost(
                host,
                CONNECTIVITY_TIMEOUT);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            return exchangeMessage(socket, message);
        }
    }

    private static String exchangeMessage(Socket socket, String message) throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(
                socket.getInputStream(),
                StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                     socket.getOutputStream(),
                     StandardCharsets.UTF_8))) {
            output.write(message);
            output.newLine();
            output.flush();
            return input.readLine();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: host port message");
        }

        String response = echo(
                args[0],
                Integer.parseInt(args[1]),
                args[2]);
        System.out.println(response);
    }
}
