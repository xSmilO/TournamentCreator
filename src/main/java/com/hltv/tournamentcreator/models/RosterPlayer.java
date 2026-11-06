package com.hltv.tournamentcreator.models;

import java.util.UUID;

public record RosterPlayer(UUID playerId, String nickname, String firstName, String lastName,
                           String country, Double rating, String role) {
    public String fullName() {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }
}
