package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.utils.ModalManager;
import javafx.fxml.FXML;

public class PlayerSectionController {
    @FXML
    private void openPlayerCreator() {
        ModalManager.getInstance().showModal("/com/hltv/tournamentcreator/PlayerCreatorModal.fxml");
    }
}
