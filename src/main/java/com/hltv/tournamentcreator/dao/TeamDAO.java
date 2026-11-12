package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.Team;
import com.hltv.tournamentcreator.models.Player;
import com.hltv.tournamentcreator.models.RosterPlayer;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import org.postgresql.util.PSQLException;

import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamDAO {
    private final ConnectionSource connections;

    public TeamDAO() { this(DatabaseManager::getConnection); }
    TeamDAO(ConnectionSource connections) { this.connections = connections; }

    public List<Team> getAllTeams() throws SQLException {
        List<Team> teams = new ArrayList<>();
        String sql = """
                SELECT t.*, (SELECT count(*) FROM winit.players p WHERE p.team_id = t.team_id) AS player_count
                FROM winit.teams t ORDER BY world_ranking ASC NULLS LAST, name
                """;

        try (Connection conn = connections.open();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teams.add(mapRowToTeam(rs));
            }
        }
        return teams;
    }

    public List<Player> getFreeAgents() throws SQLException {
        try (Connection conn = connections.open();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT player_id, nickname FROM winit.players WHERE team_id IS NULL ORDER BY nickname");
             ResultSet rs = stmt.executeQuery()) {
            List<Player> players = new ArrayList<>();
            while (rs.next()) {
                players.add(new Player(rs.getObject("player_id", UUID.class), rs.getString("nickname"),
                        null, null, null, null, null, null, null, null));
            }
            return players;
        }
    }

    public List<String> getRoster(UUID teamId) throws SQLException {
        try (Connection conn = connections.open();
             PreparedStatement stmt = conn.prepareStatement("SELECT nickname FROM winit.players WHERE team_id = ? ORDER BY nickname")) {
            stmt.setObject(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> roster = new ArrayList<>();
                while (rs.next()) roster.add(rs.getString("nickname"));
                return roster;
            }
        }
    }

    public List<RosterPlayer> getAvailablePlayers() throws SQLException {
        return getRosterPlayers(null);
    }

    public List<RosterPlayer> getRosterMembers(UUID teamId) throws SQLException {
        return getRosterPlayers(java.util.Objects.requireNonNull(teamId));
    }

    private List<RosterPlayer> getRosterPlayers(UUID teamId) throws SQLException {
        String sql = """
                SELECT p.player_id, p.nickname, p.first_name, p.last_name, p.hltv_rating,
                       c.name AS country,
                """ + (teamId == null ? "NULL AS team_role " : "p.team_role ") + """
                FROM winit.players p LEFT JOIN winit.countries c ON c.country_id = p.country_id
                """ + (teamId == null ? "WHERE p.team_id IS NULL " : "WHERE p.team_id = ? ") + "ORDER BY p.nickname, p.player_id";
        try (Connection conn = connections.open(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (teamId != null) stmt.setObject(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<RosterPlayer> players = new ArrayList<>();
                while (rs.next()) {
                    BigDecimal rating = rs.getBigDecimal("hltv_rating");
                    players.add(new RosterPlayer(rs.getObject("player_id", UUID.class),
                        rs.getString("nickname"), rs.getString("first_name"), rs.getString("last_name"),
                        rs.getString("country"), rating == null ? null : rating.doubleValue(), rs.getString("team_role")));
                }
                return players;
            }
        }
    }

    public void assignPlayer(UUID teamId, UUID playerId, String role) throws SQLException {
        java.util.Objects.requireNonNull(teamId, "Team is required");
        java.util.Objects.requireNonNull(playerId, "Player is required");
        String validRole = CreatorValidation.text(role, "Role", 50, true);
        // One atomic statement saves membership and role and prevents stealing a player
        // who joined another team after the available-player list was loaded.
        try (Connection conn = connections.open(); PreparedStatement stmt = conn.prepareStatement("""
                UPDATE winit.players SET team_id = ?, team_role = ?
                WHERE player_id = ? AND team_id IS NULL
                """)) {
            stmt.setObject(1, teamId);
            stmt.setString(2, validRole);
            stmt.setObject(3, playerId);
            if (stmt.executeUpdate() != 1) throw new SQLException(
                    "This player is no longer available. Refresh the player list and choose again.", "P0001");
        }
    }

    public void updatePlayerRole(UUID teamId, UUID playerId, String previousRole, String role) throws SQLException {
        java.util.Objects.requireNonNull(teamId, "Team is required");
        java.util.Objects.requireNonNull(playerId, "Player is required");
        String validRole = CreatorValidation.text(role, "Role", 50, true);
        try (Connection conn = connections.open(); PreparedStatement stmt = conn.prepareStatement("""
                UPDATE winit.players SET team_role = ?
                WHERE player_id = ? AND team_id = ? AND team_role IS NOT DISTINCT FROM ?
                """)) {
            stmt.setString(1, validRole);
            stmt.setObject(2, playerId);
            stmt.setObject(3, teamId);
            stmt.setString(4, previousRole);
            if (stmt.executeUpdate() != 1) throw new SQLException(
                    "This player's team or role changed. Return to the roster, refresh it, and try again.", "P0001");
        }
    }

    public Team createTeam(String name, Integer worldRanking, List<UUID> playerIds) throws SQLException {
        try (Connection conn = connections.open()) {
            conn.setAutoCommit(false);
            try {
                Team team = new Team(UUID.randomUUID(), name, worldRanking, playerIds.size());
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO winit.teams (team_id, name, world_ranking) VALUES (?, ?, ?)")) {
                    stmt.setObject(1, team.teamId());
                    stmt.setString(2, team.name());
                    stmt.setObject(3, team.worldRanking(), Types.INTEGER);
                    try {
                        stmt.executeUpdate();
                    } catch (SQLException error) {
                        throw rankingConflict(error, worldRanking);
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE winit.players SET team_id = ? WHERE player_id = ? AND team_id IS NULL")) {
                    for (UUID playerId : playerIds) {
                        stmt.setObject(1, team.teamId());
                        stmt.setObject(2, playerId);
                        if (stmt.executeUpdate() != 1) {
                            throw new SQLException("A selected player is no longer available. Reopen the creator to refresh the roster.");
                        }
                    }
                }
                conn.commit();
                return team;
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            }
        }
    }

    static SQLException rankingConflict(SQLException error, Integer ranking) {
        if (error instanceof PSQLException postgres && "23505".equals(error.getSQLState())
                && postgres.getServerErrorMessage() != null
                && "teams_world_ranking_unique".equals(postgres.getServerErrorMessage().getConstraint())) {
            return new SQLException("World ranking #" + ranking + " is already assigned to another team.", "P0001", error);
        }
        return error;
    }

    // Helper method to keep mapping logic in one place
    private Team mapRowToTeam(ResultSet rs) throws SQLException {
        return new Team(
                rs.getObject("team_id", UUID.class),
                rs.getString("name"),
                rs.getObject("world_ranking", Integer.class),
                rs.getInt("player_count")
        );
    }
}
