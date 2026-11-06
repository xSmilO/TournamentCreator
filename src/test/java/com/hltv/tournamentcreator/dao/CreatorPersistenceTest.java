package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.Tournament;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatorPersistenceTest {
    @Test void teamAndRosterCommitTogetherWithGeneratedId() throws Exception {
        JdbcStub jdbc = new JdbcStub();
        UUID player = UUID.randomUUID();
        var team = new TeamDAO(jdbc::connection).createTeam("Test", null, List.of(player));
        assertNotNull(team.teamId());
        assertTrue(jdbc.committed);
        assertFalse(jdbc.rolledBack);
        assertEquals(team.teamId(), jdbc.statements.get(0).parameters.get(1));
        assertEquals("Test", jdbc.statements.get(0).parameters.get(2));
        assertNull(jdbc.statements.get(0).parameters.get(3));
        assertTrue(jdbc.statements.get(1).sql.contains("AND team_id IS NULL"));
        assertEquals(team.teamId(), jdbc.statements.get(1).parameters.get(1));
        assertEquals(player, jdbc.statements.get(1).parameters.get(2));
        assertTrue(jdbc.closed);
    }

    @Test void unavailablePlayerRollsBackNewTeam() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.playerAvailable = false;
        assertThrows(SQLException.class, () -> new TeamDAO(jdbc::connection)
                .createTeam("Test", 1, List.of(UUID.randomUUID())));
        assertFalse(jdbc.committed);
        assertTrue(jdbc.rolledBack);
        assertTrue(jdbc.closed);
    }

    @Test void tournamentParticipantsKeepSelectionOrderAsSeeds() throws Exception {
        JdbcStub jdbc = new JdbcStub();
        List<UUID> teams = List.of(UUID.randomUUID(), UUID.randomUUID());
        Tournament tournament = tournament();
        new TournamentDAO(jdbc::connection).createTournament(tournament, teams);
        assertTrue(jdbc.committed);
        assertFalse(jdbc.rolledBack);
        assertEquals(tournament.tournamentId(), jdbc.statements.get(0).parameters.get(1));
        assertEquals(tournament.teamLimit(), jdbc.statements.get(0).parameters.get(9));
        StatementStub participants = jdbc.statements.get(1);
        assertEquals(2, participants.batches.size());
        for (int i = 0; i < 2; i++) {
            assertEquals(tournament.tournamentId(), participants.batches.get(i).get(1));
            assertEquals(teams.get(i), participants.batches.get(i).get(2));
            assertEquals(i + 1, participants.batches.get(i).get(3));
        }
    }

    @Test void failedParticipantsRollBackTournament() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.failBatch = true;
        assertThrows(SQLException.class, () -> new TournamentDAO(jdbc::connection)
                .createTournament(tournament(), List.of(UUID.randomUUID())));
        assertFalse(jdbc.committed);
        assertTrue(jdbc.rolledBack);
        assertTrue(jdbc.closed);
    }

    @Test void emptyTournamentCanBeSavedWithoutParticipants() throws Exception {
        JdbcStub jdbc = new JdbcStub();
        new TournamentDAO(jdbc::connection).createTournament(tournament(), List.of());
        assertTrue(jdbc.committed);
        assertTrue(jdbc.statements.get(1).batches.isEmpty());
    }

    @Test void tournamentCreationRejectsTeamsOverCapacityBeforeWriting() {
        JdbcStub jdbc = new JdbcStub();
        Tournament tournament = new Tournament(UUID.randomUUID(), "Test", null, null, null, null, null, null, 0, 2);
        assertThrows(IllegalArgumentException.class, () -> new TournamentDAO(jdbc::connection).createTournament(tournament,
                List.of(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID())));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test void playerMembershipAndRoleAreSavedInOneGuardedUpdate() throws Exception {
        JdbcStub jdbc = new JdbcStub();
        UUID team = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        new TeamDAO(jdbc::connection).assignPlayer(team, player, "  AWPer  ");
        assertEquals(1, jdbc.statements.size());
        StatementStub stmt = jdbc.statements.get(0);
        assertTrue(stmt.sql.contains("SET team_id = ?, team_role = ?"));
        assertTrue(stmt.sql.contains("AND team_id IS NULL"));
        assertEquals(team, stmt.parameters.get(1));
        assertEquals("AWPer", stmt.parameters.get(2));
        assertEquals(player, stmt.parameters.get(3));
        assertTrue(jdbc.closed);
    }

    @Test void assignmentRejectsPlayerWhoAlreadyJoinedAnotherTeam() {
        JdbcStub jdbc = new JdbcStub();
        jdbc.playerAvailable = false;
        SQLException error = assertThrows(SQLException.class, () -> new TeamDAO(jdbc::connection)
                .assignPlayer(UUID.randomUUID(), UUID.randomUUID(), "Support"));
        assertEquals("P0001", error.getSQLState());
        assertTrue(jdbc.closed);
    }

    @Test void assignmentRequiresRoleWithinDatabaseLimitBeforeOpeningConnection() {
        JdbcStub jdbc = new JdbcStub();
        TeamDAO dao = new TeamDAO(jdbc::connection);
        assertThrows(IllegalArgumentException.class, () -> dao.assignPlayer(UUID.randomUUID(), UUID.randomUUID(), " "));
        assertThrows(IllegalArgumentException.class, () -> dao.assignPlayer(UUID.randomUUID(), UUID.randomUUID(), "x".repeat(51)));
        assertTrue(jdbc.statements.isEmpty());
        assertFalse(jdbc.closed);
    }

    @Test void availablePlayersConvertPostgresNumericRatingsAndPreserveNull() throws Exception {
        JdbcStub jdbc = new JdbcStub();
        jdbc.ratings = Arrays.asList(new BigDecimal("1.21"), null, BigDecimal.ZERO);
        var players = new TeamDAO(jdbc::connection).getAvailablePlayers();
        assertEquals(3, players.size());
        assertEquals(1.21, players.get(0).rating(), 0.000001);
        assertNull(players.get(1).rating());
        assertEquals(0.0, players.get(2).rating(), 0.000001);
        assertTrue(jdbc.closed);
    }

    @Test void rosterConvertsPostgresNumericRatingsAndPreservesNull() throws Exception {
        JdbcStub jdbc = new JdbcStub();
        jdbc.ratings = Arrays.asList(new BigDecimal("0.98"), null);
        UUID teamId = UUID.randomUUID();
        var players = new TeamDAO(jdbc::connection).getRosterMembers(teamId);
        assertEquals(2, players.size());
        assertEquals(0.98, players.get(0).rating(), 0.000001);
        assertNull(players.get(1).rating());
        assertEquals(teamId, jdbc.statements.get(0).parameters.get(1));
        assertTrue(jdbc.closed);
    }

    private Tournament tournament() {
        return new Tournament(UUID.randomUUID(), "Test", null, null, null, null, null, null, 0);
    }

    /** Strict JDBC double: unexpected interactions fail the test. No live database writes. */
    private static final class JdbcStub {
        boolean committed, rolledBack, closed, failBatch;
        boolean playerAvailable = true;
        final List<StatementStub> statements = new ArrayList<>();
        List<BigDecimal> ratings = List.of();

        ResultSet resultSet() {
            int[] row = {-1};
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "next": return ++row[0] < ratings.size();
                            case "getBigDecimal":
                                assertEquals("hltv_rating", args[0]);
                                return ratings.get(row[0]);
                            case "getObject":
                                if (args[1] == Double.class)
                                    throw new SQLException("conversion to class java.lang.Double from numeric not supported");
                                assertEquals("player_id", args[0]);
                                assertEquals(UUID.class, args[1]);
                                return UUID.randomUUID();
                            case "getString": return args[0] + " " + row[0];
                            case "close": return null;
                            default: throw new AssertionError("Unexpected ResultSet call: " + method.getName());
                        }
                    });
        }

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setAutoCommit": assertEquals(false, args[0]); return null;
                            case "prepareStatement":
                                StatementStub stmt = new StatementStub((String) args[0], this);
                                statements.add(stmt);
                                return stmt.proxy();
                            case "commit": committed = true; return null;
                            case "rollback": rolledBack = true; return null;
                            case "close": closed = true; return null;
                            default: throw new AssertionError("Unexpected Connection call: " + method.getName());
                        }
                    });
        }
    }

    private static final class StatementStub {
        final String sql;
        final JdbcStub jdbc;
        final Map<Integer, Object> parameters = new HashMap<>();
        final List<Map<Integer, Object>> batches = new ArrayList<>();

        StatementStub(String sql, JdbcStub jdbc) { this.sql = sql; this.jdbc = jdbc; }

        PreparedStatement proxy() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setObject": case "setString": case "setInt": case "setBigDecimal":
                                parameters.put((Integer) args[0], args[1]); return null;
                            case "executeUpdate": return sql.startsWith("UPDATE") && !jdbc.playerAvailable ? 0 : 1;
                            case "executeQuery": return jdbc.resultSet();
                            case "addBatch": batches.add(new HashMap<>(parameters)); return null;
                            case "executeBatch":
                                if (jdbc.failBatch) throw new SQLException("Simulated participant failure", "23503");
                                return new int[batches.size()];
                            case "close": return null;
                            default: throw new AssertionError("Unexpected statement call: " + method.getName());
                        }
                    });
        }
    }
}
