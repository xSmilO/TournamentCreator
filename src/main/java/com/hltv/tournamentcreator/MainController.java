package com.hltv.tournamentcreator;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML
    private Label welcomeText;

    @FXML
    private Pane contentPane;

    @FXML
    private StackPane modalOverlay;

    @FXML
    private void openModalAndGetData() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hltv/tournamentcreator/example-modal.fxml"));
            Node modalNode = loader.load();

            modalOverlay.getChildren().clear();
            modalOverlay.getChildren().add(modalNode);
            modalOverlay.setVisible(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void initialize() {
//        loadView("/com/example/tournamentcreator/PlayerCreatorPanel.fxml");
        modalOverlay.setVisible(false);
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
}
