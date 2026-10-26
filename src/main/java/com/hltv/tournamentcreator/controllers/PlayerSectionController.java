package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.PlayerDAO;
import com.hltv.tournamentcreator.models.Player;
import com.hltv.tournamentcreator.utils.ModalManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

public class PlayerSectionController {
    @FXML private TableView<Player> playersTable;
    @FXML private TableColumn<Player, String> colNickname;
    @FXML private TableColumn<Player, String> colFirstName;
    @FXML private TableColumn<Player, String> colSurname;
    @FXML private TableColumn<Player, String> colCountry;
    @FXML private TableColumn<Player, String> colTeam;

    private final PlayerDAO playerDAO = new PlayerDAO();

    @FXML
    private void openPlayerCreator() {
        ModalManager.getInstance().showModal("/com/hltv/tournamentcreator/PlayerCreatorModal.fxml");
    }

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colNickname.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nickname()));
        colFirstName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().firstName()));
        colSurname.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().lastName()));


        colCountry.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().country().name()) // Assuming your Country record has name()
        );

        colTeam.setCellValueFactory(data -> {
            // Check if the team is null (Free Agent)
            return new SimpleStringProperty(
                    data.getValue().team() != null ? data.getValue().team().name() : "No team"
            );
        });
//
//        colDateOfBirth.setCellValueFactory(data ->
//                new SimpleObjectProperty<>(data.getValue().dateOfBirth())
//        );
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        colDateOfBirth.setCellFactory(column -> new TableCell<Player, LocalDate>() {
//            @Override
//            protected void updateItem(LocalDate date, boolean empty) {
//                super.updateItem(date, empty);
//                if (empty || date == null) {
//                    setText(null);
//                } else {
//                    setText(formatter.format(date));
//                }
//            }
//        });
    }

    private void loadData() {
        CompletableFuture.supplyAsync(() -> playerDAO.getAllPlayers())
                .thenAccept(players -> {
                    // Must update JavaFX UI on the main thread
                    Platform.runLater(() -> {
                        playersTable.getItems().setAll(players);
                    });
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }
}
