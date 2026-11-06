package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.CountryDAO;
import com.hltv.tournamentcreator.dao.PlayerDAO;
import com.hltv.tournamentcreator.models.Country;
import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import com.hltv.tournamentcreator.utils.ModalManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class PlayerCreatorController {
    @FXML private ComboBox<Country> countryBox;
    @FXML private TextField nicknameField, surnameField, firstNameField, dateOfBirthField;
    @FXML private Button addBtn, cancelBtn;
    @FXML private Label statusLabel;
    @FXML private VBox playerCreatorContainer;
    private boolean saving;

    @FXML public void initialize() {
        countryBox.setPromptText("Select country");
        countryBox.setConverter(new StringConverter<>() {
            @Override public String toString(Country country) { return country == null ? "" : country.name(); }
            @Override public Country fromString(String value) { return null; }
        });
        addBtn.setDisable(true);
        statusLabel.setText("Loading countries…");
        BackgroundTask.run(() -> new CountryDAO().getAllCountries(), countries -> {
            countryBox.getItems().setAll(countries);
            statusLabel.setText(countries.isEmpty() ? "No countries available. Add countries to your database first." : "");
            addBtn.setDisable(countries.isEmpty());
        }, error -> statusLabel.setText("Could not load countries. Close this form and try again."));
    }

    @FXML private void handleSavePlayer() {
        if (saving) return;
        try {
            String nickname = CreatorValidation.text(nicknameField.getText(), "Nickname", 50, true);
            String firstName = CreatorValidation.text(firstNameField.getText(), "First name", 50, true);
            String surname = CreatorValidation.text(surnameField.getText(), "Surname", 50, true);
            LocalDate birth;
            try {
                birth = LocalDate.parse(dateOfBirthField.getText().trim(),
                        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT));
            } catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter a valid date of birth as DD-MM-YYYY."); }
            if (birth.isAfter(LocalDate.now())) throw new IllegalArgumentException("Date of birth cannot be in the future.");
            Country country = countryBox.getValue();
            if (country == null) throw new IllegalArgumentException("Please select a country.");
            saving = true;
            playerCreatorContainer.setDisable(true);
            statusLabel.setText("Saving player…");
            BackgroundTask.run(() -> new PlayerDAO().createPlayer(nickname, firstName, surname, country, birth), player -> {
                saving = false;
                ModalManager.getInstance().saved();
            }, error -> {
                saving = false;
                playerCreatorContainer.setDisable(false);
                statusLabel.setText("Could not save the player. Check the database connection and try again.");
            });
        } catch (IllegalArgumentException error) { statusLabel.setText(error.getMessage()); }
    }

    @FXML private void handleCancel() { if (!saving) ModalManager.getInstance().hideModal(); }
}
