package com.hltv.tournamentcreator;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;

public class DatabaseManager {
    private static final Dotenv dotenv = Dotenv.load();

    private static final String DB_URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASS = dotenv.get("DB_PASS");

    static public void printAllTeams() {
        String query = "SELECT nickname FROM WinIt.Players";

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                System.out.println("Team: " + rs.getString("nickname"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
