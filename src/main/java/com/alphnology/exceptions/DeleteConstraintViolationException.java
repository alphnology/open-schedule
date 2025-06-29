package com.alphnology.exceptions;

/**
 * @author me@fredpena.dev
 * @created 08/02/2025  - 14:25
 */
public class DeleteConstraintViolationException extends Exception {
    public DeleteConstraintViolationException(String message) {
        super(message);
    }
}