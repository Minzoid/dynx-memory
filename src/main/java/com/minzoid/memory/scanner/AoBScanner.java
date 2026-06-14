package com.minzoid.memory.scanner;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.minzoid.memory.nativeapi.Kernel32Extended;
import com.minzoid.memory.nativeapi.NativeMemoryIO;
import com.minzoid.memory.scanner.PatternMatcher.CompiledPattern;
import com.minzoid.memory.structures.MemoryBasicInformation;
import com.minzoid.memory.exceptions.ScanException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Array-of-Bytes (AoB) scanner that searches an entire process address space
 * for occurrences of a given byte pattern.
 *
 * <p>The scanner works in two phases:
 * <ol>
 *   <li><b>Region enumeration</b> — calls {@code VirtualQueryEx} in a loop to
 *       build a list of committed, readable memory regions.</li>
 *   <li><b>Parallel scan</b> — distributes region scanning across
 *       {@link ForkJoinPool#commonPool()} using {@link RegionScanTask} leaf
 *       actions. Results are accumulated into a {@link CopyOnWriteArrayList}.</li>
 * </ol>
 *
 * <p>Thread-safe: yes — all mutable state is scoped to individual scan
 * invocations; the scanner instance itself holds no mutable state.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public final class AoBScanner {

    private static final Logger log = LoggerFactory.getLogger(AoBScanner.class);

    /**
     * Maximum number of bytes read per region chunk during scanning.
     * Larger values reduce system call overhead but increase memory pressure.
     */
    private static final int MAX_CHUNK_SIZE = 4 * 1024 * 1024; // 4 MB

    private final Kernel32Extended kernel;
    private final NativeMemoryIO   memoryIO;

    /**
     * Constructs an {@code AoBScanner} with the default JNA kernel instance.
     */
    public AoBScanner() {
        this(Kernel32Extended.INSTANCE, new NativeMemoryIO());
    }

    /**
     * Constructs an {@code AoBScanner} with injectable dependencies (for testing).
     *
     * @param kernel   the Kernel32 interface implementation
     * @param memoryIO the native memory I/O wrapper
     */
    public AoBScanner(final Kernel32Extended kernel, final NativeMemoryIO memoryIO) {
        this.kernel   = kernel;
        this.memoryIO = memoryIO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Scans the entire readable address space of the target process for
     * occurrences of the given byte pattern and returns all matching addresses.
     *
     * <p>The pattern string uses space-separated hex bytes and {@code ??}
     * wildcards:
     * <pre>
     *   memory.scan("89 AB ?? FF")
     *   memory.scan("DE AD BE EF 00 ?? 01")
     * </pre>
     *
     * @param handle     open process handle with {@code PROCESS_VM_READ} rights
     * @param patternStr AoB pattern string (e.g. {@code "89 AB ?? FF"})
     * @return list of virtual addresses where the pattern was found; empty if none
     * @throws ScanException if the pattern is invalid or an unrecoverable scan
     *                       error occurs
     */
    public List<Long> scan(final HANDLE handle, final String patternStr) {
        log.info("Starting AoB scan for pattern: '{}'", patternStr);

        final CompiledPattern pattern = PatternMatcher.compile(patternStr);
        log.debug("Compiled: {}", pattern);

        final List<ScanRegion> regions = enumerateReadableRegions(handle);
        log.info("Found {} readable memory regions to scan", regions.size());

        final CopyOnWriteArrayList<Long> results = new CopyOnWriteArrayList<>();

        ForkJoinPool.commonPool().invoke(
                new RegionBatchTask(handle, regions, 0, regions.size(), pattern, results)
        );

        log.info("Scan complete. Found {} match(es) for pattern '{}'", results.size(), patternStr);
        return new ArrayList<>(results);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Region enumeration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Walks the target process virtual address space using {@code VirtualQueryEx}
     * and returns all committed, readable, non-guard regions.
     *
     * @param handle process handle
     * @return list of scannable regions
     */
    private List<ScanRegion> enumerateReadableRegions(final HANDLE handle) {
        final List<ScanRegion> regions  = new ArrayList<>();
        final MemoryBasicInformation mbi = new MemoryBasicInformation();
        final int mbiSize               = mbi.size();
        long address                    = 0L;

        while (true) {
            final int result = kernel.VirtualQueryEx(
                    handle,
                    new Pointer(address),
                    mbi,
                    mbiSize
            );

            if (result == 0) {
                break; // End of address space or error
            }

            final long base = mbi.getBaseAddressLong();
            final long size = mbi.regionSize;

            if (mbi.isReadable() && size > 0) {
                regions.add(new ScanRegion(base, size));
                log.trace("Readable region: base=0x{} size={}", Long.toHexString(base), size);
            }

            // Advance past this region; guard against wrap-around on 32-bit
            final long next = base + size;
            if (Long.compareUnsigned(next, address) <= 0) {
                break; // Wrapped or did not advance — stop
            }
            address = next;
        }

        return regions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal data structures
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lightweight record of a scannable memory region.
     */
    private record ScanRegion(long base, long size) {}

    // ─────────────────────────────────────────────────────────────────────────
    // ForkJoin tasks
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ForkJoin task that divides a list of regions into halves until a single
     * region is reached, then delegates to {@link RegionScanTask}.
     */
    private final class RegionBatchTask extends RecursiveAction {

        private static final long serialVersionUID = 1L;

        /** Split into leaf tasks when the batch has this many or fewer regions. */
        private static final int LEAF_THRESHOLD = 4;

        private final HANDLE handle;
        private final List<ScanRegion> regions;
        private final int from;
        private final int to;
        private final CompiledPattern pattern;
        private final CopyOnWriteArrayList<Long> results;

        RegionBatchTask(final HANDLE handle, final List<ScanRegion> regions,
                        final int from, final int to,
                        final CompiledPattern pattern,
                        final CopyOnWriteArrayList<Long> results) {
            this.handle  = handle;
            this.regions = regions;
            this.from    = from;
            this.to      = to;
            this.pattern = pattern;
            this.results = results;
        }

        @Override
        protected void compute() {
            final int count = to - from;
            if (count <= LEAF_THRESHOLD) {
                // Scan each region sequentially within this leaf
                for (int i = from; i < to; i++) {
                    new RegionScanTask(handle, regions.get(i), pattern, results).compute();
                }
            } else {
                final int mid = from + (count / 2);
                invokeAll(
                        new RegionBatchTask(handle, regions, from, mid, pattern, results),
                        new RegionBatchTask(handle, regions, mid, to,  pattern, results)
                );
            }
        }
    }

    /**
     * Leaf ForkJoin task that reads a single memory region (in chunks if needed)
     * and searches for pattern matches.
     */
    private final class RegionScanTask extends RecursiveAction {

        private static final long serialVersionUID = 1L;

        private final HANDLE handle;
        private final ScanRegion region;
        private final CompiledPattern pattern;
        private final CopyOnWriteArrayList<Long> results;

        RegionScanTask(final HANDLE handle, final ScanRegion region,
                       final CompiledPattern pattern,
                       final CopyOnWriteArrayList<Long> results) {
            this.handle  = handle;
            this.region  = region;
            this.pattern = pattern;
            this.results = results;
        }

        @Override
        protected void compute() {
            final long regionBase = region.base();
            final long regionSize = region.size();
            final int  patLen     = pattern.length();

            long offset = 0;
            while (offset < regionSize) {
                // Read a chunk (may be smaller at the end of the region)
                final int chunkSize = (int) Math.min(MAX_CHUNK_SIZE, regionSize - offset);
                if (chunkSize < patLen) break; // Remaining bytes can't fit the pattern

                byte[] chunk;
                try {
                    chunk = memoryIO.readBytes(handle, regionBase + offset, chunkSize);
                } catch (final Exception e) {
                    log.trace("Skipping unreadable chunk at 0x{}: {}",
                            Long.toHexString(regionBase + offset), e.getMessage());
                    offset += chunkSize;
                    continue;
                }

                // Scan the chunk
                final int searchLen = chunk.length - patLen + 1;
                for (int i = 0; i < searchLen; i++) {
                    if (PatternMatcher.matches(chunk, i, pattern)) {
                        final long matchAddr = regionBase + offset + i;
                        log.debug("Pattern match at 0x{}", Long.toHexString(matchAddr));
                        results.add(matchAddr);
                    }
                }

                // Advance; overlap by (patLen - 1) to catch patterns spanning chunks
                offset += chunkSize - (patLen - 1);
            }
        }
    }
}

