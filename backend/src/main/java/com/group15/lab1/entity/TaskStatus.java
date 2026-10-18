package com.group15.lab1.entity;

import java.util.Arrays;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE;

    public static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Status cannot be blank");
        }

        String normalized = rawValue.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(status -> status.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status must be one of: TODO, IN_PROGRESS, DONE"))
                .name();
    }
}
