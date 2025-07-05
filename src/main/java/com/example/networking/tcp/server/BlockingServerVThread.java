package com.example.networking.tcp.server;

import com.example.networking.tcp.client.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockingServerVThread {
    private static final int PORT = 12345; // Port number for the servere pool
    private static boolean running = true; // Flag to control server's running state

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("BlockingServerVThread started on port " + PORT);

        while (running) {
            try {
                // Accept a new client connection. This is a blocking call.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                Thread.startVirtualThread(new ClientHandler(clientSocket));
            } catch (IOException e) {
                if (running) { // Only print error if server is still supposed to be running
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }

}