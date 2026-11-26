package com.jc2001.campustrade.common;

/** Exception carrying an error code already defined by the frozen API contract. */
public class DomainException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final ErrorCode errorCode;
    private final Object details;

    public DomainException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), null);
    }

    public DomainException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public DomainException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }
}
