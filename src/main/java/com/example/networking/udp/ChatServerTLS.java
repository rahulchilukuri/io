package com.example.networking.udp;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;

public class ChatServerTLS {
    private static final int PORT = 8443;
    private static Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private static final boolean USE_VIRTUAL_THREADS = true;

    public static void main(String[] args) throws Exception {
        SSLServerSocketFactory factory = createSSLContext().getServerSocketFactory();
        try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PORT)) {
            System.out.println("TLS Chat server running on port " + PORT);

            ExecutorService pool = Executors.newFixedThreadPool(20);

            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();

                Runnable handler = () -> handleClient(clientSocket);

                if (USE_VIRTUAL_THREADS) {
                    Thread.startVirtualThread(handler);
                } else {
                    pool.submit(handler);
                }
            }
        }
    }

    private static void handleClient(Socket socket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            clients.add(out);
            String line;
            while ((line = in.readLine()) != null) {
                for (PrintWriter writer : clients) {
                    writer.println(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            clients.removeIf(pw -> pw.checkError());
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static SSLContext createSSLContext() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream keyStoreInputStream = ChatServerTLS.class.getClassLoader().getResourceAsStream("keystore.jks");
        ks.load(keyStoreInputStream, "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "password".toCharArray());
        keyStoreInputStream.close();

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }
}
