package com.example.multithreading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * ClientHandler implements Runnable to be executed by a thread from the server's
 * thread pool. Each instance handles communication with a single connected client.
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket; // The socket connected to the client

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (
            // Get input and output streams for communication with the client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); // 'true' for auto-flush
        ) {
            String clientMessage;
            // Read messages from the client until the client closes the connection
            // or sends a specific termination message.
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Received from client " + clientSocket.getInetAddress().getHostAddress() + ": " + clientMessage);

                // Process the message (e.g., convert to uppercase, add a timestamp)
                String responseMessage = "SERVER RESPONSE: " + clientMessage.toUpperCase() + " (Processed at " + System.currentTimeMillis() + ")";

                // Send the response back to the client
                out.println(responseMessage);

                // If the client sends "bye", break the loop and close the connection
                if ("bye".equalsIgnoreCase(clientMessage.trim())) {
                    System.out.println("Client " + clientSocket.getInetAddress().getHostAddress() + " sent 'bye'. Closing connection.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close(); // Ensure the client socket is closed
                    System.out.println("Client " + clientSocket.getInetAddress().getHostAddress() + " disconnected.");
                }
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}