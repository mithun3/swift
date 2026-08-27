package com.fx.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.fx.common.logging.Logger;
import com.fx.common.logging.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(TcpFixSource.class);

    private final byte[] messageBuffer = new byte[8192];
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
            logger.info("[TcpFixSource] Listening for FIX connections on port ", port);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to bind TCP source to port " + port, e);
        }
    }

    @Override
    public byte[] buffer() {
        return messageBuffer;
    }

    private int bufferPosition = 0; // Current write position from the socket
    private int readPosition = 0;   // Current read position for the parser

    @Override
    public int poll(final byte[] buf, final int offset, final int maxLength) {
        try {
            if (activeClient == null) {
                activeClient = serverChannel.accept(); // Non-blocking
                if (activeClient != null) {
                    activeClient.configureBlocking(false);
                    logger.info("[TcpFixSource] Client connected: ", activeClient.getRemoteAddress());
                }
                return 0; // Yield to event loop
            }

            // If we have a full message buffered, return it immediately
            if (bufferPosition - readPosition >= 76) {
                System.arraycopy(messageBuffer, readPosition, buf, offset, 76);
                readPosition += 76;
                // Compact buffer if fully read
                if (readPosition == bufferPosition) {
                    readPosition = 0;
                    bufferPosition = 0;
                }
                return 76;
            }

            // We need more data. First, compact any partial message to the front of the buffer
            if (readPosition > 0) {
                int remaining = bufferPosition - readPosition;
                if (remaining > 0) {
                    System.arraycopy(messageBuffer, readPosition, messageBuffer, 0, remaining);
                }
                bufferPosition = remaining;
                readPosition = 0;
            }

            // Attempt a non-blocking read into the remaining space
            nioBuffer.limit(messageBuffer.length);
            nioBuffer.position(bufferPosition);

            final int bytesRead = activeClient.read(nioBuffer);
            if (bytesRead < 0) {
                logger.info("[TcpFixSource] Client disconnected.");
                activeClient.close();
                activeClient = null;
                bufferPosition = 0;
                readPosition = 0;
                return 0; // Return 0 to keep the event loop alive but yield
            }

            if (bytesRead > 0) {
                bufferPosition += bytesRead;
            }

            // Check again if we now have a complete message
            if (bufferPosition - readPosition >= 76) {
                System.arraycopy(messageBuffer, readPosition, buf, offset, 76);
                readPosition += 76;
                if (readPosition == bufferPosition) {
                    readPosition = 0;
                    bufferPosition = 0;
                }
                return 76;
            }

            return 0; // Not enough bytes yet
        } catch (final IOException e) {
            logger.error("[TcpFixSource] I/O error during poll: ", e);
            try {
                if (activeClient != null) {
                    activeClient.close();
                }
            } catch (final IOException ignored) {}
            activeClient = null;
            bufferPosition = 0;
            readPosition = 0;
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
            logger.info("[TcpFixSource] Server socket closed.");
        } catch (final IOException e) {
            logger.error("[TcpFixSource] Error closing sockets: ", e);
        }
    }
}
