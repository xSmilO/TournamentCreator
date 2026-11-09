package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LifecyclePersistenceTest {
    private final UUID event=UUID.randomUUID(),match=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
    private Map<String,Object> row(Object... pairs){var result=new HashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);return result;}
    private TournamentMatch fixture(String status,String outcome,UUID winner){return new TournamentMatch(match,event,a,"A",b,"B","Bo1",status,null,SingleElimination.STAGE,"Final",winner,"Played".equals(outcome)&&winner!=null?1:0,0,null,null,null,outcome,null);}
    private Map<String,Object> matchRow(TournamentMatch m){return row("match_id",m.matchId(),"tournament_id",event,"team1_id",m.team1Id(),"team2_id",m.team2Id(),"team1_name","A","team2_name","B","match_format",m.format(),"status",m.status(),"stage",m.stage(),"round_name",m.round(),"winner_id",m.winnerId(),"score1",m.team1Maps(),"score2",m.team2Maps(),"next_match_id",m.nextMatchId(),"next_match_slot",m.nextSlot(),"outcome_type",m.outcome(),"outcome_reason",m.outcomeReason(),"group_id",m.groupId());}
    private MatchPersistenceTest.Jdbc jdbc(String state,boolean archived,List<TournamentMatch> matches){
        var j=new MatchPersistenceTest.Jdbc();j.query=sql -> {
            if(sql.startsWith("SELECT tournament_id") || sql.startsWith("SELECT start_date"))return List.of(row("tournament_id",event,"lifecycle_state",state,"archived",archived));
            if(sql.startsWith("SELECT m.*"))return matches.stream().map(this::matchRow).toList();
            if(sql.startsWith("SELECT team_id"))return List.of(row("team_id",a),row("team_id",b));
            if(sql.startsWith("SELECT g.group_id") || sql.startsWith("SELECT 1") || sql.startsWith("SELECT * FROM winit.games"))return List.of();
            throw new AssertionError(sql);
        };return j;
    }
    private void act(MatchPersistenceTest.Jdbc j,String state,String action)throws SQLException{new LifecycleDAO(j::connection).act(event,new TournamentLifecycle.State(state,false),action,"Organizer decision");}
    @Test void registrationOpensExplicitlyAndIsAudited()throws Exception{
        var j=jdbc("Draft",false,List.of());act(j,"Draft","Open registration");assertTrue(j.committed);
        assertEquals("Registration",j.updates.getFirst().parameters().get(1));assertTrue(j.updates.getLast().sql().contains("tournament_audit"));
    }
    @Test void registrationCannotReopenOverExistingDraw(){var j=jdbc("Draft",false,List.of(fixture("Upcoming","Played",null)));assertThrows(SQLException.class,() -> act(j,"Draft","Open registration"));assertTrue(j.rolledBack);assertTrue(j.updates.isEmpty());}
    @Test void startRequiresFixtures(){var j=jdbc("Draft",false,List.of());assertThrows(SQLException.class,() -> act(j,"Draft","Start tournament"));assertTrue(j.rolledBack);}
    @Test void startSetsInProgress()throws Exception{var j=jdbc("Draft",false,List.of(fixture("Upcoming","Played",null)));act(j,"Draft","Start tournament");assertEquals("In progress",j.updates.getFirst().parameters().get(1));assertTrue(j.committed);}
    @Test void finishSavesPlacementsAndStateAtomically()throws Exception{
        var j=jdbc("In progress",false,List.of(fixture("Completed","Played",a)));act(j,"In progress","Finish tournament");
        var places=j.updates.stream().filter(u -> u.sql().contains("SET placement=?")).toList();assertEquals(2,places.size());
        assertTrue(places.stream().anyMatch(u -> u.parameters().get(1).equals(1)&&u.parameters().get(3).equals(a)));
        assertTrue(j.updates.stream().anyMatch(u -> "Completed".equals(u.parameters().get(1))));assertTrue(j.committed);
    }
    @Test void finishFailureRollsBackPlacementsAndState(){var j=jdbc("In progress",false,List.of(fixture("Completed","Played",a)));j.failUpdate="INSERT INTO winit.tournament_audit";assertThrows(SQLException.class,() -> act(j,"In progress","Finish tournament"));assertTrue(j.rolledBack);assertFalse(j.committed);}
    @Test void unfinishedFinalCannotFinish(){var j=jdbc("In progress",false,List.of(fixture("Upcoming","Played",null)));assertThrows(IllegalArgumentException.class,() -> act(j,"In progress","Finish tournament"));assertTrue(j.updates.isEmpty());assertTrue(j.rolledBack);}
    @Test void reopenClearsPlacementsBeforeUnlocking()throws Exception{var j=jdbc("Completed",false,List.of(fixture("Completed","Played",a)));act(j,"Completed","Reopen tournament");assertTrue(j.updates.getFirst().sql().contains("placement=NULL"));assertEquals("In progress",j.updates.get(1).parameters().get(1));assertTrue(j.committed);}
    @Test void cancelKeepsMatchHistoryAndClearsPlacements()throws Exception{var j=jdbc("In progress",false,List.of(fixture("Upcoming","Played",null)));act(j,"In progress","Cancel tournament");assertFalse(j.updates.stream().anyMatch(u -> u.sql().startsWith("DELETE")));assertEquals("Cancelled",j.updates.get(1).parameters().get(1));}
    @Test void archiveRequiresTerminalState(){var j=jdbc("In progress",false,List.of());assertThrows(SQLException.class,() -> act(j,"In progress","Archive"));assertTrue(j.rolledBack);}
    @Test void archivedEventCannotReopenUntilUnarchived(){var j=jdbc("Completed",true,List.of());assertThrows(SQLException.class,() -> new LifecycleDAO(j::connection).act(event,new TournamentLifecycle.State("Completed",true),"Reopen tournament","Correction"));assertTrue(j.rolledBack);}
    @Test void staleLifecycleWindowCannotChangeState(){var j=jdbc("Completed",false,List.of());assertThrows(SQLException.class,() -> act(j,"Draft","Open registration"));assertTrue(j.updates.isEmpty());}
    @Test void missingReasonRejectedBeforeConnection(){assertThrows(IllegalArgumentException.class,() -> new LifecycleDAO(() -> {throw new AssertionError();}).act(event,new TournamentLifecycle.State("Draft",false),"Reset draw"," "));}
    @Test void resetUnplayedFixturesKeepsRegistrations()throws Exception{var j=jdbc("Draft",false,List.of(fixture("Upcoming","Played",null)));act(j,"Draft","Reset draw");assertTrue(j.committed);assertTrue(j.updates.stream().anyMatch(u -> u.sql().startsWith("DELETE FROM winit.matches")));assertFalse(j.updates.stream().anyMatch(u -> u.sql().startsWith("DELETE FROM winit.tournament_participants")));}
    @Test void resetCannotEraseAwardedOrStartedMatches(){for(var m:List.of(fixture("Completed","Forfeit",a),fixture("Live","Played",null))){var j=jdbc("Draft",false,List.of(m));assertThrows(SQLException.class,() -> act(j,"Draft","Reset draw"));assertTrue(j.updates.isEmpty());}}
    @Test void resetCannotRunAfterEventStarts(){var j=jdbc("In progress",false,List.of(fixture("Upcoming","Played",null)));assertThrows(SQLException.class,() -> act(j,"In progress","Reset draw"));assertTrue(j.updates.isEmpty());}
    @Test void closedEventsBlockExistingWriterEntryPoints(){for(String state:List.of("Completed","Cancelled")){var j=jdbc(state,false,List.of());assertThrows(SQLException.class,() -> new MatchDAO(j::connection).generateBracket(event,"Bo1"));assertThrows(SQLException.class,() -> new TournamentDAO(j::connection).updateTeamLimit(event,16));assertTrue(j.updates.isEmpty());}}
    @Test void forfeitWritesNoGamesAndLogsReason()throws Exception{var m=fixture("Upcoming","Played",null);var j=jdbc("In progress",false,List.of(m));new MatchDAO(j::connection).saveOutcome(m,List.of(),"Forfeit",a,"No show");assertTrue(j.committed);assertFalse(j.updates.stream().anyMatch(u -> u.sql().startsWith("INSERT INTO winit.games")));var update=j.updates.stream().filter(u -> u.sql().startsWith("UPDATE winit.matches")).findFirst().orElseThrow();assertEquals(a,update.parameters().get(2));assertEquals("Forfeit",update.parameters().get(3));assertEquals("No show",update.parameters().get(4));}
    @Test void withdrawalUsesAwardedWinner()throws Exception{var m=fixture("Upcoming","Played",null);var j=jdbc("In progress",false,List.of(m));new MatchDAO(j::connection).saveOutcome(m,List.of(),"Withdrawal",b,"A withdrew from this match");assertTrue(j.committed);assertTrue(j.updates.stream().anyMatch(u -> "Withdrawal".equals(u.parameters().get(3))));}
    @Test void outcomeRequiresStartedEvent(){var m=fixture("Upcoming","Played",null);var j=jdbc("Draft",false,List.of(m));assertThrows(SQLException.class,() -> new MatchDAO(j::connection).saveOutcome(m,List.of(),"Forfeit",a,"No show"));assertTrue(j.updates.isEmpty());}
    @Test void bracketCannotBeCancelledWithoutWinner(){var m=fixture("Upcoming","Played",null);var j=jdbc("In progress",false,List.of(m));assertThrows(SQLException.class,() -> new MatchDAO(j::connection).saveOutcome(m,List.of(),"Cancelled",null,"Cancelled"));assertTrue(j.updates.isEmpty());}
    @Test void staleOutcomeWindowCannotEraseNewResult(){var m=fixture("Upcoming","Played",null);var j=jdbc("In progress",false,List.of(fixture("Completed","Forfeit",a)));assertThrows(SQLException.class,() -> new MatchDAO(j::connection).saveOutcome(m,List.of(),"Forfeit",b,"Wrong winner"));assertTrue(j.updates.isEmpty());}
    @Test void resetAwardRemovesWinnerAndLogsCorrection()throws Exception{var m=fixture("Completed","Forfeit",a);var j=jdbc("In progress",false,List.of(m));new MatchDAO(j::connection).saveOutcome(m,List.of(),"Reset result",null,"No show reversed");var update=j.updates.stream().filter(u -> u.sql().startsWith("UPDATE winit.matches")).findFirst().orElseThrow();assertEquals("Upcoming",update.parameters().get(1));assertNull(update.parameters().get(2));assertEquals("Played",update.parameters().get(3));assertTrue(j.committed);}
    @Test void completedScoreCorrectionRequiresReason(){var m=fixture("Completed","Played",a);assertThrows(IllegalArgumentException.class,() -> new MatchDAO(() -> {throw new AssertionError();}).saveScores(m,List.of(),List.of()));}
    @Test void registrationStateIgnoresCalendarDates()throws Exception{var j=jdbc("Registration",false,List.of());assertTrue(new TournamentDAO(j::connection).isRegistrationOpen(event));var closed=jdbc("Draft",false,List.of());assertFalse(new TournamentDAO(closed::connection).isRegistrationOpen(event));}

    @Test void forfeitAdvancesWinnerAndBlockedDownstreamRollsBack()throws Exception {
        UUID next=UUID.randomUUID();
        var m=new TournamentMatch(match,event,a,"A",b,"B","Bo1","Upcoming",null,SingleElimination.STAGE,"Semifinal",null,0,0,next,1);
        for(boolean started:List.of(false,true)) {
            var j=jdbc("In progress",false,List.of(m));var original=j.query;
            j.query=sql -> sql.contains("AS played") ? List.of(row("tournament_id",event,"status",started?"Live":"Upcoming","played",started,"team1_id",null,"winner_id",null)) : original.apply(sql);
            var dao=new MatchDAO(j::connection);
            if(started){assertThrows(SQLException.class,() -> dao.saveOutcome(m,List.of(),"Forfeit",a,"No show"));assertTrue(j.rolledBack);assertTrue(j.updates.isEmpty());}
            else {dao.saveOutcome(m,List.of(),"Forfeit",a,"No show");assertTrue(j.committed);assertEquals(a,j.updates.getFirst().parameters().get(1));assertTrue(j.updates.getFirst().sql().contains("SET team1_id"));}
        }
    }
    @Test void changedMapDetailsBlockOutcomeEvenWhenSeriesScoreIsUnchanged() {
        var m=fixture("Upcoming","Played",null);var j=jdbc("In progress",false,List.of(m));var original=j.query;
        j.query=sql -> sql.startsWith("SELECT * FROM winit.games") ? List.of(row("game_id",UUID.randomUUID(),"map_name","Mirage","map_order",1,"team1_score",3,"team2_score",2,"status","Live")) : original.apply(sql);
        assertThrows(SQLException.class,() -> new MatchDAO(j::connection).saveOutcome(m,List.of(),"Reset result",null,"Correction"));assertTrue(j.updates.isEmpty());
    }
    @Test void playedMapsMustBeExplicitlyResetBeforeForfeit() {
        var m=fixture("Live","Played",null);var j=jdbc("In progress",false,List.of(m));var original=j.query;
        var map=new MapResult(UUID.randomUUID(),"Mirage",1,3,2,"Live");
        j.query=sql -> sql.startsWith("SELECT * FROM winit.games") ? List.of(row("game_id",map.gameId(),"map_name",map.mapName(),"map_order",1,"team1_score",3,"team2_score",2,"status","Live")) : original.apply(sql);
        assertThrows(SQLException.class,() -> new MatchDAO(j::connection).saveOutcome(m,List.of(map),"Forfeit",a,"No show"));assertTrue(j.updates.isEmpty());
    }
    @Test void manualMatchCanBeCancelledWithoutWinnerOrMaps()throws Exception {
        var m=new TournamentMatch(match,event,a,"A",b,"B","Bo1","Upcoming",null,"Exhibition","Match",null,0,0,null,null);
        var j=jdbc("In progress",false,List.of(m));new MatchDAO(j::connection).saveOutcome(m,List.of(),"Cancelled",null,"Match cancelled");
        var update=j.updates.stream().filter(u -> u.sql().startsWith("UPDATE winit.matches")).findFirst().orElseThrow();assertNull(update.parameters().get(2));assertEquals("Cancelled",update.parameters().get(3));assertTrue(j.committed);
    }
    @Test void auditFailureRollsBackAward() {
        var m=fixture("Upcoming","Played",null);var j=jdbc("In progress",false,List.of(m));j.failUpdate="INSERT INTO winit.tournament_audit";
        assertThrows(SQLException.class,() -> new MatchDAO(j::connection).saveOutcome(m,List.of(),"Forfeit",a,"No show"));assertTrue(j.rolledBack);assertFalse(j.committed);
    }
    @Test void openRegistrationWithPastDatesRemainsOpen()throws Exception {
        var j=jdbc("Registration",false,List.of());var original=j.query;
        j.query=sql -> sql.startsWith("SELECT start_date") ? List.of(row("lifecycle_state","Registration","archived",false,"start_date",java.time.LocalDate.now().minusDays(5),"end_date",java.time.LocalDate.now().minusDays(1))) : original.apply(sql);
        assertTrue(new TournamentDAO(j::connection).isRegistrationOpen(event));
    }
}
