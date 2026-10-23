package com.hltv.tournamentcreator.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

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
}
