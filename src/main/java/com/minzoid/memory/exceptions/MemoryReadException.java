package com.minzoid.memory.exceptions;

/**
 * Thrown when a {@code ReadProcessMemory} call fails or returns fewer bytes
 * than requested.
 *
 * <p>Typical causes:
 * <ul>
 *   <li>The target address is not mapped (access violation in the target).</li>
 *   <li>The process handle was closed before the read completed.</li>
 *   <li>Insufficient read access rights on the process handle.</li>
 * </ul>
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public class MemoryReadException extends MemoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MemoryReadException} with the specified detail message.
     *
     * @param message the detail message
     */
    public MemoryReadException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MemoryReadException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public MemoryReadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

