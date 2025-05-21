package com.example.roi.mcs;

/**
 * Custom exception for invalid input parameters in the MCS lookup functionality.
 * Thrown when input parameters are outside their allowed ranges or otherwise invalid.
 */
public class InvalidParameterException extends RuntimeException {
    public InvalidParameterException(String message) {
        super(message);
    }
} 