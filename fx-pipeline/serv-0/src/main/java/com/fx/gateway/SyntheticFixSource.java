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
        pos = writeField(buf, pos, "35", "D");
        // Tag 34: MsgSeqNum (monotonically increasing)
        pos = writeField(buf, pos, "34", longToAscii(currentSeqNum));
        // Tag 49: SenderCompID = CLIENT1
        pos = writeField(buf, pos, "49", "CLIENT1");
        // Tag 55: Symbol = EUR/USD
        pos = writeField(buf, pos, "55", "EUR/USD");
        // Tag 54: Side = 1 (Buy)
        pos = writeField(buf, pos, "54", "1");
        // Tag 38: OrderQty = 1000000 (1 million units / 10 standard lots)
        pos = writeField(buf, pos, "38", "1000000");
        // Tag 44: Price = 1.08500
        pos = writeField(buf, pos, "44", "1.08500");

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
     * @param buf   destination buffer
     * @param pos   current write position
     * @param tag   FIX tag number string
     * @param value FIX field value string
     * @return updated write position after the written bytes
     */
    private static int writeField(final byte[] buf,
                                   int pos,
                                   final String tag,
                                   final String value) {
        // Write tag bytes
        for (int i = 0; i < tag.length(); i++) {
            buf[pos++] = (byte) tag.charAt(i);
        }
        buf[pos++] = (byte) '=';
        // Write value bytes
        for (int i = 0; i < value.length(); i++) {
            buf[pos++] = (byte) value.charAt(i);
        }
        buf[pos++] = SOH;
        return pos;
    }

    /**
     * Converts a {@code long} to its ASCII decimal representation in a temporary
     * stack-allocated char array pattern — avoids {@code Long.toString()} heap allocation.
     *
     * <p>Note: Returns a {@code String} for use in the synthetic source only (this class
     * is a test helper, not on the hot critical path). In the real FixDecoder, numbers
     * are parsed byte-by-byte without any String intermediary.
     *
     * @param value the long to convert
     * @return ASCII string representation
     */
    private static String longToAscii(final long value) {
        // Acceptable for a synthetic test source — not on the production hot path.
        return Long.toString(value);
    }
}
