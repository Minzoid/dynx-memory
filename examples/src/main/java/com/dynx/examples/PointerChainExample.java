package com.dynx.examples;

import com.dynx.memory.Memory;
import com.dynx.memory.exceptions.DynxMemoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example demonstrating AddressResolver with a multi-level pointer chain.
 */
public class PointerChainExample {
    private static final Logger log = LoggerFactory.getLogger(PointerChainExample.class);

    public static void main(String[] args) {
        try (Memory memory = new Memory()) {
            memory.openProcess("notepad.exe");

            // Resolve a pointer chain:
            // 1. Get base address of notepad.exe + offset 0x1000
            // 2. Read pointer there, add 0x20
            // 3. Read pointer there, add 0x30
            // This will likely throw an exception in notepad because it's a dummy path,
            // but demonstrates the syntax.
            String expression = "notepad.exe+1000,20,30";
            
            log.info("Resolving expression: {}", expression);
            try {
                long address = memory.resolve(expression);
                log.info("Resolved address: 0x{}", Long.toHexString(address));
                
                // Read int from the resolved address
                int value = memory.readInt(address);
                log.info("Value at address: {}", value);
                
            } catch (DynxMemoryException e) {
                log.info("Expected failure on dummy pointer path: {}", e.getMessage());
            }

        } catch (DynxMemoryException e) {
            log.error("Failed", e);
        }
    }
}
