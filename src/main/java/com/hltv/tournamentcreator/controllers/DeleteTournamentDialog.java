package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.TournamentDAO;
import com.hltv.tournamentcreator.models.Tournament;
import com.hltv.tournamentcreator.utils.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public final class DeleteTournamentDialog {
    private final Tournament tournament;
    private final TournamentDAO dao;
    private final Runnable onDeleted;
    @FXML private VBox root;
    @FXML private Label title, status;
    @FXML private TextField confirmation;
    @FXML private Button delete;
    public DeleteTournamentDialog(Tournament tournament, TournamentDAO dao, Runnable onDeleted) {
        this.tournament=tournament; this.dao=dao; this.onDeleted=onDeleted;
    }
    public void show() {
        var view=FxmlViews.load("DeleteTournament.fxml",this);
        title.setText(tournament.name());
        status.visibleProperty().bind(status.textProperty().isNotEmpty());
        status.managedProperty().bind(status.visibleProperty());
        delete.disableProperty().bind(confirmation.textProperty().isNotEqualTo(tournament.name()));
        ModalManager.getInstance().showModal(view.root());
    }
    @FXML private void cancel() { ModalManager.getInstance().hideModal(); }
    @FXML private void delete() {
        if (!tournament.name().equals(confirmation.getText())) return;
        root.setDisable(true); status.setText("Deleting tournament…");
        BackgroundTask.run(() -> { dao.deleteTournament(tournament.tournamentId(),tournament.name()); return null; }, ignored -> {
            ModalManager.getInstance().hideModal(); onDeleted.run();
        }, error -> {
            root.setDisable(false);
            status.setText(error instanceof java.sql.SQLException sql && "P0001".equals(sql.getSQLState())
                    ? sql.getMessage() : "Could not confirm deletion. Close this dialog and refresh before retrying.");
        });
    }
}
