package com.fx.client;

import java.io.OutputStream;
import java.net.Socket;

/**
 * {@code FixClientMain} — Standalone TCP Client to send FIX messages.
 *
 * <p>Connects to the Gateway (serv-0) running in TCP mode and sends
 * a single hardcoded FIX NewOrderSingle message.
 */
public final class FixClientMain {

    private static final byte SOH = 0x01;

    public static void main(String[] args) {
        final String host = System.getProperty("fx.client.host", "127.0.0.1");
        final int port = Integer.getInteger("fx.client.port", 5000);

        // A minimal FIX 4.4 New Order Single message
        String fixMessage = "35=D|34=1|49=CLIENT1|55=EUR/USD|54=1|38=1000000|44=1.08500|";
        fixMessage = fixMessage.replace('|', (char) SOH);

        System.out.println("[Client] Attempting to connect to " + host + ":" + port);

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {

            System.out.println("[Client] Connected!");
            System.out.println("[Client] Sending FIX message...");
            
            out.write(fixMessage.getBytes("US-ASCII"));
            out.flush();

            System.out.println("[Client] Message sent successfully.");
            
            // Wait briefly to ensure delivery before closing
            Thread.sleep(100);

        } catch (Exception e) {
            System.err.println("[Client] Error: " + e.getMessage());
            System.err.println("[Client] Ensure serv-0 is running with -Dfx.gateway.mode=tcp");
        }
    }
}
