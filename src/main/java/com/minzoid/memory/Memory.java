package com.minzoid.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minzoid.memory.exceptions.MemoryException;
import com.minzoid.memory.modules.Module;
import com.minzoid.memory.modules.ModuleManager;
import com.minzoid.memory.nativeapi.NativeMemoryIO;
import com.minzoid.memory.pointer.AddressResolver;
import com.minzoid.memory.process.ProcessManager;
import com.minzoid.memory.protection.MemoryProtection;
import com.minzoid.memory.protection.ProtectionManager;
import com.minzoid.memory.scanner.AoBScanner;
import com.minzoid.memory.utils.ByteUtils;
import com.sun.jna.platform.win32.WinNT.HANDLE;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Primary entry point for the Memory Java library.
 *
 * <p>The {@code Memory} class acts as a <em>Façade</em> over all internal
 * subsystems (process management, memory I/O, module enumeration, address
 * resolution, AoB scanning, and protection management). All public operations
 * are exposed through this single class.
 *
 * <h2>Quick start</h2>
 * <pre>
 * // Open by process name
 * Memory memory = new Memory();
 * memory.openProcess("notepad.exe");
 *
 * // Read a value
 * int value = memory.readInt(0x7FF000001234L);
 *
 * // Write a value
 * memory.writeInt(0x7FF000001234L, 42);
 *
 * // Resolve a pointer chain
 * long addr = memory.resolve("Game.dll+ABCD,10,20");
 *
 * // AoB scan
 * List&lt;Long&gt; hits = memory.scan("89 AB ?? FF");
 *
 * // Always clean up
 * memory.close();
 * </pre>
 *
 * <h2>Fluent API</h2>
 * <pre>
 * int hp = Memory.attach("Game.exe").readInt(address);
 * </pre>
 *
 * <p>Implements {@link AutoCloseable} for use in try-with-resources blocks.
 *
 * <p>Thread-safe: yes — internal subsystems use appropriate locking.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Memory implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Memory.class);

    // ─────────────────────────────────────────────────────────────────────────────
    // Static initialiser — suppress JNA restricted-method warnings
    //
    // On Java 22+ the Enable-Native-Access manifest attribute (set in build.gradle)
    // is enough. For Java 17–21 we temporarily swap System.err with a filtering
    // PrintStream, force-load com.sun.jna.Native so its one-time warning fires
    // while the filter is active, then restore the original stream.
    // This gives consumers zero-config native access with no JVM flags required.
    // ─────────────────────────────────────────────────────────────────────────────
    static {
        suppressJnaNativeAccessWarnings();
    }

    /**
     * Temporarily replaces {@link System#err} with a filtering stream that
     * drops the four-line JNA native-access {@code WARNING} block, then forces
     * {@code com.sun.jna.Native} to load so that any one-time JVM warning is
     * emitted (and silently discarded) during class initialisation rather than
     * later during user code execution.
     *
     * <p>The original {@code System.err} is always restored, even on exception.
     *
     * <p>Only active on Java versions prior to 22; on Java 22+ the
     * {@code Enable-Native-Access} JAR manifest attribute suppresses the
     * warning natively.
     */
    private static void suppressJnaNativeAccessWarnings() {
        final int javaVersion = Runtime.version().feature();
        if (javaVersion >= 22) {
            // Manifest attribute handles it — nothing to do here.
            return;
        }

        final PrintStream original = System.err;
        final PrintStream filtered = new PrintStream(original, true) {
            /**
             * Drops any line that is part of the JNA restricted-method warning
             * block emitted by the JVM on Java 17–21.
             *
             * <p>Only lines that are clearly JVM-generated JNA warnings are
             * suppressed; all other stderr output passes through normally.
             */
            @Override
            public void println(final String line) {
                if (line != null && isJnaNativeWarning(line)) {
                    return; // silently drop
                }
                super.println(line);
            }

            private boolean isJnaNativeWarning(final String line) {
                if (!line.startsWith("WARNING:")) {
                    return false;
                }
                return line.contains("com.sun.jna.Native")
                    || line.contains("--enable-native-access")
                    || line.contains("Restricted methods will be blocked")
                    || (line.contains("restricted method") && line.contains("java.lang.System"));
            }
        };

        System.setErr(filtered);
        try {
            // Force JNA to initialise now, while the filter is active.
            // After this point the one-time warning will never fire again.
            Class.forName("com.sun.jna.Native");
        } catch (final ClassNotFoundException ignored) {
            // JNA is not on the classpath — nothing to suppress.
        } finally {
            System.setErr(original);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subsystems
    // ─────────────────────────────────────────────────────────────────────────

    private final ProcessManager    processManager;
    private final ModuleManager     moduleManager;
    private final NativeMemoryIO    memoryIO;
    private final AddressResolver   addressResolver;
    private final AoBScanner        scanner;
    private final ProtectionManager protectionManager;

    // ─────────────────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constructs a new {@code Memory} instance with default subsystems.
     *
     * <p>No process is opened until {@link #openProcess(int)} or
     * {@link #openProcess(String)} is called.
     */
    public Memory() {
        this.processManager    = new ProcessManager();
        this.moduleManager     = new ModuleManager();
        this.memoryIO          = new NativeMemoryIO();
        this.addressResolver   = new AddressResolver(moduleManager, memoryIO);
        this.scanner           = new AoBScanner();
        this.protectionManager = new ProtectionManager();
    }

    /**
     * Package-private constructor for injecting subsystems (used in testing).
     *
     * @param processManager    the process manager
     * @param moduleManager     the module manager
     * @param memoryIO          the native memory I/O
     * @param addressResolver   the address resolver
     * @param scanner           the AoB scanner
     * @param protectionManager the protection manager
     */
    Memory(final ProcessManager    processManager,
           final ModuleManager     moduleManager,
           final NativeMemoryIO    memoryIO,
           final AddressResolver   addressResolver,
           final AoBScanner        scanner,
           final ProtectionManager protectionManager) {
        this.processManager    = processManager;
        this.moduleManager     = moduleManager;
        this.memoryIO          = memoryIO;
        this.addressResolver   = addressResolver;
        this.scanner           = scanner;
        this.protectionManager = protectionManager;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Static factory — fluent API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new {@code Memory} instance and immediately opens the process
     * with the given name. Intended for fluent, one-liner usage.
     *
     * <pre>
     *   int hp = Memory.attach("Game.exe").readInt(hpAddress);
     * </pre>
     *
     * @param processName the executable name to attach to (e.g. {@code "notepad.exe"})
     * @return a new {@code Memory} instance attached to the specified process
     * @throws com.minzoid.memory.exceptions.ProcessNotFoundException if the process is not running
     * @throws com.minzoid.memory.exceptions.AccessDeniedException    if access is denied
     */
    public static Memory attach(final String processName) {
        final Memory memory = new Memory();
        memory.openProcess(processName);
        return memory;
    }

    /**
     * Creates a new {@code Memory} instance and immediately opens the process
     * with the given PID. Intended for fluent, one-liner usage.
     *
     * <pre>
     *   int hp = Memory.attach(1234).readInt(hpAddress);
     * </pre>
     *
     * @param pid the numeric process identifier
     * @return a new {@code Memory} instance attached to the specified process
     * @throws com.minzoid.memory.exceptions.ProcessNotFoundException if the process is not found
     * @throws com.minzoid.memory.exceptions.AccessDeniedException    if access is denied
     */
    public static Memory attach(final int pid) {
        final Memory memory = new Memory();
        memory.openProcess(pid);
        return memory;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the target process by numeric PID.
     *
     * @param pid the process identifier
     * @throws com.minzoid.memory.exceptions.ProcessNotFoundException if the process does not exist
     * @throws com.minzoid.memory.exceptions.AccessDeniedException    if OS access is denied
     */
    public void openProcess(final int pid) {
        moduleManager.invalidateCache();
        processManager.openByPid(pid);
        log.info("Attached to PID {}", pid);
    }

    /**
     * Opens the first running process whose executable name matches
     * {@code processName} (case-insensitive).
     *
     * @param processName executable name (e.g. {@code "notepad.exe"})
     * @throws com.minzoid.memory.exceptions.ProcessNotFoundException if no matching process is running
     * @throws com.minzoid.memory.exceptions.AccessDeniedException    if OS access is denied
     */
    public void openProcess(final String processName) {
        moduleManager.invalidateCache();
        processManager.openByName(processName);
        log.info("Attached to process '{}'", processName);
    }

    /**
     * Closes the current process handle and releases all associated resources.
     * Safe to call even if no process is open.
     */
    public void closeProcess() {
        moduleManager.invalidateCache();
        processManager.close();
        log.info("Process detached");
    }

    /**
     * Closes the current process handle. Equivalent to {@link #closeProcess()}.
     * Implements {@link AutoCloseable} for try-with-resources support.
     */
    @Override
    public void close() {
        closeProcess();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Process information
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the PID of the currently attached process.
     *
     * @return process identifier
     * @throws IllegalStateException if no process is open
     */
    public int getPid() {
        return processManager.getPid();
    }

    /**
     * Returns {@code true} if the currently attached process is a native 64-bit
     * process; {@code false} if it is 32-bit (WOW64).
     *
     * @return {@code true} for 64-bit processes
     * @throws IllegalStateException if no process is open
     */
    public boolean is64Bit() {
        return processManager.is64Bit();
    }

    /**
     * Returns {@code true} if a process is currently open.
     *
     * @return {@code true} if attached to a process
     */
    public boolean isOpen() {
        return processManager.isOpen();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Memory reading
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads a single {@code byte} from the target process at the given address.
     *
     * @param address virtual address to read from
     * @return the byte value
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public byte readByte(final long address) {
        return ByteUtils.toByte(memoryIO.readBytes(handle(), address, 1));
    }

    /**
     * Reads a little-endian {@code short} (2 bytes) from the target process.
     *
     * @param address virtual address to read from
     * @return the short value
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public short readShort(final long address) {
        return ByteUtils.toShort(memoryIO.readBytes(handle(), address, 2));
    }

    /**
     * Reads a little-endian {@code int} (4 bytes) from the target process.
     *
     * @param address virtual address to read from
     * @return the int value
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public int readInt(final long address) {
        return ByteUtils.toInt(memoryIO.readBytes(handle(), address, 4));
    }

    /**
     * Reads a little-endian {@code long} (8 bytes) from the target process.
     *
     * @param address virtual address to read from
     * @return the long value
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public long readLong(final long address) {
        return ByteUtils.toLong(memoryIO.readBytes(handle(), address, 8));
    }

    /**
     * Reads a little-endian IEEE-754 {@code float} (4 bytes) from the target process.
     *
     * @param address virtual address to read from
     * @return the float value
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public float readFloat(final long address) {
        return ByteUtils.toFloat(memoryIO.readBytes(handle(), address, 4));
    }

    /**
     * Reads a little-endian IEEE-754 {@code double} (8 bytes) from the target process.
     *
     * @param address virtual address to read from
     * @return the double value
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public double readDouble(final long address) {
        return ByteUtils.toDouble(memoryIO.readBytes(handle(), address, 8));
    }

    /**
     * Reads a UTF-8 encoded string of the specified byte length from the target process.
     *
     * @param address virtual address to read from
     * @param length  number of bytes to read
     * @return the decoded string (null characters stripped)
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     * @throws IllegalArgumentException if {@code length} is not positive
     */
    public String readString(final long address, final int length) {
        return readString(address, length, StandardCharsets.UTF_8);
    }

    /**
     * Reads a string of the specified byte length from the target process,
     * decoded using the given {@link Charset}.
     *
     * @param address virtual address to read from
     * @param length  number of bytes to read
     * @param charset the character set to use for decoding
     * @return the decoded string (null characters stripped)
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public String readString(final long address, final int length, final Charset charset) {
        final byte[] raw = memoryIO.readBytes(handle(), address, length);
        final String s   = new String(raw, charset);
        // Strip null terminator and trailing nulls
        final int nullIdx = s.indexOf('\0');
        return nullIdx >= 0 ? s.substring(0, nullIdx) : s;
    }

    /**
     * Reads a raw byte array of the specified length from the target process.
     *
     * @param address virtual address to read from
     * @param length  number of bytes to read
     * @return byte array containing the read data
     * @throws com.minzoid.memory.exceptions.MemoryReadException if the read fails
     */
    public byte[] readBytes(final long address, final int length) {
        return memoryIO.readBytes(handle(), address, length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Memory writing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes a single {@code byte} to the target process at the given address.
     *
     * @param address virtual address to write to
     * @param value   the byte value to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeByte(final long address, final byte value) {
        memoryIO.writeBytes(handle(), address, ByteUtils.fromByte(value));
    }

    /**
     * Writes a little-endian {@code short} (2 bytes) to the target process.
     *
     * @param address virtual address to write to
     * @param value   the short value to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeShort(final long address, final short value) {
        memoryIO.writeBytes(handle(), address, ByteUtils.fromShort(value));
    }

    /**
     * Writes a little-endian {@code int} (4 bytes) to the target process.
     *
     * @param address virtual address to write to
     * @param value   the int value to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeInt(final long address, final int value) {
        memoryIO.writeBytes(handle(), address, ByteUtils.fromInt(value));
    }

    /**
     * Writes a little-endian {@code long} (8 bytes) to the target process.
     *
     * @param address virtual address to write to
     * @param value   the long value to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeLong(final long address, final long value) {
        memoryIO.writeBytes(handle(), address, ByteUtils.fromLong(value));
    }

    /**
     * Writes a little-endian IEEE-754 {@code float} (4 bytes) to the target process.
     *
     * @param address virtual address to write to
     * @param value   the float value to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeFloat(final long address, final float value) {
        memoryIO.writeBytes(handle(), address, ByteUtils.fromFloat(value));
    }

    /**
     * Writes a little-endian IEEE-754 {@code double} (8 bytes) to the target process.
     *
     * @param address virtual address to write to
     * @param value   the double value to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeDouble(final long address, final double value) {
        memoryIO.writeBytes(handle(), address, ByteUtils.fromDouble(value));
    }

    /**
     * Writes a UTF-8 encoded string (without null terminator) to the target process.
     *
     * @param address virtual address to write to
     * @param value   the string to write
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeString(final long address, final String value) {
        writeString(address, value, StandardCharsets.UTF_8);
    }

    /**
     * Writes a string encoded with the given {@link Charset} to the target process.
     *
     * @param address virtual address to write to
     * @param value   the string to write
     * @param charset the character set to use for encoding
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeString(final long address, final String value, final Charset charset) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        memoryIO.writeBytes(handle(), address, value.getBytes(charset));
    }

    /**
     * Writes a raw byte array to the target process at the given address.
     *
     * @param address virtual address to write to
     * @param data    the bytes to write (must not be null or empty)
     * @throws com.minzoid.memory.exceptions.MemoryWriteException if the write fails
     */
    public void writeBytes(final long address, final byte[] data) {
        memoryIO.writeBytes(handle(), address, data);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Module operations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the {@link Module} for the given module name (case-insensitive).
     *
     * <p>Module information is cached after the first call; use
     * {@link ModuleManager#refreshModules(HANDLE)} directly if you need to
     * force a re-enumeration.
     *
     * @param moduleName the module name to look up (e.g. {@code "Game.dll"})
     * @return the {@link Module} object with base address and size
     * @throws MemoryException if the module is not found
     * @throws IllegalStateException if no process is open
     */
    public Module getModule(final String moduleName) {
        return moduleManager.getModule(handle(), moduleName);
    }

    /**
     * Returns a list of all modules currently loaded in the target process.
     *
     * @return unmodifiable list of all loaded modules
     * @throws IllegalStateException if no process is open
     */
    public List<Module> getAllModules() {
        return moduleManager.getAllModules(handle());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Address resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves an address expression to a concrete virtual memory address.
     *
     * <p>Supported formats:
     * <pre>
     *   "7FF000001234"             → raw hex address
     *   "Game.dll+1234"            → module base + hex offset
     *   "Game.dll+1234,10,20,30"   → multi-level pointer chain
     * </pre>
     *
     * @param expression the address expression to resolve
     * @return the resolved virtual address
     * @throws com.minzoid.memory.exceptions.InvalidAddressException if resolution fails
     * @throws IllegalStateException if no process is open
     */
    public long resolve(final String expression) {
        return addressResolver.resolve(handle(), expression);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AoB scanning
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scans the target process memory for all occurrences of the given
     * Array-of-Bytes pattern and returns a list of matching addresses.
     *
     * <p>Wildcard tokens ({@code ??}) match any byte:
     * <pre>
     *   memory.scan("89 AB ?? FF")
     * </pre>
     *
     * @param patternStr the AoB pattern string
     * @return list of addresses where the pattern was found; empty if none found
     * @throws com.minzoid.memory.exceptions.ScanException if the pattern is malformed
     * @throws IllegalStateException if no process is open
     */
    public List<Long> scan(final String patternStr) {
        return scanner.scan(handle(), patternStr);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Memory protection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Changes the memory protection of the page containing {@code address}
     * to the specified {@link MemoryProtection} and returns the previous protection.
     *
     * @param address    virtual address within the page to protect
     * @param protection the new protection to apply
     * @return the previous {@link MemoryProtection}
     * @throws MemoryException   if the protection change fails
     * @throws IllegalStateException if no process is open
     */
    public MemoryProtection changeProtection(final long address,
                                             final MemoryProtection protection) {
        return protectionManager.changeProtection(handle(), address, protection);
    }

    /**
     * Changes the memory protection of a region of the given size starting at
     * {@code address} and returns the previous protection.
     *
     * @param address    virtual start address of the region
     * @param size       size of the region in bytes
     * @param protection the new protection to apply
     * @return the previous {@link MemoryProtection}
     * @throws MemoryException   if the protection change fails
     * @throws IllegalStateException if no process is open
     */
    public MemoryProtection changeProtection(final long address, final int size,
                                             final MemoryProtection protection) {
        return protectionManager.changeProtection(handle(), address, size, protection);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current process handle, asserting that a process is open.
     *
     * @return the open {@link HANDLE}
     * @throws IllegalStateException if no process is currently open
     */
    private HANDLE handle() {
        return processManager.getHandle();
    }
}

