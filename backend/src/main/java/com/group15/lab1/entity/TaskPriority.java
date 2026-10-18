package com.group15.lab1.entity;

import java.util.Arrays;

public enum TaskPriority {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    private final String value;

    TaskPriority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String normalize(String rawValue) {
        if (rawValue == null) {
            throw new IllegalArgumentException("Priority cannot be blank");
        }

        String normalized = rawValue.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(priority -> priority.value.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Priority must be one of: low, medium, high, urgent"))
                .value;
    }
}
