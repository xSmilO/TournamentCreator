package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

public record Player(
        UUID playerId,
        String nickname,
        String firstName,
        String lastName,
        int countryId,
        Date dateOfBirth,
        UUID teamId,
        double headshotPercentage,
        double hltvRating,
        LocalDateTime createdAt
) {
}