package com.minzoid.memory.exceptions;

/**
 * Thrown when an Array-of-Bytes (AoB) scan fails to complete successfully.
 *
 * <p>This exception is raised by {@code AoBScanner} when:
 * <ul>
 *   <li>The pattern string is malformed or empty.</li>
 *   <li>A memory region cannot be read during scanning.</li>
 *   <li>An unexpected error occurs in the parallel scan executor.</li>
 * </ul>
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public class ScanException extends MemoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ScanException} with the specified detail message.
     *
     * @param message the detail message
     */
    public ScanException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ScanException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public ScanException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

