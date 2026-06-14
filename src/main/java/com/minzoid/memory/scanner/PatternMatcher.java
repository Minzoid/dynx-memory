package com.minzoid.memory.scanner;

import com.minzoid.memory.exceptions.ScanException;
import com.minzoid.memory.utils.HexUtils;

/**
 * Stateless byte-pattern matcher supporting exact byte values and single-byte
 * wildcards ({@code ??} or {@code ?}).
 *
 * <p>A pattern is a space-separated string of hex bytes or wildcard tokens:
 * <pre>
 *   "89 AB ?? FF"   — matches 0x89, any byte, any byte, 0xFF  (wait — ?? = 1 wildcard)
 *   "A? ?? 00"      — partial-nibble wildcard is treated as full wildcard
 *   "DE AD BE EF"   — exact 4-byte match
 * </pre>
 *
 * <p>Instances are stateless and thread-safe; they hold no mutable state.
 * The compiled representation (parallel {@code pattern} / {@code wildcards}
 * arrays) is returned by {@link #compile(String)} and passed back to
 * {@link #matches}.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PatternMatcher {

    /** Wildcard token accepted in pattern strings. */
    private static final String WILDCARD = "??";

    /** Single-character wildcard prefix (partial nibble wildcard). */
    private static final char   WILDCARD_CHAR = '?';

    /**
     * Prevent instantiation; use static factory {@link #compile(String)}.
     */
    private PatternMatcher() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Compiled pattern
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable compiled representation of a byte pattern.
     *
     * <p>Instances are produced by {@link PatternMatcher#compile(String)} and
     * consumed by {@link PatternMatcher#matches(byte[], int, CompiledPattern)}.
     */
    public static final class CompiledPattern {

        /** Byte values to match; ignored at wildcard positions. */
        private final byte[] pattern;

        /** {@code true} at positions that are wildcards (any byte matches). */
        private final boolean[] wildcards;

        private CompiledPattern(final byte[] pattern, final boolean[] wildcards) {
            this.pattern   = pattern;
            this.wildcards = wildcards;
        }

        /**
         * Returns the number of bytes in the pattern.
         *
         * @return pattern length
         */
        public int length() {
            return pattern.length;
        }

        /**
         * Returns the pattern bytes. Wildcard positions hold {@code 0x00}.
         *
         * @return pattern byte array (do not modify)
         */
        public byte[] getPattern() {
            return pattern.clone();
        }

        /**
         * Returns the wildcard mask. {@code true} at index {@code i} means
         * any byte matches at position {@code i}.
         *
         * @return wildcard mask (do not modify)
         */
        public boolean[] getWildcards() {
            return wildcards.clone();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Pattern[");
            for (int i = 0; i < pattern.length; i++) {
                if (i > 0) sb.append(' ');
                sb.append(wildcards[i] ? "??" : String.format("%02X", pattern[i] & 0xFF));
            }
            return sb.append(']').toString();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Compilation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses an AoB pattern string into a {@link CompiledPattern} ready for
     * fast matching.
     *
     * <p>Tokens must be separated by whitespace. Each token is either:
     * <ul>
     *   <li>A two-character hex byte: {@code "AB"}, {@code "ff"}, etc.</li>
     *   <li>A wildcard: {@code "??"} or any token containing {@code '?'}.</li>
     * </ul>
     *
     * @param patternStr the pattern string (e.g. {@code "89 AB ?? FF"})
     * @return compiled pattern ready for {@link #matches}
     * @throws ScanException if the pattern is null, empty, or contains invalid tokens
     */
    public static CompiledPattern compile(final String patternStr) {
        if (patternStr == null || patternStr.isBlank()) {
            throw new ScanException("Pattern string must not be null or empty");
        }

        final String[] tokens = patternStr.trim().split("\\s+");
        if (tokens.length == 0) {
            throw new ScanException("Pattern string produced no tokens: '" + patternStr + "'");
        }

        final byte[]    pattern   = new byte[tokens.length];
        final boolean[] wildcards = new boolean[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            final String token = tokens[i].trim();
            if (token.isEmpty()) {
                throw new ScanException("Empty token at index " + i + " in pattern: '" + patternStr + "'");
            }

            // Any token containing '?' is treated as a wildcard
            if (token.indexOf(WILDCARD_CHAR) >= 0) {
                wildcards[i] = true;
                pattern[i]   = 0x00;
            } else {
                wildcards[i] = false;
                try {
                    final byte[] decoded = HexUtils.fromHexString(token);
                    if (decoded.length != 1) {
                        throw new ScanException(
                                "Token '" + token + "' at index " + i
                                        + " must be exactly one byte (2 hex chars)");
                    }
                    pattern[i] = decoded[0];
                } catch (final IllegalArgumentException e) {
                    throw new ScanException(
                            "Invalid hex token '" + token + "' at index " + i
                                    + " in pattern: '" + patternStr + "'", e);
                }
            }
        }

        return new CompiledPattern(pattern, wildcards);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Matching
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tests whether the bytes in {@code buffer} starting at {@code offset}
     * match the given {@link CompiledPattern}.
     *
     * <p>This is the hot path called millions of times during an AoB scan;
     * it is designed to be branch-predictor-friendly and allocation-free.
     *
     * @param buffer  the byte buffer to search within
     * @param offset  the starting index in {@code buffer} to compare
     * @param cp      the compiled pattern to match against
     * @return {@code true} if all non-wildcard bytes match
     * @throws IllegalArgumentException if {@code buffer} is null, {@code offset}
     *                                  is negative, or the pattern extends beyond
     *                                  the buffer
     */
    public static boolean matches(final byte[] buffer, final int offset,
                                  final CompiledPattern cp) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        final int len = cp.pattern.length;
        if (offset + len > buffer.length) {
            return false; // pattern extends beyond buffer
        }

        final byte[]    pattern   = cp.pattern;
        final boolean[] wildcards = cp.wildcards;

        for (int i = 0; i < len; i++) {
            if (!wildcards[i] && buffer[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convenience overload that compiles the pattern string and tests a single
     * position. Prefer pre-compiling with {@link #compile(String)} for repeated
     * calls.
     *
     * @param buffer     the byte buffer to search within
     * @param offset     the starting index in {@code buffer} to compare
     * @param patternStr the raw pattern string
     * @return {@code true} if the pattern matches at {@code offset}
     */
    public static boolean matches(final byte[] buffer, final int offset,
                                  final String patternStr) {
        return matches(buffer, offset, compile(patternStr));
    }
}

