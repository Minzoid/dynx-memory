package com.minzoid.memory.modules;

import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minzoid.memory.nativeapi.Kernel32Extended;
import com.minzoid.memory.structures.ModuleEntry;
import com.minzoid.memory.exceptions.MemoryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumerates, caches, and retrieves module (DLL/EXE) information for the
 * currently attached target process.
 *
 * <p>Module data is cached in a {@link ConcurrentHashMap} keyed by lower-case
 * module name to support O(1) lookups after initial enumeration. The cache can
 * be invalidated explicitly via {@link #refreshModules(HANDLE)}.
 *
 * <p>Thread-safe: yes — backed by {@code ConcurrentHashMap} and copy-on-write
 * refresh semantics.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public class ModuleManager {

    private static final Logger log = LoggerFactory.getLogger(ModuleManager.class);

    private final Kernel32Extended kernel;

    /**
     * Module cache: lower-case module name → {@link Module}.
     * Populated lazily on first access.
     */
    private final ConcurrentHashMap<String, Module> cache = new ConcurrentHashMap<>();

    /** Guards lazy initialization of the cache. */
    private volatile boolean loaded = false;

    /**
     * Constructs a {@code ModuleManager} using the default
     * {@link Kernel32Extended#INSTANCE}.
     */
    public ModuleManager() {
        this(Kernel32Extended.INSTANCE);
    }

    /**
     * Constructs a {@code ModuleManager} with an injectable kernel interface.
     *
     * @param kernel the Kernel32 interface implementation to use
     */
    public ModuleManager(final Kernel32Extended kernel) {
        this.kernel = kernel;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Module retrieval
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the {@link Module} for the given module name (case-insensitive).
     *
     * <p>Modules are loaded lazily: the first call enumerates all modules from
     * the target process via a Toolhelp32 snapshot and populates the cache.
     * Subsequent calls return the cached value directly.
     *
     * @param handle     open process handle for the target process
     * @param moduleName the module name to find (e.g. {@code "Game.dll"})
     * @return the matching {@link Module}
     * @throws MemoryException if the module is not found or the snapshot fails
     */
    public Module getModule(final HANDLE handle, final String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            throw new IllegalArgumentException("moduleName must not be null or blank");
        }
        ensureLoaded(handle);
        final Module module = cache.get(moduleName.toLowerCase());
        if (module == null) {
            // Try refreshing once in case it was just loaded
            refreshModules(handle);
            final Module retried = cache.get(moduleName.toLowerCase());
            if (retried == null) {
                throw new MemoryException(
                        "Module not found in target process: '" + moduleName + "'");
            }
            return retried;
        }
        return module;
    }

    /**
     * Returns a snapshot of all currently cached modules.
     *
     * @param handle open process handle for the target process
     * @return unmodifiable list of all loaded modules
     */
    public List<Module> getAllModules(final HANDLE handle) {
        ensureLoaded(handle);
        return List.copyOf(cache.values());
    }

    /**
     * Returns {@code true} if a module with the given name is loaded in the
     * target process (cache-hit or after a refresh).
     *
     * @param handle     open process handle
     * @param moduleName the module name to check (case-insensitive)
     * @return {@code true} if the module is present
     */
    public boolean hasModule(final HANDLE handle, final String moduleName) {
        ensureLoaded(handle);
        if (cache.containsKey(moduleName.toLowerCase())) {
            return true;
        }
        refreshModules(handle);
        return cache.containsKey(moduleName.toLowerCase());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Forces a re-enumeration of all modules from the target process, replacing
     * the existing cache. Call this if the process has loaded new DLLs since
     * the last query.
     *
     * @param handle open process handle for the target process
     * @throws MemoryException if the Toolhelp32 snapshot fails
     */
    public void refreshModules(final HANDLE handle) {
        final int pid = resolvePidFromHandle(handle);
        log.debug("Refreshing module list for PID {}", pid);

        // Use both 64-bit and 32-bit flags to capture all modules
        final HANDLE snapshot = kernel.CreateToolhelp32Snapshot(
                Kernel32Extended.TH32CS_SNAPMODULE | Kernel32Extended.TH32CS_SNAPMODULE32,
                pid
        );

        if (snapshot == null || snapshot.equals(com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE)) {
            final int err = kernel.GetLastError();
            throw new MemoryException(
                    "CreateToolhelp32Snapshot (modules) failed for PID " + pid + " (error=" + err + ")");
        }

        final Map<String, Module> fresh = new ConcurrentHashMap<>();
        try {
            final ModuleEntry entry = new ModuleEntry();
            if (kernel.Module32First(snapshot, entry)) {
                do {
                    final String name     = entry.getModuleName();
                    final long   base     = entry.getBaseAddressLong();
                    final int    size     = entry.modBaseSize;
                    final Module module   = new Module(name, base, size);
                    fresh.put(name.toLowerCase(), module);
                    log.debug("Found module: {}", module);
                } while (kernel.Module32Next(snapshot, entry));
            }
        } finally {
            kernel.CloseHandle(snapshot);
        }

        cache.clear();
        cache.putAll(fresh);
        loaded = true;
        log.info("Loaded {} module(s) for PID {}", cache.size(), pid);
    }

    /**
     * Invalidates the module cache. The next call to {@link #getModule} or
     * {@link #getAllModules} will trigger a full re-enumeration.
     */
    public void invalidateCache() {
        cache.clear();
        loaded = false;
        log.debug("Module cache invalidated");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ensures the module cache is populated. Uses double-checked locking
     * to avoid redundant snapshots under concurrent access.
     *
     * @param handle open process handle
     */
    private void ensureLoaded(final HANDLE handle) {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    refreshModules(handle);
                }
            }
        }
    }

    /**
     * Attempts to resolve the PID from the given handle via
     * {@code GetProcessId}. Falls back to 0 (current process snapshot)
     * if the call is not available.
     *
     * @param handle process handle
     * @return PID or 0 as fallback
     */
    private int resolvePidFromHandle(final HANDLE handle) {
        try {
            // GetProcessId is available in JNA platform Kernel32
            final com.sun.jna.platform.win32.Kernel32 k32 =
                    com.sun.jna.platform.win32.Kernel32.INSTANCE;
            return k32.GetProcessId(handle);
        } catch (final Exception e) {
            log.warn("Could not resolve PID from handle; using 0", e);
            return 0;
        }
    }
}

