package com.hltv.tournamentcreator.dao;


import com.hltv.tournamentcreator.models.Player;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerDAO {
    private final Connection conn;

    public PlayerDAO(Connection conn) {
        this.conn = conn;
    }

    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM winit.players ORDER BY nickname ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                players.add(mapRowToPlayer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public List<Player> getPlayersByTeam(UUID teamId) {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT * FROM winit.players WHERE team_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, teamId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    players.add(mapRowToPlayer(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public Player createPlayer(String nickname, String firstName, String lastName, String country, UUID teamId) {
        String sql = """
            INSERT INTO winit.players (nickname, first_name, last_name, country, team_id) 
            VALUES (?, ?, ?, ?, ?) 
            RETURNING *
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nickname);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, country);
            stmt.setObject(5, teamId); // Will safely insert NULL if teamId is null

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPlayer(rs);
                }
            }
        } catch (SQLException e) {
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
                rs.getString("country"),
                rs.getObject("team_id", UUID.class),
                rs.getObject("headshot_percentage", Double.class),
                rs.getObject("hltv_rating", Double.class),
                rs.getObject("created_at", LocalDateTime.class)
        );
    }
}