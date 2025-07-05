package com.example.multithreading.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Server class uses Java NIO for non-blocking I/O to handle
 * multiple client connections efficiently with a single thread (or a few).
 * It uses a Selector to monitor channels for readiness events.
 */
public class NonBlockingIOServer {
    private static final int PORT = 12345; // Port number for the server
    private static final int BUFFER_SIZE = 1024; // Size of the read/write buffer

    private Selector selector; // Monitors channels for I/O events
    private ServerSocketChannel serverChannel; // Channel for accepting new connections
    private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE); // Buffer for reading data from clients

    // A map to store outgoing data for each client.
    // Key: SocketChannel, Value: ByteBuffer containing data to be sent.
    private Map<SocketChannel, ByteBuffer> writeBuffers = new ConcurrentHashMap<>();

    private volatile boolean running = true; // Flag to control server's running state

    public NonBlockingIOServer() {
        // No ExecutorService for client handling directly in this non-blocking model,
        // as a single thread handles all I/O events.
        // If heavy processing is needed, a separate processing thread pool would be used.
    }

    /**
     * Starts the non-blocking server, binds it to the specified port, and
     * continuously listens for I/O events using a Selector.
     */
    public void start() {
        try {
            // 1. Open a Selector
            selector = Selector.open();

            // 2. Open a ServerSocketChannel
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false); // Set to non-blocking mode

            // 3. Bind the server socket to the port
            serverChannel.socket().bind(new InetSocketAddress(PORT));
            System.out.println("Non-blocking Server started on port " + PORT);

            // 4. Register the server channel with the selector for ACCEPT events
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            // Add a shutdown hook to gracefully shut down the server
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                stop();
            }));

            // Main server loop: process I/O events
            while (running) {
                // This call blocks until at least one registered channel is ready for an event
                selector.select();

                // Get the set of keys representing channels that are ready for new events
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove(); // Remove the key from the selected set to avoid processing it again

                    try {
                        if (!key.isValid()) {
                            continue; // Skip invalid keys
                        }

                        if (key.isAcceptable()) {
                            // A new connection is ready to be accepted
                            acceptConnection(key);
                        } else if (key.isReadable()) {
                            // A channel is ready for reading data
                            readData(key);
                        } else if (key.isWritable()) {
                            // A channel is ready for writing data
                            writeData(key);
                        }
                    } catch (IOException e) {
                        // Handle client disconnection or other I/O errors
                        System.err.println("Error processing key for " + key.channel() + ": " + e.getMessage());
                        key.cancel(); // Invalidate the key
                        try {
                            key.channel().close(); // Close the channel
                        } catch (IOException ex) {
                            System.err.println("Error closing channel: " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start non-blocking server on port " + PORT + ": " + e.getMessage());
            running = false;
        } finally {
            stop(); // Ensure resources are closed
        }
    }

    /**
     * Accepts a new client connection.
     * @param key The SelectionKey representing the ServerSocketChannel.
     * @throws IOException If an I/O error occurs during acceptance.
     */
    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = server.accept(); // Accept the connection
        if (clientChannel != null) {
            clientChannel.configureBlocking(false); // Set client channel to non-blocking
            // Register the new client channel with the selector for READ events
            clientChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("Client connected: " + clientChannel.getRemoteAddress());
        }
    }

    /**
     * Reads data from a client channel.
     * @param key The SelectionKey representing the client's SocketChannel.
     * @throws IOException If an I/O error occurs during reading.
     */
    private void readData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        readBuffer.clear(); // Clear the buffer for new data

        int bytesRead = clientChannel.read(readBuffer); // Read data into the buffer

        if (bytesRead == -1) {
            // Client has closed the connection
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
            clientChannel.close(); // Close the channel
            key.cancel(); // Cancel the key
            writeBuffers.remove(clientChannel); // Remove any pending write data for this client
            return;
        }

        if (bytesRead > 0) {
            readBuffer.flip(); // Prepare buffer for reading (limit = current position, position = 0)
            byte[] data = new byte[bytesRead];
            readBuffer.get(data);
            String clientMessage = new String(data).trim(); // Convert bytes to string

            System.out.println("Received from client " + clientChannel.getRemoteAddress() + ": " + clientMessage);

            // Process the message and prepare a response
            String responseMessage = "SERVER RESPONSE: " + clientMessage.toUpperCase() + " (Processed at " + System.currentTimeMillis() + ")";
            ByteBuffer responseBuffer = ByteBuffer.wrap(responseMessage.getBytes());

            // Store the response for writing
            writeBuffers.put(clientChannel, responseBuffer);

            // Register the channel for WRITE events (in addition to READ)
            // This tells the selector that we are interested in knowing when we can write to this channel
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);

            // If the client sends "bye", prepare to close the connection after sending response
            if ("bye".equalsIgnoreCase(clientMessage)) {
                // We'll let the writeData handle the actual closing after sending "bye" response
                System.out.println("Client " + clientChannel.getRemoteAddress() + " sent 'bye'. Preparing to close.");
            }
        }
    }

    /**
     * Writes data to a client channel.
     * @param key The SelectionKey representing the client's SocketChannel.
     * @throws IOException If an I/O error occurs during writing.
     */
    private void writeData(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = writeBuffers.get(clientChannel);

        if (buffer != null && buffer.hasRemaining()) {
            clientChannel.write(buffer); // Write data from the buffer to the channel
        }

        if (buffer == null || !buffer.hasRemaining()) {
            // All data has been written or there was no data to write
            writeBuffers.remove(clientChannel); // Remove the buffer
            // Remove OP_WRITE interest, as we have nothing more to write for now
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            // If the last message was "bye", close the channel now
            // This check is a simplification; a more robust solution might use a flag
            // associated with the client to indicate it's ready for final close.
            if (key.isValid() && clientChannel.isOpen() && key.isReadable() && !writeBuffers.containsKey(clientChannel) && key.attachment() != null && ((String) key.attachment()).contains("bye")) {
                System.out.println("Closing client " + clientChannel.getRemoteAddress() + " after 'bye' response.");
                clientChannel.close();
                key.cancel();
            }
        }
    }

    /**
     * Stops the server gracefully by closing the selector and server channel.
     */
    public void stop() {
        running = false; // Set running flag to false to stop the main loop

        if (selector != null) {
            try {
                // Close all registered channels
                for (SelectionKey key : selector.keys()) {
                    if (key.isValid() && key.channel() != null) {
                        key.channel().close();
                    }
                }
                selector.close();
                System.out.println("Selector closed.");
            } catch (IOException e) {
                System.err.println("Error closing selector: " + e.getMessage());
            }
        }

        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close();
                System.out.println("Server channel closed.");
            } catch (IOException e) {
                System.err.println("Error closing server channel: " + e.getMessage());
            }
        }
        System.out.println("Server stopped.");
    }

    public static void main(String[] args) {
        NonBlockingIOServer server = new NonBlockingIOServer();
        server.start();
    }
}