package com.example.multithreading;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * The Client class connects to the server, sends messages, and receives responses.
 * This can be run multiple times to simulate multiple concurrent clients.
 */
public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // Server IP address or hostname
    private static final int SERVER_PORT = 12345; // Server port number

    public static void main(String[] args) {
        try (
            // Establish a connection to the server
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            // Get input and output streams for communication
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // 'true' for auto-flush
            Scanner scanner = new Scanner(System.in); // For reading user input
        ) {
            System.out.println("Connected to server at " + SERVER_ADDRESS + ":" + SERVER_PORT);
            String userInput;

            // Loop to send messages and receive responses
            while (true) {
                System.out.print("Enter message (or 'bye' to exit): ");
                userInput = scanner.nextLine();

                // Send the message to the server
                out.println(userInput);

                // Read and print the server's response
                String serverResponse = in.readLine();
                if (serverResponse != null) {
                    System.out.println("Server says: " + serverResponse);
                }

                // If the user types "bye", exit the client
                if ("bye".equalsIgnoreCase(userInput.trim())) {
                    System.out.println("Exiting client.");
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}