package com.example.networking.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

public class NIOMultiThreadedServer {
    private static final int PORT = 12345;
    private static final int BUFFER_SIZE = 1024;

    // Thread pool for handling messages
    private final ExecutorService workerPool = Executors.newFixedThreadPool(10);
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    public static void main(String[] args) throws IOException {
        new NIOMultiThreadedServer().start();
    }

    public void start() throws IOException {
        Selector selector = Selector.open();

        // Setup the server socket channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(PORT));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("NIO server started on port " + PORT);

        while (true) {
            selector.select(); // Blocking until I/O events occur
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove(); // Remove from set to avoid re-processing

                try {
                    if (key.isAcceptable()) {
                        accept(selector, key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                    key.cancel();
                    key.channel().close();
                }
            }
        }
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Accepted connection from " + client.getRemoteAddress());
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        int read = client.read(buffer);

        if (read == -1) {
            System.out.println("Client disconnected: " + client.getRemoteAddress());
            client.close();
            key.cancel();
            return;
        }

        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        // Delegate message processing to worker thread
        workerPool.submit(() -> processMessage(client, data));
    }

    private void processMessage(SocketChannel client, byte[] data) {
        String msg = new String(data).trim();
        System.out.println("Received: " + msg);

        String response = "[Echo] " + msg+"\n";
        ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());

        try {
            client.write(responseBuffer);
        } catch (IOException e) {
            System.err.println("Failed to write to client: " + e.getMessage());
        }
    }
}