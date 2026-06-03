package com.dynx.memory.exceptions;

/**
 * Thrown when a memory address is null, unmapped, or otherwise invalid
 * for the requested operation.
 *
 * <p>This exception may be raised by:
 * <ul>
 *   <li>{@code AddressResolver} when an expression cannot be resolved.</li>
 *   <li>{@code MemoryReader} / {@code MemoryWriter} when the target address
 *       is {@code 0} or outside the process address space.</li>
 * </ul>
 *
 * @author DYNX Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class InvalidAddressException extends DynxMemoryException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code InvalidAddressException} with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidAddressException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code InvalidAddressException} with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the cause of this exception
     */
    public InvalidAddressException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
