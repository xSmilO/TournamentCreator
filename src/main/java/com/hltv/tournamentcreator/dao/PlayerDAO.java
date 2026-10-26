package com.hltv.tournamentcreator.dao;


import com.hltv.tournamentcreator.models.Country;
import com.hltv.tournamentcreator.models.Player;
import com.hltv.tournamentcreator.models.Team;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDAO {
    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();

        String sql = """
                    SELECT p.*, 
                           t.team_id, t.name AS t_name,
                           t.world_ranking AS t_world_ranking,
                           c.country_id, c.name AS c_name,
                           c.code AS c_code
                    FROM winit.players p 
                    LEFT JOIN winit.teams t ON p.team_id = t.team_id
                    LEFT JOIN winit.countries c ON p.country_id = c.country_id
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // 1. Build the Country object
                Country country = new Country(
                        rs.getInt("country_id"),
                        rs.getString("c_name"),
                        rs.getString("c_code")
                );

                // 2. Build the Team object (check if null first)
                Team team = null;
                if (rs.getString("t_name") != null) {
                    team = new Team(
                            rs.getObject("team_id", java.util.UUID.class),
                            rs.getString("t_name"),
                            rs.getInt("t_world_ranking")
                    );
                }

                // 3. Build the Player with the composed objects
                Player player = new Player(
                        rs.getObject("player_id", UUID.class),
                        rs.getString("nickname"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        country,
                        rs.getObject("date_of_birth", java.time.LocalDate.class),
                        team,
                        rs.getDouble("headshot_percentage"),
                        rs.getDouble("hltv_rating"),
                        rs.getObject("created_at", java.time.LocalDateTime.class)
                );

                players.add(player);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public Player createPlayer(String nickname, String firstName, String lastName, Country country, java.time.LocalDate dateOfBirth) {
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
            stmt.setInt(4, country.countryId());
            stmt.setObject(5, dateOfBirth); // Safely handles null UUIDs

            // 2. Execute and process the ResultSet
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Player(
                            rs.getObject("player_id", java.util.UUID.class),
                            nickname,
                            firstName,
                            lastName,
                            country,      // We already have the full Country object from the method arguments!
                            dateOfBirth,
                            null,         // Team is intentionally set to null (Free Agent)
                            rs.getDouble("headshot_percentage"), // 0.0 by default from DB
                            rs.getDouble("hltv_rating"),         // 0.0 by default from DB
                            rs.getObject("created_at", java.time.LocalDateTime.class)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert new player: " + nickname);
            e.printStackTrace();
        }
        return null;
    }
}