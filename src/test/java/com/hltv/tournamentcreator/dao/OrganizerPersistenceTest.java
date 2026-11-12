package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OrganizerPersistenceTest {
    private final UUID id = UUID.randomUUID(), team = UUID.randomUUID();
    private MatchPersistenceTest.Jdbc jdbc() {
        var jdbc = new MatchPersistenceTest.Jdbc();
        jdbc.query = sql -> sql.startsWith("SELECT tournament_id") ? List.of(Map.of("tournament_id", id)) : List.of();
        return jdbc;
    }
    private Player player() { return new Player(id, "nick", null, null, null, null, null, null, null, null); }
    private RosterPlayer roster() { return new RosterPlayer(id, "nick", null, null, null, null, "IGL"); }
    private Tournament tournament(LocalDate start) { return new Tournament(id, "Cup", null, start, null, null, null, "Custom", 4, 16); }
    private Tournament changed(Tournament t, LocalDate start, String format) { return new Tournament(id, "New cup", "A-Tier", start, null, new BigDecimal("100.00"), "Warsaw", format, 4, 16); }
    private TournamentMatch match() { return new TournamentMatch(UUID.randomUUID(), id, team, "A", UUID.randomUUID(), "B", "Bo3", "Upcoming", null, "Single elimination", "Final", null, 0, 0, null, null); }

    @Test void playerEditUsesOriginalNullableFieldsAndDoesNotTouchTeamOrStats() throws Exception {
        var j=jdbc(); new OrganizerDAO(j::connection).updatePlayer(player(), " new ", "First", "Last", new Country(1,"Poland","PL"), LocalDate.of(2000,1,1));
        var update=j.updates.getFirst();
        assertEquals("new", update.parameters().get(1)); assertEquals(id,update.parameters().get(6));
        assertEquals("nick",update.parameters().get(7)); assertNull(update.parameters().get(8));
        assertTrue(update.sql().contains("country_id IS NOT DISTINCT FROM ?"));
        assertFalse(update.sql().contains("team_id")); assertFalse(update.sql().contains("hltv_rating"));
    }
    @Test void invalidPlayerDateRejectedBeforeDatabase() {
        var dao=new OrganizerDAO(() -> { throw new AssertionError("No connection expected"); });
        assertThrows(IllegalArgumentException.class, () -> dao.updatePlayer(player(),"N",null,null,null,LocalDate.now().plusDays(1)));
    }
    @Test void stalePlayerAndTeamEditsAreRejected() {
        var j=jdbc(); j.updateCount=0; var dao=new OrganizerDAO(j::connection);
        assertEquals("P0001",assertThrows(SQLException.class, () -> dao.updatePlayer(player(),"New",null,null,null,null)).getSQLState());
        assertThrows(SQLException.class, () -> dao.updateTeam(new Team(team,"Old",null),"New",1));
    }
    @Test void teamCanBeRenamedAndUnrankedWithoutTouchingRoster() throws Exception {
        var j=jdbc(); new OrganizerDAO(j::connection).updateTeam(new Team(team,"Old",2),"New",null);
        var values=j.updates.getFirst().parameters();
        assertNull(values.get(2)); assertEquals(team,values.get(3)); assertEquals("Old",values.get(4)); assertEquals(2,values.get(5));
    }
    @Test void releaseClearsBothTeamAndRoleInOneWrite() throws Exception {
        var j=jdbc(); new OrganizerDAO(j::connection).movePlayer(team,roster(),null,"ignored");
        assertEquals(1,j.updates.size()); var values=j.updates.getFirst().parameters();
        assertNull(values.get(1)); assertNull(values.get(2)); assertEquals(team,values.get(4)); assertEquals("IGL",values.get(5));
    }
    @Test void transferWritesDestinationAndRoleTogether() throws Exception {
        var j=jdbc(); UUID destination=UUID.randomUUID();
        new OrganizerDAO(j::connection).movePlayer(team,roster(),destination," AWPer ");
        assertEquals(1,j.updates.size()); assertEquals(destination,j.updates.getFirst().parameters().get(1));
        assertEquals("AWPer",j.updates.getFirst().parameters().get(2));
    }
    @Test void staleTransferAndReleaseAreRejected() {
        var j=jdbc(); j.updateCount=0; var dao=new OrganizerDAO(j::connection);
        assertThrows(SQLException.class, () -> dao.movePlayer(team,roster(),UUID.randomUUID(),"AWPer"));
        assertThrows(SQLException.class, () -> dao.movePlayer(team,roster(),null,null));
    }
    @Test void invalidTransferCannotWrite() {
        var dao=new OrganizerDAO(() -> { throw new AssertionError("No connection expected"); });
        assertThrows(IllegalArgumentException.class, () -> dao.movePlayer(team,roster(),team,"AWPer"));
        assertThrows(IllegalArgumentException.class, () -> dao.movePlayer(team,roster(),UUID.randomUUID()," "));
    }
    @Test void tournamentMetadataEditableAfterStart() throws Exception {
        var j=jdbc(); var old=tournament(LocalDate.now().minusDays(1));
        new OrganizerDAO(j::connection).updateTournament(old,changed(old,old.startDate(),old.format()));
        assertTrue(j.committed); assertFalse(j.updates.getFirst().sql().contains("team_limit"));
    }
    @Test void tournamentDatesLockedAfterStart() {
        var j=jdbc(); var old=tournament(LocalDate.now());
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateTournament(old,changed(old,LocalDate.now().plusDays(1),old.format())));
        assertTrue(j.rolledBack); assertTrue(j.updates.isEmpty());
    }
    @Test void tournamentFormatLockedAfterDraw() {
        var j=jdbc(); j.query=sql -> List.of(Map.of("tournament_id",id));
        var old=tournament(LocalDate.now().plusDays(1));
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateTournament(old,changed(old,old.startDate(),"New format")));
        assertTrue(j.rolledBack); assertTrue(j.updates.isEmpty());
    }
    @Test void tournamentDatesAndFormatEditableBeforeDraw() throws Exception {
        var j=jdbc(); var old=tournament(null);
        new OrganizerDAO(j::connection).updateTournament(old,changed(old,LocalDate.now().plusDays(2),"New format"));
        assertTrue(j.committed); assertEquals(15,j.updates.getFirst().parameters().size());
    }
    @Test void staleTournamentRollsBack() {
        var j=jdbc(); j.updateCount=0; var old=tournament(null);
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateTournament(old,changed(old,null,old.format())));
        assertTrue(j.rolledBack); assertFalse(j.committed);
    }
    @Test void seedEditCommitsAndChecksPreviousSeed() throws Exception {
        var j=jdbc(); new OrganizerDAO(j::connection).updateSeed(id,new TournamentParticipant(team,"A",2),3);
        assertTrue(j.committed); assertEquals(2,j.updates.getFirst().parameters().get(4));
    }
    @Test void duplicateSeedIsRejected() {
        var j=jdbc(); j.query=sql -> sql.contains("UNION ALL") ? List.of() : List.of(Map.of("tournament_id",id));
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateSeed(id,new TournamentParticipant(team,"A",2),1));
        assertTrue(j.rolledBack); assertTrue(j.updates.isEmpty());
    }
    @Test void seedLockedAfterCompetitionGenerated() {
        var j=jdbc(); j.query=sql -> List.of(Map.of("tournament_id",id));
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateSeed(id,new TournamentParticipant(team,"A",2),3));
        assertTrue(j.rolledBack); assertTrue(j.updates.isEmpty());
    }
    @Test void staleSeedIsRejected() {
        var j=jdbc(); j.updateCount=0;
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateSeed(id,new TournamentParticipant(team,"A",2),3));
        assertTrue(j.rolledBack);
    }
    @Test void scheduleWritesOnlyTimeWithStatusAndOriginalTimeGuard() throws Exception {
        var j=jdbc(); var when=LocalDateTime.of(2027,1,2,18,30);
        new OrganizerDAO(j::connection).updateSchedule(match(),when);
        assertTrue(j.committed); var update=j.updates.getFirst();
        assertEquals(when,update.parameters().get(1)); assertNull(update.parameters().get(4));
        assertTrue(update.sql().contains("status='Upcoming'")); assertTrue(update.sql().contains("start_time IS NOT DISTINCT FROM ?"));
    }
    @Test void scheduleCanBeCleared() throws Exception {
        var j=jdbc(); new OrganizerDAO(j::connection).updateSchedule(match(),null);
        assertTrue(j.committed); assertNull(j.updates.getFirst().parameters().get(1));
    }
    @Test void staleOrStartedMatchCannotBeRescheduled() {
        var j=jdbc(); j.updateCount=0;
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateSchedule(match(),null));
        assertTrue(j.rolledBack); assertFalse(j.committed);
    }
    @Test void scheduleFailureRollsBack() {
        var j=jdbc(); j.failUpdate="UPDATE winit.matches";
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateSchedule(match(),null));
        assertTrue(j.rolledBack); assertFalse(j.committed);
    }

    @Test void formatChangeCommitsWithNewMapLimit() throws Exception {
        var j = jdbc();
        TournamentMatch match = match();
        new OrganizerDAO(j::connection).updateMatchFormat(match, "Bo5");
        assertTrue(j.committed);
        assertEquals("Bo5", j.updates.getFirst().parameters().get(1));
        assertEquals(match.matchId(), j.updates.getFirst().parameters().get(2));
        assertEquals("Bo3", j.updates.getFirst().parameters().get(4));
        assertEquals(5, j.updates.getLast().parameters().get(2));
    }

    @Test void rejectedFormatChangeLeavesPlannedMapsAlone() {
        var j = jdbc();
        j.updateCount = 0;
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateMatchFormat(match(), "Bo1"));
        assertTrue(j.rolledBack);
        assertEquals(1, j.updates.size());
    }

    @Test void failedMapCleanupRollsBackFormatChange() {
        var j = jdbc();
        j.failUpdate = "DELETE FROM winit.games";
        assertThrows(SQLException.class, () -> new OrganizerDAO(j::connection).updateMatchFormat(match(), "Bo1"));
        assertTrue(j.rolledBack);
        assertFalse(j.committed);
    }
    @Test void playerNumericMappingPreservesNullZeroAndDecimals() throws Exception {
        var j=jdbc(); var missing=new HashMap<String,Object>(); missing.put("player_id",id);
        var zero=new HashMap<>(missing); zero.put("hltv_rating",BigDecimal.ZERO); zero.put("headshot_percentage",new BigDecimal("52.35"));
        j.query=sql -> List.of(missing,zero);
        var players=new PlayerDAO(j::connection).getAllPlayers();
        assertNull(players.get(0).hltvRating()); assertNull(players.get(0).headshotPercentage());
        assertEquals(0.0,players.get(1).hltvRating()); assertEquals(52.35,players.get(1).headshotPercentage());
    }
}
