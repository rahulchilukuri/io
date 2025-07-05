package com.example.networking.udp;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;

public class ChatClientTLS {
    private static final String HOST = "localhost";
    private static final int PORT = 8443;

    public static void main(String[] args) throws Exception {
        SSLSocketFactory factory = createSSLContext().getSocketFactory();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(HOST, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to TLS chat server.");
            Thread reader = Thread.startVirtualThread(() -> {
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        System.out.println("back from server >> " + line);
                    }
                } catch (IOException ignored) {}
            });

            String input;
            while ((input = userInput.readLine()) != null) {
                out.println(input);
            }
        }
    }

    private static SSLContext createSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            }
        }, new java.security.SecureRandom());
        return sslContext;
    }
}
