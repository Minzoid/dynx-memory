package com.minzoid.memory.pointer;

import com.minzoid.memory.modules.Module;
import com.minzoid.memory.modules.ModuleManager;
import com.minzoid.memory.nativeapi.NativeMemoryIO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddressResolverTest {

    // Simple mocks for testing AddressResolver logic without hitting JNA
    
    private static class MockModuleManager extends ModuleManager {
        public MockModuleManager() {
            super(null);
        }
        
        @Override
        public Module getModule(HANDLE handle, String moduleName) {
            if ("Game.dll".equalsIgnoreCase(moduleName)) {
                return new Module("Game.dll", 0x10000000L, 0x10000);
            }
            throw new com.minzoid.memory.exceptions.MemoryException("Module not found");
        }
    }
    
    private static class MockNativeMemoryIO extends NativeMemoryIO {
        public MockNativeMemoryIO() {
            super(null);
        }
        
        @Override
        public byte[] readBytes(HANDLE handle, long address, int length) {
            // Mock memory layout for pointer chain:
            // [0x10001234] -> 0x20000000
            // [0x20000010] -> 0x30000000
            
            if (length != 8) {
                throw new IllegalArgumentException("Test mock only supports reading 8 bytes (pointers)");
            }
            
            if (address == 0x10001234L) {
                return com.minzoid.memory.utils.ByteUtils.fromLong(0x20000000L);
            } else if (address == 0x20000010L) {
                return com.minzoid.memory.utils.ByteUtils.fromLong(0x30000000L);
            }
            throw new com.minzoid.memory.exceptions.MemoryReadException("Unmapped test address");
        }
    }

    private AddressResolver resolver;
    private HANDLE dummyHandle;

    @BeforeEach
    void setUp() {
        resolver = new AddressResolver(new MockModuleManager(), new MockNativeMemoryIO());
        dummyHandle = new HANDLE();
    }

    @Test
    void testResolveRawHex() {
        assertEquals(0x7FF000001234L, resolver.resolve(dummyHandle, "7FF000001234"));
        assertEquals(0x7FF000001234L, resolver.resolve(dummyHandle, "0x7FF000001234"));
    }

    @Test
    void testResolveModuleOffset() {
        assertEquals(0x1000ABCDL, resolver.resolve(dummyHandle, "Game.dll+ABCD"));
    }
    
    @Test
    void testResolvePointerChain() {
        // base = Game.dll+1234 -> 0x10001234
        // read 0x10001234 -> 0x20000000
        // add 0x10 -> 0x20000010
        // read 0x20000010 -> 0x30000000
        // add 0x20 -> 0x30000020 (final address, no read)
        assertEquals(0x30000020L, resolver.resolve(dummyHandle, "Game.dll+1234,10,20"));
    }
    
    @Test
    void testResolvePointerChainRawBase() {
        // base = 0x10001234
        // read 0x10001234 -> 0x20000000
        // add 0x10 -> 0x20000010 (final address)
        assertEquals(0x20000010L, resolver.resolve(dummyHandle, "10001234,10"));
    }
}

