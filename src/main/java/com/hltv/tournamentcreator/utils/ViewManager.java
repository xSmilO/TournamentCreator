package com.hltv.tournamentcreator.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

import java.net.URL;

public class ViewManager {
    private static ViewManager instance;
    private final Pane contentPane;
    private Button currentActiveButton;

    private ViewManager(Pane contentPane) {
        this.contentPane = contentPane;
    }

    public static void initialize(Pane contentPane) {
        if(instance == null)
            instance = new ViewManager(contentPane);
    }

    public static ViewManager getInstance() {
        return instance;
    }

    public void setCurrentActiveButton(Button button) {
        this.currentActiveButton = button;
    }

    public void switchSection(String fxmlPath, Button clickedButton) {
        try {
            if (currentActiveButton != null) {
                currentActiveButton.getStyleClass().remove("active");
            }

            if (clickedButton != null) {
                if (!clickedButton.getStyleClass().contains("active")) {
                    clickedButton.getStyleClass().add("active");
                }
                currentActiveButton = clickedButton;
            }

            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new RuntimeException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            System.err.println("Failed to load view: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
