package com.hltv.tournamentcreator.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record Player(
        UUID playerId,
        String nickname,
        String firstName,
        String lastName,
        Country country,
        LocalDate dateOfBirth,
        Team team,
        Double headshotPercentage,
        Double hltvRating,
        LocalDateTime createdAt
) {
}
