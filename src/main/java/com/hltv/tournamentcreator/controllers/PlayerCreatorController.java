package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.CountryDAO;
import com.hltv.tournamentcreator.dao.DatabaseManager;
import com.hltv.tournamentcreator.dao.PlayerDAO;
import com.hltv.tournamentcreator.models.Country;
import com.hltv.tournamentcreator.models.Player;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerCreatorController {
    @FXML
    private ComboBox<Country> countryBox;

    @FXML
    private TextField nicknameField;
    @FXML
    private TextField surnameField;
    @FXML
    private TextField nameField;
    @FXML
    private DatePicker dateOfBirthPicker;

    private final PlayerDAO playerDao = new PlayerDAO();
    private final CountryDAO countryDAO = new CountryDAO();

    @FXML
    public void initialize() {
        System.out.println("1. Controller initialized!");

        if (countryBox == null) {
            System.out.println("CRITICAL ERROR: countryBox is NULL! Check your FXML fx:id!");
            return; // Stop here so the app doesn't crash
        }

        System.out.println("2. countryBox is linked correctly.");
        countryBox.setPromptText("Select country");
        setupCountryComboBox();
        loadCountries();
    }

    private void setupCountryComboBox() {
        // This tells the ComboBox how to display the Country object as text
        countryBox.setConverter(new StringConverter<Country>() {
            @Override
            public String toString(Country country) {
                if (country == null) {
                    return "";
                }
                // This is what the user sees in the dropdown (e.g., "Poland")
                return country.name();
            }

            @Override
            public Country fromString(String string) {
                // Not needed for a standard dropdown, just return null
                return null;
            }
        });
    }

    private void loadCountries() {
        // Fetch from DB in the background
        CompletableFuture.supplyAsync(() -> countryDAO.getAllCountries())
                .thenAccept(countries -> {
                    // Update UI on the JavaFX thread
                    Platform.runLater(() -> {
                        countryBox.getItems().setAll(countries);
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    @FXML
    private void handleSavePlayer() {
        String nickname = nicknameField.getText().trim();
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        LocalDate dateOfBirth = dateOfBirthPicker.getValue();
        Country selectedCountry = countryBox.getValue();

        if (nickname.isEmpty() || name.isEmpty() || surname.isEmpty() ||
                selectedCountry == null || dateOfBirth == null) {
            System.out.println("Validation Error: Please fill all fields, select a country, and pick a date.");
            return;
        }

        int countryId = selectedCountry.countryId();

        Player newPlayer = playerDao.createPlayer(nickname, name, surname, countryId, Date.valueOf(dateOfBirth));

        if (newPlayer != null) {
            System.out.println("Success! Created player: " + newPlayer.nickname());
            
        }
    }

}
