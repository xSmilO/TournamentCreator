package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.MapResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Derives series results from map results; neither winner is entered independently. */
public final class MatchResults {
    private MatchResults() {}
    public record Summary(List<MapResult> maps, int team1Maps, int team2Maps, String status, UUID winnerId) {}

    public static int mapLimit(String format) {
        if (format == null) throw new IllegalArgumentException("Choose Bo1, Bo3 or Bo5.");
        return switch (format) {
            case "Bo1" -> 1;
            case "Bo3" -> 3;
            case "Bo5" -> 5;
            default -> throw new IllegalArgumentException("Choose Bo1, Bo3 or Bo5.");
        };
    }

    public static Summary evaluate(String format, UUID team1, UUID team2, List<MapResult> input) {
        int limit = mapLimit(format), required = limit / 2 + 1;
        if (team1 == null || team2 == null || team1.equals(team2))
            throw new IllegalArgumentException("Choose two different registered teams.");
        if (input.size() > limit) throw new IllegalArgumentException("Too many maps for " + format + ".");
        List<MapResult> maps = new ArrayList<>();
        int wins1 = 0, wins2 = 0, previousOrder = 0;
        boolean pending = false, live = false;
        for (MapResult map : input) {
            if (map.order() != previousOrder + 1 || map.order() > limit)
                throw new IllegalArgumentException("Maps must be ordered consecutively starting at 1.");
            previousOrder = map.order();
            if (map.status() == null || !Set.of("Upcoming", "Live", "Completed").contains(map.status()))
                throw new IllegalArgumentException("Choose a valid map status.");
            if (map.team1Score() < 0 || map.team2Score() < 0)
                throw new IllegalArgumentException("Round scores cannot be negative.");
            if ("Upcoming".equals(map.status()) && (map.team1Score() != 0 || map.team2Score() != 0))
                throw new IllegalArgumentException("A map with round scores must be Live or Completed.");
            String name = CreatorValidation.text(map.mapName(), "Map " + map.order() + " name", 50, false);
            if (name == null) {
                if (!"Upcoming".equals(map.status())) throw new IllegalArgumentException("Enter a name for map " + map.order() + ".");
                pending = true;
                continue;
            }
            if (maps.size() != map.order() - 1) throw new IllegalArgumentException("Name earlier maps before adding later maps.");
            if (!"Upcoming".equals(map.status()) && (pending || wins1 == required || wins2 == required))
                throw new IllegalArgumentException("Finish earlier maps first and stop playing once the series is won.");
            if ("Completed".equals(map.status())) {
                if (map.team1Score() == map.team2Score()) throw new IllegalArgumentException("A completed map cannot end in a draw.");
                if (map.team1Score() > map.team2Score()) wins1++; else wins2++;
            } else {
                pending = true;
                live |= "Live".equals(map.status());
            }
            maps.add(new MapResult(map.gameId(), name, map.order(), map.team1Score(), map.team2Score(), map.status()));
        }
        UUID winner = wins1 == required ? team1 : wins2 == required ? team2 : null;
        String status = winner != null ? "Completed" : live || wins1 + wins2 > 0 ? "Live" : "Upcoming";
        return new Summary(List.copyOf(maps), wins1, wins2, status, winner);
    }
}
