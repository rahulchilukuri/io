package com.example.networking.udp;


import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPBroadcastServer {
    public static void main(String[] args) throws Exception {
        final int PORT = 5000;
        byte[] buffer = new byte[1024];

        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("UDP Server listening on port " + PORT + "...");

            while (true) {
                // Receive packet from client
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);

                String msg = new String(requestPacket.getData(), 0, requestPacket.getLength());
                String clientInfo = requestPacket.getAddress() + ":" + requestPacket.getPort();
                System.out.println("Received from " + clientInfo + " -> " + msg);

                String response;
                if ("bye".equalsIgnoreCase(msg.trim())) {
                    response = "Goodbye!";
                    System.out.println("Client " + clientInfo + " sent 'bye'. Responded and continuing...");
                } else {
                    response = "Echo: " + msg;
                }

                // Send response
                byte[] responseData = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(
                        responseData,
                        responseData.length,
                        requestPacket.getAddress(),
                        requestPacket.getPort()
                );
                socket.send(responsePacket);

                // Clear buffer
                buffer = new byte[1024];
            }
        }
    }
}
