package com.hltv.tournamentcreator.models;

import java.util.List;
import java.util.UUID;

public record TournamentGroup(UUID groupId, String name, int order, List<TournamentParticipant> teams) {
    public TournamentGroup { teams = List.copyOf(teams); }
}
