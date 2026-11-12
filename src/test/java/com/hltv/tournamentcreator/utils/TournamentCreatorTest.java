package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.MapResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TournamentCreatorTest {
    @Test
    void trimsTeamName() {
        assertEquals("Alpha", CreatorValidation.text("  Alpha  ", "Team name", 100, true));
    }

    @Test
    void rejectsEmptyTeamName() {
        assertThrows(IllegalArgumentException.class,
                () -> CreatorValidation.text(" ", "Team name", 100, true));
    }

    @Test
    void acceptsEmptyWorldRanking() {
        assertNull(CreatorValidation.ranking(""));
    }

    @Test
    void rejectsZeroWorldRanking() {
        assertThrows(IllegalArgumentException.class, () -> CreatorValidation.ranking("0"));
    }

    @Test
    void acceptsMaximumTeamLimit() {
        assertEquals(256, CreatorValidation.teamLimit("256"));
    }

    @Test
    void rejectsTeamLimitAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> CreatorValidation.teamLimit("257"));
    }

    @Test
    void rejectsTournamentEndBeforeStart() {
        LocalDate start = LocalDate.of(2026, 11, 12);
        assertThrows(IllegalArgumentException.class,
                () -> CreatorValidation.dates(start, start.minusDays(1)));
    }

    @Test
    void bestOfFiveAllowsFiveMaps() {
        assertEquals(5, MatchResults.mapLimit("Bo5"));
    }

    @Test
    void winningOneMapWinsABestOfOneMatch() {
        UUID firstTeam = UUID.randomUUID();
        var map = new MapResult(UUID.randomUUID(), "Mirage", 1, 13, 8, "Completed");

        var result = MatchResults.evaluate("Bo1", firstTeam, UUID.randomUUID(), List.of(map));

        assertEquals(firstTeam, result.winnerId());
    }

    @Test
    void twoTeamsGenerateOneFinal() {
        var bracket = SingleElimination.generate(List.of(UUID.randomUUID(), UUID.randomUUID()));

        assertEquals(1, bracket.size());
        assertEquals("Final", bracket.getFirst().round());
    }
}
