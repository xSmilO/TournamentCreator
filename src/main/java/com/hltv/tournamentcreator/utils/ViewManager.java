package com.hltv.tournamentcreator.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import com.hltv.tournamentcreator.controllers.DatabaseSectionController;

import java.net.URL;

public class ViewManager {
    private static ViewManager instance;
    private final Pane contentPane;
    private Button currentActiveButton;
    private Object currentController;
    private final StringProperty searchText = new SimpleStringProperty("");
    private java.util.function.Consumer<String> navigation;

    public void setNavigation(java.util.function.Consumer<String> navigation) { this.navigation = navigation; }
    public void navigate(String section) { if (navigation != null) navigation.accept(section); }

    public StringProperty searchTextProperty() { return searchText; }
    public void openCreator() {
        if (currentController instanceof DatabaseSectionController<?> controller) controller.createEntry();
    }

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
            if (currentController instanceof DatabaseSectionController<?> previous) previous.dispose();
            currentController = loader.getController();

            if (view instanceof Region region) {
                region.prefWidthProperty().bind(contentPane.widthProperty());
                region.prefHeightProperty().bind(contentPane.heightProperty());
            }

            contentPane.getChildren().setAll(view);

        } catch (Exception e) {
            System.err.println("Failed to load view: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
