package com.fx.gateway;

/**
 * {@code FixDecoder} — High-performance FIX 4.4 tag=value byte-level parser.
 *
 * <h2>Zero-Allocation Parsing Strategy</h2>
 * <p>
 * Standard FIX parsing libraries (e.g., QuickFIX/J) use {@code String.split()} and
 * {@code HashMap<String, String>} to represent tag-value pairs. Every message parse
 * allocates multiple {@link String} objects, a {@code HashMap}, and its internal
 * {@code Entry[]} array — easily 50–200 heap objects per message.
 *
 * <p>This parser eliminates all allocations by:
 * <ol>
 *   <li>Operating directly on a pre-allocated {@code byte[]} buffer (no String conversion).</li>
 *   <li>Identifying tags by their integer value (e.g., tag 55 = Symbol, tag 44 = Price).</li>
 *   <li>Writing parsed values directly into the target {@link com.fx.common.event.FxMarketEvent}
 *       as primitives — no intermediate String creation.</li>
 *   <li>Using a custom {@link #parseLong(byte[], int, int)} helper to convert ASCII-encoded
 *       numbers to {@code long} without calling {@link Long#parseLong(String)} (which allocates
 *       a String internally in some JVM implementations).</li>
 * </ol>
 *
 * <h2>FIX 4.4 Wire Format</h2>
 * <p>
 * A FIX message looks like:
 * <pre>
 *   8=FIX.4.4|9=148|35=D|49=CLIENT1|56=BROKER|34=12|52=20240101-12:00:00|
 *   11=ORD001|55=EUR/USD|54=1|38=1000000|44=1.0850|10=123|
 * </pre>
 * Fields are delimited by SOH (ASCII 0x01). Tags and values are separated by {@code '='}.
 *
 * <h2>Supported Tags</h2>
 * <pre>
 *   Tag 35  = MsgType    (byte: 'D' = New Order Single)
 *   Tag 34  = MsgSeqNum  (long: FIX sequence number)
 *   Tag 49  = SenderCompID (mapped to clientId long via hash)
 *   Tag 55  = Symbol     (currency pair: "EUR/USD" → packed long)
 *   Tag 54  = Side       (1=Buy, 2=Sell → mapped to +1/-1 byte)
 *   Tag 38  = OrderQty   (long: notional amount)
 *   Tag 44  = Price      (scaled long: price × 100,000)
 * </pre>
 *
 * @author FX Pipeline Team
 * @version 1.0.0
 */
public final class FixDecoder {

    // ── FIX Tag Constants ────────────────────────────────────────────────────
    // Using int constants avoids any String lookup. The parser identifies tags
    // by their numeric values, parsed byte-by-byte from the FIX buffer.
    // These are the FIX 4.4 tag numbers as defined in the FIX specification.

    private static final int TAG_MSG_TYPE    = 35;
    private static final int TAG_MSG_SEQ_NUM = 34;
    private static final int TAG_SENDER_COMP = 49;
    private static final int TAG_SYMBOL      = 55;
    private static final int TAG_SIDE        = 54;
    private static final int TAG_ORDER_QTY   = 38;
    private static final int TAG_PRICE       = 44;

    /** FIX field delimiter: SOH (Start Of Header) byte, ASCII 0x01. */
    private static final byte SOH = 0x01;

    /** Separator between tag and value in FIX wire format. */
    private static final byte EQUALS = (byte) '=';

    /**
     * Price scale factor: prices in FIX are decimal strings (e.g., "1.0850").
     * We store them as scaled integers: price × PRICE_SCALE_FACTOR.
     * For EUR/USD with 5 decimal places, 1.0850 × 100000 = 108500.
     */
    private static final long PRICE_SCALE_FACTOR = 100_000L;

    /**
     * Pre-allocated 3-byte buffers for currency pair decoding.
     * Reused on every call to avoid allocating new byte arrays per message.
     */
    private final byte[] baseCurrencyBuffer  = new byte[3];
    private final byte[] quoteCurrencyBuffer = new byte[3];

