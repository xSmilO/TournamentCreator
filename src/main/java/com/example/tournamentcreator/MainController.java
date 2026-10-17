package com.example.tournamentcreator;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {
    @FXML
    private Label welcomeText;

    @FXML
    private StackPane contentPane;

    @FXML
    public void initialize() {
        loadView("/com/example/tournamentcreator/PlayerCreatorPanel.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            var resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                throw new RuntimeException(
                        "Nie znaleziono pliku FXML: " + fxmlPath
                );
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Witaj świat");
    }
}
