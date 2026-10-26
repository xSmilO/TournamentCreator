package com.hltv.tournamentcreator.controllers;

import javafx.fxml.FXML;

public class PlayerSectionController {

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void openPlayerCreator() {
        if (mainController != null)
            mainController.openModalAndGetData("/com/hltv/tournamentcreator/PlayerCreatorModal.fxml");
    }
}
