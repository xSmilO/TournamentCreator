package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.BracketMatch;
import java.util.*;

public final class SingleElimination {
    public static final String STAGE = "Single elimination";
    private SingleElimination() {}

    public static List<BracketMatch> generate(List<UUID> teams) {
        if (teams.size() < 2) throw new IllegalArgumentException("Register at least two teams first.");
        if (new HashSet<>(teams).size() != teams.size()) throw new IllegalArgumentException("Teams must be distinct.");
        if (teams.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Each team must have an ID.");
        if (teams.size() > 256) throw new IllegalArgumentException("A bracket can contain at most 256 teams.");
        int size = 2;
        while (size < teams.size()) size *= 2;
        List<Integer> seeds = new ArrayList<>(List.of(1, 2));
        for (int n = 4; n <= size; n *= 2) {
            List<Integer> next = new ArrayList<>();
            for (int seed : seeds) { next.add(seed); next.add(n + 1 - seed); }
            seeds = next;
        }
        List<List<UUID>> ids = new ArrayList<>();
        for (int count = size / 2; count >= 1; count /= 2) {
            List<UUID> round = new ArrayList<>();
            for (int i = 0; i < count; i++) round.add(UUID.randomUUID());
            ids.add(round);
        }
        Map<UUID, UUID[]> entrants = new HashMap<>();
        for (List<UUID> round : ids) for (UUID id : round) entrants.put(id, new UUID[2]);
        for (int i = 0; i < seeds.size(); i++) {
            if (seeds.get(i) <= teams.size()) entrants.get(ids.get(0).get(i / 2))[i % 2] = teams.get(seeds.get(i) - 1);
        }
        List<BracketMatch> result = new ArrayList<>();
        for (int r = 0; r < ids.size(); r++) {
            int roundSize = size >> r;
            String name = roundSize == 2 ? "Final" : roundSize == 4 ? "Semifinals" : roundSize == 8 ? "Quarterfinals" : "Round of " + roundSize;
            for (int i = 0; i < ids.get(r).size(); i++) {
                UUID id = ids.get(r).get(i);
                UUID[] pair = entrants.get(id);
                UUID nextId = r + 1 < ids.size() ? ids.get(r + 1).get(i / 2) : null;
                Integer slot = nextId == null ? null : i % 2 + 1;
                UUID winner = r == 0 && (pair[0] == null || pair[1] == null) ? (pair[0] == null ? pair[1] : pair[0]) : null;
                if (winner != null && nextId != null) entrants.get(nextId)[slot - 1] = winner;
                result.add(new BracketMatch(id, pair[0], pair[1], name, nextId, slot, winner));
            }
        }
        return List.copyOf(result);
    }
}
