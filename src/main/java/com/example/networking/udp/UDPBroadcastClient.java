package com.example.networking.udp;

import java.net.*;
import java.util.Scanner;

public class UDPBroadcastClient {
    private static final String SERVER_ADDRESS = "127.0.0.1"; // or your server IP
    private static final int SERVER_PORT = 5000;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws Exception {
        try (
        DatagramSocket socket = new DatagramSocket(); // client socket
            Scanner scanner = new Scanner(System.in)
        ) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);
            byte[] receiveBuffer = new byte[BUFFER_SIZE];

            System.out.println("UDP client started. Type 'bye' to exit.");

            while (true) {
                System.out.print("Enter message (or 'bye' to exit): ");
                String message = scanner.nextLine();

                // Send message to server
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
                socket.send(sendPacket);

                // Receive response from server
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(receivePacket); // blocking until a response is received

                String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Server says: " + response);

                // Exit condition
                if ("bye".equalsIgnoreCase(message.trim())) {
                    System.out.println("Exiting client.");
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
