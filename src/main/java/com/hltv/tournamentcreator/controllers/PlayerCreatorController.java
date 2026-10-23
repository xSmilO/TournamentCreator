package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.DatabaseManager;
import com.hltv.tournamentcreator.dao.PlayerDAO;
import com.hltv.tournamentcreator.models.Player;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PlayerCreatorController {
    @FXML
    private ComboBox countryBox;

    @FXML
    public void initialize() {
        countryBox.getItems().addAll(
                "Polska",
                "Niemcy",
                "Finlandia"
        );

        countryBox.setPromptText("Select country");
    }

    private void loadPlayerData() {
        try (Connection conn = DatabaseManager.getConnection()) {

            PlayerDAO playerDAO = new PlayerDAO(conn);
            List<Player> players = playerDAO.getAllPlayers();

            ObservableList<Player> observablePlayers = FXCollections.observableArrayList(players);
//            playerTable.setItems(observablePlayers);

        } catch (SQLException e) {
            System.err.println("Failed to load players from database.");
            e.printStackTrace();
        }
    }
}
