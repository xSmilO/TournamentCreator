package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.*;
import java.util.*;

public final class GroupStage {
    public static final String STAGE = "Group stage";
    private GroupStage() {}
    public record Fixture(UUID matchId, UUID groupId, UUID team1, UUID team2, int round) {}

    /** Snake-seeded pots distribute the highest seeds across different groups. */
    public static List<TournamentGroup> draw(List<TournamentParticipant> entrants) {
        if (entrants.size() < 4 || entrants.size() > 256 || entrants.size() % 4 != 0)
            throw new IllegalArgumentException("Groups require 4–256 registered teams, in multiples of four.");
        Set<UUID> ids = new HashSet<>();
        for (var team : entrants) if (team.teamId() == null || !ids.add(team.teamId()))
            throw new IllegalArgumentException("Every registered team must be distinct.");
        List<TournamentParticipant> sorted = new ArrayList<>(entrants);
        sorted.sort(Comparator.comparing((TournamentParticipant t) -> t.seed() == null ? Integer.MAX_VALUE : t.seed()).thenComparing(TournamentParticipant::teamId));
        int count = sorted.size() / 4;
        List<List<TournamentParticipant>> teams = new ArrayList<>();
        for (int i = 0; i < count; i++) teams.add(new ArrayList<>());
        for (int i = 0; i < sorted.size(); i++) {
            int pot = i / count, position = i % count;
            teams.get(pot % 2 == 0 ? position : count - position - 1).add(sorted.get(i));
        }
        List<TournamentGroup> groups = new ArrayList<>();
        for (int i = 0; i < count; i++) groups.add(new TournamentGroup(UUID.randomUUID(), "Group " + letters(i), i + 1, teams.get(i)));
        return List.copyOf(groups);
    }

    private static String letters(int index) {
        StringBuilder result = new StringBuilder();
        for (int n = index + 1; n > 0; n = (n - 1) / 26) result.insert(0, (char) ('A' + (n - 1) % 26));
        return result.toString();
    }

    public static List<Fixture> fixtures(TournamentGroup group) {
        if (group.teams().size() != 4) throw new IllegalArgumentException("Each group needs exactly four teams.");
        List<UUID> rotation = new ArrayList<>(group.teams().stream().map(TournamentParticipant::teamId).toList());
        List<Fixture> fixtures = new ArrayList<>();
        for (int round = 1; round <= 3; round++) {
            fixtures.add(new Fixture(UUID.randomUUID(), group.groupId(), rotation.get(0), rotation.get(3), round));
            fixtures.add(new Fixture(UUID.randomUUID(), group.groupId(), rotation.get(1), rotation.get(2), round));
            rotation.add(1, rotation.remove(3));
        }
        return List.copyOf(fixtures);
    }

