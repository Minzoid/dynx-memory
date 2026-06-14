package com.minzoid.memory.exceptions;

/**
 * Thrown when a {@code WriteProcessMemory} call fails or writes fewer bytes
 * than expected.
 *
 * <p>Typical causes:
 * <ul>
 *   <li>The target address is write-protected; use {@code ProtectionManager}
 *       to change the memory protection first.</li>
 *   <li>The process handle lacks write access.</li>
 *   <li>The target process terminated before the write completed.</li>
 * </ul>
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public class MemoryWriteException extends MemoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MemoryWriteException} with the specified detail message.
     *
     * @param message the detail message
     */
    public MemoryWriteException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MemoryWriteException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public MemoryWriteException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

