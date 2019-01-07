package com.minorityhobbies.ttls;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.minorityhobbies.ttls.Tunnel.bridgeStreams;

/**
 * Class to add SSL security to any TCP-based service.
 *
 * Inputs:
 *  - A target socket address (hostname + port)
 *  - A server bind address (bind address + port)
 *  - A boolean to indicate if client authentication is required
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

class SSLServerTunnel {
    private final ExecutorService threads = Executors.newCachedThreadPool();
    private final SSLServerSocket serverSocket;
    private final String targetHost;
    private final int targetPort;

    public SSLServerTunnel(String targetHost, int targetPort, int bindPort) {
        this(targetHost, targetPort, bindPort, false);
    }

    public SSLServerTunnel(String targetHost, int targetPort, int bindPort, boolean clientAuth) {
        this(targetHost, targetPort, "0.0.0.0", bindPort, clientAuth);
    }

    public SSLServerTunnel(String targetHost, int targetPort, String bindAddress, int bindPort, boolean requireClientAuth) {
        this.targetHost = targetHost;
        this.targetPort = targetPort;

        try {
            // listen on bind address
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault()
                    .createServerSocket(bindPort, 50, Inet4Address.getByName(bindAddress));
            serverSocket.setWantClientAuth(requireClientAuth);
            serverSocket.setNeedClientAuth(requireClientAuth);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket newConnection = serverSocket.accept();
                InputStream sourceInput = newConnection.getInputStream();
                OutputStream sourceOutput = newConnection.getOutputStream();

                // connect to target address
                Socket socket = new Socket(targetHost, targetPort);
                InputStream targetInput = socket.getInputStream();
                OutputStream targetOutput = socket.getOutputStream();

                // connect source input to target output: read input and write to target
                threads.submit(() -> bridgeStreams(sourceInput, targetOutput));

                // connect target input to source output: read responses and write back to client
                threads.submit(() -> bridgeStreams(targetInput, sourceOutput));
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
