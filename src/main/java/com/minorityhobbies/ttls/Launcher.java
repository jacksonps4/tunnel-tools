package com.minorityhobbies.ttls;

public class Launcher {
    public static void main(String[] args) {
        int listenPort = Integer.parseInt(args[0]);

        String targetService = args[1];
        String[] targetParts = targetService.split(":");
        if (targetParts.length != 2) {
            throw new IllegalArgumentException();
        }
        String targetHost = targetParts[0];
        int targetPort = Integer.parseInt(targetParts[1]);

        boolean serverMode = Boolean.getBoolean("listen");
        if (serverMode) {
            boolean needsClientAuth = Boolean.getBoolean("clientAuth");
            SSLServerTunnel sslServerTunnel = new SSLServerTunnel(targetHost, targetPort, listenPort, needsClientAuth);
            sslServerTunnel.run();
            sslServerTunnel.close();
        } else {
            SSLClientTunnel sslServerTunnel = new SSLClientTunnel(targetHost, targetPort, listenPort);
            sslServerTunnel.run();
            sslServerTunnel.close();
        }
    }
}
