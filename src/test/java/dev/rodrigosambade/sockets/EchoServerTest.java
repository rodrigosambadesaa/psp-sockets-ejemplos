package dev.rodrigosambade.sockets;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EchoServerTest {

    @Test
    void localEchoWorksWithoutInternetGate() throws Exception {
        // Close the listening socket before waiting for the server thread to terminate.
        // Resources are closed in reverse declaration order; otherwise executor.close()
        // deadlocks waiting for a serve() loop still blocked in accept().
        try (ExecutorService executor = Executors.newSingleThreadExecutor();
             EchoServer server = new EchoServer(0)) {
            executor.submit(() -> runServer(server));

            String response = EchoClient.echo(
                    "localhost",
                    server.port(),
                    "hola");

            assertEquals("ECHO hola", response);
        }
    }

    private static void runServer(EchoServer server) {
        try {
            server.serve();
        } catch (IOException ignored) {
            // Closing the server socket terminates the accept loop in the test.
        }
    }
}
