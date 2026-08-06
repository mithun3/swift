package com.fx.unit;

import com.fx.gateway.FixDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FixDecoder} — byte-level FIX 4.4 parser.
 *
 * @author FX Pipeline Team
 */
@DisplayName("FixDecoder Tests")
class FixDecoderTest {

    /** SOH byte delimiter used in constructing FIX test messages. */
    private static final byte SOH = 0x01;

    private FixDecoder decoder;
    private FixDecoder.FxMessageFrame frame;

    @BeforeEach
    void setUp() {
        decoder = new FixDecoder();
        frame   = new FixDecoder.FxMessageFrame();
    }

    /**
     * Builds a raw FIX 4.4 NewOrderSingle byte array with SOH delimiters.
     */
    private byte[] buildFixMessage(final String symbol, final String side,
                                    final String qty, final String price) {
        final String msg = "35=D" + (char) SOH
                + "34=1" + (char) SOH
                + "49=CLIENT1" + (char) SOH
                + "55=" + symbol + (char) SOH
                + "54=" + side + (char) SOH
                + "38=" + qty + (char) SOH
                + "44=" + price + (char) SOH;
        return msg.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Test
    @DisplayName("Decodes a well-formed EUR/USD BUY NewOrderSingle")
    void testDecodeValidBuyOrder() {
        final byte[] buf = buildFixMessage("EUR/USD", "1", "1000000", "1.08500");
        frame.reset();
        final boolean result = decoder.decode(buf, 0, buf.length, frame);

        assertTrue(result, "Decode should succeed for a valid message");
        assertEquals('D',    frame.msgType,  "MsgType should be 'D'");
        assertEquals(1L,     frame.seqNum,   "SeqNum should be 1");
        assertEquals(1000000L,     frame.notionalMinorUnits, "Notional should be parsed");
        assertEquals((byte) 1, frame.side,   "Buy side should be +1");
        assertEquals(108500L, frame.requestedPriceScaled, "Price 1.08500 * 100000 = 108500");
        assertNotEquals(0L,  frame.clientId, "ClientId hash should be non-zero");
        assertNotEquals(0L,  frame.currencyPairCode, "CurrencyPairCode should be non-zero");
    }

    @Test
    @DisplayName("Decodes a SELL order with side=-1")
    void testDecodeSellOrder() {
        final byte[] buf = buildFixMessage("GBP/USD", "2", "500000", "1.25000");
        frame.reset();
        final boolean result = decoder.decode(buf, 0, buf.length, frame);

        assertTrue(result);
        assertEquals((byte) -1, frame.side, "Sell side should be -1");
    }

    @Test
    @DisplayName("Returns false for null buffer")
    void testNullBuffer() {
        assertFalse(decoder.decode(null, 0, 10, frame));
    }

    @Test
    @DisplayName("Returns false for zero-length message")
    void testZeroLength() {
        assertFalse(decoder.decode(new byte[10], 0, 0, frame));
    }

    @Test
    @DisplayName("Returns false when mandatory tags are missing")
    void testMissingMandatoryTags() {
        // Missing tag 55 (Symbol) — incomplete message
        final String msg = "35=D" + (char) SOH + "34=1" + (char) SOH + "49=C" + (char) SOH;
        final byte[] buf = msg.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertFalse(decoder.decode(buf, 0, buf.length, frame),
                "Should fail when Symbol (tag 55) is missing");
    }

    @Test
    @DisplayName("parseLong returns -1 for non-digit input")
    void testParseLongInvalidInput() {
        final byte[] buf = "12X4".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(-1L, FixDecoder.parseLong(buf, 0, 4));
    }

    @Test
    @DisplayName("parseLong correctly parses a large number")
    void testParseLongLargeNumber() {
        final byte[] buf = "1234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(1234567890L, FixDecoder.parseLong(buf, 0, 10));
    }

    @Test
    @DisplayName("parseScaledPrice correctly scales 1.08500 to 108500")
    void testParseScaledPrice() {
        final byte[] buf = "1.08500".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(108500L, FixDecoder.parseScaledPrice(buf, 0, buf.length));
    }

    @Test
    @DisplayName("parseScaledPrice handles whole number prices")
    void testParseScaledPriceWholeNumber() {
        final byte[] buf = "2".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(200000L, FixDecoder.parseScaledPrice(buf, 0, buf.length));
    }

    @Test
    @DisplayName("hashBytes produces different values for different inputs")
    void testHashBytesUniqueness() {
        final byte[] a = "CLIENT1".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        final byte[] b = "CLIENT2".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertNotEquals(FixDecoder.hashBytes(a, 0, a.length),
                FixDecoder.hashBytes(b, 0, b.length),
                "Different client IDs should produce different hashes");
    }

    @Test
    @DisplayName("hashBytes is deterministic for the same input")
    void testHashBytesDeterministic() {
        final byte[] a = "CLIENT1".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(FixDecoder.hashBytes(a, 0, a.length),
                FixDecoder.hashBytes(a, 0, a.length),
                "Hash should be deterministic");
    }

    @Test
    @DisplayName("FxMessageFrame.reset() zeroes all fields")
    void testFrameReset() {
        frame.msgType            = 'D';
        frame.seqNum             = 99L;
        frame.clientId           = 42L;
        frame.currencyPairCode   = 12345L;
        frame.side               = 1;
        frame.notionalMinorUnits = 1000L;
        frame.requestedPriceScaled = 108500L;

        frame.reset();

        assertEquals(0,  frame.msgType);
        assertEquals(0L, frame.seqNum);
        assertEquals(0L, frame.clientId);
        assertEquals(0L, frame.currencyPairCode);
        assertEquals(0,  frame.side);
        assertEquals(0L, frame.notionalMinorUnits);
        assertEquals(0L, frame.requestedPriceScaled);
    }
}
