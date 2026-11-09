package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;

class MatchPersistenceTest {
    private final UUID tournament=UUID.randomUUID(), matchId=UUID.randomUUID(), nextId=UUID.randomUUID(), a=UUID.randomUUID(), b=UUID.randomUUID();
    private TournamentMatch match(UUID winner) {
        return new TournamentMatch(matchId,tournament,a,"A",b,"B","Bo1",winner==null?"Upcoming":"Completed",null,"Single elimination","Semifinals",winner,0,0,nextId,1);
    }
    private MapResult result(int first,int second) { return new MapResult(UUID.randomUUID(),"Mirage",1,first,second,"Completed"); }
    private Map<String,Object> row(Object... pairs) {
        Map<String,Object> row=new HashMap<>();
        for(int i=0;i<pairs.length;i+=2) row.put((String)pairs[i],pairs[i+1]);
        return row;
    }
    private Jdbc jdbc(UUID oldWinner,String downstreamStatus,List<MapResult> maps) {
        Jdbc jdbc=new Jdbc();
        jdbc.query=sql -> {
            if(sql.startsWith("SELECT tournament_id")) return List.of(row("tournament_id",tournament,"lifecycle_state","In progress"));
            if(sql.startsWith("SELECT * FROM winit.matches")) return List.of(row("status",oldWinner==null?"Upcoming":"Completed","outcome_type","Played","team1_id",a,"team2_id",b,"match_format","Bo1","winner_id",oldWinner,"next_match_id",nextId,"next_match_slot",1));
            if(sql.startsWith("SELECT * FROM winit.games")) return maps.stream().map(m -> row("game_id",m.gameId(),"map_name",m.mapName(),"map_order",m.order(),"team1_score",m.team1Score(),"team2_score",m.team2Score(),"status",m.status())).toList();
            if(sql.startsWith("SELECT m.*")) return List.of(row("tournament_id",tournament,"status",downstreamStatus,"played",!downstreamStatus.equals("Upcoming"),"winner_id",null,"team1_id",oldWinner));
            throw new AssertionError("Unexpected query: "+sql);
        };
        return jdbc;
    }
    @Test void completesMapMatchAndAdvancesWinnerInSameTransaction() throws Exception {
        Jdbc jdbc=jdbc(null,"Upcoming",List.of());
        new MatchDAO(jdbc::connection).saveScores(match(null),List.of(),List.of(result(13,9)));
        assertTrue(jdbc.committed); assertFalse(jdbc.rolledBack); assertTrue(jdbc.closed);
        Update advance=jdbc.updates.stream().filter(u -> u.sql.contains("SET team1_id=?")).findFirst().orElseThrow();
        assertEquals(a,advance.parameters.get(1)); assertEquals(nextId,advance.parameters.get(2));
        Update map=jdbc.updates.stream().filter(u -> u.sql.startsWith("INSERT INTO winit.games")).findFirst().orElseThrow();
        assertEquals(matchId,map.parameters.get(2)); assertEquals(13,map.parameters.get(5)); assertEquals(9,map.parameters.get(6));
        assertEquals("Completed",map.parameters.get(7)); assertEquals(a,map.parameters.get(8));
        assertTrue(map.sql.contains("map_id FROM winit.maps WHERE name=?"));
        assertEquals("Mirage", map.parameters.get(9));
        Update series=jdbc.updates.stream().filter(u -> u.sql.startsWith("UPDATE winit.matches SET status")).findFirst().orElseThrow();
        assertEquals("Completed",series.parameters.get(1)); assertEquals(a,series.parameters.get(2));
    }
    @Test void blocksWinnerCorrectionAfterNextMatchStarts() {
        MapResult old=result(13,9);
        Jdbc jdbc=jdbc(a,"Live",List.of(old));
        SQLException error=assertThrows(SQLException.class, () -> new MatchDAO(jdbc::connection).saveScores(match(a),List.of(old),List.of(new MapResult(old.gameId(),"Mirage",1,9,13,"Completed")),"Corrected score"));
        assertEquals("P0001",error.getSQLState()); assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed); assertTrue(jdbc.updates.isEmpty());
    }
    @Test void allowsWinnerCorrectionBeforeNextMatchStarts() throws Exception {
        MapResult old=result(13,9); Jdbc jdbc=jdbc(a,"Upcoming",List.of(old));
        new MatchDAO(jdbc::connection).saveScores(match(a),List.of(old),List.of(new MapResult(old.gameId(),"Mirage",1,9,13,"Completed")),"Corrected score");
        assertEquals(b,jdbc.updates.get(0).parameters.get(1)); assertTrue(jdbc.committed);
    }
    @Test void staleScoreEditorCannotOverwriteNewerResults() {
        Jdbc jdbc=jdbc(null,"Upcoming",List.of(result(8,4)));
        assertThrows(SQLException.class, () -> new MatchDAO(jdbc::connection).saveScores(match(null),List.of(),List.of(result(13,9))));
        assertTrue(jdbc.rolledBack); assertTrue(jdbc.updates.isEmpty());
    }
    @Test void mapInsertFailureRollsBackBracketAdvanceAndMatchResult() {
        Jdbc jdbc=jdbc(null,"Upcoming",List.of()); jdbc.failUpdate="INSERT INTO winit.games";
        assertThrows(SQLException.class, () -> new MatchDAO(jdbc::connection).saveScores(match(null),List.of(),List.of(result(13,9))));
        assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed); assertTrue(jdbc.closed);
    }
    @Test void registersTeamsWithConsecutiveSeedsAndCommitsTogether() throws Exception {
        Jdbc jdbc=new Jdbc();
        jdbc.query=sql -> sql.startsWith("SELECT tournament_id") ? List.of(row("tournament_id",tournament,"team_limit",6))
                : sql.startsWith("SELECT start_date") ? List.of(row("start_date", null,"lifecycle_state","Registration")) : sql.startsWith("SELECT COALESCE") ? List.of(row("next_seed",4,"participant_count",4)) : List.of();
        new TournamentDAO(jdbc::connection).registerTeams(tournament,List.of(a,b));
        assertTrue(jdbc.committed); assertEquals(2,jdbc.updates.size());
        assertEquals(5,jdbc.updates.get(0).parameters.get(3)); assertEquals(6,jdbc.updates.get(1).parameters.get(3));
    }
    @Test void registrationIsBlockedOnceBracketExists() {
        Jdbc jdbc=new Jdbc(); jdbc.query=sql -> List.of(row("tournament_id",tournament));
        assertThrows(SQLException.class, () -> new TournamentDAO(jdbc::connection).registerTeams(tournament,List.of(a)));
        assertTrue(jdbc.rolledBack); assertTrue(jdbc.updates.isEmpty());
    }
    @Test void generatedBracketInsertsFinalBeforeItsFeeders() throws Exception {
        Jdbc jdbc=new Jdbc(); List<UUID> teams=List.of(a,b,UUID.randomUUID());
        jdbc.query=sql -> sql.startsWith("SELECT tournament_id") ? List.of(row("tournament_id",tournament))
                : sql.startsWith("SELECT team_id") ? teams.stream().map(t -> row("team_id",t)).toList()
                : sql.startsWith("SELECT team_limit") ? List.of(row("team_limit",16)) : List.of();
        new MatchDAO(jdbc::connection).generateBracket(tournament,"Bo3");
        var inserts=jdbc.updates.stream().filter(u -> u.sql.startsWith("INSERT INTO winit.matches")).toList();
        assertEquals(3,inserts.size()); Set<Object> inserted=new HashSet<>();
        for(Update update:inserts) {
            Object next=update.parameters.get(9);
            if(next!=null) assertTrue(inserted.contains(next));
            inserted.add(update.parameters.get(1));
        }
        assertTrue(jdbc.committed);
    }

    @Test void groupResultsAreLockedAfterPlayoffQualification() {
        Jdbc jdbc=jdbc(null,"Upcoming",List.of()); var original=jdbc.query;
        jdbc.query=sql -> {
            if(sql.startsWith("SELECT 1 FROM winit.matches"))return List.of(row("1",1));
            var rows=original.apply(sql);
            if(sql.startsWith("SELECT * FROM winit.matches"))rows.get(0).put("group_id",UUID.randomUUID());
            return rows;
        };
        SQLException error=assertThrows(SQLException.class, () -> new MatchDAO(jdbc::connection).saveScores(match(null),List.of(),List.of(result(13,9))));
        assertTrue(error.getMessage().contains("Group results are locked"));
        assertTrue(jdbc.rolledBack);assertTrue(jdbc.updates.isEmpty());
    }

    @Test void rejectsRegistrationBatchThatWouldExceedCapacityWithoutPartialWrites() {
        Jdbc jdbc=new Jdbc();
        jdbc.query=sql -> sql.startsWith("SELECT tournament_id") ? List.of(row("tournament_id",tournament,"team_limit",2))
                : sql.startsWith("SELECT start_date") ? List.of(row("start_date", null,"lifecycle_state","Registration")) : sql.startsWith("SELECT COALESCE") ? List.of(row("next_seed",1,"participant_count",1)) : List.of();
        SQLException error=assertThrows(SQLException.class, () -> new TournamentDAO(jdbc::connection).registerTeams(tournament,List.of(a,b)));
        assertEquals("P0001",error.getSQLState());
        assertTrue(jdbc.rolledBack); assertFalse(jdbc.committed); assertTrue(jdbc.updates.isEmpty());
    }
    @Test void rejectsRegistrationWhenLastSlotWasAlreadyClaimed() {
        Jdbc jdbc=new Jdbc();
        jdbc.query=sql -> sql.startsWith("SELECT tournament_id") ? List.of(row("tournament_id",tournament,"team_limit",2))
                : sql.startsWith("SELECT start_date") ? List.of(row("start_date", null,"lifecycle_state","Registration")) : sql.startsWith("SELECT COALESCE") ? List.of(row("next_seed",2,"participant_count",2)) : List.of();
        assertThrows(SQLException.class, () -> new TournamentDAO(jdbc::connection).registerTeams(tournament,List.of(a)));
        assertTrue(jdbc.rolledBack); assertTrue(jdbc.updates.isEmpty());
    }
    @Test void limitCannotBeReducedBelowRegisteredCount() {
        Jdbc jdbc=new Jdbc();
        jdbc.query=sql -> sql.startsWith("SELECT tournament_id") ? List.of(row("tournament_id",tournament))
                : List.of(row("participant_count",4));
        assertThrows(SQLException.class, () -> new TournamentDAO(jdbc::connection).updateTeamLimit(tournament,2));
        assertTrue(jdbc.rolledBack); assertTrue(jdbc.updates.isEmpty());
    }
    @Test void limitCanBeChangedToExactlyRegisteredCount() throws Exception {
        Jdbc jdbc=new Jdbc();
        jdbc.query=sql -> sql.startsWith("SELECT tournament_id") ? List.of(row("tournament_id",tournament))
                : List.of(row("participant_count",4));
        new TournamentDAO(jdbc::connection).updateTeamLimit(tournament,4);
        assertTrue(jdbc.committed); assertEquals(4,jdbc.updates.get(0).parameters.get(1));
        assertEquals(tournament,jdbc.updates.get(0).parameters.get(2));
    }

    record Update(String sql,Map<Integer,Object> parameters) {}
    @Test void teamCanBeRemovedBeforeStartWithoutDeletingTheTeam() throws Exception {
        Jdbc jdbc = registration(java.time.LocalDate.now().plusDays(1), false);
        new TournamentDAO(jdbc::connection).unregisterTeam(tournament, a);
        assertTrue(jdbc.committed);
        assertEquals(1, jdbc.updates.size());
        assertTrue(jdbc.updates.get(0).sql().startsWith("DELETE FROM winit.tournament_participants"));
        assertEquals(a, jdbc.updates.get(0).parameters().get(2));
    }

    @Test void inProgressTournamentRejectsBothAddingAndRemovingTeams() {
        for (var date : List.of(java.time.LocalDate.now(), java.time.LocalDate.now().minusDays(1))) {
            Jdbc add = registration(date, false), remove = registration(date, false);
            assertThrows(SQLException.class, () -> new TournamentDAO(add::connection).registerTeams(tournament, List.of(a)));
            assertThrows(SQLException.class, () -> new TournamentDAO(remove::connection).unregisterTeam(tournament, a));
            assertTrue(add.rolledBack && remove.rolledBack);
            assertTrue(add.updates.isEmpty() && remove.updates.isEmpty());
        }
    }

    @Test void generatedCompetitionRejectsTeamRemovalBeforeScheduledStart() {
        Jdbc jdbc = registration(java.time.LocalDate.now().plusDays(1), true);
        assertThrows(SQLException.class, () -> new TournamentDAO(jdbc::connection).unregisterTeam(tournament, a));
        assertTrue(jdbc.rolledBack); assertTrue(jdbc.updates.isEmpty());
    }

    @Test void upcomingTournamentAllowsRegistration() throws Exception {
        Jdbc jdbc = registration(java.time.LocalDate.now().plusDays(1), false);
        new TournamentDAO(jdbc::connection).registerTeams(tournament, List.of(a));
        assertTrue(jdbc.committed); assertEquals(1, jdbc.updates.size());
    }

    private Jdbc registration(java.time.LocalDate start, boolean generated) {
        Jdbc jdbc = new Jdbc();
        jdbc.query = sql -> {
            if (sql.startsWith("SELECT tournament_id")) return List.of(row("tournament_id", tournament, "team_limit", 16));
            if (sql.startsWith("SELECT start_date")) return List.of(row("start_date", start,"lifecycle_state",start!=null && !start.isAfter(java.time.LocalDate.now()) ? "In progress" : "Registration"));
            if (sql.contains("UNION ALL")) return generated ? List.of(row("1", 1)) : List.of();
            if (sql.startsWith("SELECT COALESCE")) return List.of(row("next_seed", 0, "participant_count", 0));
            return List.of();
        };
        return jdbc;
    }
    static final class Jdbc {
        boolean committed,rolledBack,closed;
        String failUpdate;
        int updateCount = 1;
        Function<String,List<Map<String,Object>>> query=sql -> List.of();
        final List<Update> updates=new ArrayList<>();
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(proxy,method,args) -> {
                switch(method.getName()) {
                    case "setAutoCommit": assertEquals(false,args[0]); return null;
                    case "prepareStatement": return statement((String)args[0]);
                    case "commit": committed=true; return null;
                    case "rollback": rolledBack=true; return null;
                    case "close": closed=true; return null;
                    default: throw new AssertionError(method.getName());
                }
            });
        }
        PreparedStatement statement(String sql) {
            Map<Integer,Object> parameters=new HashMap<>();
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(proxy,method,args) -> {
                switch(method.getName()) {
                    case "setObject": case "setString": case "setInt": parameters.put((Integer)args[0],args[1]); return null;
                    case "executeQuery": return result(query.apply(sql));
                    case "executeUpdate":
                        if(failUpdate!=null && sql.startsWith(failUpdate)) throw new SQLException("Simulated failure");
                        updates.add(new Update(sql,new HashMap<>(parameters))); return updateCount;
                    case "close": return null;
                    default: throw new AssertionError(method.getName());
                }
            });
        }
        ResultSet result(List<Map<String,Object>> rows) {
            int[] index={-1};
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSet.class},(proxy,method,args) -> {
                if(method.getName().equals("next")) return ++index[0]<rows.size();
                if(method.getName().equals("close")) return null;
                Object value=rows.get(index[0]).get(args[0].toString());
                // Simulate defaults of migration 005 for legacy fixture rows.
                if(value==null && !rows.get(index[0]).containsKey(args[0].toString())) {
                    if("lifecycle_state".equals(args[0])) value="Draft";
                    if("outcome_type".equals(args[0])) value="Played";
                }
                return switch(method.getName()) {
                    case "getObject" -> value;
                    case "getBigDecimal" -> value;
                    case "getString" -> value==null ? null : value.toString();
                    case "getInt" -> value==null ? 0 : ((Number)value).intValue();
                    case "getBoolean" -> Boolean.TRUE.equals(value);
                    default -> throw new AssertionError(method.getName());
                };
            });
        }
    }
}
