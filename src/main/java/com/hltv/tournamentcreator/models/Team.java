package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Team(
        UUID teamId,
        String name,
        Integer worldRanking,
        int playerCount
) {
    public Team(UUID teamId, String name, Integer worldRanking) { this(teamId, name, worldRanking, 0); }
}
