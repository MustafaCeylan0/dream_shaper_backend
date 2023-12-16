package com.seng.comfy.backend.jwt;

// CustomJwtException.java
public class CustomJwtException extends RuntimeException {
    public CustomJwtException(String message) {
        super(message);
    }
}