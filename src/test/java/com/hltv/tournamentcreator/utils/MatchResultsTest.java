package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.MapResult;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class MatchResultsTest {
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    private MapResult map(int order, int first, int second, String status) {
        return new MapResult(UUID.randomUUID(), "Map " + order, order, first, second, status);
    }

    @Test void bo3UsesMapWinsRatherThanRoundTotals() {
        var summary = MatchResults.evaluate("Bo3", a, b, List.of(map(1,13,11,"Completed"), map(2,0,13,"Completed"), map(3,13,11,"Completed")));
        assertEquals(a, summary.winnerId()); assertEquals(2, summary.team1Maps());
        assertEquals(1, summary.team2Maps()); assertEquals("Completed", summary.status());
    }
    @Test void bo1FinishesAfterOneMap() {
        var summary = MatchResults.evaluate("Bo1", a, b, List.of(map(1,10,13,"Completed")));
        assertEquals(b, summary.winnerId()); assertEquals("Completed", summary.status());
    }
    @Test void bo5RequiresThreeWins() {
        var twoWins = MatchResults.evaluate("Bo5", a, b, List.of(map(1,13,5,"Completed"), map(2,13,9,"Completed")));
        assertNull(twoWins.winnerId()); assertEquals("Live", twoWins.status());
        var threeWins = MatchResults.evaluate("Bo5", a, b, List.of(map(1,13,5,"Completed"), map(2,13,9,"Completed"), map(3,16,14,"Completed")));
        assertEquals(a, threeWins.winnerId());
    }
    @Test void incompleteOrLiveMapsDoNotAwardWins() {
        var summary = MatchResults.evaluate("Bo3", a, b, List.of(map(1,12,3,"Live"), map(2,0,0,"Upcoming")));
        assertEquals("Live", summary.status()); assertNull(summary.winnerId()); assertEquals(0, summary.team1Maps());
    }
    @Test void emptyMapsRemainUnscheduledAndAreNotPersisted() {
        var summary = MatchResults.evaluate("Bo3", a, b, List.of(new MapResult(UUID.randomUUID(), " ", 1,0,0,"Upcoming")));
        assertEquals("Upcoming", summary.status()); assertTrue(summary.maps().isEmpty());
    }
    @Test void completedMapsCannotTieAndScoresCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo1", a,b,List.of(map(1,12,12,"Completed"))));
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo1", a,b,List.of(map(1,-1,13,"Completed"))));
    }
    @Test void scoresCannotBeHiddenInUpcomingMap() {
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo1", a,b,List.of(map(1,13,10,"Upcoming"))));
    }
    @Test void cannotPlayLaterMapBeforeEarlierOneFinishes() {
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo3", a,b,List.of(map(1,10,3,"Live"), map(2,13,5,"Completed"))));
    }
    @Test void cannotPlayAfterSeriesIsAlreadyWon() {
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo3", a,b,List.of(map(1,13,5,"Completed"), map(2,13,5,"Completed"), map(3,1,0,"Live"))));
        assertDoesNotThrow(() -> MatchResults.evaluate("Bo3", a,b,List.of(map(1,13,5,"Completed"), map(2,13,5,"Completed"), map(3,0,0,"Upcoming"))));
    }
    @Test void validatesFormatsOpponentsAndMapOrder() {
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo2", a,b,List.of()));
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo1", a,a,List.of()));
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo1", a,null,List.of()));
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo3", a,b,List.of(map(2,13,5,"Completed"))));
        assertThrows(IllegalArgumentException.class, () -> MatchResults.evaluate("Bo1", a,b,List.of(map(1,13,5,"Completed"),map(2,13,5,"Completed"))));
    }
}
