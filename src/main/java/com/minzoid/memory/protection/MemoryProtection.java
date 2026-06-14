package com.minzoid.memory.protection;

/**
 * Enumeration of Windows memory protection constants used with
 * {@code VirtualProtectEx}.
 *
 * <p>Each constant maps directly to the corresponding {@code PAGE_*} value
 * defined in {@code winnt.h} and documented on MSDN.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/memory/memory-protection-constants">
 *      Memory Protection Constants (MSDN)</a>
 */
public enum MemoryProtection {

    /**
     * {@code PAGE_NOACCESS} (0x01) — Disables all access to the committed
     * region of pages. Any attempt to read, write, or execute causes an
     * access violation.
     */
    NO_ACCESS(0x01),

    /**
     * {@code PAGE_READONLY} (0x02) — Enables read-only access to the committed
     * region of pages. Write or execute attempts cause an access violation.
     */
    READ_ONLY(0x02),

    /**
     * {@code PAGE_READWRITE} (0x04) — Enables read and write access to the
     * committed region of pages. Execute attempts cause an access violation.
     */
    READ_WRITE(0x04),

    /**
     * {@code PAGE_WRITECOPY} (0x08) — Enables copy-on-write to a mapped view
     * of a file mapping object.
     */
    WRITE_COPY(0x08),

    /**
     * {@code PAGE_EXECUTE} (0x10) — Enables execute access to the committed
     * region of pages. Read or write attempts cause an access violation.
     */
    EXECUTE(0x10),

    /**
     * {@code PAGE_EXECUTE_READ} (0x20) — Enables execute and read-only access
     * to the committed region of pages. Write attempts cause an access violation.
     */
    EXECUTE_READ(0x20),

    /**
     * {@code PAGE_EXECUTE_READWRITE} (0x40) — Enables execute, read, and write
     * access to the committed region of pages.
     */
    EXECUTE_READ_WRITE(0x40),

    /**
     * {@code PAGE_EXECUTE_WRITECOPY} (0x80) — Enables execute, read, and
     * copy-on-write access to a mapped view of a file mapping object.
     */
    EXECUTE_WRITE_COPY(0x80);

    /** The Windows {@code PAGE_*} constant value. */
    private final int nativeValue;

    /**
     * Constructs a {@code MemoryProtection} with the given native constant value.
     *
     * @param nativeValue the {@code PAGE_*} constant
     */
    MemoryProtection(final int nativeValue) {
        this.nativeValue = nativeValue;
    }

    /**
     * Returns the Windows {@code PAGE_*} constant value for this protection.
     *
     * @return native protection constant
     */
    public int getNativeValue() {
        return nativeValue;
    }

    /**
     * Looks up a {@code MemoryProtection} by its native {@code PAGE_*} value.
     *
     * @param nativeValue the Windows constant to look up
     * @return the matching {@code MemoryProtection}
     * @throws IllegalArgumentException if no constant matches the given value
     */
    public static MemoryProtection fromNativeValue(final int nativeValue) {
        for (final MemoryProtection mp : values()) {
            if (mp.nativeValue == nativeValue) {
                return mp;
            }
        }
        throw new IllegalArgumentException(
                "Unknown memory protection value: 0x" + Integer.toHexString(nativeValue));
    }

    @Override
    public String toString() {
        return name() + "(0x" + Integer.toHexString(nativeValue) + ")";
    }
}

