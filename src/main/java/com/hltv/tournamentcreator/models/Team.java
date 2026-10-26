package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record Team(
        UUID teamId,
        String name,
        Integer worldRanking
) {}