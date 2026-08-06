package com.fx.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code TcpFixSource} — Non-blocking TCP socket reader for FIX messages.
 *
 * <p>This implementation of {@link GatewayEventLoop.FixMessageSource} listens on
 * a specified TCP port using NIO in non-blocking mode. It accepts a single client
 * connection at a time. This keeps the implementation simple for testing while
 * remaining fully allocation-free on the hot path (polling and reading).
 *
 * @author FX Pipeline Team
 */
public final class TcpFixSource implements GatewayEventLoop.FixMessageSource, AutoCloseable {

    private static final int BUFFER_SIZE = 1024;

    private final byte[] messageBuffer = new byte[BUFFER_SIZE];
    private final ByteBuffer nioBuffer = ByteBuffer.wrap(messageBuffer);

    private final ServerSocketChannel serverChannel;
    private SocketChannel activeClient;

    /**
     * Starts listening on the given port in non-blocking mode.
     *
     * @param port the TCP port to listen on
     * @throws RuntimeException if the server socket cannot be bound
     */
    public TcpFixSource(final int port) {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            System.out.println("[TcpFixSource] Listening for FIX connections on port " + port);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to bind TCP source to port " + port, e);
        }
    }

    @Override
    public byte[] buffer() {
        return messageBuffer;
    }

    /**
     * Non-blocking poll for the next FIX message.
     *
     * <p>If no client is connected, it attempts to accept one without blocking.
     * If a client is connected, it attempts a non-blocking read into the buffer.
     *
     * @param buf       destination buffer (same as returned by {@link #buffer()})
     * @param offset    start position
     * @param maxLength maximum bytes to write
     * @return bytes read (> 0), 0 if no data is available, -1 on EOF or error
     */
    @Override
    public int poll(final byte[] buf, final int offset, final int maxLength) {
        try {
            if (activeClient == null) {
                activeClient = serverChannel.accept(); // Non-blocking
                if (activeClient != null) {
                    activeClient.configureBlocking(false);
                    System.out.println("[TcpFixSource] Client connected: " + activeClient.getRemoteAddress());
                }
                return 0; // Yield to event loop
            }

            nioBuffer.clear();
            nioBuffer.limit(offset + Math.min(maxLength, BUFFER_SIZE - offset));
            nioBuffer.position(offset);

            final int bytesRead = activeClient.read(nioBuffer);
            if (bytesRead < 0) {
                System.out.println("[TcpFixSource] Client disconnected.");
                activeClient.close();
                activeClient = null;
                return 0; // Return 0 to keep the event loop alive but yield
            }

            return bytesRead;
        } catch (final IOException e) {
            System.err.println("[TcpFixSource] I/O error during poll: " + e.getMessage());
            try {
                if (activeClient != null) {
                    activeClient.close();
                }
            } catch (final IOException ignored) {}
            activeClient = null;
            return 0;
        }
    }

    @Override
    public boolean isExhausted() {
        return false; // A TCP server is never exhausted; it waits for new clients.
    }

    @Override
    public void close() {
        try {
            if (activeClient != null) {
                activeClient.close();
            }
            if (serverChannel != null) {
                serverChannel.close();
            }
            System.out.println("[TcpFixSource] Server socket closed.");
        } catch (final IOException e) {
            System.err.println("[TcpFixSource] Error closing sockets: " + e.getMessage());
        }
    }
}
