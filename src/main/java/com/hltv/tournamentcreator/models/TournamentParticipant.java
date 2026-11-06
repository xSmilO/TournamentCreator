package com.hltv.tournamentcreator.models;

import java.util.UUID;

public record TournamentParticipant(UUID teamId, String name, Integer seed) {}
