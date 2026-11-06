package com.hltv.tournamentcreator.models;

import java.util.UUID;

public record GroupStanding(UUID teamId, String name, int seed, int played, int wins, int losses,
                            int mapsWon, int mapsLost, int points, int headToHeadPoints) {
    public int mapDifference() { return mapsWon - mapsLost; }
}
