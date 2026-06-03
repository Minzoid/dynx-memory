package com.dynx.memory.exceptions;

/**
 * Thrown when the JVM process does not have sufficient operating-system
 * privileges to open or read/write the target process.
 *
 * <p>Common causes:
 * <ul>
 *   <li>The target process is running at a higher integrity level.</li>
 *   <li>The JVM was not launched with Administrator privileges.</li>
 *   <li>Windows UIPI or anticheat software is blocking access.</li>
 * </ul>
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class AccessDeniedException extends DynxMemoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code AccessDeniedException} with the specified detail message.
     *
     * @param message the detail message
     */
    public AccessDeniedException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code AccessDeniedException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public AccessDeniedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
