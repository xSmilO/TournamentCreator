package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.GroupStage;
import com.hltv.tournamentcreator.utils.MatchResults;
import com.hltv.tournamentcreator.utils.SingleElimination;
import java.sql.*;
import java.util.*;

public class GroupStageDAO {
    private final ConnectionSource connections;
    public GroupStageDAO() { this(DatabaseManager::getConnection); }
    GroupStageDAO(ConnectionSource connections) { this.connections = connections; }

    public List<TournamentGroup> getGroups(UUID tournamentId) throws SQLException {
        try (Connection conn = connections.open()) { return getGroups(conn, tournamentId); }
    }

    List<TournamentGroup> getGroups(Connection conn, UUID tournamentId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("""
                SELECT g.group_id, g.name AS group_name, g.group_order, t.team_id, t.name AS team_name, p.seed_number
                FROM winit.tournament_groups g
                LEFT JOIN winit.tournament_group_teams gt ON gt.group_id=g.group_id AND gt.tournament_id=g.tournament_id
                LEFT JOIN winit.teams t ON t.team_id=gt.team_id
                LEFT JOIN winit.tournament_participants p ON p.tournament_id=g.tournament_id AND p.team_id=gt.team_id
                WHERE g.tournament_id=? ORDER BY g.group_order, gt.group_seed
                """)) {
            stmt.setObject(1, tournamentId);
            try (ResultSet rs = stmt.executeQuery()) {
                Map<UUID,TournamentGroup> groups = new LinkedHashMap<>();
                Map<UUID,List<TournamentParticipant>> teams = new HashMap<>();
                while (rs.next()) {
                    UUID id = rs.getObject("group_id", UUID.class);
                    groups.putIfAbsent(id, new TournamentGroup(id, rs.getString("group_name"), rs.getInt("group_order"), List.of()));
                    teams.computeIfAbsent(id, key -> new ArrayList<>());
                    UUID team = rs.getObject("team_id", UUID.class);
                    if (team != null) teams.get(id).add(new TournamentParticipant(team, rs.getString("team_name"), rs.getObject("seed_number", Integer.class)));
                }
                return groups.values().stream().map(g -> new TournamentGroup(g.groupId(),g.name(),g.order(),teams.get(g.groupId()))).toList();
            }
        }
    }

    public void generateGroups(UUID tournamentId, String format) throws SQLException {
        MatchResults.mapLimit(format);
        transaction(conn -> {
            MatchDAO.lockTournament(conn, tournamentId);
            requireEmpty(conn, "SELECT 1 FROM winit.matches WHERE tournament_id=? LIMIT 1", tournamentId,
                    "This tournament already has matches. Groups can only be drawn before matches are created.");
            requireEmpty(conn, "SELECT 1 FROM winit.tournament_groups WHERE tournament_id=? LIMIT 1", tournamentId,
                    "Groups have already been generated.");
            List<TournamentParticipant> teams = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT p.team_id,t.name,p.seed_number FROM winit.tournament_participants p
                    JOIN winit.teams t ON t.team_id=p.team_id WHERE p.tournament_id=?
                    ORDER BY p.seed_number NULLS LAST,p.team_id
                    """)) {
                stmt.setObject(1,tournamentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) teams.add(new TournamentParticipant(rs.getObject("team_id",UUID.class),rs.getString("name"),rs.getObject("seed_number",Integer.class)));
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("SELECT team_limit FROM winit.tournaments WHERE tournament_id=?")) {
                stmt.setObject(1,tournamentId);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    if (teams.size()>rs.getInt("team_limit")) throw new SQLException("Registered teams exceed the tournament's team limit.","P0001");
                }
            }
            List<TournamentGroup> groups = GroupStage.draw(teams);
            try (PreparedStatement groupStmt = conn.prepareStatement("INSERT INTO winit.tournament_groups (group_id,tournament_id,name,group_order) VALUES (?,?,?,?)");
                 PreparedStatement teamStmt = conn.prepareStatement("INSERT INTO winit.tournament_group_teams (group_id,tournament_id,team_id,group_seed) VALUES (?,?,?,?)");
                 PreparedStatement matchStmt = conn.prepareStatement("""
                         INSERT INTO winit.matches (match_id,tournament_id,team1_id,team2_id,match_format,status,stage,round_name,group_id)
                         VALUES (?,?,?,?,?,'Upcoming',?,?,?)
                         """)) {
                for (var group : groups) {
                    groupStmt.setObject(1,group.groupId()); groupStmt.setObject(2,tournamentId); groupStmt.setString(3,group.name()); groupStmt.setInt(4,group.order());
                    groupStmt.executeUpdate();
                    for (int i=0;i<group.teams().size();i++) {
                        teamStmt.setObject(1,group.groupId()); teamStmt.setObject(2,tournamentId); teamStmt.setObject(3,group.teams().get(i).teamId()); teamStmt.setInt(4,i+1);
                        teamStmt.executeUpdate();
                    }
                    for (var fixture : GroupStage.fixtures(group)) {
                        matchStmt.setObject(1,fixture.matchId()); matchStmt.setObject(2,tournamentId);
                        matchStmt.setObject(3,fixture.team1()); matchStmt.setObject(4,fixture.team2()); matchStmt.setString(5,format);
                        matchStmt.setString(6,GroupStage.STAGE); matchStmt.setString(7,group.name()+" · Round "+fixture.round()); matchStmt.setObject(8,group.groupId());
                        matchStmt.executeUpdate();
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE winit.tournaments SET format='Groups → Playoffs', lifecycle_state=CASE WHEN lifecycle_state='Registration' THEN 'Draft' ELSE lifecycle_state END WHERE tournament_id=?")) {
                stmt.setObject(1,tournamentId); stmt.executeUpdate();
            }
        });
    }

    public void generatePlayoffs(UUID tournamentId, String format) throws SQLException {
        MatchResults.mapLimit(format);
        transaction(conn -> {
            MatchDAO.lockTournament(conn,tournamentId);
            try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM winit.matches WHERE tournament_id=? AND stage=? LIMIT 1")) {
                stmt.setObject(1,tournamentId); stmt.setString(2,SingleElimination.STAGE);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) throw new SQLException("Playoffs have already been generated.","P0001");
                }
            }
            List<TournamentGroup> groups = getGroups(conn,tournamentId);
            List<TournamentMatch> matches = new MatchDAO(connections).getMatches(conn,tournamentId);
            List<UUID> qualifiers = GroupStage.playoffSeeds(groups,matches);
            MatchDAO.insertBracket(conn,tournamentId,format,SingleElimination.generate(qualifiers));
        });
    }

    private void requireEmpty(Connection conn,String sql,UUID id,String message) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1,id);
            try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) throw new SQLException(message,"P0001"); }
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
