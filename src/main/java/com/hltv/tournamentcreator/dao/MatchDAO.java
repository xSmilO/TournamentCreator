package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import com.hltv.tournamentcreator.utils.MatchResults;
import com.hltv.tournamentcreator.utils.SingleElimination;
import com.hltv.tournamentcreator.utils.GroupStage;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class MatchDAO {
    private final ConnectionSource connections;
    public MatchDAO() { this(DatabaseManager::getConnection); }
    MatchDAO(ConnectionSource connections) { this.connections = connections; }

    public List<TournamentMatch> getMatches(UUID tournamentId) throws SQLException {
        try (Connection conn = connections.open()) { return getMatches(conn, tournamentId); }
    }

    List<TournamentMatch> getMatches(Connection conn, UUID tournamentId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT m.*, COALESCE(a.name, 'TBD') AS team1_name, COALESCE(b.name, 'TBD') AS team2_name,
                  (SELECT count(*) FROM winit.games g WHERE g.match_id=m.match_id AND g.status='Completed' AND g.winner_id=m.team1_id) AS score1,
                  (SELECT count(*) FROM winit.games g WHERE g.match_id=m.match_id AND g.status='Completed' AND g.winner_id=m.team2_id) AS score2
                FROM winit.matches m LEFT JOIN winit.teams a ON a.team_id=m.team1_id
                LEFT JOIN winit.teams b ON b.team_id=m.team2_id WHERE m.tournament_id=?
                ORDER BY m.start_time NULLS LAST, m.round_name, m.match_id
                """)) {
            stmt.setObject(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<TournamentMatch> matches = new ArrayList<>();
                while (rs.next()) matches.add(new TournamentMatch(rs.getObject("match_id", UUID.class), tournamentId,
                        rs.getObject("team1_id", UUID.class), rs.getString("team1_name"), rs.getObject("team2_id", UUID.class),
                        rs.getString("team2_name"), rs.getString("match_format"), rs.getString("status"),
                        rs.getObject("start_time", LocalDateTime.class), rs.getString("stage"), rs.getString("round_name"),
                        rs.getObject("winner_id", UUID.class), rs.getInt("score1"), rs.getInt("score2"), rs.getObject("next_match_id", UUID.class),
                        rs.getObject("next_match_slot", Integer.class), rs.getObject("group_id", UUID.class)));
                return matches;
            }
        }
    }

    public List<MapResult> getMaps(UUID matchId) throws SQLException {
        try (Connection conn = connections.open()) { return getMaps(conn, matchId); }
    }

    public List<String> getMapNames() throws SQLException {
        try (Connection conn = connections.open();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM winit.maps ORDER BY name");
             ResultSet rs = stmt.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rs.next()) names.add(rs.getString("name"));
            return names;
        }
    }

    private List<MapResult> getMaps(Connection conn, UUID matchId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM winit.games WHERE match_id=? ORDER BY map_order NULLS LAST, game_id")) {
            stmt.setObject(1, matchId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<MapResult> maps = new ArrayList<>();
                while (rs.next()) maps.add(new MapResult(rs.getObject("game_id", UUID.class), rs.getString("map_name"),
                        rs.getInt("map_order"), rs.getInt("team1_score"), rs.getInt("team2_score"), rs.getString("status")));
                return maps;
            }
        }
    }

    public void createMatch(UUID tournamentId, UUID team1, UUID team2, String format,
                            LocalDateTime start, String stage, String round) throws SQLException {
        MatchResults.evaluate(format, team1, team2, List.of());
        String validStage = CreatorValidation.text(stage, "Stage", 50, false);
        String validRound = CreatorValidation.text(round, "Round", 50, false);
        if (SingleElimination.STAGE.equals(validStage) || GroupStage.STAGE.equals(validStage))
            throw new IllegalArgumentException("Use the group or bracket generator for managed competition matches.");
        transaction(conn -> {
            lockTournament(conn, tournamentId);
            ensureNoBracket(conn, tournamentId);
            try (PreparedStatement stmt = conn.prepareStatement("SELECT team_id FROM winit.tournament_participants WHERE tournament_id=? AND team_id IN (?, ?) FOR KEY SHARE")) {
                stmt.setObject(1, tournamentId); stmt.setObject(2, team1); stmt.setObject(3, team2);
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0; while (rs.next()) count++;
                    if (count != 2) throw new SQLException("Both opponents must be registered in this tournament.", "P0001");
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO winit.matches (match_id,tournament_id,team1_id,team2_id,match_format,status,start_time,stage,round_name)
                    VALUES (?,?,?,?,?,'Upcoming',?,?,?)
                    """)) {
                stmt.setObject(1, UUID.randomUUID()); stmt.setObject(2, tournamentId); stmt.setObject(3, team1); stmt.setObject(4, team2);
                stmt.setString(5, format); stmt.setObject(6, start); stmt.setString(7, validStage); stmt.setString(8, validRound);
                stmt.executeUpdate();
            }
        });
    }

    public void generateBracket(UUID tournamentId, String format) throws SQLException {
        generateBracket(tournamentId, format, false);
    }

    public void generateBracket(UUID tournamentId, String format, boolean randomDraw) throws SQLException {
        MatchResults.mapLimit(format);
        transaction(conn -> {
            lockTournament(conn, tournamentId);
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM winit.matches WHERE tournament_id=? LIMIT 1")) {
                stmt.setObject(1, tournamentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) throw new SQLException("This tournament already has matches. A bracket can only be generated for a tournament without matches.", "P0001");
                }
            }
            List<UUID> teams = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT team_id FROM winit.tournament_participants WHERE tournament_id=? ORDER BY seed_number NULLS LAST, team_id")) {
                stmt.setObject(1, tournamentId);
                try (ResultSet rs = stmt.executeQuery()) { while (rs.next()) teams.add(rs.getObject("team_id", UUID.class)); }
            }
            if (randomDraw) Collections.shuffle(teams);
            List<BracketMatch> bracket = SingleElimination.generate(teams);
            try (PreparedStatement stmt = conn.prepareStatement("SELECT team_limit FROM winit.tournaments WHERE tournament_id=?")) {
                stmt.setObject(1, tournamentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    if (teams.size() > rs.getInt("team_limit")) throw new SQLException("Registered teams exceed the tournament's team limit.", "P0001");
                }
            }
            insertBracket(conn, tournamentId, format, bracket);
        });
    }

    static void insertBracket(Connection conn, UUID tournamentId, String format, List<BracketMatch> bracket) throws SQLException {
        // Insert the final first so the next_match_id foreign keys already exist.
        try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO winit.matches (match_id,tournament_id,team1_id,team2_id,match_format,status,stage,round_name,next_match_id,next_match_slot,winner_id)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """)) {
                for (int i = bracket.size() - 1; i >= 0; i--) {
                    BracketMatch match = bracket.get(i);
                    stmt.setObject(1, match.matchId()); stmt.setObject(2, tournamentId);
                    stmt.setObject(3, match.team1Id()); stmt.setObject(4, match.team2Id()); stmt.setString(5, format);
                    stmt.setString(6, match.winnerId() == null ? "Upcoming" : "Completed");
                    stmt.setString(7, SingleElimination.STAGE); stmt.setString(8, match.round());
                    stmt.setObject(9, match.nextMatchId()); stmt.setObject(10, match.nextSlot(), Types.INTEGER); stmt.setObject(11, match.winnerId());
                    stmt.executeUpdate();
                }
        }
    }

    public void saveScores(TournamentMatch match, List<MapResult> original, List<MapResult> input) throws SQLException {
        MatchResults.Summary result = MatchResults.evaluate(match.format(), match.team1Id(), match.team2Id(), input);
        Set<UUID> ids = new HashSet<>();
        for (MapResult map : result.maps()) {
            if (map.gameId() == null || !ids.add(map.gameId())) throw new IllegalArgumentException("Each map must have a distinct ID.");
        }
        transaction(conn -> {
            // All writers lock the tournament before its matches, keeping bracket updates atomic.
            lockTournament(conn, match.tournamentId());
            UUID oldWinner, nextId;
            Integer nextSlot;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM winit.matches WHERE match_id=? AND tournament_id=? FOR UPDATE")) {
                stmt.setObject(1, match.matchId()); stmt.setObject(2, match.tournamentId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) throw new SQLException("This match no longer exists.", "P0001");
                    if (!Objects.equals(match.team1Id(), rs.getObject("team1_id", UUID.class))
                            || !Objects.equals(match.team2Id(), rs.getObject("team2_id", UUID.class))
                            || !Objects.equals(match.format(), rs.getString("match_format")))
                        throw new SQLException("The opponents or format changed. Reopen the match.", "P0001");
                    oldWinner = rs.getObject("winner_id", UUID.class);
                    nextId = rs.getObject("next_match_id", UUID.class);
                    nextSlot = rs.getObject("next_match_slot", Integer.class);
                    if (rs.getObject("group_id", UUID.class) != null) {
                        try (PreparedStatement playoffs = conn.prepareStatement("SELECT 1 FROM winit.matches WHERE tournament_id=? AND stage=? LIMIT 1")) {
                            playoffs.setObject(1, match.tournamentId()); playoffs.setString(2, SingleElimination.STAGE);
                            try (ResultSet played = playoffs.executeQuery()) {
                                if (played.next()) throw new SQLException("Group results are locked because playoffs have already been generated.", "P0001");
                            }
                        }
                    }
                }
            }
            if (!getMaps(conn, match.matchId()).equals(original) || !Objects.equals(oldWinner, match.winnerId()))
                throw new SQLException("Results changed since you opened this match. Reopen it before saving.", "P0001");
            if (nextId != null && !Objects.equals(oldWinner, result.winnerId()))
                advanceWinner(conn, match.tournamentId(), match.matchId(), nextId, nextSlot, oldWinner, result.winnerId());
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM winit.games WHERE game_id=? AND match_id=?")) {
                for (MapResult previous : original) if (!ids.contains(previous.gameId())) {
                    stmt.setObject(1, previous.gameId()); stmt.setObject(2, match.matchId()); stmt.executeUpdate();
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO winit.games AS existing (game_id,match_id,map_name,map_order,team1_score,team2_score,status,winner_id,map_id)
                    SELECT ?,?,?,?,?,?,?,?,map_id FROM winit.maps WHERE name=?
                    ON CONFLICT (game_id) DO UPDATE SET map_name=EXCLUDED.map_name,map_id=EXCLUDED.map_id,
                    map_order=EXCLUDED.map_order,team1_score=EXCLUDED.team1_score,team2_score=EXCLUDED.team2_score,
                    status=EXCLUDED.status,winner_id=EXCLUDED.winner_id WHERE existing.match_id=EXCLUDED.match_id
                    """)) {
                for (MapResult map : result.maps()) {
                    UUID winner = "Completed".equals(map.status()) ? (map.team1Score() > map.team2Score() ? match.team1Id() : match.team2Id()) : null;
                    stmt.setObject(1, map.gameId()); stmt.setObject(2, match.matchId()); stmt.setString(3, map.mapName()); stmt.setInt(4, map.order());
                    stmt.setInt(5, map.team1Score()); stmt.setInt(6, map.team2Score()); stmt.setString(7, map.status()); stmt.setObject(8, winner);
                    stmt.setString(9, map.mapName());
                    if (stmt.executeUpdate() != 1) throw new SQLException("The map is missing from the catalogue or belongs to another match. Reopen the score editor.", "P0001");
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE winit.matches SET status=?,winner_id=? WHERE match_id=?")) {
                stmt.setString(1, result.status()); stmt.setObject(2, result.winnerId()); stmt.setObject(3, match.matchId()); stmt.executeUpdate();
            }
        });
    }

    private void advanceWinner(Connection conn, UUID tournamentId, UUID source, UUID nextId, Integer slot, UUID oldWinner, UUID winner) throws SQLException {
        if (source.equals(nextId) || slot == null || (slot != 1 && slot != 2)) throw new SQLException("This bracket link is invalid.", "P0001");
        String column = slot == 1 ? "team1_id" : "team2_id";
        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT m.*, EXISTS(SELECT 1 FROM winit.games g WHERE g.match_id=m.match_id
                  AND (g.status IN ('Live','Completed') OR COALESCE(g.team1_score,0)>0 OR COALESCE(g.team2_score,0)>0)) AS played
                FROM winit.matches m WHERE m.match_id=? FOR UPDATE
                """)) {
            stmt.setObject(1, nextId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || !Objects.equals(tournamentId, rs.getObject("tournament_id", UUID.class))
                        || !"Upcoming".equals(rs.getString("status")) || rs.getBoolean("played")
                        || rs.getObject("winner_id") != null || !Objects.equals(oldWinner, rs.getObject(column, UUID.class)))
                    throw new SQLException("The next match has started or its slot changed. Correct that later match before changing this winner.", "P0001");
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE winit.matches SET " + column + "=? WHERE match_id=?")) {
            stmt.setObject(1, winner); stmt.setObject(2, nextId); stmt.executeUpdate();
        }
    }

    static void lockTournament(Connection conn, UUID id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT tournament_id FROM winit.tournaments WHERE tournament_id=? FOR UPDATE")) {
            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) { if (!rs.next()) throw new SQLException("This tournament no longer exists.", "P0001"); }
        }
    }

    static void ensureNoBracket(Connection conn, UUID id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM winit.matches WHERE tournament_id=? AND stage IN (?, ?) LIMIT 1")) {
            stmt.setObject(1, id); stmt.setString(2, SingleElimination.STAGE);
            stmt.setString(3, GroupStage.STAGE);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) throw new SQLException("Groups or a bracket are already generated. Registration is closed; use the existing matches.", "P0001"); }
        }
    }

    @FunctionalInterface private interface Transaction { void run(Connection connection) throws SQLException; }
    private void transaction(Transaction work) throws SQLException {
        try (Connection conn = connections.open()) {
            conn.setAutoCommit(false);
            try { work.run(conn); conn.commit(); }
            catch (SQLException | RuntimeException error) {
                try { conn.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                throw error;
            }
        }
    }
}
