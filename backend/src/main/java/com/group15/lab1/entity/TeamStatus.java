package com.group15.lab1.entity;

import java.util.Arrays;

public enum TeamStatus {
    ACTIVE,
    DISBANDED;

    public static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Team status cannot be blank");
        }

        String normalized = rawValue.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Team status must be one of: ACTIVE, DISBANDED"))
                .name();
    }
}
