package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.Tournament;
import com.hltv.tournamentcreator.models.Team;
import com.hltv.tournamentcreator.models.TournamentParticipant;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TournamentDAO {
    private final ConnectionSource connections;

    public TournamentDAO() { this(DatabaseManager::getConnection); }
    TournamentDAO(ConnectionSource connections) { this.connections = connections; }

    public List<Tournament> getAllTournaments() throws SQLException {
        String sql = """
                SELECT t.*, (SELECT count(*) FROM winit.tournament_participants p
                             WHERE p.tournament_id = t.tournament_id) AS participant_count
                FROM winit.tournaments t ORDER BY start_date DESC NULLS LAST, name
                """;
        try (Connection conn = connections.open();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Tournament> tournaments = new ArrayList<>();
            while (rs.next()) {
                tournaments.add(new Tournament(rs.getObject("tournament_id", UUID.class), rs.getString("name"),
                        rs.getString("tier"), rs.getObject("start_date", java.time.LocalDate.class),
                        rs.getObject("end_date", java.time.LocalDate.class), rs.getBigDecimal("prize_pool"),
                        rs.getString("location"), rs.getString("format"), rs.getInt("participant_count"), rs.getInt("team_limit")));
            }
            return tournaments;
        }
    }

    public void createTournament(Tournament tournament, List<UUID> teamIds) throws SQLException {
        CreatorValidation.teamLimit(Integer.toString(tournament.teamLimit()));
        if (teamIds.size() > tournament.teamLimit()) throw new IllegalArgumentException("Selected teams exceed the tournament's team limit.");
        try (Connection conn = connections.open()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement("""
                        INSERT INTO winit.tournaments
                        (tournament_id, name, tier, start_date, end_date, prize_pool, location, format, team_limit)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    stmt.setObject(1, tournament.tournamentId());
                    stmt.setString(2, tournament.name());
                    stmt.setString(3, tournament.tier());
                    stmt.setObject(4, tournament.startDate());
                    stmt.setObject(5, tournament.endDate());
                    stmt.setBigDecimal(6, tournament.prizePool());
                    stmt.setString(7, tournament.location());
                    stmt.setString(8, tournament.format());
                    stmt.setInt(9, tournament.teamLimit());
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("""
                        INSERT INTO winit.tournament_participants (tournament_id, team_id, seed_number)
                        VALUES (?, ?, ?)
                        """)) {
                    for (int i = 0; i < teamIds.size(); i++) {
                        stmt.setObject(1, tournament.tournamentId());
                        stmt.setObject(2, teamIds.get(i));
                        stmt.setInt(3, i + 1);
                        stmt.addBatch();
                    }
                    if (!teamIds.isEmpty()) stmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            }
        }
    }

    public List<String> getParticipants(UUID tournamentId) throws SQLException {
        try (Connection conn = connections.open();
             PreparedStatement stmt = conn.prepareStatement("""
                     SELECT t.name, p.seed_number FROM winit.tournament_participants p
                     JOIN winit.teams t ON t.team_id = p.team_id WHERE p.tournament_id = ?
                     ORDER BY p.seed_number NULLS LAST, t.name
                     """)) {
            stmt.setObject(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> participants = new ArrayList<>();
                while (rs.next()) participants.add((rs.getObject("seed_number") == null ? "—" : "#" + rs.getInt("seed_number"))
                        + "   " + rs.getString("name"));
                return participants;
            }
        }
    }

    public List<TournamentParticipant> getRegisteredTeams(UUID tournamentId) throws SQLException {
        try (Connection conn = connections.open(); PreparedStatement stmt = conn.prepareStatement("""
                SELECT t.team_id, t.name, p.seed_number FROM winit.tournament_participants p
                JOIN winit.teams t ON t.team_id = p.team_id WHERE p.tournament_id = ?
                ORDER BY p.seed_number NULLS LAST, t.name
                """)) {
            stmt.setObject(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<TournamentParticipant> teams = new ArrayList<>();
                while (rs.next()) teams.add(new TournamentParticipant(rs.getObject("team_id", UUID.class),
                        rs.getString("name"), rs.getObject("seed_number", Integer.class)));
                return teams;
            }
        }
    }

    public List<Team> getEligibleTeams(UUID tournamentId) throws SQLException {
        try (Connection conn = connections.open(); PreparedStatement stmt = conn.prepareStatement("""
                SELECT t.team_id, t.name, t.world_ranking FROM winit.teams t WHERE NOT EXISTS
                (SELECT 1 FROM winit.tournament_participants p WHERE p.tournament_id = ? AND p.team_id = t.team_id)
                ORDER BY t.world_ranking NULLS LAST, t.name
                """)) {
            stmt.setObject(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Team> teams = new ArrayList<>();
                while (rs.next()) teams.add(new Team(rs.getObject("team_id", UUID.class), rs.getString("name"),
                        rs.getObject("world_ranking", Integer.class)));
                return teams;
            }
        }
    }

    public void registerTeams(UUID tournamentId, List<UUID> teamIds) throws SQLException {
        if (teamIds.isEmpty()) throw new IllegalArgumentException("Select at least one team.");
        if (new java.util.HashSet<>(teamIds).size() != teamIds.size()) throw new IllegalArgumentException("A team may only be registered once.");
        try (Connection conn = connections.open()) {
            conn.setAutoCommit(false);
            try {
                // Serialize registrations so consecutive seeds stay unique for this tournament.
                int limit;
                try (PreparedStatement stmt = conn.prepareStatement("SELECT tournament_id, team_limit FROM winit.tournaments WHERE tournament_id = ? FOR UPDATE")) {
                    stmt.setObject(1, tournamentId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new SQLException("This tournament no longer exists.", "P0001");
                        limit = rs.getInt("team_limit");
                    }
                }
                int seed;
                MatchDAO.ensureNoBracket(conn, tournamentId);
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COALESCE(MAX(seed_number), 0) AS next_seed, COUNT(*) AS participant_count FROM winit.tournament_participants WHERE tournament_id = ?")) {
                    stmt.setObject(1, tournamentId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next(); seed = rs.getInt("next_seed");
                        if ((long) rs.getInt("participant_count") + teamIds.size() > limit)
                            throw new SQLException("These teams exceed the tournament's limit of " + limit + ". Refresh and select fewer teams.", "P0001");
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO winit.tournament_participants (tournament_id, team_id, seed_number) VALUES (?, ?, ?)")) {
                    for (UUID teamId : teamIds) {
                        stmt.setObject(1, tournamentId); stmt.setObject(2, teamId); stmt.setInt(3, ++seed);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException | RuntimeException error) {
                try { conn.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        }
    }

    public int getTeamLimit(UUID tournamentId) throws SQLException {
        try (Connection conn = connections.open(); PreparedStatement stmt = conn.prepareStatement("SELECT team_limit FROM winit.tournaments WHERE tournament_id=?")) {
            stmt.setObject(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new SQLException("This tournament no longer exists.", "P0001");
                return rs.getInt("team_limit");
            }
        }
    }

    public void updateTeamLimit(UUID tournamentId, int limit) throws SQLException {
        CreatorValidation.teamLimit(Integer.toString(limit));
        try (Connection conn = connections.open()) {
            conn.setAutoCommit(false);
            try {
                MatchDAO.lockTournament(conn, tournamentId);
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS participant_count FROM winit.tournament_participants WHERE tournament_id=?")) {
                    stmt.setObject(1, tournamentId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        if (rs.getInt("participant_count") > limit)
                            throw new SQLException("The team limit cannot be smaller than the number of registered teams.", "P0001");
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE winit.tournaments SET team_limit=? WHERE tournament_id=?")) {
                    stmt.setInt(1, limit); stmt.setObject(2, tournamentId); stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RuntimeException error) {
                try { conn.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        }
    }
}
