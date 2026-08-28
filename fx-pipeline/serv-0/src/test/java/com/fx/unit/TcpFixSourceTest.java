package com.fx.unit;

import com.fx.gateway.TcpFixSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for {@link TcpFixSource} — the NIO {@link
 * java.nio.channels.ServerSocketChannel}-backed {@code FixMessageSource} used
 * in production ({@code -Dfx.gateway.mode=tcp}), as opposed to {@code
 * SyntheticFixSource} used by default/tests. These tests lock in the current
 * fixed-message-length (76 bytes) framing, buffering/compaction, and
 * connect/disconnect behaviour exactly as implemented — no behavioral changes.
 *
 * <p>Tests bind to an ephemeral port ({@code new TcpFixSource(0)}) and recover
 * the OS-assigned port via reflection on the private {@code serverChannel}
 * field, since {@code TcpFixSource} exposes no accessor for it. This avoids
 * CI port collisions from a hardcoded port number.
 *
 * @author FX Pipeline Team
 */
@DisplayName("TcpFixSource Characterization Tests")
class TcpFixSourceTest {

    /** Fixed message length declared as {@code TcpFixSource.MESSAGE_LENGTH}. */
    private static final int MESSAGE_LENGTH = 76;

    private static int boundPort(final TcpFixSource source) throws Exception {
        final Field field = TcpFixSource.class.getDeclaredField("serverChannel");
        field.setAccessible(true);
        final ServerSocketChannel channel = (ServerSocketChannel) field.get(source);
        return ((InetSocketAddress) channel.getLocalAddress()).getPort();
    }

    private static byte[] message(final int fillStart) {
        final byte[] msg = new byte[MESSAGE_LENGTH];
        for (int i = 0; i < MESSAGE_LENGTH; i++) {
            msg[i] = (byte) (fillStart + i);
        }
        return msg;
    }

