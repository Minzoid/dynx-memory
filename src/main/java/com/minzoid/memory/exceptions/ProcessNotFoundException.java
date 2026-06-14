package com.minzoid.memory.exceptions;

/**
 * Thrown when a target process cannot be found by PID or name.
 *
 * <p>This exception is raised by {@code ProcessManager} when:
 * <ul>
 *   <li>No process with the given PID exists on the system.</li>
 *   <li>No running process matches the given executable name.</li>
 * </ul>
 *
 * @author Minzoid
 * @version 1.0.0
 * @since 1.0.0
 */
public class ProcessNotFoundException extends MemoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ProcessNotFoundException} with the specified detail message.
     *
     * @param message the detail message
     */
    public ProcessNotFoundException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ProcessNotFoundException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public ProcessNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

