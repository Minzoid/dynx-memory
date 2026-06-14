package com.minzoid.memory.exceptions;

/**
 * Root exception for all Memory library errors.
 *
 * <p>All library-specific exceptions extend this class, allowing callers to
 * catch either the specific subtype or any Memory error via this base
 * class.
 *
 * <p>This exception is unchecked (extends {@link RuntimeException}) to keep
 * the API fluent and avoid forcing callers to declare checked exceptions.
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public class MemoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MemoryException} with the specified detail message.
     *
     * @param message the detail message
     */
    public MemoryException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MemoryException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public MemoryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

