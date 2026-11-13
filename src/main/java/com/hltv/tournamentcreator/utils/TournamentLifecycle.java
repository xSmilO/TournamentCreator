package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.*;
import java.util.*;

public final class TournamentLifecycle {
    private TournamentLifecycle() {}
    public record State(String state, boolean archived) {
        public boolean terminal() { return "Completed".equals(state) || "Cancelled".equals(state); }
        public boolean canChangeTeamLimit() { return !archived && ("Draft".equals(state) || "Registration".equals(state)); }
        public boolean editable() { return !archived && state!=null && Set.of("Draft","Registration","In progress").contains(state); }
    }
    public static void requireEditable(State state) {
        if (!state.editable()) throw new IllegalArgumentException("This tournament is completed, cancelled, or archived. Reopen it from Event control before making changes.");
    }
    public static void requireRunning(State state) {
        requireEditable(state);
        if (!"In progress".equals(state.state())) throw new IllegalArgumentException("Start the tournament from Event control before entering results.");
    }

    /** Shared places reflect elimination rounds; no fabricated third-place playoff. */
    public static Map<UUID,Integer> placements(List<UUID> entrants, List<TournamentGroup> groups, List<TournamentMatch> matches) {
        if (entrants.size()<2 || new HashSet<>(entrants).size()!=entrants.size()) throw new IllegalArgumentException("At least two distinct registered teams are required.");
        if (matches.isEmpty() || matches.stream().anyMatch(m -> !"Completed".equals(m.status())))
            throw new IllegalArgumentException("Resolve every match before finishing the tournament.");
        List<TournamentMatch> bracket=matches.stream().filter(m -> SingleElimination.STAGE.equals(m.stage())).toList();
        List<TournamentMatch> finals=bracket.stream().filter(m -> m.nextMatchId()==null).toList();
        if (finals.size()!=1) throw new IllegalArgumentException("A single elimination final is required to determine the champion.");
        Map<UUID,TournamentMatch> byId=new HashMap<>();
        Set<UUID> playing=new HashSet<>();
        Set<String> slots=new HashSet<>();
        for (var m:bracket) {
            if (byId.put(m.matchId(),m)!=null || m.cancelled() || m.winnerId()==null
                || !Objects.equals(m.winnerId(),m.team1Id()) && !Objects.equals(m.winnerId(),m.team2Id()))
                throw new IllegalArgumentException("Every bracket match needs a valid winner.");
            if (m.team1Id()!=null) playing.add(m.team1Id());
            if (m.team2Id()!=null) playing.add(m.team2Id());
            if (m.team1Id()!=null && m.team1Id().equals(m.team2Id())) throw new IllegalArgumentException("A team cannot play itself.");
            if (!"Bye".equals(m.outcome()) && (m.team1Id()==null || m.team2Id()==null)) throw new IllegalArgumentException("A played or awarded match requires two opponents.");
            if ("Bye".equals(m.outcome()) && (m.team1Id()==null)==(m.team2Id()==null)) throw new IllegalArgumentException("A bye requires exactly one opponent.");
            if (m.awarded() && (m.team1Maps()!=0 || m.team2Maps()!=0)) throw new IllegalArgumentException("An awarded result cannot contain map wins.");
            if ("Played".equals(m.outcome())) {
                int needed=MatchResults.mapLimit(m.format())/2+1;
                int wins=Objects.equals(m.winnerId(),m.team1Id()) ? m.team1Maps():m.team2Maps();
                int losses=Objects.equals(m.winnerId(),m.team1Id()) ? m.team2Maps():m.team1Maps();
                if (wins!=needed || losses<0 || losses>=needed) throw new IllegalArgumentException("A bracket result has inconsistent map wins.");
            }
        }
        Set<UUID> registered=new HashSet<>(entrants);
        if (!registered.containsAll(playing)) throw new IllegalArgumentException("The bracket includes an unregistered team.");
        if (!groups.isEmpty()) {
            if (!new HashSet<>(GroupStage.playoffSeeds(groups,matches)).equals(playing))
                throw new IllegalArgumentException("The bracket does not match the group qualifiers.");
        } else if (!playing.equals(registered)) throw new IllegalArgumentException("Every registered team must appear in the bracket.");
        Map<UUID,Integer> places=new HashMap<>();
        UUID finalId=finals.getFirst().matchId();
        places.put(finals.getFirst().winnerId(),1);
        for (var m:bracket) {
            var current=m; Set<UUID> visited=new HashSet<>(); int depth=0;
            while (current.nextMatchId()!=null) {
                if (!visited.add(current.matchId()) || ++depth>8) throw new IllegalArgumentException("Invalid or cyclic bracket links.");
                var next=byId.get(current.nextMatchId());
                if (next==null || current.nextSlot()==null || current.nextSlot()<1 || current.nextSlot()>2
                    || !Objects.equals(current.winnerId(),current.nextSlot()==1 ? next.team1Id():next.team2Id()))
                    throw new IllegalArgumentException("A winner has not advanced to the correct bracket slot.");
                current=next;
            }
            if (!current.matchId().equals(finalId)) throw new IllegalArgumentException("The bracket is disconnected.");
            if (m.nextMatchId()!=null && !slots.add(m.nextMatchId()+":"+m.nextSlot())) throw new IllegalArgumentException("Two matches feed the same bracket slot.");
            UUID loser=Objects.equals(m.winnerId(),m.team1Id()) ? m.team2Id():m.team1Id();
            if (loser!=null && places.putIfAbsent(loser,(1<<depth)+1)!=null) throw new IllegalArgumentException("A team has multiple elimination results.");
        }
        for (UUID team:entrants) if (!playing.contains(team)) places.put(team,playing.size()+1);
        if (!places.keySet().equals(registered)) throw new IllegalArgumentException("The final standings are incomplete.");
        return Map.copyOf(places);
    }
}