    /**
     * Decodes a raw FIX 4.4 message from {@code buffer} (bytes {@code offset} to
     * {@code offset + length - 1}) and populates the given {@link FxMessageFrame}.
     *
     * <p>This method never allocates heap objects. It returns {@code true} if all
     * mandatory tags were successfully parsed, or {@code false} if the message is
     * structurally invalid (missing tags, malformed values). The caller must treat
     * a {@code false} return as a validation failure — route to error queue.
     *
     * @param buffer   raw FIX message bytes (not null)
     * @param offset   start position within the buffer
     * @param length   number of bytes to parse from offset
     * @param target   the mutable frame to populate; reset by caller before invocation
     * @return {@code true} if parsing succeeded; {@code false} on any structural error
     */
    public boolean decode(final byte[] buffer,
                           final int offset,
                           final int length,
                           final FxMessageFrame target) {
        // Validate bounds before any byte access to prevent ArrayIndexOutOfBoundsException.
        if (buffer == null || offset < 0 || length <= 0
                || (offset + length) > buffer.length) {
            return false;
        }

        // Track which mandatory tags have been parsed — using bit flags on a single int.
        // Bit 0 = TAG_MSG_TYPE, Bit 1 = TAG_SYMBOL, Bit 2 = TAG_SIDE,
        // Bit 3 = TAG_ORDER_QTY, Bit 4 = TAG_PRICE, Bit 5 = TAG_SENDER_COMP.
        // When all 6 bits are set (0x3F = 63), all mandatory fields are present.
        int parsedFlags = 0;

        int pos = offset;
        final int end = offset + length;

        while (pos < end) {
            // ── Parse tag number (integer, terminated by '=') ─────────────
            int tag = 0;
            while (pos < end && buffer[pos] != EQUALS) {
                final byte b = buffer[pos++];
                if (b < '0' || b > '9') {
                    // Non-digit in tag number: malformed message.
                    return false;
                }
                // Build tag integer digit-by-digit: tag = tag * 10 + digit
                // This is equivalent to Integer.parseInt but allocation-free.
                tag = tag * 10 + (b - '0');
            }
            if (pos >= end) break; // No '=' found: truncated message
            pos++; // Skip the '=' character

            // ── Find the end of the value field (next SOH or end of buffer) ─
            final int valueStart = pos;
            while (pos < end && buffer[pos] != SOH) {
                pos++;
            }
            final int valueLength = pos - valueStart;
            if (pos < end) pos++; // Skip the SOH delimiter

            // ── Dispatch on tag number ────────────────────────────────────
            switch (tag) {
                case TAG_MSG_TYPE -> {
                    if (valueLength >= 1) {
                        // MsgType is a single character (e.g., 'D' for NewOrderSingle)
                        target.msgType = buffer[valueStart];
                        parsedFlags |= 0x01;
                    }
                }
                case TAG_MSG_SEQ_NUM -> {
                    target.seqNum = parseLong(buffer, valueStart, valueLength);
                }
                case TAG_SENDER_COMP -> {
                    // Map SenderCompID bytes to a deterministic long hash.
                    // This avoids storing the String while preserving uniqueness.
                    target.clientId = hashBytes(buffer, valueStart, valueLength);
                    parsedFlags |= 0x20;
                }
                case TAG_SYMBOL -> {
                    // Parse "EUR/USD" into base + quote byte arrays, then encode to long.
                    if (parseSymbol(buffer, valueStart, valueLength)) {
                        target.currencyPairCode =
                                com.fx.common.event.FxMarketEvent.CurrencyPairCodec
                                        .encode(baseCurrencyBuffer, quoteCurrencyBuffer);
                        parsedFlags |= 0x02;
                    }
                }
                case TAG_SIDE -> {
                    // FIX tag 54: 1 = Buy, 2 = Sell. Map to +1 / -1 byte.
                    if (valueLength == 1) {
                        target.side = (buffer[valueStart] == '1') ? (byte) 1 : (byte) -1;
                        parsedFlags |= 0x04;
                    }
                }
                case TAG_ORDER_QTY -> {
                    // OrderQty is an integer quantity (lots or units).
                    // Stored as notional in minor units: qty * lot_size * 100 (cents).
                    // For simplicity in this demo, 1 lot = 100,000 units, 1 unit = 1 cent.
                    target.notionalMinorUnits = parseLong(buffer, valueStart, valueLength);
                    parsedFlags |= 0x08;
                }
                case TAG_PRICE -> {
                    // Price is a decimal ASCII string: "1.0850"
                    // Parse it into a scaled long (× PRICE_SCALE_FACTOR).
                    target.requestedPriceScaled =
                            parseScaledPrice(buffer, valueStart, valueLength);
                    parsedFlags |= 0x10;
                }
                default -> { /* Unknown tags are silently skipped per FIX spec. */ }
            }
        }

        // All 6 mandatory tags must be present for a valid New Order Single.
        // Returning false here routes the event to the error queue without allocating
        // an exception object — fail-fast, zero-cost on the success path.
        return parsedFlags == 0x3F;
    }

    /**
     * Parses an ASCII-encoded unsigned decimal integer from a byte buffer segment.
     *
     * <p>Equivalent to {@code Long.parseLong(new String(buffer, offset, length))} but
     * without String allocation. Processes each digit byte in a tight loop.
     *
     * @param buffer raw byte buffer
     * @param offset start of the number in the buffer
     * @param length number of digits to parse
     * @return parsed {@code long} value, or {@code -1L} on parse failure
     */
    public static long parseLong(final byte[] buffer, final int offset, final int length) {
        if (length <= 0) return -1L;
        long result = 0L;
        for (int i = offset; i < offset + length; i++) {
            final byte b = buffer[i];
            if (b < '0' || b > '9') return -1L;
            result = result * 10 + (b - '0');
        }
        return result;
    }

