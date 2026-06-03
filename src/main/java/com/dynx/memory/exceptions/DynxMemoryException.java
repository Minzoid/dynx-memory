package com.dynx.memory.exceptions;

/**
 * Root exception for all DYNX Memory library errors.
 *
 * <p>All library-specific exceptions extend this class, allowing callers to
 * catch either the specific subtype or any DYNX memory error via this base
 * class.
 *
 * <p>This exception is unchecked (extends {@link RuntimeException}) to keep
 * the API fluent and avoid forcing callers to declare checked exceptions.
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class DynxMemoryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code DynxMemoryException} with the specified detail message.
     *
     * @param message the detail message
     */
    public DynxMemoryException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DynxMemoryException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public DynxMemoryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
