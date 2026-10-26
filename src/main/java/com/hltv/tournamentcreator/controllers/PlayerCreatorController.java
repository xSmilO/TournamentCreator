package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.CountryDAO;
import com.hltv.tournamentcreator.dao.DatabaseManager;
import com.hltv.tournamentcreator.dao.PlayerDAO;
import com.hltv.tournamentcreator.models.Country;
import com.hltv.tournamentcreator.models.Player;
import com.hltv.tournamentcreator.utils.Toast;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

public class PlayerCreatorController {
    @FXML
    private ComboBox<Country> countryBox;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextField surnameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField dateOfBirthField;
    @FXML
    private Button addBtn;
    @FXML
    private VBox playerCreatorContainer;

    private final PlayerDAO playerDao = new PlayerDAO();
    private final CountryDAO countryDAO = new CountryDAO();

    private void setupDateFormatter() {
        UnaryOperator<TextFormatter.Change> dateMaskFilter = change -> {

            if (change.isDeleted()) {
                return change;
            }

            String newText = change.getControlNewText();

            if (!newText.matches("[0-9\\-]*")) {
                return null;
            }

            if (newText.length() > 10) {
                return null;
            }

            if (newText.length() == 2 || newText.length() == 5) {
                change.setText(change.getText() + "-");

                change.setCaretPosition(change.getCaretPosition() + 1);
                change.setAnchor(change.getAnchor() + 1);
            }

            return change;
        };

        dateOfBirthField.setTextFormatter(new TextFormatter<>(dateMaskFilter));
    }

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
        countryBox.setConverter(new StringConverter<Country>() {
            @Override
            public String toString(Country country) {
                if (country == null) {
                    return "";
                }
                return country.name();
            }

            @Override
            public Country fromString(String string) {
                return null;
            }
        });
    }

    private void loadCountries() {
        CompletableFuture.supplyAsync(() -> countryDAO.getAllCountries())
                .thenAccept(countries -> {
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
        Stage stage = (Stage) playerCreatorContainer.getScene().getWindow();

        try {
            String nickname = nicknameField.getText().trim();
            String name = firstNameField.getText().trim();
            String surname = surnameField.getText().trim();
            String dateString = dateOfBirthField.getText();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            LocalDate dateOfBirth = LocalDate.parse(dateString, dateFormatter);
            Country selectedCountry = countryBox.getValue();

            if (nickname.isEmpty() || name.isEmpty() || surname.isEmpty() ||
                    selectedCountry == null || dateOfBirth == null) {
                Toast.show(stage, "Validation Error: Please fill all fields, select a country, and pick a date.", Toast.ToastType.WARNING);
                return;
            }

            int countryId = selectedCountry.countryId();

            Player newPlayer = playerDao.createPlayer(nickname, name, surname, countryId, Date.valueOf(dateOfBirth));

            if (newPlayer != null) {
                Toast.show(stage, "Player " + nickname + " saved successfully", Toast.ToastType.SUCCESS);
            } else {
                Toast.show(stage, "Something went wrong while saving the player", Toast.ToastType.ERROR);
            }
        } catch (Exception e) {
            Toast.show(stage, e.getMessage(), Toast.ToastType.ERROR);
        }
    }

    @FXML
    private void handleCancel() {
        Pane modalOverlay = (Pane) playerCreatorContainer.getParent();
        if (modalOverlay != null) {
            modalOverlay.getChildren().clear();
            modalOverlay.setVisible(false);
        }
    }
}
