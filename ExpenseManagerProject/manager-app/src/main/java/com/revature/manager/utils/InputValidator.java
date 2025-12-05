package com.revature.manager.utils;

import com.revature.manager.exceptions.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class InputValidator {
    private InputValidator() {
        // Utility class; no instances required.
    }

    /**
     * Ensures the provided value is not null or blank.
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }
    }

    /**
     * Ensures the status matches one of the supported values.
     */
    public static void requireStatus(String status) {
        if (!"pending".equalsIgnoreCase(status)
            && !"approved".equalsIgnoreCase(status)
            && !"denied".equalsIgnoreCase(status)) {
            throw new ValidationException("Status must be pending, approved, or denied");
        }
    }

    /**
     * Parses a date value and raises a readable exception when the format is wrong.
     */
    public static LocalDate parseIsoDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must be in YYYY-MM-DD format");
        }
    }
}
