package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Player(
        UUID playerId,
        String nickname,
        String firstName,
        String lastName,
        String country,
        UUID teamId,
        Double headshotPercentage,
        Double hltvRating,
        LocalDateTime createdAt
) {
}