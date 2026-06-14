package com.minzoid.memory.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HexUtilsTest {

    @Test
    void testToHexString() {
        assertEquals("89AB00", HexUtils.toHexString(new byte[]{(byte)0x89, (byte)0xAB, 0x00}));
        assertEquals("", HexUtils.toHexString(new byte[]{}));
    }

    @Test
    void testFromHexString() {
        assertArrayEquals(new byte[]{(byte)0x89, (byte)0xAB, 0x00}, HexUtils.fromHexString("89AB00"));
        assertArrayEquals(new byte[]{(byte)0x89, (byte)0xAB, 0x00}, HexUtils.fromHexString("89 AB 00"));
        assertArrayEquals(new byte[]{(byte)0x89, (byte)0xAB, 0x00}, HexUtils.fromHexString(" 89  aB  00 "));
        assertArrayEquals(new byte[]{}, HexUtils.fromHexString(""));
        
        assertThrows(IllegalArgumentException.class, () -> HexUtils.fromHexString("89A")); // odd chars
    }

    @Test
    void testParseAddress() {
        assertEquals(0x7FF000001234L, HexUtils.parseAddress("7FF000001234"));
        assertEquals(0x7FF000001234L, HexUtils.parseAddress("0x7FF000001234"));
        assertEquals(0x7FF000001234L, HexUtils.parseAddress("0X7FF000001234"));
        assertEquals(0xDEADL, HexUtils.parseAddress("DEAD"));
        
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parseAddress(""));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.parseAddress("NOTHEX"));
    }

    @Test
    void testFormatAddress() {
        assertEquals("0x00007FF000001234", HexUtils.formatAddress(0x7FF000001234L));
        assertEquals("0x000000000000DEAD", HexUtils.formatAddress(0xDEADL));
    }

    @Test
    void testIsHexAddress() {
        assertTrue(HexUtils.isHexAddress("7FF000001234"));
        assertTrue(HexUtils.isHexAddress("0x7FF000001234"));
        assertTrue(HexUtils.isHexAddress("DEAD"));
        assertFalse(HexUtils.isHexAddress(""));
        assertFalse(HexUtils.isHexAddress("NOTHEX"));
    }
}

