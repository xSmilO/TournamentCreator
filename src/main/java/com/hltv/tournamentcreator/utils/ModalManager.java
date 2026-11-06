package com.hltv.tournamentcreator.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import java.net.URL;

public class ModalManager {

    private static ModalManager instance;
    private final Pane modalOverlay;
    private Runnable onSaved;

    // Private constructor for Singleton pattern
    private ModalManager(Pane modalOverlay) {
        this.modalOverlay = modalOverlay;
    }

    public static void initialize(Pane modalOverlay) {
        if (instance == null) {
            instance = new ModalManager(modalOverlay);
        }
    }

    public static ModalManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ModalManager is not initialized! Call initialize() first.");
        }
        return instance;
    }

    public void showModal(String fxmlPath) {
        showModal(fxmlPath, null);
    }

    public void showModal(String fxmlPath, Runnable onSaved) {
        this.onSaved = onSaved;
        try {
            URL resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                throw new RuntimeException("Cannot find FXML file: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node modalNode = loader.load();

            modalOverlay.getChildren().clear();

            modalOverlay.getChildren().add(modalNode);
            modalOverlay.setVisible(true);

            modalOverlay.toFront();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load modal: " + fxmlPath, e);
        }
    }

    public void hideModal() {
        onSaved = null;
        modalOverlay.getChildren().clear();
        modalOverlay.setVisible(false);

        modalOverlay.toBack();
    }

    public void showModal(Node content) {
        onSaved = null;
        modalOverlay.getChildren().setAll(content);
        modalOverlay.setVisible(true);
        modalOverlay.toFront();
    }

    public void saved() {
        Runnable callback = onSaved;
        hideModal();
        if (callback != null) callback.run();
    }
}
