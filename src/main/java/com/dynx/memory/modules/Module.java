package com.dynx.memory.modules;

import java.util.Objects;

/**
 * Immutable value object representing a loaded module (DLL or EXE) within
 * a target Windows process.
 *
 * <p>Instances are obtained via {@link ModuleManager#getModule} and
 * carry the module name, base load address, and size in bytes.
 *
 * <p>Thread-safe: yes (immutable).
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Module {

    /** Name of the module as reported by the OS (e.g. {@code "Game.dll"}). */
    private final String name;

    /**
     * Virtual base address at which this module is loaded in the target process.
     * Stored as a signed {@code long}; interpret as unsigned for addresses above 2 GB.
     */
    private final long baseAddress;

    /** Size of the module image in bytes. */
    private final int size;

    /**
     * Constructs a new {@code Module} value object.
     *
     * @param name        the module name (must not be null or blank)
     * @param baseAddress the virtual base address in the target process
     * @param size        the module image size in bytes
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Module(final String name, final long baseAddress, final int size) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("module name must not be null or blank");
        }
        this.name        = name;
        this.baseAddress = baseAddress;
        this.size        = size;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Accessors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the module name as reported by the operating system.
     *
     * @return module name (e.g. {@code "Game.dll"})
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the virtual base address at which the module is loaded in the
     * target process.
     *
     * <p>Use {@code Long.toUnsignedString(module.getBaseAddress(), 16)} to
     * display addresses above {@link Long#MAX_VALUE} correctly.
     *
     * @return base address as a signed {@code long}
     */
    public long getBaseAddress() {
        return baseAddress;
    }

    /**
     * Returns the size of the module image in bytes.
     *
     * @return module size in bytes
     */
    public int getSize() {
        return size;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Object overrides
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Module other)) return false;
        return baseAddress == other.baseAddress
                && size == other.size
                && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase(), baseAddress, size);
    }

    @Override
    public String toString() {
        return String.format("Module{name='%s', base=0x%X, size=%d}", name, baseAddress, size);
    }
}
