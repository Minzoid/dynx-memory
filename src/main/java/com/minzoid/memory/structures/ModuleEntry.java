package com.minzoid.memory.structures;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.HMODULE;

import java.util.Arrays;
import java.util.List;

/**
 * JNA mapping of the Windows {@code MODULEENTRY32} structure,
 * as returned by {@code Module32First} / {@code Module32Next}
 * from the Toolhelp32 API.
 *
 * <p>
 * Used by {@code ModuleManager} to enumerate all loaded modules
 * in a target process snapshot.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href=
 *      "https://learn.microsoft.com/en-us/windows/win32/api/tlhelp32/ns-tlhelp32-moduleentry32">
 *      MODULEENTRY32 (MSDN)</a>
 */
public class ModuleEntry extends Structure {

    /** Maximum module name length (including null terminator). */
    public static final int MAX_MODULE_NAME32 = 256;

    /** Maximum path length (including null terminator). */
    public static final int MAX_PATH = 260;

    // ─── Structure fields (must match MODULEENTRY32 layout exactly) ───────────

    /** Size of the structure, in bytes. Must be set before first use. */
    public int dwSize;

    /** Module identifier (unused; always set to 0). */
    public int th32ModuleID;

    /** PID of the process whose module list is to be examined. */
    public int th32ProcessID;

    /** Global usage count of the module. */
    public int GlblcntUsage;

    /** Module usage count in the context of owning process. */
    public int ProccntUsage;

    /** Base address of the module in the owning process. */
    public byte[] modBaseAddr = new byte[8]; // pointer-sized field

    /** Size of the module, in bytes. */
    public int modBaseSize;

    /** Handle to the module in the context of the owning process. */
    public HMODULE hModule;

    /** Name of the module (null-terminated). */
    public char[] szModule = new char[MAX_MODULE_NAME32];

    /** Full path to the module (null-terminated). */
    public char[] szExePath = new char[MAX_PATH];

    /**
     * Constructs a new {@code ModuleEntry} and sets {@code dwSize} automatically,
     * as required by the Toolhelp32 API.
     */
    public ModuleEntry() {
        super(ALIGN_DEFAULT);
        dwSize = size();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "dwSize",
                "th32ModuleID",
                "th32ProcessID",
                "GlblcntUsage",
                "ProccntUsage",
                "modBaseAddr",
                "modBaseSize",
                "hModule",
                "szModule",
                "szExePath");
    }

    /**
     * Returns the module name as a trimmed Java {@code String},
     * stripping any null characters from the char array.
     *
     * @return module name (e.g. {@code "Game.dll"})
     */
    public String getModuleName() {
        return new String(szModule).replace("\0", "").trim();
    }

    /**
     * Returns the full executable path of the module as a trimmed Java
     * {@code String}.
     *
     * @return full path (e.g. {@code "C:\\Games\\Game.exe"})
     */
    public String getExePath() {
        return new String(szExePath).replace("\0", "").trim();
    }

    /**
     * Decodes the {@code modBaseAddr} byte array into an unsigned {@code long}
     * representing the module base address (little-endian, pointer-sized).
     *
     * @return module base address
     */
    public long getBaseAddressLong() {
        long addr = 0;
        for (int i = modBaseAddr.length - 1; i >= 0; i--) {
            addr = (addr << 8) | (modBaseAddr[i] & 0xFF);
        }
        return addr;
    }
}

