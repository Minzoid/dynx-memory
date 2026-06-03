package com.dynx.examples;

import com.dynx.memory.Memory;
import com.dynx.memory.exceptions.DynxMemoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Example demonstrating how to scan a process for a specific byte pattern
 * using the parallel AoB scanner.
 */
public class AoBScanExample {
    private static final Logger log = LoggerFactory.getLogger(AoBScanExample.class);

    public static void main(String[] args) {
        try (Memory memory = new Memory()) {
            memory.openProcess("notepad.exe");

            // Define pattern with wildcards
            String pattern = "4D 5A ?? ?? ?? ?? 00 00"; 
            
            log.info("Scanning for pattern: {}", pattern);
            long start = System.currentTimeMillis();
            
            List<Long> results = memory.scan(pattern);
            
            long duration = System.currentTimeMillis() - start;
            log.info("Scan completed in {} ms. Found {} matches.", duration, results.size());
            
            for (int i = 0; i < Math.min(results.size(), 10); i++) {
                log.info("Match {}: 0x{}", i, Long.toHexString(results.get(i)));
            }
            if (results.size() > 10) {
                log.info("... and {} more", results.size() - 10);
            }

        } catch (DynxMemoryException e) {
            log.error("Scan failed", e);
        }
    }
}
