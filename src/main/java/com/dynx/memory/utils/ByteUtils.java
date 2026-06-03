package com.dynx.memory.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for converting between primitive Java types and little-endian
 * byte arrays, as used by x86/x64 Windows processes.
 *
 * <p>All methods are stateless and thread-safe. This class cannot be
 * instantiated.
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ByteUtils {

    /** Prevent instantiation. */
    private ByteUtils() {
        throw new UnsupportedOperationException("ByteUtils is a utility class");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // From bytes → primitives (little-endian)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts the first byte of the array to a Java {@code byte}.
     *
     * @param bytes source array (must have length &ge; 1)
     * @return the byte at index 0
     * @throws IllegalArgumentException if the array is null or too short
     */
    public static byte toByte(final byte[] bytes) {
        requireLength(bytes, 1);
        return bytes[0];
    }

    /**
     * Reads a little-endian {@code short} from the first 2 bytes of the array.
     *
     * @param bytes source array (must have length &ge; 2)
     * @return the decoded short value
     * @throws IllegalArgumentException if the array is null or too short
     */
    public static short toShort(final byte[] bytes) {
        requireLength(bytes, 2);
        return ByteBuffer.wrap(bytes, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    /**
     * Reads a little-endian {@code int} from the first 4 bytes of the array.
     *
     * @param bytes source array (must have length &ge; 4)
     * @return the decoded int value
     * @throws IllegalArgumentException if the array is null or too short
     */
    public static int toInt(final byte[] bytes) {
        requireLength(bytes, 4);
        return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads a little-endian {@code long} from the first 8 bytes of the array.
     *
     * @param bytes source array (must have length &ge; 8)
     * @return the decoded long value
     * @throws IllegalArgumentException if the array is null or too short
     */
    public static long toLong(final byte[] bytes) {
        requireLength(bytes, 8);
        return ByteBuffer.wrap(bytes, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /**
     * Reads a little-endian IEEE-754 {@code float} from the first 4 bytes of the array.
     *
     * @param bytes source array (must have length &ge; 4)
     * @return the decoded float value
     * @throws IllegalArgumentException if the array is null or too short
     */
    public static float toFloat(final byte[] bytes) {
        requireLength(bytes, 4);
        return ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    /**
     * Reads a little-endian IEEE-754 {@code double} from the first 8 bytes of the array.
     *
     * @param bytes source array (must have length &ge; 8)
     * @return the decoded double value
     * @throws IllegalArgumentException if the array is null or too short
     */
    public static double toDouble(final byte[] bytes) {
        requireLength(bytes, 8);
        return ByteBuffer.wrap(bytes, 0, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // From primitives → bytes (little-endian)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encodes a {@code byte} value as a single-element byte array.
     *
     * @param value the value to encode
     * @return a 1-byte array
     */
    public static byte[] fromByte(final byte value) {
        return new byte[]{value};
    }

    /**
     * Encodes a {@code short} value as a 2-byte little-endian array.
     *
     * @param value the value to encode
     * @return a 2-byte array in little-endian order
     */
    public static byte[] fromShort(final short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }

    /**
     * Encodes an {@code int} value as a 4-byte little-endian array.
     *
     * @param value the value to encode
     * @return a 4-byte array in little-endian order
     */
    public static byte[] fromInt(final int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    /**
     * Encodes a {@code long} value as an 8-byte little-endian array.
     *
     * @param value the value to encode
     * @return an 8-byte array in little-endian order
     */
    public static byte[] fromLong(final long value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    /**
     * Encodes a {@code float} value as a 4-byte little-endian IEEE-754 array.
     *
     * @param value the value to encode
     * @return a 4-byte array in little-endian order
     */
    public static byte[] fromFloat(final float value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
    }

    /**
     * Encodes a {@code double} value as an 8-byte little-endian IEEE-754 array.
     *
     * @param value the value to encode
     * @return an 8-byte array in little-endian order
     */
    public static byte[] fromDouble(final double value) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Asserts that {@code bytes} is non-null and has at least {@code minLen} elements.
     *
     * @param bytes  the array to validate
     * @param minLen the minimum required length
     * @throws IllegalArgumentException if the array is null or too short
     */
    private static void requireLength(final byte[] bytes, final int minLen) {
        if (bytes == null) {
            throw new IllegalArgumentException("byte array must not be null");
        }
        if (bytes.length < minLen) {
            throw new IllegalArgumentException(
                    "byte array length " + bytes.length + " is less than required " + minLen);
        }
    }
}
