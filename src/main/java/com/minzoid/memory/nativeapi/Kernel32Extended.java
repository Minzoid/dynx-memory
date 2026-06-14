package com.minzoid.memory.nativeapi;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.PROCESS_INFORMATION;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import com.minzoid.memory.structures.MemoryBasicInformation;
import com.minzoid.memory.structures.ModuleEntry;

/**
 * Extended JNA interface for Windows Kernel32 API functions not included in
 * JNA Platform's built-in {@link Kernel32} mapping.
 *
 * <p>This interface adds the following additional Win32 functions:
 * <ul>
 *   <li>{@code ReadProcessMemory} — read bytes from a target process</li>
 *   <li>{@code WriteProcessMemory} — write bytes to a target process</li>
 *   <li>{@code VirtualQueryEx} — query memory region information</li>
 *   <li>{@code VirtualProtectEx} — change memory protection flags</li>
 *   <li>{@code IsWow64Process} — detect 32-bit process on 64-bit OS</li>
 *   <li>{@code CreateToolhelp32Snapshot} — create process/module snapshot</li>
 *   <li>{@code Module32First} / {@code Module32Next} — enumerate modules</li>
 *   <li>{@code Process32First} / {@code Process32Next} — enumerate processes</li>
 * </ul>
 *
 * <p>A singleton instance is accessible via {@link #INSTANCE}.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public interface Kernel32Extended extends Kernel32 {

    /**
     * Singleton instance of this interface, loaded with Unicode options.
     * Use this field for all native calls.
     */
    Kernel32Extended INSTANCE = Native.load(
            "kernel32",
            Kernel32Extended.class,
            W32APIOptions.DEFAULT_OPTIONS
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Toolhelp32 Snapshot constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Include all processes in the system in the snapshot. */
    int TH32CS_SNAPPROCESS = 0x00000002;

    /** Include all modules of the specified process in the snapshot. */
    int TH32CS_SNAPMODULE  = 0x00000008;

    /** Include 32-bit modules in a 64-bit process snapshot. */
    int TH32CS_SNAPMODULE32 = 0x00000010;

    // ─────────────────────────────────────────────────────────────────────────
    // Process access rights
    // ─────────────────────────────────────────────────────────────────────────

    /** Full access to the process. */
    int PROCESS_ALL_ACCESS = 0x001FFFFF;

    /** Required to read memory in a process using ReadProcessMemory. */
    int PROCESS_VM_READ    = 0x0010;

    /** Required to write to memory in a process using WriteProcessMemory. */
    int PROCESS_VM_WRITE   = 0x0020;

    /** Required to perform an operation on the address space of a process. */
    int PROCESS_VM_OPERATION = 0x0008;

    /** Required to retrieve certain information about a process. */
    int PROCESS_QUERY_INFORMATION = 0x0400;

    // ─────────────────────────────────────────────────────────────────────────
    // Memory region state / type constants
    // ─────────────────────────────────────────────────────────────────────────

    /** Memory region is committed. */
    int MEM_COMMIT  = 0x1000;

    /** Memory region is free. */
    int MEM_FREE    = 0x10000;

    /** Memory region is reserved. */
    int MEM_RESERVE = 0x2000;

    // ─────────────────────────────────────────────────────────────────────────
    // Memory I/O
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads data from an area of memory in a specified process.
     *
     * @param hProcess          handle to the process with memory to be read
     * @param lpBaseAddress     pointer to the base address in the specified process
     * @param lpBuffer          pointer to a buffer that receives the contents
     * @param nSize             number of bytes to be read
     * @param lpNumberOfBytesRead receives the number of bytes transferred (may be null)
     * @return {@code true} if the function succeeds
     */
    boolean ReadProcessMemory(
            HANDLE hProcess,
            Pointer lpBaseAddress,
            Pointer lpBuffer,
            int nSize,
            IntByReference lpNumberOfBytesRead
    );

    /**
     * Writes data to an area of memory in a specified process.
     *
     * @param hProcess           handle to the process whose memory is to be modified
     * @param lpBaseAddress      pointer to the base address in the specified process
     * @param lpBuffer           pointer to the buffer that contains data to be written
     * @param nSize              number of bytes to be written
     * @param lpNumberOfBytesWritten receives the number of bytes transferred (may be null)
     * @return {@code true} if the function succeeds
     */
    boolean WriteProcessMemory(
            HANDLE hProcess,
            Pointer lpBaseAddress,
            Pointer lpBuffer,
            int nSize,
            IntByReference lpNumberOfBytesWritten
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Virtual memory queries and protection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves information about a range of pages within the virtual address
     * space of a specified process.
     *
     * @param hProcess   handle to the process whose memory information is queried
     * @param lpAddress  pointer to the base address of the region to query
     * @param lpBuffer   {@link MemoryBasicInformation} structure that receives information
     * @param dwLength   size of the {@code lpBuffer} parameter, in bytes
     * @return size of the returned information, in bytes; 0 on failure
     */
    int VirtualQueryEx(
            HANDLE hProcess,
            Pointer lpAddress,
            MemoryBasicInformation lpBuffer,
            int dwLength
    );

    /**
     * Changes the protection on a region of committed pages in the virtual address
     * space of a specified process.
     *
     * @param hProcess          handle to the process whose memory protection is to be changed
     * @param lpAddress         pointer to the base address of the region of pages
     * @param dwSize            size of the region whose access protection attributes are to be changed
     * @param flNewProtect      the new memory protection option (PAGE_* constant)
     * @param lpflOldProtect    receives the previous access protection value
     * @return {@code true} if the function succeeds
     */
    boolean VirtualProtectEx(
            HANDLE hProcess,
            Pointer lpAddress,
            int dwSize,
            int flNewProtect,
            IntByReference lpflOldProtect
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Architecture detection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Determines whether the specified process is running under WOW64 (i.e.,
     * a 32-bit process on a 64-bit OS).
     *
     * @param hProcess      handle to the process
     * @param Wow64Process  receives a boolean; {@code true} if the process is WOW64
     * @return {@code true} if the function succeeds
     */
    boolean IsWow64Process(
            HANDLE hProcess,
            IntByReference Wow64Process
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Toolhelp32 Snapshot — Module enumeration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Takes a snapshot of the specified processes, heaps, modules, and threads
     * used by the processes. Used for module and process enumeration.
     *
     * @param dwFlags       type of information to include (TH32CS_* constants)
     * @param th32ProcessID PID of the process to include (0 for current process)
     * @return handle to the snapshot; {@code INVALID_HANDLE_VALUE} on failure
     */
    HANDLE CreateToolhelp32Snapshot(int dwFlags, int th32ProcessID);

    /**
     * Retrieves information about the first module associated with a process.
     *
     * @param hSnapshot handle to the snapshot returned by {@code CreateToolhelp32Snapshot}
     * @param lpme      {@link ModuleEntry} structure that receives information
     * @return {@code true} if the first entry is copied to the buffer
     */
    boolean Module32First(HANDLE hSnapshot, ModuleEntry lpme);

    /**
     * Retrieves information about the next module associated with a process.
     *
     * @param hSnapshot handle to the snapshot returned by {@code CreateToolhelp32Snapshot}
     * @param lpme      {@link ModuleEntry} structure that receives information
     * @return {@code true} if the next entry is copied to the buffer
     */
    boolean Module32Next(HANDLE hSnapshot, ModuleEntry lpme);

    // ─────────────────────────────────────────────────────────────────────────
    // Toolhelp32 Snapshot — Process enumeration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * JNA mapping of the {@code PROCESSENTRY32} structure used when walking
     * the process list via Toolhelp32.
     */
    class PROCESSENTRY32 extends com.sun.jna.Structure
            implements com.sun.jna.Structure.ByReference {

        /** Size of the structure; must be set before first call. */
        public int    dwSize;
        /** Number of references to the process (unused). */
        public int    cntUsage;
        /** Process ID. */
        public int    th32ProcessID;
        /** Default heap ID (unused). */
        public Pointer th32DefaultHeapID;
        /** Module ID (unused). */
        public int    th32ModuleID;
        /** Number of threads in the process. */
        public int    cntThreads;
        /** PID of the parent process. */
        public int    th32ParentProcessID;
        /** Base priority of threads in the process. */
        public int    pcPriClassBase;
        /** Flags (unused). */
        public int    dwFlags;
        /** Name of the executable (null-terminated). */
        public char[] szExeFile = new char[260];

        /** Default constructor required by JNA; sets {@code dwSize}. */
        public PROCESSENTRY32() {
            super(ALIGN_DEFAULT);
            dwSize = size();
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.Arrays.asList(
                    "dwSize", "cntUsage", "th32ProcessID",
                    "th32DefaultHeapID", "th32ModuleID", "cntThreads",
                    "th32ParentProcessID", "pcPriClassBase", "dwFlags", "szExeFile"
            );
        }

        /**
         * Returns the executable name as a trimmed Java {@code String}.
         *
         * @return process executable name (e.g. {@code "notepad.exe"})
         */
        public String getExeName() {
            return new String(szExeFile).replace("\0", "").trim();
        }
    }

    /**
     * Retrieves information about the first process encountered in a system snapshot.
     *
     * @param hSnapshot handle to the snapshot
     * @param lppe      {@link PROCESSENTRY32} that receives information
     * @return {@code true} if the first entry is copied to the buffer
     */
    boolean Process32First(HANDLE hSnapshot, PROCESSENTRY32 lppe);

    /**
     * Retrieves information about the next process recorded in a system snapshot.
     *
     * @param hSnapshot handle to the snapshot
     * @param lppe      {@link PROCESSENTRY32} that receives information
     * @return {@code true} if the next entry is copied to the buffer
     */
    boolean Process32Next(HANDLE hSnapshot, PROCESSENTRY32 lppe);
}

