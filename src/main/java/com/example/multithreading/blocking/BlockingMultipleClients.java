package com.example.multithreading.blocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The Client class connects to the server, sends messages, and receives responses.
 * This version can simulate multiple concurrent clients connecting to the server
 * using a thread pool.
 */
public class BlockingMultipleClients {
    private static final String SERVER_ADDRESS = "localhost"; // Server IP address or hostname
    private static final int SERVER_PORT = 12345; // Server port number
    private static final int NUMBER_OF_SIMULATED_CLIENTS = 50; // Number of concurrent clients to simulate
    private static final int CLIENT_THREAD_POOL_SIZE = 5; // Thread pool size for client tasks

    public static void main(String[] args) {
        // Create an ExecutorService for managing client tasks
        ExecutorService clientExecutor = Executors.newFixedThreadPool(CLIENT_THREAD_POOL_SIZE);

        System.out.println("Starting " + NUMBER_OF_SIMULATED_CLIENTS + " simulated client connections...");

        for (int i = 0; i < NUMBER_OF_SIMULATED_CLIENTS; i++) {
            final int clientId = i + 1; // Assign a unique ID to each client
            clientExecutor.submit(() -> runSingleClientInteraction(clientId));
            try {
                // Add a small delay between launching client tasks to prevent
                // all clients from connecting at precisely the same millisecond.
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Client launcher interrupted: " + e.getMessage());
            }
        }

        // Shut down the client executor service gracefully
        clientExecutor.shutdown();
        try {
            if (!clientExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                clientExecutor.shutdownNow();
                System.out.println("Forcibly shutting down client executor service.");
            }
            System.out.println("All simulated clients finished or timed out.");
        } catch (InterruptedException e) {
            clientExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            System.err.println("Client executor termination interrupted: " + e.getMessage());
        }
    }

    /**
     * This method encapsulates the logic for a single client's interaction
     * with the server. It connects, sends a few messages, and then disconnects.
     *
     * @param clientId A unique identifier for this client simulation.
     */
    private static void runSingleClientInteraction(int clientId) {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            System.out.println("Client " + clientId + " connected to server.");

            // Simulate sending a few messages
            for (int i = 1; i <= 3; i++) {
                String message = "Hello from Client " + clientId + ", message " + i;
                out.println(message);
                System.out.println("Client " + clientId + " sent: " + message);

                String serverResponse = in.readLine();
                if (serverResponse != null) {
                    System.out.println("Client " + clientId + " received: " + serverResponse);
                }
                Thread.sleep(1000); // Wait a bit before sending next message
            }

            // Send a "bye" message to gracefully close the connection
            out.println("bye");
            System.out.println("Client " + clientId + " sent 'bye' and is disconnecting.");

        } catch (IOException e) {
            System.err.println("Client " + clientId + " error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Client " + clientId + " interrupted during sleep: " + e.getMessage());
        } finally {
            System.out.println("Client " + clientId + " disconnected.");
        }
    }
}