package com.fx.gateway;

import com.fx.common.queue.QueuePaths;

/**
 * {@code SyntheticFixSource} — Deterministic FIX 4.4 message generator for testing.
 *
 * <h2>Purpose</h2>
 * <p>
 * This implementation of {@link GatewayEventLoop.FixMessageSource} generates
 * well-formed synthetic FIX 4.4 New Order Single (MsgType=D) messages into a
 * pre-allocated byte buffer. It enables full pipeline testing with no TCP socket,
 * no external FIX engine, and no heap allocation after construction.
 *
 * <h2>Message Format</h2>
 * <p>
 * Each generated message contains the minimum mandatory tags for the pipeline:
 * <pre>
 *   35=D|34=&lt;seqNum&gt;|49=CLIENT1|55=EUR/USD|54=1|38=1000000|44=1.08500|
 * </pre>
 * Fields are delimited by SOH (0x01). The message is written into the pre-allocated
 * {@code byte[]} buffer on every {@link #poll} call without creating any new objects.
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class SyntheticFixSource implements GatewayEventLoop.FixMessageSource {

    /** SOH field delimiter — ASCII 0x01. */
    private static final byte SOH = 0x01;

    /**
     * Maximum FIX message size: 256 bytes is sufficient for a minimal NewOrderSingle.
     * Pre-allocated once at construction.
     */
    private static final int BUFFER_SIZE = 256;

    /** Pre-allocated buffer — populated on each poll() call, never replaced. */
    private final byte[] messageBuffer = new byte[BUFFER_SIZE];

    /** Total number of messages this source will produce before exhaustion. */
    private final long totalMessages;

    /** Sequence number of the next message to generate. */
    private long currentSeqNum;

    /**
     * Constructs a new synthetic FIX source.
     *
     * @param totalMessages total number of FIX messages to produce;
     *                      after this count, {@link #isExhausted()} returns {@code true}
     */
    public SyntheticFixSource(final long totalMessages) {
        this.totalMessages  = totalMessages;
        this.currentSeqNum  = 1L;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] buffer() {
        return messageBuffer;
    }

    /**
     * Fills {@code buf} with a synthetic FIX NewOrderSingle message if messages remain.
     *
     * <p>The message is written byte-by-byte into the pre-allocated buffer using
     * ASCII encoding. The sequence number increments on each call. No String,
     * StringBuilder, or other objects are allocated.
     *
     * @param buf       destination buffer (must be {@link #buffer()})
     * @param offset    start position (expected: 0)
     * @param maxLength maximum bytes to write
     * @return number of bytes written, or {@code -1} if source is exhausted
     */
    @Override
    public int poll(final byte[] buf, final int offset, final int maxLength) {
        if (isExhausted()) {
            return -1;
        }

        int pos = offset;

        // Tag 35: MsgType = D (New Order Single)
        pos = writeField(buf, pos, (byte)'3', (byte)'5', "D");
        // Tag 34: MsgSeqNum (monotonically increasing)
        // writeLong() writes the decimal digits directly into buf — zero allocation.
        pos = writeTagPrefix(buf, pos, (byte)'3', (byte)'4');
        pos = writeLong(buf, pos, currentSeqNum);
        buf[pos++] = SOH;
        // Tag 49: SenderCompID = CLIENT1
        pos = writeField(buf, pos, (byte)'4', (byte)'9', "CLIENT1");
        // Tag 55: Symbol = EUR/USD
        pos = writeField(buf, pos, (byte)'5', (byte)'5', "EUR/USD");
        // Tag 54: Side = 1 (Buy)
        pos = writeField(buf, pos, (byte)'5', (byte)'4', "1");
        // Tag 38: OrderQty = 1000000 (1 million units / 10 standard lots)
        pos = writeField(buf, pos, (byte)'3', (byte)'8', "1000000");
        // Tag 44: Price = 1.08500
        pos = writeField(buf, pos, (byte)'4', (byte)'4', "1.08500");

        currentSeqNum++;
        return pos - offset; // number of bytes written
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExhausted() {
        return currentSeqNum > totalMessages;
    }

    /**
     * Writes a FIX tag=value pair followed by SOH into {@code buf} at {@code pos}.
     *
     * <p>Tag is written as two literal ASCII bytes (both provided by the caller as
     * pre-computed constants) to avoid String indexing overhead.
     *
     * @param buf   destination buffer
     * @param pos   current write position
     * @param tag0  first ASCII byte of the 2-digit tag number (e.g., '3' for tag 35)
     * @param tag1  second ASCII byte of the 2-digit tag number (e.g., '5' for tag 35)
     * @param value FIX field value string literal (compile-time constant)
     * @return updated write position after the written bytes
     */
    private static int writeField(final byte[] buf,
                                   int pos,
                                   final byte tag0,
                                   final byte tag1,
                                   final String value) {
        buf[pos++] = tag0;
        buf[pos++] = tag1;
        buf[pos++] = (byte) '=';
        for (int i = 0; i < value.length(); i++) {
            buf[pos++] = (byte) value.charAt(i);
        }
        buf[pos++] = SOH;
        return pos;
    }

    /**
     * Writes a tag prefix ({@code tag0}{@code tag1}{@code =}) into {@code buf} without SOH.
     *
     * <p>Used when the value is written separately (e.g., for the sequence number
     * written via {@link #writeLong}).
     *
     * @param buf  destination buffer
     * @param pos  current write position
     * @param tag0 first ASCII byte of the tag
     * @param tag1 second ASCII byte of the tag
     * @return updated write position
     */
    private static int writeTagPrefix(final byte[] buf, int pos,
                                       final byte tag0, final byte tag1) {
        buf[pos++] = tag0;
        buf[pos++] = tag1;
        buf[pos++] = (byte) '=';
        return pos;
    }

    /**
     * Writes a non-negative {@code long} as ASCII decimal digits directly into {@code buf}.
     *
     * <p>This method is the allocation-free replacement for {@code Long.toString()}.
     * It uses a classic digit-extraction technique: repeatedly divides by 10 and writes
     * each digit from the rightmost position. The result is then reversed in-place.
     * No objects are created — all operations are on the pre-allocated {@code byte[]}.
     *
     * @param buf   destination buffer
     * @param pos   current write position
     * @param value the non-negative {@code long} to write
     * @return updated write position after the last digit byte
     */
    private static int writeLong(final byte[] buf, final int pos, long value) {
        if (value == 0L) {
            buf[pos] = (byte) '0';
            return pos + 1;
        }
        final int start = pos;
        int end = pos;
        // Write digits in reverse order
        while (value > 0L) {
            buf[end++] = (byte) ('0' + (value % 10L));
            value /= 10L;
        }
        // Reverse the digit bytes in-place
        int left = start;
        int right = end - 1;
        while (left < right) {
            final byte tmp = buf[left];
            buf[left++] = buf[right];
            buf[right--] = tmp;
        }
        return end;
    }
}
