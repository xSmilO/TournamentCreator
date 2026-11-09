package com.hltv.tournamentcreator.models;

import java.time.LocalDateTime;
import java.util.UUID;

public record TournamentMatch(UUID matchId, UUID tournamentId, UUID team1Id, String team1Name,
                              UUID team2Id, String team2Name, String format, String status,
                              LocalDateTime start, String stage, String round, UUID winnerId,
                              int team1Maps, int team2Maps, UUID nextMatchId, Integer nextSlot, UUID groupId, String outcome, String outcomeReason) {
    public TournamentMatch(UUID matchId, UUID tournamentId, UUID team1Id, String team1Name,
                           UUID team2Id, String team2Name, String format, String status,
                           LocalDateTime start, String stage, String round, UUID winnerId,
                           int team1Maps, int team2Maps, UUID nextMatchId, Integer nextSlot, UUID groupId) {
        this(matchId,tournamentId,team1Id,team1Name,team2Id,team2Name,format,status,start,stage,round,winnerId,
             team1Maps,team2Maps,nextMatchId,nextSlot,groupId,
             "Completed".equals(status) && (team1Id==null || team2Id==null) ? "Bye" : "Played",null);
    }
    public boolean awarded() { return "Forfeit".equals(outcome) || "Withdrawal".equals(outcome); }
    public boolean cancelled() { return "Cancelled".equals(outcome); }

    public TournamentMatch(UUID matchId, UUID tournamentId, UUID team1Id, String team1Name,
                           UUID team2Id, String team2Name, String format, String status,
                           LocalDateTime start, String stage, String round, UUID winnerId,
                           int team1Maps, int team2Maps, UUID nextMatchId, Integer nextSlot) {
        this(matchId, tournamentId, team1Id, team1Name, team2Id, team2Name, format, status,
                start, stage, round, winnerId, team1Maps, team2Maps, nextMatchId, nextSlot, null);
    }
    public String opponents() { return team1Name + " vs " + team2Name; }
    public String score() { return "Played".equals(outcome) ? team1Maps + " : " + team2Maps : outcome; }
}
