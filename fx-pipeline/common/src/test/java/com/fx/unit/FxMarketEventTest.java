package com.fx.unit;

import com.fx.common.event.EventStatus;
import com.fx.common.event.FxMarketEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FxMarketEvent} flyweight DTO.
 *
 * <p>Validates reset behaviour, field mutation, copy semantics, and the
 * embedded {@link FxMarketEvent.CurrencyPairCodec}.
 *
 * @author FX Pipeline Team
 */
@DisplayName("FxMarketEvent Flyweight Tests")
class FxMarketEventTest {

    private FxMarketEvent event;

    @BeforeEach
    void setUp() {
        event = new FxMarketEvent();
    }

    @Test
    @DisplayName("reset() should zero all fields")
    void testResetClearsAllFields() {
        // Arrange: populate with non-zero values
        event.correlationId        = 999L;
        event.ingressNanoTime      = 123456789L;
        event.currencyPairCode     = 0xDEADBEEFL;
        event.side                 = 1;
        event.notionalMinorUnits   = 50_000_000L;
        event.clientTier           = 2;
        event.requestedPriceScaled = 108500L;
        event.executedPriceScaled  = 108510L;
        event.spreadScaled         = 10L;
        event.eventStatus          = EventStatus.PRICED;
        event.clientId             = 42L;
        event.fixMsgType           = 'D';
        event.fixSeqNum            = 77L;

        // Act
        event.reset();

        // Assert: every field should be 0 / zero-sentinel
        assertEquals(0L, event.correlationId,        "correlationId should be 0 after reset");
        assertEquals(0L, event.ingressNanoTime,      "ingressNanoTime should be 0 after reset");
        assertEquals(0L, event.currencyPairCode,     "currencyPairCode should be 0 after reset");
        assertEquals(0,  event.side,                 "side should be 0 after reset");
        assertEquals(0L, event.notionalMinorUnits,   "notionalMinorUnits should be 0 after reset");
        assertEquals(0,  event.clientTier,           "clientTier should be 0 after reset");
        assertEquals(0L, event.requestedPriceScaled, "requestedPriceScaled should be 0 after reset");
        assertEquals(0L, event.executedPriceScaled,  "executedPriceScaled should be 0 after reset");
        assertEquals(0L, event.spreadScaled,         "spreadScaled should be 0 after reset");
        assertEquals(EventStatus.RECEIVED, event.eventStatus, "eventStatus should be RECEIVED after reset");
        assertEquals(0L, event.clientId,             "clientId should be 0 after reset");
        assertEquals(0,  event.fixMsgType,           "fixMsgType should be 0 after reset");
        assertEquals(0L, event.fixSeqNum,            "fixSeqNum should be 0 after reset");
    }

    @Test
    @DisplayName("copyFrom() should replicate all primitive fields")
    void testCopyFromReplicatesAllFields() {
        // Arrange
        final FxMarketEvent source = new FxMarketEvent();
        source.correlationId        = 100L;
        source.ingressNanoTime      = 200L;
        source.currencyPairCode     = 300L;
        source.side                 = -1;
        source.notionalMinorUnits   = 400L;
        source.clientTier           = 3;
        source.requestedPriceScaled = 108500L;
        source.executedPriceScaled  = 108495L;
        source.spreadScaled         = 10L;
        source.eventStatus          = EventStatus.PRICED;
        source.clientId             = 500L;
        source.fixMsgType           = 'D';
        source.fixSeqNum            = 600L;

        // Act
        event.copyFrom(source);

        // Assert
        assertEquals(100L,             event.correlationId);
        assertEquals(200L,             event.ingressNanoTime);
        assertEquals(300L,             event.currencyPairCode);
        assertEquals(-1,               event.side);
        assertEquals(400L,             event.notionalMinorUnits);
        assertEquals(3,                event.clientTier);
        assertEquals(108500L,          event.requestedPriceScaled);
        assertEquals(108495L,          event.executedPriceScaled);
        assertEquals(10L,              event.spreadScaled);
        assertEquals(EventStatus.PRICED, event.eventStatus);
        assertEquals(500L,             event.clientId);
        assertEquals('D',             event.fixMsgType);
        assertEquals(600L,             event.fixSeqNum);
    }

    @Test
    @DisplayName("CurrencyPairCodec.encode/decode round-trip for EUR/USD")
    void testCurrencyPairCodecEurUsd() {
        final byte[] base  = {'E', 'U', 'R'};
        final byte[] quote = {'U', 'S', 'D'};

        final long code = FxMarketEvent.CurrencyPairCodec.encode(base, quote);
        final String decoded = FxMarketEvent.CurrencyPairCodec.decode(code);

        assertEquals("EUR/USD", decoded, "Decoded pair should match original");
    }

    @Test
    @DisplayName("CurrencyPairCodec.encode/decode round-trip for GBP/JPY")
    void testCurrencyPairCodecGbpJpy() {
        final byte[] base  = {'G', 'B', 'P'};
        final byte[] quote = {'J', 'P', 'Y'};

        final long code = FxMarketEvent.CurrencyPairCodec.encode(base, quote);
        final String decoded = FxMarketEvent.CurrencyPairCodec.decode(code);

        assertEquals("GBP/JPY", decoded);
    }

    @Test
    @DisplayName("CurrencyPairCodec different pairs produce different codes")
    void testCurrencyPairCodecUniqueness() {
        final long eurUsd = FxMarketEvent.CurrencyPairCodec.encode(
                new byte[]{'E','U','R'}, new byte[]{'U','S','D'});
        final long gbpUsd = FxMarketEvent.CurrencyPairCodec.encode(
                new byte[]{'G','B','P'}, new byte[]{'U','S','D'});
        final long usdJpy = FxMarketEvent.CurrencyPairCodec.encode(
                new byte[]{'U','S','D'}, new byte[]{'J','P','Y'});

        assertNotEquals(eurUsd, gbpUsd, "EUR/USD and GBP/USD codes must differ");
        assertNotEquals(eurUsd, usdJpy, "EUR/USD and USD/JPY codes must differ");
        assertNotEquals(gbpUsd, usdJpy, "GBP/USD and USD/JPY codes must differ");
    }

    @Test
    @DisplayName("CurrencyPairCodec throws IllegalArgumentException for wrong-length arrays")
    void testCurrencyPairCodecInvalidLength() {
        assertThrows(IllegalArgumentException.class,
                () -> FxMarketEvent.CurrencyPairCodec.encode(
                        new byte[]{'E','U'}, new byte[]{'U','S','D'}),
                "Should throw for 2-byte base currency");

        assertThrows(IllegalArgumentException.class,
                () -> FxMarketEvent.CurrencyPairCodec.encode(
                        new byte[]{'E','U','R'}, new byte[]{'U','S'}),
                "Should throw for 2-byte quote currency");
    }
}
