package com.dynx.memory.modules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModuleTest {

    @Test
    void testModuleCreation() {
        Module mod = new Module("Game.dll", 0x10000000L, 0x10000);
        assertEquals("Game.dll", mod.getName());
        assertEquals(0x10000000L, mod.getBaseAddress());
        assertEquals(0x10000, mod.getSize());
    }

    @Test
    void testEqualsAndHashCode() {
        Module mod1 = new Module("Game.dll", 0x10000000L, 0x10000);
        Module mod2 = new Module("game.dll", 0x10000000L, 0x10000); // case insensitive
        Module mod3 = new Module("Other.dll", 0x10000000L, 0x10000);
        
        assertEquals(mod1, mod2);
        assertEquals(mod1.hashCode(), mod2.hashCode());
        assertNotEquals(mod1, mod3);
    }
}
