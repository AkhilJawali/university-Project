package com.utms.common.exception;

public class ValidationException extends RuntimeException {

    private final String field;
    private final Object rejectedValue;

    public ValidationException(String field, String message, Object rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }
}