    public static List<GroupStanding> standings(TournamentGroup group, List<TournamentMatch> matches) {
        Map<UUID, int[]> stats = new HashMap<>();
        for (var team : group.teams()) stats.put(team.teamId(), new int[5]); // played, wins, losses, maps won, maps lost
        List<TournamentMatch> completed = new ArrayList<>();
        Set<Set<UUID>> pairs = new HashSet<>();
        for (var match : matches) if (group.groupId().equals(match.groupId())) {
            if (match.team1Id() == null || match.team2Id() == null || match.team1Id().equals(match.team2Id())
                    || !stats.containsKey(match.team1Id()) || !stats.containsKey(match.team2Id()))
                throw new IllegalArgumentException("A group match contains invalid opponents.");
            if (!pairs.add(Set.of(match.team1Id(), match.team2Id()))) throw new IllegalArgumentException("A group contains duplicate fixtures.");
            if (!"Completed".equals(match.status())) continue;
            int required = MatchResults.mapLimit(match.format()) / 2 + 1;
            boolean firstWon = match.team1Maps() == required && match.team2Maps() >= 0 && match.team2Maps() < required;
            boolean secondWon = match.team2Maps() == required && match.team1Maps() >= 0 && match.team1Maps() < required;
            if ((!firstWon && !secondWon) || !Objects.equals(match.winnerId(), firstWon ? match.team1Id() : match.team2Id()))
                throw new IllegalArgumentException("A completed group match has inconsistent map scores or winner.");
            int[] a = stats.get(match.team1Id()), b = stats.get(match.team2Id());
            a[0]++; b[0]++;
            a[firstWon ? 1 : 2]++; b[firstWon ? 2 : 1]++;
            a[3] += match.team1Maps(); a[4] += match.team2Maps(); b[3] += match.team2Maps(); b[4] += match.team1Maps();
            completed.add(match);
        }
        Map<UUID,Integer> miniLeague = new HashMap<>();
        for (var match : completed) if (stats.get(match.team1Id())[1] == stats.get(match.team2Id())[1])
            miniLeague.merge(match.winnerId(), 3, Integer::sum);
        List<GroupStanding> standings = new ArrayList<>();
        for (var team : group.teams()) {
            int[] s = stats.get(team.teamId());
            standings.add(new GroupStanding(team.teamId(), team.name(), team.seed() == null ? Integer.MAX_VALUE : team.seed(),
                    s[0], s[1], s[2], s[3], s[4], s[1] * 3, miniLeague.getOrDefault(team.teamId(), 0)));
        }
        standings.sort(Comparator.comparingInt(GroupStanding::points).reversed()
                .thenComparing(Comparator.comparingInt(GroupStanding::headToHeadPoints).reversed())
                .thenComparing(Comparator.comparingInt(GroupStanding::mapDifference).reversed())
                .thenComparing(Comparator.comparingInt(GroupStanding::mapsWon).reversed())
                .thenComparingInt(GroupStanding::seed).thenComparing(GroupStanding::teamId));
        return List.copyOf(standings);
    }

    public static boolean complete(List<TournamentGroup> groups, List<TournamentMatch> matches) {
        if (groups.isEmpty()) return false;
        for (var group : groups) {
            if (group.teams().size() != 4) return false;
            standings(group, matches); // Validate fixture uniqueness and completed results.
            List<TournamentMatch> fixtures = matches.stream().filter(m -> group.groupId().equals(m.groupId())).toList();
            if (fixtures.size() != 6 || fixtures.stream().anyMatch(m -> !"Completed".equals(m.status()))) return false;
        }
        return true;
    }

    /** Winners get the higher seeds; each runner-up goes in the opposite half to its group winner. */
    public static List<UUID> playoffSeeds(List<TournamentGroup> groups, List<TournamentMatch> matches) {
        if (!complete(groups, matches)) throw new IllegalArgumentException("Complete all six matches in every group before generating playoffs.");
        List<TournamentGroup> ordered = groups.stream().sorted(Comparator.comparingInt(TournamentGroup::order)).toList();
        int count = groups.size(), size = 2;
        while (size < count * 2) size *= 2;
        List<Integer> positions = SingleElimination.seedOrder(size);
        List<UUID> seeds = new ArrayList<>(Collections.nCopies(count * 2, null));
        List<Deque<Integer>> slots = List.of(new ArrayDeque<>(), new ArrayDeque<>());
        for (int seed = count + 1; seed <= count * 2; seed++) slots.get(positions.indexOf(seed) < size / 2 ? 0 : 1).add(seed);
        for (int i = 0; i < count; i++) {
            List<GroupStanding> table = standings(ordered.get(i), matches);
            seeds.set(i, table.get(0).teamId());
            int half = positions.indexOf(i + 1) < size / 2 ? 0 : 1;
            Integer runnerSeed = slots.get(1 - half).pollFirst();
            if (runnerSeed == null) throw new IllegalArgumentException("Could not separate group qualifiers into opposite bracket halves.");
            seeds.set(runnerSeed - 1, table.get(1).teamId());
        }
        return List.copyOf(seeds);
    }
}
