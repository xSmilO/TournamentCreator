package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.GroupStage;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class GroupStagePersistenceTest {
    private final UUID tournament=UUID.randomUUID(), group=UUID.randomUUID();
    private final List<TournamentParticipant> teams=IntStream.rangeClosed(1,4).mapToObj(i -> new TournamentParticipant(UUID.randomUUID(),"Team "+i,i)).toList();
    private Map<String,Object> row(Object... pairs) {
        Map<String,Object> row=new HashMap<>();
        for(int i=0;i<pairs.length;i+=2)row.put((String)pairs[i],pairs[i+1]);
        return row;
    }
    private MatchPersistenceTest.Jdbc drawingJdbc() {
        var jdbc=new MatchPersistenceTest.Jdbc();
        jdbc.query=sql -> {
            if(sql.startsWith("SELECT tournament_id"))return List.of(row("tournament_id",tournament));
            if(sql.startsWith("SELECT team_limit"))return List.of(row("team_limit",16));
            if(sql.startsWith("SELECT p.team_id"))return teams.stream().map(t -> row("team_id",t.teamId(),"name",t.name(),"seed_number",t.seed())).toList();
            if(sql.startsWith("SELECT 1"))return List.of();
            throw new AssertionError(sql);
        };
        return jdbc;
    }
    private MatchPersistenceTest.Jdbc playoffJdbc(boolean complete) {
        var jdbc=new MatchPersistenceTest.Jdbc();
        var fixtures=GroupStage.fixtures(new TournamentGroup(group,"Group A",1,teams));
        jdbc.query=sql -> {
            if(sql.startsWith("SELECT tournament_id"))return List.of(row("tournament_id",tournament));
            if(sql.startsWith("SELECT 1"))return List.of();
            if(sql.startsWith("SELECT g.group_id"))return teams.stream().map(t -> row("group_id",group,"group_name","Group A","group_order",1,"team_id",t.teamId(),"team_name",t.name(),"seed_number",t.seed())).toList();
            if(sql.startsWith("SELECT m.*"))return fixtures.stream().map(f -> {
                var a=teams.stream().filter(t -> t.teamId().equals(f.team1())).findFirst().orElseThrow();
                var b=teams.stream().filter(t -> t.teamId().equals(f.team2())).findFirst().orElseThrow();
                boolean aWins=a.seed()<b.seed();
                boolean played=complete || f!=fixtures.get(0);
                return row("match_id",f.matchId(),"team1_id",a.teamId(),"team2_id",b.teamId(),"team1_name",a.name(),"team2_name",b.name(),
                        "match_format","Bo3","status",played?"Completed":"Upcoming","stage",GroupStage.STAGE,"round_name","Round "+f.round(),
                        "winner_id",played?(aWins?a.teamId():b.teamId()):null,"score1",played&&aWins?2:0,"score2",played&&!aWins?2:0,"group_id",group);
            }).toList();
            throw new AssertionError(sql);
        };
        return jdbc;
    }
    @Test void groupDrawStoresMembershipsAndAllSixFixturesTogether() throws Exception {
        var jdbc=drawingJdbc();
        new GroupStageDAO(jdbc::connection).generateGroups(tournament,"Bo3");
        assertTrue(jdbc.committed);assertFalse(jdbc.rolledBack);
        assertEquals(1,jdbc.updates.stream().filter(u -> u.sql().startsWith("INSERT INTO winit.tournament_groups ")).count());
        assertEquals(4,jdbc.updates.stream().filter(u -> u.sql().startsWith("INSERT INTO winit.tournament_group_teams")).count());
        assertEquals(6,jdbc.updates.stream().filter(u -> u.sql().startsWith("INSERT INTO winit.matches")).count());
        for(var update:jdbc.updates)if(update.sql().startsWith("INSERT INTO winit.matches")) {
            assertEquals(GroupStage.STAGE,update.parameters().get(6));assertNotNull(update.parameters().get(8));
        }
    }
    @Test void fixtureFailureRollsBackGroupsAndMemberships() {
        var jdbc=drawingJdbc();jdbc.failUpdate="INSERT INTO winit.matches";
        assertThrows(SQLException.class, () -> new GroupStageDAO(jdbc::connection).generateGroups(tournament,"Bo1"));
        assertTrue(jdbc.rolledBack);assertFalse(jdbc.committed);assertTrue(jdbc.closed);
    }
    @Test void preventsDrawingGroupsOverExistingMatches() {
        var jdbc=drawingJdbc();var original=jdbc.query;
        jdbc.query=sql -> sql.startsWith("SELECT 1 FROM winit.matches")?List.of(row("1",1)):original.apply(sql);
        assertThrows(SQLException.class, () -> new GroupStageDAO(jdbc::connection).generateGroups(tournament,"Bo3"));
        assertTrue(jdbc.rolledBack);assertTrue(jdbc.updates.isEmpty());
    }
    @Test void playoffsPersistOnlyTheTopTwoQualifiers() throws Exception {
        var jdbc=playoffJdbc(true);
        new GroupStageDAO(jdbc::connection).generatePlayoffs(tournament,"Bo5");
        assertTrue(jdbc.committed);assertEquals(1,jdbc.updates.size());
        var match=jdbc.updates.get(0);
        assertEquals(teams.get(0).teamId(),match.parameters().get(3));
        assertEquals(teams.get(1).teamId(),match.parameters().get(4));
        assertEquals("Bo5",match.parameters().get(5));
    }
    @Test void incompleteGroupsCannotWriteAnyPlayoffMatches() {
        var jdbc=playoffJdbc(false);
        assertThrows(IllegalArgumentException.class, () -> new GroupStageDAO(jdbc::connection).generatePlayoffs(tournament,"Bo3"));
        assertTrue(jdbc.rolledBack);assertTrue(jdbc.updates.isEmpty());
    }
    @Test void duplicateQualificationIsRejectedBeforeAnyWrites() {
        var jdbc=playoffJdbc(true);var original=jdbc.query;
        jdbc.query=sql -> sql.startsWith("SELECT 1 FROM winit.matches")?List.of(row("1",1)):original.apply(sql);
        assertThrows(SQLException.class, () -> new GroupStageDAO(jdbc::connection).generatePlayoffs(tournament,"Bo3"));
        assertTrue(jdbc.rolledBack);assertTrue(jdbc.updates.isEmpty());
    }
}
