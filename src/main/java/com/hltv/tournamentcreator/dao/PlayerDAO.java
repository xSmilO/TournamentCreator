package com.hltv.tournamentcreator.dao;


import com.hltv.tournamentcreator.models.Player;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDAO {
    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM winit.players ORDER BY nickname ASC";

        // The DAO handles its own connection lifecycle
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                players.add(mapRowToPlayer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public Player createPlayer(String nickname, String firstName, String lastName, int countryId, Date dateOfBirth) {
        String sql = """
                    INSERT INTO winit.players (nickname, first_name, last_name, country_id, date_of_birth) 
                    VALUES (?, ?, ?, ?, ?) 
                    RETURNING *
                """;

        // 1. Open the Connection AND the PreparedStatement together
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nickname);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setInt(4, countryId);
            stmt.setObject(5, dateOfBirth); // Safely handles null UUIDs

            // 2. Execute and process the ResultSet
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPlayer(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert new player: " + nickname);
            e.printStackTrace();
        }
        return null;
    }

    private Player mapRowToPlayer(ResultSet rs) throws SQLException {
        return new Player(
                rs.getObject("player_id", UUID.class),
                rs.getString("nickname"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getInt("country_id"),
                rs.getDate("date_of_birth"),
                rs.getObject("team_id", UUID.class),
                rs.getDouble("headshot_percentage"),
                rs.getDouble("hltv_rating"),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}