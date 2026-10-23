package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Team(
        UUID teamId,
        String name,
        String country,
        Integer worldRanking, // Integer instead of int allows it to be null
        LocalDateTime createdAt
) {}