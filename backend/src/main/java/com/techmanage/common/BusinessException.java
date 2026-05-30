package com.techmanage.common;

/**
 * Business logic exception — message is safe to show to the user.
 * Thrown for validation failures, precondition checks, and other
 * expected error conditions at the service boundary.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
