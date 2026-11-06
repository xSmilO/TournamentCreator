package com.hltv.tournamentcreator.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Tournament(UUID tournamentId, String name, String tier, LocalDate startDate,
                         LocalDate endDate, BigDecimal prizePool, String location, String format,
                         int participantCount, int teamLimit) {
    public Tournament(UUID tournamentId, String name, String tier, LocalDate startDate,
                      LocalDate endDate, BigDecimal prizePool, String location, String format,
                      int participantCount) {
        this(tournamentId, name, tier, startDate, endDate, prizePool, location, format, participantCount, 16);
    }
}
