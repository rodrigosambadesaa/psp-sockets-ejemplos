package dev.rodrigosambade.sockets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EchoServer implements AutoCloseable {

    private static final int CLIENT_TIMEOUT_MS = 30_000;
    private static final int DEFAULT_PORT = 9_000;

    private final ServerSocket serverSocket;
    private final ExecutorService clientExecutor;
    private volatile boolean running = true;

    public EchoServer(int port) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
        clientExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serve() throws IOException {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                clientExecutor.submit(() -> handle(client));
            } catch (SocketException exception) {
                if (running) {
                    throw exception;
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(),
                     StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                     socket.getOutputStream(),
                     StandardCharsets.UTF_8))) {
            socket.setSoTimeout(CLIENT_TIMEOUT_MS);

            String line;
            while ((line = input.readLine()) != null) {
                output.write("ECHO " + line);
                output.newLine();
                output.flush();
            }
        } catch (IOException ignored) {
            // A single client failure must not stop the server.
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        serverSocket.close();
        clientExecutor.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : DEFAULT_PORT;

        try (EchoServer server = new EchoServer(port)) {
            System.out.println("Escuchando en " + server.port());
            server.serve();
        }
    }
}
