package com.minorityhobbies.ttls;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.minorityhobbies.ttls.Tunnel.bridgeStreams;

/**
 * Client for services which have been wrapped in SSL. The idea is that you can send
 * plaintext to the local socket bound on the loopback interface and that uses an
 * authenticated SSL tunnel to talk to the remote service, keeping the data secure
 * and authentic.
 *
 * Inputs:
 *  - A target SSL socket address (hostname + port)
 *  - A local server bind address (port)
 *
 * Operation:
 *  1. Listens on the specified bind address and forwards traffic for each connection to the target address
 *     (each new connection to the server creates a new connection to the target socket)
 *  2. Connects to the target socket address (throws an exception if cannot connect) and bridges streams
 *
 *  Implementation:
 *   - Create a thread to dispatch new connections to a thread
 *     - That thread should attempt to connect to the target and bridge the streams until:
 *       - An EOF occurs reading from the target
 *       - The source input stream closes
 */

class SSLClientTunnel {
    private final ExecutorService threads = Executors.newCachedThreadPool();
    private final ServerSocket serverSocket;
    private final String targetHost;
    private final int targetPort;

    public SSLClientTunnel(String targetHost, int targetPort, int bindPort) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;

        try {
            // listen on bind address
            serverSocket = new ServerSocket(bindPort, 50, Inet4Address.getLoopbackAddress());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket newConnection = serverSocket.accept();
                InputStream clientInput = newConnection.getInputStream();
                OutputStream clientOutput = newConnection.getOutputStream();

                // connect to target address
                Socket socket = SSLSocketFactory.getDefault()
                    .createSocket(targetHost, targetPort);
                InputStream targetInput = socket.getInputStream();
                OutputStream targetOutput = socket.getOutputStream();

                // connect client input to target output: read input and write to target
                threads.submit(() -> bridgeStreams(clientInput, targetOutput));

                // connect target input to client output: read responses and write back to client
                threads.submit(() -> bridgeStreams(targetInput, clientOutput));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        threads.shutdownNow();
        try {
            serverSocket.close();
        } catch (IOException e) {}
    }
}
