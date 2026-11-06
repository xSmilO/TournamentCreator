package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.BracketMatch;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class SingleEliminationTest {
    private List<UUID> teams(int count) { return IntStream.range(0,count).mapToObj(i -> UUID.randomUUID()).toList(); }

    @Test void seededEightTeamBracketHasSevenMatchesAndOneFinal() {
        List<UUID> teams = teams(8);
        var bracket = SingleElimination.generate(teams);
        assertEquals(7,bracket.size());
        assertEquals(1,bracket.stream().filter(m -> m.nextMatchId()==null).count());
        assertEquals(teams.get(0),bracket.get(0).team1Id()); assertEquals(teams.get(7),bracket.get(0).team2Id());
        assertEquals(teams.get(3),bracket.get(1).team1Id()); assertEquals(teams.get(4),bracket.get(1).team2Id());
        assertEquals(bracket.get(0).nextMatchId(),bracket.get(1).nextMatchId());
        assertEquals(1,bracket.get(0).nextSlot()); assertEquals(2,bracket.get(1).nextSlot());
    }
    @Test void fiveTeamsReceiveThreeByesAndAdvanceImmediately() {
        var bracket = SingleElimination.generate(teams(5));
        assertEquals(3,bracket.stream().filter(m -> m.winnerId()!=null).count());
        Map<UUID,BracketMatch> byId = new HashMap<>(); bracket.forEach(m -> byId.put(m.matchId(),m));
        for (BracketMatch match:bracket) if (match.winnerId()!=null) {
            BracketMatch next = byId.get(match.nextMatchId());
            assertEquals(match.winnerId(),match.nextSlot()==1 ? next.team1Id() : next.team2Id());
        }
    }
    @Test void everyTeamAppearsOnceInFirstRoundAndEveryLinkTargetsExistingMatch() {
        for (int count=2;count<=32;count++) {
            List<UUID> teams = teams(count);
            var bracket = SingleElimination.generate(teams);
            Set<UUID> ids = new HashSet<>(); bracket.forEach(m -> assertTrue(ids.add(m.matchId())));
            Set<UUID> firstRound = new HashSet<>(); String round = bracket.get(0).round();
            for (BracketMatch m:bracket) {
                if (m.round().equals(round)) {
                    if(m.team1Id()!=null) assertTrue(firstRound.add(m.team1Id()));
                    if(m.team2Id()!=null) assertTrue(firstRound.add(m.team2Id()));
                }
                if(m.nextMatchId()!=null) { assertTrue(ids.contains(m.nextMatchId())); assertNotEquals(m.matchId(),m.nextMatchId()); }
            }
            assertEquals(new HashSet<>(teams),firstRound);
        }
    }
    @Test void rejectsTooFewOrDuplicateTeams() {
        assertThrows(IllegalArgumentException.class, () -> SingleElimination.generate(teams(1)));
        UUID team=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> SingleElimination.generate(List.of(team,team)));
    }
}
