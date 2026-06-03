package com.dynx.examples;

import com.dynx.memory.Memory;
import com.dynx.memory.exceptions.DynxMemoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating basic process attachment, module base address lookup,
 * and primitive memory reading.
 */
public class BasicReadExample {
    private static final Logger log = LoggerFactory.getLogger(BasicReadExample.class);

    public static void main(String[] args) {
        // We use try-with-resources so the process handle is closed automatically.
        try (Memory memory = new Memory()) {
            log.info("Attaching to notepad.exe...");
            memory.openProcess("notepad.exe");

            // Look up module base
            long base = memory.getModule("notepad.exe").getBaseAddress();
            log.info("Notepad base address: 0x{}", Long.toHexString(base));

            // Read MZ header magic bytes (should be 0x5A4D or 'MZ')
            short mz = memory.readShort(base);
            log.info("MZ header (short): 0x{}", Integer.toHexString(mz & 0xFFFF));

        } catch (DynxMemoryException e) {
            log.error("Memory operation failed", e);
        }
    }
}
