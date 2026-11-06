package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record TournamentMatch(UUID matchId, UUID tournamentId, UUID team1Id, String team1Name,
                              UUID team2Id, String team2Name, String format, String status,
                              LocalDateTime start, String stage, String round, UUID winnerId,
                              int team1Maps, int team2Maps, UUID nextMatchId, Integer nextSlot) {
    public String opponents() { return team1Name + " vs " + team2Name; }
    public String score() { return team1Maps + " : " + team2Maps; }
}
