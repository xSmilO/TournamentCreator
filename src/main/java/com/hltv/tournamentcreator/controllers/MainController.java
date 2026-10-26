package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.utils.ModalManager;
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
    public void initialize() {
        modalOverlay.setVisible(false);
        ViewManager.initialize(contentPane);
        ModalManager.initialize(modalOverlay);

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
