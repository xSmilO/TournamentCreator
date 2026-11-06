package com.hltv.tournamentcreator.models;

import java.util.UUID;

public record BracketMatch(UUID matchId, UUID team1Id, UUID team2Id, String round,
                           UUID nextMatchId, Integer nextSlot, UUID winnerId) {}
