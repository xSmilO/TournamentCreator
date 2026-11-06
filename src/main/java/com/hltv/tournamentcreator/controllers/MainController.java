package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.utils.Icons;
import com.hltv.tournamentcreator.utils.ModalManager;
import com.hltv.tournamentcreator.utils.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

public class MainController {
    @FXML private StackPane contentPane;
    @FXML private StackPane modalOverlay;
    @FXML private Button navDashboardBtn, navTournamentsBtn, navTeamsBtn, navPlayersBtn;
    @FXML private TextField globalSearch;

    @FXML public void initialize() {
        ViewManager.initialize(contentPane);
        ModalManager.initialize(modalOverlay);
        navDashboardBtn.setGraphic(Icons.of("grid"));
        navTournamentsBtn.setGraphic(Icons.of("trophy"));
        navTeamsBtn.setGraphic(Icons.of("users"));
        navPlayersBtn.setGraphic(Icons.of("user"));
        globalSearch.textProperty().bindBidirectional(ViewManager.getInstance().searchTextProperty());
        ViewManager.getInstance().setNavigation(section -> {
            switch (section) {
                case "Teams": onTeamsClicked(); break;
                case "Players": onPlayersClicked(); break;
                default: onTournamentsClicked(); break;
            }
        });
        onDashboardClicked();
    }

    private void navigate(String section, Button button) {
        ViewManager.getInstance().searchTextProperty().set("");
        globalSearch.setDisable(section.equals("Overview"));
        ViewManager.getInstance().switchSection("/com/hltv/tournamentcreator/" + section + "Section.fxml", button);
    }

    @FXML private void onDashboardClicked() { navigate("Overview", navDashboardBtn); }
    @FXML private void onTournamentsClicked() { navigate("Tournaments", navTournamentsBtn); }
    @FXML private void onTeamsClicked() { navigate("Teams", navTeamsBtn); }
    @FXML private void onPlayersClicked() { navigate("Players", navPlayersBtn); }
    @FXML private void createTournament() { onTournamentsClicked(); ViewManager.getInstance().openCreator(); }
    @FXML private void createTeam() { onTeamsClicked(); ViewManager.getInstance().openCreator(); }
    @FXML private void createPlayer() { onPlayersClicked(); ViewManager.getInstance().openCreator(); }
}
