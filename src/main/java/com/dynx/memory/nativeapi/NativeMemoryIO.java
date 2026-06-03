package com.dynx.memory.nativeapi;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dynx.memory.exceptions.MemoryReadException;
import com.dynx.memory.exceptions.MemoryWriteException;

/**
 * Low-level wrapper around {@link Kernel32Extended} that provides typed
 * {@code readBytes} and {@code writeBytes} operations against a target process.
 *
 * <p>This class contains no business logic; it is responsible only for:
 * <ul>
 *   <li>Allocating JNA {@link Memory} buffers for I/O.</li>
 *   <li>Invoking {@code ReadProcessMemory} and {@code WriteProcessMemory}.</li>
 *   <li>Translating Win32 failures into {@link MemoryReadException} /
 *       {@link MemoryWriteException}.</li>
 * </ul>
 *
 * <p>Instances of this class are stateless and thread-safe. The caller is
 * responsible for providing a valid, open process handle.
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class NativeMemoryIO {

    private static final Logger log = LoggerFactory.getLogger(NativeMemoryIO.class);

    /** The underlying JNA Kernel32 extended interface. */
    private final Kernel32Extended kernel;

    /**
     * Constructs a new {@code NativeMemoryIO} using the default
     * {@link Kernel32Extended#INSTANCE}.
     */
    public NativeMemoryIO() {
        this(Kernel32Extended.INSTANCE);
    }

    /**
     * Constructs a new {@code NativeMemoryIO} with the given kernel interface.
     * Useful for unit-testing with a mock.
     *
     * @param kernel the Kernel32 interface to use
     */
    public NativeMemoryIO(final Kernel32Extended kernel) {
        this.kernel = kernel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads {@code length} bytes from the target process starting at
     * {@code address} and returns them as a new byte array.
     *
     * @param handle  open process handle with {@code PROCESS_VM_READ} rights
     * @param address virtual address in the target process to read from
     * @param length  number of bytes to read (must be &gt; 0)
     * @return byte array containing the read data
     * @throws MemoryReadException      if the native call fails or returns fewer
     *                                  bytes than requested
     * @throws IllegalArgumentException if {@code length} is not positive
     */
    public byte[] readBytes(final HANDLE handle, final long address, final int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive, got: " + length);
        }

        final Pointer targetPtr = new Pointer(address);
        final Memory  buffer    = new Memory(length);
        final IntByReference bytesRead = new IntByReference(0);

        log.debug("ReadProcessMemory: address=0x{} length={}", Long.toHexString(address), length);

        final boolean ok = kernel.ReadProcessMemory(handle, targetPtr, buffer, length, bytesRead);

        if (!ok || bytesRead.getValue() != length) {
            final int err = kernel.GetLastError();
            throw new MemoryReadException(
                    String.format(
                            "ReadProcessMemory failed at 0x%X: requested=%d read=%d error=%d",
                            address, length, bytesRead.getValue(), err
                    )
            );
        }

        return buffer.getByteArray(0, length);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Writes the contents of {@code data} into the target process at
     * {@code address}.
     *
     * @param handle  open process handle with {@code PROCESS_VM_WRITE} and
     *                {@code PROCESS_VM_OPERATION} rights
     * @param address virtual address in the target process to write to
     * @param data    bytes to write (must not be null or empty)
     * @throws MemoryWriteException     if the native call fails or writes fewer
     *                                  bytes than requested
     * @throws IllegalArgumentException if {@code data} is null or empty
     */
    public void writeBytes(final HANDLE handle, final long address, final byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data must not be null or empty");
        }

        final Pointer targetPtr = new Pointer(address);
        final Memory  buffer    = new Memory(data.length);
        buffer.write(0, data, 0, data.length);

        final IntByReference bytesWritten = new IntByReference(0);

        log.debug("WriteProcessMemory: address=0x{} length={}", Long.toHexString(address), data.length);

        final boolean ok = kernel.WriteProcessMemory(handle, targetPtr, buffer, data.length, bytesWritten);

        if (!ok || bytesWritten.getValue() != data.length) {
            final int err = kernel.GetLastError();
            throw new MemoryWriteException(
                    String.format(
                            "WriteProcessMemory failed at 0x%X: requested=%d written=%d error=%d",
                            address, data.length, bytesWritten.getValue(), err
                    )
            );
        }

        log.debug("WriteProcessMemory: wrote {} bytes at 0x{}", data.length, Long.toHexString(address));
    }
}
