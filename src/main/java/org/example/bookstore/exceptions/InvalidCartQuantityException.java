package org.example.bookstore.exceptions;

/** An invalid quantity supplied to a domain operation, including callers outside HTTP. */
public class InvalidCartQuantityException extends IllegalArgumentException {
    /**
     * Creates a quantity-validation failure with the allowed range in its explanation.
     *
     * @param message client-safe explanation
     */
    public InvalidCartQuantityException(String message) { super(message); }
}
