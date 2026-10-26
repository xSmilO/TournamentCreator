package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.utils.ViewManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML
    private Pane contentPane;
    @FXML
    private StackPane modalOverlay;

    @FXML
    private Button navDashboardBtn;
    @FXML
    private Button navTournamentsBtn;
    @FXML
    private Button navTeamsBtn;
    @FXML
    private Button navPlayersBtn;


    @FXML
    public void openModalAndGetData(String modalPath) {
        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/hltv/tournamentcreator/PlayerCreatorModal.fxml"));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(modalPath));
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
        modalOverlay.setVisible(false);
        ViewManager.initialize(contentPane);

        ViewManager.getInstance().switchSection("/com/hltv/tournamentcreator/PlayersSection.fxml", navPlayersBtn);
    }

    @FXML
    private void onDashboardClicked() {
        ViewManager.getInstance().switchSection("", navDashboardBtn);
    }

    @FXML
    private void onTournamentsClicked() {
        ViewManager.getInstance().switchSection("", navTournamentsBtn);
    }

    @FXML
    private void onTeamsClicked() {
        ViewManager.getInstance().switchSection("", navTeamsBtn);
    }

    @FXML
    private void onPlayersClicked() {
        ViewManager.getInstance().switchSection("/com/hltv/tournamentcreator/PlayersSection.fxml", navPlayersBtn);
    }
}
