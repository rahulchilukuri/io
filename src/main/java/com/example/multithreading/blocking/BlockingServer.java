package com.example.multithreading.blocking;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.example.multithreading.ClientHandler;

/**
 * The Server class listens for incoming client connections and
 * uses a thread pool to handle each client concurrently.
 */
public class BlockingServer {
    private static final int PORT = 12345; // Port number for the server
    private static final int THREAD_POOL_SIZE = 10; // Maximum number of threads in the pool
    private ExecutorService executorService; // Thread pool for handling client connections
    private ServerSocket serverSocket; // Socket for listening to incoming connections
    private volatile boolean running = true; // Flag to control server's running state

    public BlockingServer() {
        // Initialize a fixed-size thread pool. This pool will reuse a fixed
        // number of threads operating off a shared unbounded queue.
        // If additional tasks are submitted when all threads are active, they
        // will wait in the queue until a thread becomes available.
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    /**
     * Starts the server, binds it to the specified port, and
     * continuously listens for client connections.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            // Add a shutdown hook to gracefully shut down the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                stop();
            }));

            while (running) {
                try {
                    // Accept a new client connection. This is a blocking call.
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                    // Submit the ClientHandler task to the thread pool.
                    // The executor service will pick an available thread from the pool
                    // or queue the task if all threads are busy.
                    executorService.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (running) { // Only print error if server is still supposed to be running
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + PORT + ": " + e.getMessage());
            running = false; // Ensure running flag is false if server socket fails to open
        } finally {
            stop(); // Ensure resources are closed even if an exception occurs
        }
    }

    /**
     * Stops the server gracefully by shutting down the thread pool
     * and closing the server socket.
     */
    public void stop() {
        running = false; // Set running flag to false to stop the accept loop

        // Attempt to gracefully shut down the executor service.
        // It will stop accepting new tasks and finish existing ones.
        executorService.shutdown();
        try {
            // Wait for all tasks to complete or for a timeout.
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // If tasks didn't complete within the timeout, force shutdown.
                executorService.shutdownNow();
                System.out.println("Forcibly shutting down executor service.");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow(); // Re-interrupt if current thread was interrupted
            Thread.currentThread().interrupt(); // Restore interrupt status
        }

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
        System.out.println("Server stopped.");
    }

    public static void main(String[] args) {
        com.example.multithreading.blocking.BlockingServer server = new com.example.multithreading.blocking.BlockingServer();
        server.start();
    }
}