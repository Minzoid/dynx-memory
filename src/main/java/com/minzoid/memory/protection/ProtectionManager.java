package com.minzoid.memory.protection;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minzoid.memory.nativeapi.Kernel32Extended;
import com.minzoid.memory.exceptions.MemoryException;

/**
 * Manages memory protection changes on committed pages within a target process
 * via {@code VirtualProtectEx}.
 *
 * <p>Each call to {@link #changeProtection} atomically replaces the protection
 * of the specified region and returns the previous protection value, allowing
 * callers to restore it when done.
 *
 * <p>Thread-safe: yes — stateless; all mutable state is passed via parameters.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ProtectionManager {

    private static final Logger log = LoggerFactory.getLogger(ProtectionManager.class);

    private final Kernel32Extended kernel;

    /**
     * Constructs a {@code ProtectionManager} using the default
     * {@link Kernel32Extended#INSTANCE}.
     */
    public ProtectionManager() {
        this(Kernel32Extended.INSTANCE);
    }

    /**
     * Constructs a {@code ProtectionManager} with an injectable kernel interface.
     *
     * @param kernel the Kernel32 interface implementation to use
     */
    public ProtectionManager(final Kernel32Extended kernel) {
        this.kernel = kernel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Protection change
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Changes the memory protection of a region starting at {@code address}
     * with the given {@code size} to the specified {@link MemoryProtection}.
     *
     * <p>The previous protection value is returned so the caller can restore it:
     * <pre>
     *   MemoryProtection old = pm.changeProtection(handle, addr, 4, MemoryProtection.READ_WRITE);
     *   // ... write bytes ...
     *   pm.changeProtection(handle, addr, 4, old);
     * </pre>
     *
     * @param handle     open process handle with {@code PROCESS_VM_OPERATION} rights
     * @param address    virtual address of the first page whose protection is changed
     * @param size       size of the region to change, in bytes (must be &gt; 0)
     * @param protection the new protection to apply
     * @return the previous {@link MemoryProtection} of the region
     * @throws MemoryException      if {@code VirtualProtectEx} fails
     * @throws IllegalArgumentException if {@code size} is not positive or
     *                                  {@code protection} is null
     */
    public MemoryProtection changeProtection(final HANDLE handle,
                                             final long address,
                                             final int size,
                                             final MemoryProtection protection) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive, got: " + size);
        }
        if (protection == null) {
            throw new IllegalArgumentException("protection must not be null");
        }

        final Pointer    ptr         = new Pointer(address);
        final IntByReference oldProtect = new IntByReference(0);

        log.debug("VirtualProtectEx: address=0x{} size={} newProtect={}",
                Long.toHexString(address), size, protection);

        final boolean ok = kernel.VirtualProtectEx(
                handle, ptr, size, protection.getNativeValue(), oldProtect);

        if (!ok) {
            final int err = kernel.GetLastError();
            throw new MemoryException(
                    String.format(
                            "VirtualProtectEx failed at 0x%X size=%d protection=%s (error=%d)",
                            address, size, protection, err
                    )
            );
        }

        final int oldNative = oldProtect.getValue();
        log.debug("VirtualProtectEx succeeded; old protection=0x{}", Integer.toHexString(oldNative));

        // Translate old native value back to enum; if unknown, return READ_WRITE as safe default
        try {
            return MemoryProtection.fromNativeValue(oldNative);
        } catch (final IllegalArgumentException e) {
            log.warn("Unknown old protection value 0x{}; defaulting to READ_WRITE",
                    Integer.toHexString(oldNative));
            return MemoryProtection.READ_WRITE;
        }
    }

    /**
     * Convenience overload that uses a default size of {@code 1} page (4096 bytes).
     *
     * @param handle     open process handle
     * @param address    start address of the region to change
     * @param protection the new protection
     * @return the previous {@link MemoryProtection}
     */
    public MemoryProtection changeProtection(final HANDLE handle,
                                             final long address,
                                             final MemoryProtection protection) {
        return changeProtection(handle, address, 4096, protection);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Temporary protection helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Temporarily changes memory protection to {@code newProtection}, executes
     * the given action, and automatically restores the original protection —
     * even if the action throws.
     *
     * <p>Example:
     * <pre>
     *   pm.withProtection(handle, address, 4, MemoryProtection.READ_WRITE,
     *       () -&gt; memoryIO.writeBytes(handle, address, data));
     * </pre>
     *
     * @param handle        open process handle
     * @param address       start address of the region to protect
     * @param size          region size in bytes
     * @param newProtection temporary protection to apply
     * @param action        the action to execute under the temporary protection
     * @throws MemoryException if protection change fails
     */
    public void withProtection(final HANDLE handle,
                               final long address,
                               final int size,
                               final MemoryProtection newProtection,
                               final Runnable action) {
        final MemoryProtection original = changeProtection(handle, address, size, newProtection);
        try {
            action.run();
        } finally {
            try {
                changeProtection(handle, address, size, original);
            } catch (final Exception e) {
                log.error("Failed to restore memory protection at 0x{}: {}",
                        Long.toHexString(address), e.getMessage());
            }
        }
    }
}

