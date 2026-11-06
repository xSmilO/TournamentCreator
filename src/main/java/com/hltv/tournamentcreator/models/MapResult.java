package com.hltv.tournamentcreator.models;

import java.util.UUID;

public record MapResult(UUID gameId, String mapName, int order, int team1Score, int team2Score, String status) {}
