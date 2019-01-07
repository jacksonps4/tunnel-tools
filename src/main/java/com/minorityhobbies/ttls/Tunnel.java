package com.minorityhobbies.ttls;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;

class Tunnel {
    static void bridgeStreams(InputStream input, OutputStream output) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                int v = input.read();
                if (v == -1) {
                    break;
                }
                if (v > 0) {
                    output.write(v);
                }
            }
        } catch (EOFException e) {
            // done
        } catch (Exception e) {
            // connection error
            e.printStackTrace();
        }
    }
}
