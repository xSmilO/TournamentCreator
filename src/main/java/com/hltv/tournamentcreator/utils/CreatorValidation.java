package com.hltv.tournamentcreator.utils;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class CreatorValidation {
    private CreatorValidation() {}

    public static String text(String value, String label, int maxLength, boolean required) {
        String trimmed = value == null ? "" : value.trim();
        if (required && trimmed.isEmpty()) throw new IllegalArgumentException(label + " is required.");
        if (trimmed.codePointCount(0, trimmed.length()) > maxLength)
            throw new IllegalArgumentException(label + " must be at most " + maxLength + " characters.");
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Integer ranking(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            int ranking = Integer.parseInt(value.trim());
            if (ranking > 0) return ranking;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("World ranking must be a positive whole number.");
    }

    public static int teamLimit(String value) {
        try {
            int limit = Integer.parseInt(value == null ? "" : value.trim());
            if (limit >= 2 && limit <= 256) return limit;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Team limit must be a whole number between 2 and 256.");
    }

    public static BigDecimal prizePool(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            if (amount.signum() >= 0 && amount.scale() <= 2
                    && amount.compareTo(new BigDecimal("99999999.99")) <= 0) return amount;
        } catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException("Prize pool must be between 0 and 99999999.99, with at most two decimal places.");
    }

    public static void dates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start))
            throw new IllegalArgumentException("End date cannot be before the start date.");
    }
}
