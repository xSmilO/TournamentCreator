package com.hltv.tournamentcreator.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public record Player(
        UUID playerId,
        String nickname,
        String firstName,
        String lastName,
        Country country,
        LocalDate dateOfBirth,
        Team team,
        double headshotPercentage,
        double hltvRating,
        LocalDateTime createdAt
) {
}