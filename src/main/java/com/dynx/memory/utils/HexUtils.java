package com.dynx.memory.utils;

/**
 * Utility class for converting between hexadecimal strings and byte arrays,
 * as well as parsing hex address expressions used throughout the library.
 *
 * <p>All methods are stateless and thread-safe. This class cannot be
 * instantiated.
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class HexUtils {

    /** Hex character lookup table for fast encoding. */
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /** Prevent instantiation. */
    private HexUtils() {
        throw new UnsupportedOperationException("HexUtils is a utility class");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Byte array ↔ Hex string
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts a byte array to an uppercase hexadecimal string.
     * Each byte is represented by exactly two hex digits.
     *
     * <p>Example: {@code [0x89, 0xAB, 0x00]} → {@code "89AB00"}
     *
     * @param bytes the byte array to encode (must not be null)
     * @return uppercase hex string; empty string if {@code bytes} is empty
     * @throws IllegalArgumentException if {@code bytes} is null
     */
    public static String toHexString(final byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("byte array must not be null");
        }
        final char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int v = bytes[i] & 0xFF;
            result[i * 2]     = HEX_CHARS[v >>> 4];
            result[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(result);
    }

    /**
     * Converts an uppercase or lowercase hexadecimal string to a byte array.
     * Spaces in the input are ignored, allowing patterns like {@code "89 AB 00"}.
     *
     * <p>Example: {@code "89 AB 00"} → {@code [0x89, 0xAB, 0x00]}
     *
     * @param hex the hex string to decode (must not be null; length after stripping spaces must be even)
     * @return decoded byte array
     * @throws IllegalArgumentException if {@code hex} is null or has an odd number of non-space characters
     */
    public static byte[] fromHexString(final String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex string must not be null");
        }
        final String stripped = hex.replace(" ", "");
        if (stripped.isEmpty()) {
            return new byte[0];
        }
        if (stripped.length() % 2 != 0) {
            throw new IllegalArgumentException(
                    "hex string must have an even number of characters, got: " + stripped.length());
        }
        final byte[] result = new byte[stripped.length() / 2];
        for (int i = 0; i < result.length; i++) {
            final int high = hexCharToInt(stripped.charAt(i * 2));
            final int low  = hexCharToInt(stripped.charAt(i * 2 + 1));
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Address parsing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses a hexadecimal address string (with or without a {@code 0x} prefix)
     * into a {@code long} value.
     *
     * <p>Examples:
     * <pre>
     *   parseAddress("0x7FF000001234")  → 0x7FF000001234L
     *   parseAddress("7FF000001234")    → 0x7FF000001234L
     *   parseAddress("DEAD")            → 0xDEADL
     * </pre>
     *
     * @param address the address string to parse (must not be null or blank)
     * @return the parsed address as an unsigned {@code long}
     * @throws IllegalArgumentException if the string is null, blank, or not a valid hex number
     */
    public static long parseAddress(final String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address string must not be null or blank");
        }
        final String clean = address.trim().toUpperCase();
        final String stripped = clean.startsWith("0X") ? clean.substring(2) : clean;
        try {
            return Long.parseUnsignedLong(stripped, 16);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("invalid hex address: '" + address + "'", e);
        }
    }

    /**
     * Formats a memory address as a zero-padded 16-character uppercase hex string
     * prefixed with {@code 0x}.
     *
     * <p>Example: {@code 0x7FF000001234L} → {@code "0x00007FF000001234"}
     *
     * @param address the address value
     * @return formatted address string
     */
    public static String formatAddress(final long address) {
        return "0x" + String.format("%016X", address);
    }

    /**
     * Returns {@code true} if the given token is a valid hexadecimal string
     * (optionally prefixed with {@code 0x} or {@code 0X}).
     *
     * @param token the string to test
     * @return {@code true} if parseable as a hex number
     */
    public static boolean isHexAddress(final String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        final String clean = token.trim().toUpperCase();
        final String stripped = clean.startsWith("0X") ? clean.substring(2) : clean;
        if (stripped.isEmpty()) {
            return false;
        }
        for (final char c : stripped.toCharArray()) {
            if (!isHexDigit(c)) {
                return false;
            }
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static int hexCharToInt(final char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        throw new IllegalArgumentException("invalid hex character: '" + c + "'");
    }

    private static boolean isHexDigit(final char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
    }
}
