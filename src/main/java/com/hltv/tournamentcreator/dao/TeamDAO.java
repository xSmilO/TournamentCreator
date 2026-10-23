package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.Team;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamDAO {
    private final Connection conn;

    public TeamDAO(Connection conn) {
        this.conn = conn;
    }

    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM winit.teams ORDER BY world_ranking ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                teams.add(mapRowToTeam(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }

    public Team createTeam(String name, String country, Integer worldRanking, String logoUrl) {
        String sql = """
            INSERT INTO winit.teams (name, country, world_ranking, logo_url) 
            VALUES (?, ?, ?, ?) 
            RETURNING *
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, country);
            stmt.setObject(3, worldRanking, Types.INTEGER);
            stmt.setString(4, logoUrl);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTeam(rs); // Returns the newly created Team with its generated UUID
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper method to keep mapping logic in one place
    private Team mapRowToTeam(ResultSet rs) throws SQLException {
        return new Team(
                rs.getObject("team_id", UUID.class),
                rs.getString("name"),
                rs.getString("country"),
                rs.getObject("world_ranking", Integer.class),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}