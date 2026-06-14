package com.minzoid.memory.process;

import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minzoid.memory.nativeapi.Kernel32Extended;
import com.minzoid.memory.exceptions.AccessDeniedException;
import com.minzoid.memory.exceptions.ProcessNotFoundException;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages the lifecycle of a Windows process handle — opening, closing,
 * and querying architectural properties of a target process.
 *
 * <p>Process handles are protected by a {@link ReentrantReadWriteLock} to
 * allow concurrent reads (e.g., memory reads) while exclusive writes are
 * used only when opening or closing the handle.
 *
 * <p>Thread-safe: yes.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ProcessManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessManager.class);

    /** Access rights needed for full memory interaction. */
    private static final int DESIRED_ACCESS =
            Kernel32Extended.PROCESS_VM_READ        |
            Kernel32Extended.PROCESS_VM_WRITE       |
            Kernel32Extended.PROCESS_VM_OPERATION   |
            Kernel32Extended.PROCESS_QUERY_INFORMATION;

    private final Kernel32Extended kernel;

    /** Guards {@link #handle} and {@link #pid}. */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** Open handle to the target process; {@code null} if no process is attached. */
    private volatile HANDLE handle;

    /** PID of the currently attached process; {@code -1} if none. */
    private volatile int pid = -1;

    /**
     * Constructs a {@code ProcessManager} using the default
     * {@link Kernel32Extended#INSTANCE}.
     */
    public ProcessManager() {
        this(Kernel32Extended.INSTANCE);
    }

    /**
     * Constructs a {@code ProcessManager} with an injectable kernel interface.
     *
     * @param kernel the Kernel32 interface implementation to use
     */
    public ProcessManager(final Kernel32Extended kernel) {
        this.kernel = kernel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Open
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens a process by its numeric PID, acquiring all memory-interaction rights.
     *
     * @param pid the target process identifier
     * @throws ProcessNotFoundException if no process with the given PID exists
     * @throws AccessDeniedException    if the OS denies access to the process
     */
    public void openByPid(final int pid) {
        lock.writeLock().lock();
        try {
            closeInternal();
            log.info("Opening process by PID: {}", pid);
            final HANDLE h = kernel.OpenProcess(DESIRED_ACCESS, false, pid);
            if (h == null) {
                final int err = kernel.GetLastError();
                if (err == 5) { // ERROR_ACCESS_DENIED
                    throw new AccessDeniedException(
                            "Access denied opening PID " + pid + " (error=" + err + "). Run as Administrator.");
                }
                throw new ProcessNotFoundException(
                        "Could not open process with PID " + pid + " (error=" + err + ")");
            }
            this.handle = h;
            this.pid    = pid;
            log.info("Successfully opened PID {}", pid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Opens the first running process whose executable name matches
     * {@code processName} (case-insensitive).
     *
     * @param processName the executable name to find (e.g. {@code "notepad.exe"})
     * @throws ProcessNotFoundException if no matching process is running
     * @throws AccessDeniedException    if the OS denies access
     */
    public void openByName(final String processName) {
        if (processName == null || processName.isBlank()) {
            throw new IllegalArgumentException("processName must not be null or blank");
        }
        log.info("Searching for process: '{}'", processName);

        final HANDLE snapshot = kernel.CreateToolhelp32Snapshot(
                Kernel32Extended.TH32CS_SNAPPROCESS, 0);
        if (snapshot == null) {
            throw new ProcessNotFoundException(
                    "Failed to create process snapshot (error=" + kernel.GetLastError() + ")");
        }

        try {
            final Kernel32Extended.PROCESSENTRY32 entry = new Kernel32Extended.PROCESSENTRY32();
            if (!kernel.Process32First(snapshot, entry)) {
                throw new ProcessNotFoundException(
                        "Process list is empty (error=" + kernel.GetLastError() + ")");
            }

            do {
                final String name = entry.getExeName();
                if (name.equalsIgnoreCase(processName)) {
                    log.info("Found process '{}' with PID {}", name, entry.th32ProcessID);
                    openByPid(entry.th32ProcessID);
                    return;
                }
            } while (kernel.Process32Next(snapshot, entry));

        } finally {
            kernel.CloseHandle(snapshot);
        }

        throw new ProcessNotFoundException(
                "No running process found with name '" + processName + "'");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Close
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Closes the current process handle and resets state.
     * Safe to call if no process is open.
     */
    public void close() {
        lock.writeLock().lock();
        try {
            closeInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** Internal close — must be called with the write lock held. */
    private void closeInternal() {
        if (handle != null) {
            log.info("Closing handle for PID {}", pid);
            kernel.CloseHandle(handle);
            handle = null;
            pid    = -1;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Handle access
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the currently open process handle.
     *
     * @return the process handle
     * @throws IllegalStateException if no process is currently open
     */
    public HANDLE getHandle() {
        lock.readLock().lock();
        try {
            requireOpen();
            return handle;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the PID of the currently attached process.
     *
     * @return the process ID
     * @throws IllegalStateException if no process is currently open
     */
    public int getPid() {
        lock.readLock().lock();
        try {
            requireOpen();
            return pid;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns {@code true} if a process handle is currently open.
     *
     * @return {@code true} if attached to a process
     */
    public boolean isOpen() {
        lock.readLock().lock();
        try {
            return handle != null;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Architecture detection
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the attached process is a native 64-bit process.
     *
     * <p>On a 64-bit OS, {@code IsWow64Process} returns {@code true} only for
     * 32-bit processes running under WOW64 emulation. A native 64-bit process
     * will have WOW64 = {@code false}.
     *
     * <p>On a 32-bit OS, all processes are 32-bit, so this always returns
     * {@code false}.
     *
     * @return {@code true} if the target process is 64-bit
     * @throws IllegalStateException if no process is currently open
     */
    public boolean is64Bit() {
        lock.readLock().lock();
        try {
            requireOpen();
            final IntByReference isWow64 = new IntByReference(0);
            kernel.IsWow64Process(handle, isWow64);
            // WOW64 = true  → 32-bit process on 64-bit OS
            // WOW64 = false → either native 64-bit or running on 32-bit OS
            final boolean wow64 = isWow64.getValue() != 0;
            log.debug("PID {} IsWow64={}", pid, wow64);
            return !wow64;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Asserts that a process handle is currently open.
     *
     * @throws IllegalStateException if no process is open
     */
    private void requireOpen() {
        if (handle == null) {
            throw new IllegalStateException(
                    "No process is currently open. Call openByPid() or openByName() first.");
        }
    }
}

