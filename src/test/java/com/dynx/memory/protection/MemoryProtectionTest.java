package com.dynx.memory.protection;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemoryProtectionTest {

    @Test
    void testGetNativeValue() {
        assertEquals(0x02, MemoryProtection.READ_ONLY.getNativeValue());
        assertEquals(0x04, MemoryProtection.READ_WRITE.getNativeValue());
        assertEquals(0x40, MemoryProtection.EXECUTE_READ_WRITE.getNativeValue());
    }

    @Test
    void testFromNativeValue() {
        assertEquals(MemoryProtection.READ_ONLY, MemoryProtection.fromNativeValue(0x02));
        assertEquals(MemoryProtection.READ_WRITE, MemoryProtection.fromNativeValue(0x04));
        
        assertThrows(IllegalArgumentException.class, () -> MemoryProtection.fromNativeValue(0x999));
    }
}
