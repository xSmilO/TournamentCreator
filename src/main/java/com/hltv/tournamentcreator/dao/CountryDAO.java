package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CountryDAO {

    public List<Country> getAllCountries() {
        List<Country> countries = new ArrayList<>();
        String sql = "SELECT * FROM winit.countries ORDER BY name ASC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                countries.add(new Country(
                        rs.getInt("country_id"),
                        rs.getString("name"),
                        rs.getString("code")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return countries;
    }
}
