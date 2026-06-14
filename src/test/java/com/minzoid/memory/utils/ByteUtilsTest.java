package com.minzoid.memory.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ByteUtilsTest {

    @Test
    void testToByte() {
        assertEquals((byte) 0x12, ByteUtils.toByte(new byte[]{0x12}));
        assertThrows(IllegalArgumentException.class, () -> ByteUtils.toByte(new byte[]{}));
    }

    @Test
    void testToShort() {
        // Little endian: 0x34 0x12 -> 0x1234
        assertEquals((short) 0x1234, ByteUtils.toShort(new byte[]{0x34, 0x12}));
    }

    @Test
    void testToInt() {
        // Little endian: 0x78 0x56 0x34 0x12 -> 0x12345678
        assertEquals(0x12345678, ByteUtils.toInt(new byte[]{0x78, 0x56, 0x34, 0x12}));
    }

    @Test
    void testToLong() {
        assertEquals(0x1122334455667788L, ByteUtils.toLong(new byte[]{(byte)0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11}));
    }

    @Test
    void testToFloat() {
        assertEquals(1.0f, ByteUtils.toFloat(new byte[]{0x00, 0x00, (byte)0x80, 0x3F}));
    }

    @Test
    void testToDouble() {
        assertEquals(1.0d, ByteUtils.toDouble(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xF0, 0x3F}));
    }

    @Test
    void testFromByte() {
        assertArrayEquals(new byte[]{0x12}, ByteUtils.fromByte((byte) 0x12));
    }

    @Test
    void testFromShort() {
        assertArrayEquals(new byte[]{0x34, 0x12}, ByteUtils.fromShort((short) 0x1234));
    }

    @Test
    void testFromInt() {
        assertArrayEquals(new byte[]{0x78, 0x56, 0x34, 0x12}, ByteUtils.fromInt(0x12345678));
    }

    @Test
    void testFromLong() {
        assertArrayEquals(new byte[]{(byte)0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11}, ByteUtils.fromLong(0x1122334455667788L));
    }

    @Test
    void testFromFloat() {
        assertArrayEquals(new byte[]{0x00, 0x00, (byte)0x80, 0x3F}, ByteUtils.fromFloat(1.0f));
    }

    @Test
    void testFromDouble() {
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xF0, 0x3F}, ByteUtils.fromDouble(1.0d));
    }
}

