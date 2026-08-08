package com.fx.unit;

import com.fx.common.event.FxMarketEvent;
import com.fx.gateway.SyntheticFixSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SyntheticFixSource}.
 *
 * <p>
 * Validates the zero-allocation message generation, exhaustion semantics,
 * and the allocation-free integer-to-bytes writer ({@code writeLong}).
 *
 * @author FX Pipeline Team
 */
@DisplayName("SyntheticFixSource Tests")
class SyntheticFixSourceTest {

    /** SOH byte used in FIX messages. */
    private static final byte SOH = 0x01;

    @Test
    @DisplayName("buffer() always returns the same pre-allocated array")
    void testBufferReturnsSameInstance() {
        final SyntheticFixSource source = new SyntheticFixSource(5L);
        assertSame(source.buffer(), source.buffer(), "buffer() must return the same pre-allocated array");
    }

    @Test
    @DisplayName("isExhausted() is false before all messages are polled")
    void testNotExhaustedBeforeAllPolled() {
        final SyntheticFixSource source = new SyntheticFixSource(3L);
        assertFalse(source.isExhausted(), "Source should not be exhausted before any poll");
    }

    @Test
    @DisplayName("isExhausted() is true after all messages are polled")
    void testExhaustedAfterAllMessagesPolled() {
        final SyntheticFixSource source = new SyntheticFixSource(2L);
        final byte[] buf = source.buffer();
        source.poll(buf, 0, buf.length); // message 1
        source.poll(buf, 0, buf.length); // message 2
        assertTrue(source.isExhausted(), "Source should be exhausted after all messages are polled");
    }

    @Test
    @DisplayName("poll() returns -1 when source is exhausted")
    void testPollReturnsMinus1WhenExhausted() {
        final SyntheticFixSource source = new SyntheticFixSource(1L);
        final byte[] buf = source.buffer();
        source.poll(buf, 0, buf.length); // consume the only message
        assertEquals(-1, source.poll(buf, 0, buf.length),
                "poll() must return -1 when source is exhausted");
    }

    @Test
    @DisplayName("poll() returns a positive byte count for each valid message")
    void testPollReturnsBytesWritten() {
        final SyntheticFixSource source = new SyntheticFixSource(3L);
        final byte[] buf = source.buffer();
        final int bytes = source.poll(buf, 0, buf.length);
        assertTrue(bytes > 0, "poll() must return a positive byte count for a valid message");
    }

    @Test
    @DisplayName("Generated FIX message contains mandatory tags")
    void testGeneratedMessageContainsMandatoryTags() {
        final SyntheticFixSource source = new SyntheticFixSource(1L);
        final byte[] buf = source.buffer();
        final int length = source.poll(buf, 0, buf.length);

        final String msg = new String(buf, 0, length, java.nio.charset.StandardCharsets.US_ASCII)
                .replace((char) SOH, '|');

        assertTrue(msg.contains("35=D"), "Must contain MsgType=D");
        assertTrue(msg.contains("49=CLIENT1"), "Must contain SenderCompID=CLIENT1");
        assertTrue(msg.contains("55=EUR/USD"), "Must contain Symbol=EUR/USD");
        assertTrue(msg.contains("54=1"), "Must contain Side=1 (BUY)");
        assertTrue(msg.contains("38=1000000"), "Must contain OrderQty=1000000");
        assertTrue(msg.contains("44=1.08500"), "Must contain Price=1.08500");
    }

    @Test
    @DisplayName("Sequence numbers are monotonically increasing across messages")
    void testSequenceNumbersIncrement() {
        final SyntheticFixSource source = new SyntheticFixSource(3L);
        final byte[] buf = source.buffer();

        // Extract seqNum from messages via the FixDecoder to validate monotonicity
        final com.fx.gateway.FixDecoder decoder = new com.fx.gateway.FixDecoder();
        final com.fx.gateway.FixDecoder.FxMessageFrame frame = new com.fx.gateway.FixDecoder.FxMessageFrame();

        long prevSeq = 0L;
        for (int i = 0; i < 3; i++) {
            final int len = source.poll(buf, 0, buf.length);
            frame.reset();
            decoder.decode(buf, 0, len, frame);
            assertTrue(frame.seqNum > prevSeq,
                    "Sequence number must be strictly increasing: prev=" + prevSeq + " current=" + frame.seqNum);
            prevSeq = frame.seqNum;
        }
    }

    @Test
    @DisplayName("SyntheticFixSource with 0 messages is immediately exhausted")
    void testZeroMessageSourceIsImmediatelyExhausted() {
        final SyntheticFixSource source = new SyntheticFixSource(0L);
        assertTrue(source.isExhausted(), "Zero-message source should be immediately exhausted");
        assertEquals(-1, source.poll(source.buffer(), 0, source.buffer().length));
    }

    @Test
    @DisplayName("Generated message can be fully decoded by FixDecoder")
    void testGeneratedMessageIsDecodable() {
        final SyntheticFixSource source = new SyntheticFixSource(1L);
        final byte[] buf = source.buffer();
        final int len = source.poll(buf, 0, buf.length);

        final com.fx.gateway.FixDecoder decoder = new com.fx.gateway.FixDecoder();
        final com.fx.gateway.FixDecoder.FxMessageFrame frame = new com.fx.gateway.FixDecoder.FxMessageFrame();
        frame.reset();

        assertTrue(decoder.decode(buf, 0, len, frame),
                "FixDecoder must successfully decode a message from SyntheticFixSource");
        assertEquals('D', frame.msgType, "MsgType must be D");
        assertEquals(1L, frame.seqNum, "First seqNum must be 1");
        assertEquals((byte) 1, frame.side, "Side must be BUY (+1)");
        assertEquals(1000000L, frame.notionalMinorUnits, "OrderQty must be 1000000");
        assertEquals(108500L, frame.requestedPriceScaled, "Price must scale to 108500");

        // Verify the currency pair decodes to EUR/USD
        final String pair = FxMarketEvent.CurrencyPairCodec.decode(frame.currencyPairCode);
        assertEquals("EUR/USD", pair, "Currency pair must decode to EUR/USD");
    }
}
