package com.dynx.memory.pointer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dynx.memory.modules.Module;
import com.dynx.memory.modules.ModuleManager;
import com.dynx.memory.nativeapi.NativeMemoryIO;
import com.dynx.memory.utils.ByteUtils;
import com.dynx.memory.utils.HexUtils;
import com.dynx.memory.exceptions.InvalidAddressException;
import com.sun.jna.platform.win32.WinNT.HANDLE;

/**
 * Resolves address expressions into concrete virtual memory addresses for
 * the target process.
 *
 * <p>Supported expression formats:
 * <pre>
 *   "7FF000001234"              → raw hex address
 *   "0x7FF000001234"            → raw hex address with prefix
 *   "Game.dll+1234"             → module base + hex offset
 *   "Game.dll+1234,10,20,30"    → pointer chain: resolve base+offset, then
 *                                   dereference pointer + each subsequent offset
 * </pre>
 *
 * <p>Pointer chains are resolved by reading the pointer value at each
 * intermediate address, then adding the next offset. The final resolved
 * address is returned without an additional dereference.
 *
 * <p>Thread-safe: yes — stateless; all mutable state is passed via parameters.
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AddressResolver {

    private static final Logger log = LoggerFactory.getLogger(AddressResolver.class);

    /** Separator between module+offset and pointer chain offsets. */
    private static final String CHAIN_SEPARATOR = ",";

    /** Separator between module name and offset within the base expression. */
    private static final String OFFSET_SEPARATOR = "+";

    private final ModuleManager moduleManager;
    private final NativeMemoryIO memoryIO;

    /**
     * Constructs an {@code AddressResolver} with the given dependencies.
     *
     * @param moduleManager the module manager for resolving module base addresses
     * @param memoryIO      the native I/O wrapper for pointer dereferences
     */
    public AddressResolver(final ModuleManager moduleManager, final NativeMemoryIO memoryIO) {
        this.moduleManager = moduleManager;
        this.memoryIO      = memoryIO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the given address expression to a virtual memory address in the
     * target process.
     *
     * <p>Examples:
     * <pre>
     *   resolve(handle, "7FF000001234")             → 0x7FF000001234
     *   resolve(handle, "Game.dll+ABCD")            → Game.dll base + 0xABCD
     *   resolve(handle, "Game.dll+1234,10,20,30")   → pointer chain
     * </pre>
     *
     * @param handle     open process handle
     * @param expression the address expression to resolve
     * @return the resolved virtual address
     * @throws InvalidAddressException if the expression is invalid or resolution fails
     * @throws IllegalArgumentException if {@code expression} is null or blank
     */
    public long resolve(final HANDLE handle, final String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("address expression must not be null or blank");
        }

        final String trimmed = expression.trim();
        log.debug("Resolving expression: '{}'", trimmed);

        // Split on commas — first token is the base expression; remainder are chain offsets
        final String[] parts  = trimmed.split(CHAIN_SEPARATOR, -1);
        final String   base   = parts[0].trim();
        final boolean  hasChain = parts.length > 1;

        // Resolve the base address
        final long baseAddress = resolveBase(handle, base);
        log.debug("Base address resolved: 0x{}", Long.toHexString(baseAddress));

        if (!hasChain) {
            return baseAddress;
        }

        // Walk the pointer chain
        return walkChain(handle, baseAddress, parts, 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Base resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the base part of an expression (before any comma separators).
     *
     * <p>Handles:
     * <ul>
     *   <li>Raw hex address: {@code "7FF000001234"}</li>
     *   <li>Module + offset: {@code "Game.dll+ABCD"}</li>
     * </ul>
     *
     * @param handle handle to the target process
     * @param base   the base expression token
     * @return resolved base address
     * @throws InvalidAddressException on failure
     */
    private long resolveBase(final HANDLE handle, final String base) {
        // Check if this is a module+offset expression
        final int plusIndex = base.indexOf(OFFSET_SEPARATOR);
        if (plusIndex > 0) {
            final String moduleName = base.substring(0, plusIndex).trim();
            final String offsetStr  = base.substring(plusIndex + 1).trim();

            final long offset;
            try {
                offset = HexUtils.parseAddress(offsetStr);
            } catch (final IllegalArgumentException e) {
                throw new InvalidAddressException(
                        "Invalid offset in expression '" + base + "': " + e.getMessage(), e);
            }

            final Module module = moduleManager.getModule(handle, moduleName);
            final long   resolved = module.getBaseAddress() + offset;
            log.debug("Module '{}' base=0x{} + offset=0x{} = 0x{}",
                    moduleName,
                    Long.toHexString(module.getBaseAddress()),
                    Long.toHexString(offset),
                    Long.toHexString(resolved));
            return resolved;
        }

        // Raw hex address
        try {
            return HexUtils.parseAddress(base);
        } catch (final IllegalArgumentException e) {
            throw new InvalidAddressException(
                    "Cannot parse address expression: '" + base + "'", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pointer chain walking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Walks a pointer chain starting from {@code address}, applying each
     * successive offset from {@code parts[startIndex]..parts[parts.length-1]}.
     *
     * <p>For each step (except the final one):
     * <ol>
     *   <li>Read a pointer-sized value from {@code address}.</li>
     *   <li>Add the next offset to get the next address.</li>
     * </ol>
     * The final address is returned without a trailing dereference.
     *
     * @param handle     process handle
     * @param address    starting address (result of base resolution)
     * @param parts      all comma-split tokens (index 0 = base, already resolved)
     * @param startIndex first chain offset index in {@code parts}
     * @return final resolved address
     * @throws InvalidAddressException if any dereference or offset parse fails
     */
    private long walkChain(final HANDLE handle, final long address,
                           final String[] parts, final int startIndex) {
        long current = address;

        for (int i = startIndex; i < parts.length; i++) {
            // Read pointer at current address before applying offset
            log.debug("Chain step {}: reading pointer at 0x{}", i, Long.toHexString(current));
            try {
                final byte[] ptrBytes = memoryIO.readBytes(handle, current, 8);
                current = ByteUtils.toLong(ptrBytes);
                log.debug("Chain step {}: pointer value = 0x{}", i, Long.toHexString(current));
            } catch (final Exception e) {
                throw new InvalidAddressException(
                        "Failed to dereference pointer at 0x" + Long.toHexString(current)
                                + " (chain step " + i + ")", e);
            }

            final String offsetStr = parts[i].trim();
            final long offset;
            try {
                offset = HexUtils.parseAddress(offsetStr);
            } catch (final IllegalArgumentException e) {
                throw new InvalidAddressException(
                        "Invalid chain offset '" + offsetStr + "'", e);
            }

            current = current + offset;
            log.debug("Chain step {}: added offset 0x{} -> 0x{}", i, Long.toHexString(offset), Long.toHexString(current));
        }

        return current;
    }
}
