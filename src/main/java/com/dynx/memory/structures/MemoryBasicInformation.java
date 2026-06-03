package com.dynx.memory.structures;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * JNA mapping of the Windows {@code MEMORY_BASIC_INFORMATION} structure,
 * as returned by {@code VirtualQueryEx}.
 *
 * <p>Used by {@code AoBScanner} to enumerate committed, readable memory
 * regions in the target process.
 *
 * <p>Structure layout (64-bit):
 * <pre>
 * PVOID  BaseAddress;        // 8 bytes
 * PVOID  AllocationBase;     // 8 bytes
 * DWORD  AllocationProtect;  // 4 bytes
 * WORD   PartitionId;        // 2 bytes
 * WORD   _padding;           // 2 bytes
 * SIZE_T RegionSize;         // 8 bytes
 * DWORD  State;              // 4 bytes
 * DWORD  Protect;            // 4 bytes
 * DWORD  Type;               // 4 bytes
 * </pre>
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-memory_basic_information">
 *      MEMORY_BASIC_INFORMATION (MSDN)</a>
 */
public class MemoryBasicInformation extends Structure {

    // ─── Windows MEM_* and PAGE_* constants ──────────────────────────────────

    /** Memory region is committed. */
    public static final int MEM_COMMIT  = 0x1000;

    /** Memory region is free. */
    public static final int MEM_FREE    = 0x10000;

    /** Memory region is reserved. */
    public static final int MEM_RESERVE = 0x2000;

    /** Protection: no-access. */
    public static final int PAGE_NOACCESS         = 0x01;
    /** Protection: read-only. */
    public static final int PAGE_READONLY         = 0x02;
    /** Protection: read/write. */
    public static final int PAGE_READWRITE        = 0x04;
    /** Protection: guard page. */
    public static final int PAGE_GUARD            = 0x100;

    // ─── Structure fields ─────────────────────────────────────────────────────

    /** Base address of the region. */
    public Pointer baseAddress;

    /** Base address of the allocation that contains this region. */
    public Pointer allocationBase;

    /** Protection when the region was allocated. */
    public int allocationProtect;

    /** Partition ID (Windows 10 and later). */
    public short partitionId;

    /** Size of the region in bytes. */
    public long regionSize;

    /** State of the pages (MEM_COMMIT, MEM_FREE, MEM_RESERVE). */
    public int state;

    /** Current protection of the pages. */
    public int protect;

    /** Type of pages (MEM_IMAGE, MEM_MAPPED, MEM_PRIVATE). */
    public int type;

    /** Default constructor required by JNA. */
    public MemoryBasicInformation() {
        super(ALIGN_DEFAULT);
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "baseAddress",
                "allocationBase",
                "allocationProtect",
                "partitionId",
                "regionSize",
                "state",
                "protect",
                "type"
        );
    }

    /**
     * Returns {@code true} if this region is committed and readable
     * (not a guard page, not NO_ACCESS).
     *
     * @return {@code true} if the region can be read safely
     */
    public boolean isReadable() {
        if (state != MEM_COMMIT) {
            return false;
        }
        if ((protect & PAGE_NOACCESS) != 0) {
            return false;
        }
        if ((protect & PAGE_GUARD) != 0) {
            return false;
        }
        return true;
    }

    /**
     * Returns the base address as an unsigned {@code long}.
     *
     * @return base address value
     */
    public long getBaseAddressLong() {
        return Pointer.nativeValue(baseAddress);
    }
}