    /** Bounded poll — fails fast on regression instead of hanging the suite. */
    private static int pollUntilNonZero(final TcpFixSource source, final byte[] buf, final long timeoutMillis) {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (true) {
            final int result = source.poll(buf, 0, buf.length);
            if (result != 0) {
                return result;
            }
            if (System.nanoTime() > deadlineNanos) {
                fail("poll() did not return a non-zero result within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }

    @Test
    @DisplayName("poll() returns 0 and isExhausted() is false before any client connects")
    void testPollReturnsZeroWithNoClient() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0)) {
            final byte[] buf = source.buffer();
            assertEquals(0, source.poll(buf, 0, buf.length));
            assertFalse(source.isExhausted(), "a TCP server source is never exhausted");
        }
    }

    @Test
    @DisplayName("poll() delivers a complete 76-byte message once the client has sent one in a single write")
    void testPollDeliversFullMessageInOneWrite() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0);
             SocketChannel client = SocketChannel.open()) {

            client.connect(new InetSocketAddress("localhost", boundPort(source)));

            final byte[] sent = message(1);
            client.write(java.nio.ByteBuffer.wrap(sent));

            final byte[] buf = source.buffer();
            final int bytesRead = pollUntilNonZero(source, buf, 5_000L);

            assertEquals(MESSAGE_LENGTH, bytesRead);
            assertArrayEquals(sent, java.util.Arrays.copyOf(buf, MESSAGE_LENGTH));
        }
    }

    @Test
    @DisplayName("poll() buffers a partial message and only returns it once the remaining bytes arrive")
    void testPollBuffersPartialMessageAcrossWrites() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0);
             SocketChannel client = SocketChannel.open()) {

            client.connect(new InetSocketAddress("localhost", boundPort(source)));

            final byte[] sent = message(10);
            final byte[] buf = source.buffer();

            // First write: only the first half of the message.
            client.write(java.nio.ByteBuffer.wrap(sent, 0, MESSAGE_LENGTH / 2));

            // Not enough bytes yet — poll() must keep returning 0, never a short read.
            final long deadlineNanos = System.nanoTime() + 1_000L * 1_000_000L;
            while (System.nanoTime() < deadlineNanos) {
                assertEquals(0, source.poll(buf, 0, buf.length),
                        "poll() must never return a partial/short message");
            }

            // Second write: the rest of the message.
            client.write(java.nio.ByteBuffer.wrap(sent, MESSAGE_LENGTH / 2, MESSAGE_LENGTH - MESSAGE_LENGTH / 2));

            final int bytesRead = pollUntilNonZero(source, buf, 5_000L);
            assertEquals(MESSAGE_LENGTH, bytesRead);
            assertArrayEquals(sent, java.util.Arrays.copyOf(buf, MESSAGE_LENGTH));
        }
    }

    @Test
    @DisplayName("poll() compacts a buffered trailing partial message before reading more, then delivers it intact")
    void testPollCompactsTrailingPartialMessage() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0);
             SocketChannel client = SocketChannel.open()) {

            client.connect(new InetSocketAddress("localhost", boundPort(source)));

            final byte[] first = message(0);
            final byte[] second = message(100);

            // One write containing: full first message + the first 30 bytes of a second message.
            final int partialLen = 30;
            final byte[] combined = new byte[MESSAGE_LENGTH + partialLen];
            System.arraycopy(first, 0, combined, 0, MESSAGE_LENGTH);
            System.arraycopy(second, 0, combined, MESSAGE_LENGTH, partialLen);
            client.write(java.nio.ByteBuffer.wrap(combined));

            final byte[] buf = source.buffer();
            final int firstRead = pollUntilNonZero(source, buf, 5_000L);
            assertEquals(MESSAGE_LENGTH, firstRead);
            assertArrayEquals(first, java.util.Arrays.copyOf(buf, MESSAGE_LENGTH));

            // Only 30 leftover bytes remain — poll() must compact them to the front
            // before attempting to read more, rather than losing or misplacing them.
            client.write(java.nio.ByteBuffer.wrap(second, partialLen, MESSAGE_LENGTH - partialLen));

            final int secondRead = pollUntilNonZero(source, buf, 5_000L);
            assertEquals(MESSAGE_LENGTH, secondRead);
            assertArrayEquals(second, java.util.Arrays.copyOf(buf, MESSAGE_LENGTH));
        }
    }

    @Test
    @DisplayName("poll() delivers a second already-buffered message straight from the buffer, without another socket read")
    void testPollDeliversSecondBufferedMessageWithoutAnotherRead() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0);
             SocketChannel client = SocketChannel.open()) {

            client.connect(new InetSocketAddress("localhost", boundPort(source)));

            final byte[] first = message(0);
            final byte[] second = message(50);
            final byte[] combined = new byte[MESSAGE_LENGTH * 2];
            System.arraycopy(first, 0, combined, 0, MESSAGE_LENGTH);
            System.arraycopy(second, 0, combined, MESSAGE_LENGTH, MESSAGE_LENGTH);
            client.write(java.nio.ByteBuffer.wrap(combined));

            final byte[] buf = source.buffer();
            final int firstRead = pollUntilNonZero(source, buf, 5_000L);
            assertEquals(MESSAGE_LENGTH, firstRead);
            assertArrayEquals(first, java.util.Arrays.copyOf(buf, MESSAGE_LENGTH));

            // The second message is already sitting in the internal buffer (leftover == 76
            // bytes) — this poll() must return it via the top-of-method fast path, with no
            // further socket read needed.
            final int secondRead = source.poll(buf, 0, buf.length);
            assertEquals(MESSAGE_LENGTH, secondRead);
            assertArrayEquals(second, java.util.Arrays.copyOf(buf, MESSAGE_LENGTH));
        }
    }

    @Test
    @DisplayName("poll() catches an IOException from a closed channel read, logs it, and resets state instead of throwing")
    void testPollCatchesIOExceptionAndResetsState() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0);
             SocketChannel client = SocketChannel.open()) {

            client.connect(new InetSocketAddress("localhost", boundPort(source)));
            awaitClientAccepted(source, 5_000L);

            // Close the underlying channel directly (bypassing TcpFixSource.close()),
            // leaving `activeClient` non-null but the channel itself closed — the next
            // read() on it deterministically throws ClosedChannelException (an IOException).
            activeClientField(source).close();

            final byte[] buf = source.buffer();
            assertDoesNotThrow(() -> source.poll(buf, 0, buf.length),
                    "poll() must catch the IOException internally, never propagate it");
            assertNull(activeClientField(source), "activeClient must be cleared to null after the I/O error");
        }
    }

    @Test
    @DisplayName("poll() returns 0 and resets state after the client disconnects")
    void testPollHandlesClientDisconnect() throws Exception {
        try (TcpFixSource source = new TcpFixSource(0)) {
            final SocketChannel client = SocketChannel.open();
            client.connect(new InetSocketAddress("localhost", boundPort(source)));

            final byte[] buf = source.buffer();
            awaitClientAccepted(source, 5_000L);
            client.close();

            // The next poll() performing a socket read observes EOF (-1) internally,
            // cleans up state, and yields 0 to the caller rather than propagating -1.
            final long deadlineNanos = System.nanoTime() + 5_000L * 1_000_000L;
            int result = -1;
            while (System.nanoTime() < deadlineNanos) {
                result = source.poll(buf, 0, buf.length);
                if (result == 0) {
                    break;
                }
            }
            assertEquals(0, result, "poll() must return 0 (not -1) after an upstream client disconnects");
            assertNull(activeClientField(source), "activeClient must be cleared to null after disconnect");
        }
    }

    @Test
    @DisplayName("constructor wraps a bind failure (port already in use) in a RuntimeException")
    void testConstructorWrapsBindFailureInRuntimeException() throws Exception {
        try (TcpFixSource first = new TcpFixSource(0)) {
            final int busyPort = boundPort(first);

            final RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> new TcpFixSource(busyPort));
            assertTrue(ex.getCause() instanceof IOException,
                    "bind failure must be wrapped with the original IOException as cause");
        }
    }

    @Test
    @DisplayName("close() is safe to call whether or not a client is currently connected")
    void testCloseIsSafeWithAndWithoutClient() throws Exception {
        final TcpFixSource noClientSource = new TcpFixSource(0);
        assertDoesNotThrow(noClientSource::close, "close() with no client ever connected must not throw");

        final TcpFixSource withClientSource = new TcpFixSource(0);
        try (SocketChannel client = SocketChannel.open()) {
            client.connect(new InetSocketAddress("localhost", boundPort(withClientSource)));
            awaitClientAccepted(withClientSource, 5_000L);
        }
        assertDoesNotThrow(withClientSource::close, "close() with an active client must not throw");
    }

    private static SocketChannel activeClientField(final TcpFixSource source) throws Exception {
        final Field field = TcpFixSource.class.getDeclaredField("activeClient");
        field.setAccessible(true);
        return (SocketChannel) field.get(source);
    }

    /** Bounded poll — waits until the source has accepted the pending client connection. */
    private static void awaitClientAccepted(final TcpFixSource source, final long timeoutMillis) throws Exception {
        final long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (activeClientField(source) == null) {
            source.poll(source.buffer(), 0, source.buffer().length);
            if (System.nanoTime() > deadlineNanos) {
                fail("source never accepted the pending client connection within " + timeoutMillis + "ms");
            }
            Thread.onSpinWait();
        }
    }
}
