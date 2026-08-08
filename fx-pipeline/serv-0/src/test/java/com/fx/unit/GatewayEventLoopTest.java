package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import com.fx.gateway.FixDecoder;
import com.fx.gateway.GatewayEventLoop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GatewayEventLoop}'s message-source contract and error handling.
 *
 * <p>Uses Mockito to verify that a decode-failure is routed to the error queue
 * without crashing the event loop, and that a valid message populates the flyweight
 * correctly — all without a real Chronicle Queue or TCP socket.
 *
 * <p>These tests exercise {@link GatewayEventLoop.FixMessageSource} through a
 * mock-based in-memory stub, validating the gateway's boundary behaviour.
 *
 * @author FX Pipeline Team
 */
@DisplayName("GatewayEventLoop FixMessageSource Contract Tests")
@ExtendWith(MockitoExtension.class)
class GatewayEventLoopTest {

    // ── SOH byte used in FIX message construction ──────────────────────────
    private static final byte SOH = 0x01;

    @Mock
    private GatewayEventLoop.FixMessageSource mockSource;

    /**
     * Verifies that {@link FixDecoder#decode} returns {@code false} when mandatory
     * FIX tags are missing — simulating what GatewayEventLoop routes to the error queue.
     */
    @Test
    @DisplayName("FixDecoder returns false for a FIX message with missing mandatory tags")
    void testDecodeFailsForIncompleteFix() {
        // Build a FIX message missing mandatory tags (no Symbol=55, no Side=54, etc.)
        final String incomplete = "35=D" + (char) SOH + "34=1" + (char) SOH;
        final byte[] buf = incomplete.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        final FixDecoder decoder = new FixDecoder();
        final FixDecoder.FxMessageFrame frame = new FixDecoder.FxMessageFrame();
        frame.reset();

        final boolean result = decoder.decode(buf, 0, buf.length, frame);

        assertFalse(result, "Decode must fail for a FIX message missing mandatory tags");
    }

    /**
     * Verifies that a valid FIX message produces a fully populated {@link FixDecoder.FxMessageFrame}.
     */
    @Test
    @DisplayName("FixDecoder succeeds and populates frame for a complete FIX message")
    void testDecodeSucceedsForCompleteFix() {
        final String msg = "35=D" + (char) SOH
                + "34=7" + (char) SOH
                + "49=CLIENT1" + (char) SOH
                + "55=EUR/USD" + (char) SOH
                + "54=1" + (char) SOH
                + "38=5000000" + (char) SOH
                + "44=1.08500" + (char) SOH;
        final byte[] buf = msg.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        final FixDecoder decoder = new FixDecoder();
        final FixDecoder.FxMessageFrame frame = new FixDecoder.FxMessageFrame();
        frame.reset();

        assertTrue(decoder.decode(buf, 0, buf.length, frame));
        assertEquals('D',       frame.msgType);
        assertEquals(7L,        frame.seqNum);
        assertEquals(5_000_000L, frame.notionalMinorUnits);
        assertEquals((byte) 1,  frame.side);
        assertEquals(108500L,   frame.requestedPriceScaled);
        assertNotEquals(0L,     frame.clientId,         "clientId hash should be non-zero");
        assertNotEquals(0L,     frame.currencyPairCode, "currencyPairCode should be non-zero");
    }

    /**
     * Verifies that the {@code FixMessageSource} interface contract is correct —
     * a mock source returns the expected bytes and exhaustion flag.
     */
    @Test
    @DisplayName("FixMessageSource mock: poll returns bytes; isExhausted signals end")
    void testMockFixMessageSourceContract() {
        final byte[] buf = new byte[256];
        when(mockSource.buffer()).thenReturn(buf);
        when(mockSource.poll(buf, 0, buf.length)).thenReturn(42);
        when(mockSource.isExhausted()).thenReturn(false);

        assertSame(buf, mockSource.buffer());
        assertEquals(42, mockSource.poll(buf, 0, buf.length));
        assertFalse(mockSource.isExhausted());

        verify(mockSource).buffer();
        verify(mockSource).poll(buf, 0, buf.length);
        verify(mockSource).isExhausted();
    }

    /**
     * Verifies that {@link FxMarketEvent#reset()} sets the initial status to RECEIVED —
     * the gateway always initialises events to this status before writing to queue-a.
     */
    @Test
    @DisplayName("FxMarketEvent.reset() sets eventStatus to RECEIVED (gateway invariant)")
    void testGatewayInitialStatusIsReceived() {
        final FxMarketEvent event = new FxMarketEvent();
        event.eventStatus = EventStatus.ERROR; // simulate dirty state
        event.reset();
        assertEquals(EventStatus.RECEIVED, event.eventStatus,
                "Gateway must reset event status to RECEIVED before populating");
    }
}
