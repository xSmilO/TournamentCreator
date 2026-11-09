package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.Team;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {
    public record ScheduledMatch(String tournament, String team1, String team2, String stage,
                                 String format, String status, LocalDateTime start) {}
    public record Snapshot(int tournaments, int teams, int players, int played, int scheduled,
                           List<ScheduledMatch> matches, List<Team> topTeams) {}

    public Snapshot load() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            int tournaments, teams, players, played, scheduled;
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT (SELECT count(*) FROM winit.tournaments) AS tournaments,
                           (SELECT count(*) FROM winit.teams) AS teams,
                           (SELECT count(*) FROM winit.players) AS players,
                           (SELECT count(*) FROM winit.matches WHERE status = 'Completed' AND outcome_type='Played' AND team1_id IS NOT NULL AND team2_id IS NOT NULL) AS played,
                           (SELECT count(*) FROM winit.matches m JOIN winit.tournaments t ON t.tournament_id=m.tournament_id
                            WHERE m.status = 'Upcoming' AND m.team1_id IS NOT NULL AND m.team2_id IS NOT NULL
                            AND t.lifecycle_state NOT IN ('Completed','Cancelled') AND NOT t.archived) AS scheduled
                    """); ResultSet rs = stmt.executeQuery()) {
                rs.next();
                tournaments = rs.getInt("tournaments"); teams = rs.getInt("teams");
                players = rs.getInt("players"); played = rs.getInt("played"); scheduled = rs.getInt("scheduled");
            }
            List<ScheduledMatch> matches = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT COALESCE(t.name, 'Unassigned tournament') AS tournament,
                           COALESCE(a.name, 'TBD') AS team1, COALESCE(b.name, 'TBD') AS team2,
                           m.stage, m.match_format, m.status, m.start_time
                    FROM winit.matches m LEFT JOIN winit.tournaments t ON t.tournament_id = m.tournament_id
                    LEFT JOIN winit.teams a ON a.team_id = m.team1_id
                    LEFT JOIN winit.teams b ON b.team_id = m.team2_id
                    WHERE m.status IN ('Upcoming', 'Live') AND t.lifecycle_state NOT IN ('Completed','Cancelled') AND NOT t.archived AND m.team1_id IS NOT NULL AND m.team2_id IS NOT NULL
                    ORDER BY CASE WHEN m.status = 'Live' THEN 0 ELSE 1 END, m.start_time NULLS LAST LIMIT 5
                    """); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) matches.add(new ScheduledMatch(rs.getString("tournament"), rs.getString("team1"),
                        rs.getString("team2"), rs.getString("stage"), rs.getString("match_format"),
                        rs.getString("status"), rs.getObject("start_time", LocalDateTime.class)));
            }
            List<Team> topTeams = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement("""
                    SELECT t.team_id, t.name, t.world_ranking,
                           (SELECT count(*) FROM winit.players p WHERE p.team_id = t.team_id) AS player_count
                    FROM winit.teams t WHERE t.world_ranking IS NOT NULL
                    ORDER BY t.world_ranking, t.name, t.team_id LIMIT 5
                    """); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) topTeams.add(new Team(rs.getObject("team_id", java.util.UUID.class),
                        rs.getString("name"), rs.getObject("world_ranking", Integer.class), rs.getInt("player_count")));
            }
            return new Snapshot(tournaments, teams, players, played, scheduled, matches, topTeams);
        }
    }
}