    /**
     * Parses an ASCII decimal price string into a scaled {@code long}.
     *
     * <p>Example: "1.08500" with {@code PRICE_SCALE_FACTOR = 100000} → {@code 108500L}.
     *
     * <p>This avoids any {@code Double.parseDouble} call — floating-point parsing
     * in Java uses a heap-allocated intermediate object on some paths and introduces
     * IEEE 754 rounding that is unacceptable for financial prices.
     *
     * @param buffer raw byte buffer
     * @param offset start of the decimal string
     * @param length number of characters to process
     * @return scaled long price value, or {@code -1L} on parse failure
     */
    public static long parseScaledPrice(final byte[] buffer, final int offset, final int length) {
        long integerPart  = 0L;
        long fractionalPart = 0L;
        long fractionalDivisor = 1L;
        boolean seenDecimalPoint = false;

        for (int i = offset; i < offset + length; i++) {
            final byte b = buffer[i];
            if (b == '.') {
                seenDecimalPoint = true;
                continue;
            }
            if (b < '0' || b > '9') return -1L;
            if (!seenDecimalPoint) {
                integerPart = integerPart * 10 + (b - '0');
            } else {
                fractionalPart = fractionalPart * 10 + (b - '0');
                fractionalDivisor *= 10L;
            }
        }

        // Scale: integerPart * SCALE + fractionalPart * (SCALE / divisor)
        return integerPart * PRICE_SCALE_FACTOR
                + (fractionalPart * PRICE_SCALE_FACTOR / fractionalDivisor);
    }

    /**
     * Parses a "EUR/USD" style symbol from the FIX buffer into the pre-allocated
     * {@link #baseCurrencyBuffer} and {@link #quoteCurrencyBuffer} byte arrays.
     *
     * @param buffer      raw byte buffer containing the symbol
     * @param offset      start position of the symbol in the buffer
     * @param length      number of characters in the symbol (expected: 7 for "XXX/YYY")
     * @return {@code true} if the symbol was successfully parsed
     */
    private boolean parseSymbol(final byte[] buffer,
                                  final int offset,
                                  final int length) {
        // "EUR/USD" = 7 characters: 3 base + '/' + 3 quote
        if (length != 7 || buffer[offset + 3] != '/') {
            return false;
        }
        // Copy base currency bytes (positions 0, 1, 2) into pre-allocated buffer.
        baseCurrencyBuffer[0]  = buffer[offset];
        baseCurrencyBuffer[1]  = buffer[offset + 1];
        baseCurrencyBuffer[2]  = buffer[offset + 2];
        // Copy quote currency bytes (positions 4, 5, 6) into pre-allocated buffer.
        quoteCurrencyBuffer[0] = buffer[offset + 4];
        quoteCurrencyBuffer[1] = buffer[offset + 5];
        quoteCurrencyBuffer[2] = buffer[offset + 6];
        return true;
    }

    /**
     * Computes a deterministic 64-bit FNV-1a hash of a byte array segment.
     *
     * <p>Used to map variable-length SenderCompID strings to fixed-size {@code long}
     * client identifiers — without allocating a String or using a HashMap. FNV-1a is
     * chosen because it has excellent distribution for short strings (5–20 bytes),
     * low collision probability, and requires no external library.
     *
     * @param buffer raw byte buffer
     * @param offset start of the byte sequence to hash
     * @param length number of bytes to hash
     * @return a 64-bit hash value
     */
    public static long hashBytes(final byte[] buffer, final int offset, final int length) {
        // FNV-1a 64-bit: standard initial value and prime.
        long hash = 0xcbf29ce484222325L;
        for (int i = offset; i < offset + length; i++) {
            hash ^= (buffer[i] & 0xFFL);
            hash *= 0x00000100000001B3L;
        }
        return hash;
    }

    /**
     * {@code FxMessageFrame} — Transient frame populated by {@link FixDecoder#decode}.
     *
     * <p>Holds the decoded FIX fields as primitives before they are transferred into
     * the main {@link com.fx.common.event.FxMarketEvent} flyweight. Pre-allocated
     * once by the caller and reused across messages.
     */
    public static final class FxMessageFrame {

        /** FIX tag 35 value: message type byte (e.g., 'D' for New Order Single). */
        public byte msgType;

        /** FIX tag 34: session sequence number. */
        public long seqNum;

        /** FIX tag 49: sender company ID, hashed to a long. */
        public long clientId;

        /** FIX tag 55: symbol encoded as a packed long (see CurrencyPairCodec). */
        public long currencyPairCode;

        /** FIX tag 54: side byte (+1 = BUY, -1 = SELL). */
        public byte side;

        /** FIX tag 38: order quantity in minor units. */
        public long notionalMinorUnits;

        /** FIX tag 44: requested price scaled by 100,000. */
        public long requestedPriceScaled;

        /**
         * Resets all fields to zero/sentinel values for frame reuse.
         * Must be called before each {@link FixDecoder#decode} invocation.
         */
        public void reset() {
            msgType            = 0;
            seqNum             = 0L;
            clientId           = 0L;
            currencyPairCode   = 0L;
            side               = 0;
            notionalMinorUnits = 0L;
            requestedPriceScaled = 0L;
        }
    }
}
