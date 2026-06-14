package com.minzoid.memory.scanner;

import com.minzoid.memory.exceptions.ScanException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PatternMatcherTest {

    @Test
    void testCompileValidPatterns() {
        PatternMatcher.CompiledPattern cp = PatternMatcher.compile("89 AB ?? FF");
        assertEquals(4, cp.length());
        assertArrayEquals(new byte[]{(byte)0x89, (byte)0xAB, 0x00, (byte)0xFF}, cp.getPattern());
        assertArrayEquals(new boolean[]{false, false, true, false}, cp.getWildcards());
        
        // Single ? wildcard
        cp = PatternMatcher.compile("A? ?? 00");
        assertEquals(3, cp.length());
        assertArrayEquals(new byte[]{0x00, 0x00, 0x00}, cp.getPattern());
        assertArrayEquals(new boolean[]{true, true, false}, cp.getWildcards());
    }

    @Test
    void testCompileInvalidPatterns() {
        assertThrows(ScanException.class, () -> PatternMatcher.compile(""));
        assertThrows(ScanException.class, () -> PatternMatcher.compile("89 GZ"));
        assertThrows(ScanException.class, () -> PatternMatcher.compile("89 A")); // single char token
    }

    @Test
    void testMatches() {
        byte[] buffer = new byte[]{0x11, 0x22, (byte)0x89, (byte)0xAB, 0x44, (byte)0xFF, 0x55};
        
        assertTrue(PatternMatcher.matches(buffer, 2, "89 AB ?? FF"));
        assertFalse(PatternMatcher.matches(buffer, 1, "89 AB ?? FF")); // wrong offset
        assertFalse(PatternMatcher.matches(buffer, 2, "89 AB 00 FF")); // 0x44 != 0x00
        
        // Test out of bounds
        assertFalse(PatternMatcher.matches(buffer, 4, "89 AB ?? FF")); // not enough bytes remaining
    }
}

