package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.TeamDAO;
import com.hltv.tournamentcreator.models.RosterPlayer;
import com.hltv.tournamentcreator.models.Team;
import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import com.hltv.tournamentcreator.utils.FxmlViews;
import com.hltv.tournamentcreator.utils.ModalManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Function;

/** Coordinates the three FXML views for roster, player selection, and role confirmation. */
public final class TeamRosterDialog {
    private final Team team;
    private final TeamDAO dao;
    private final Runnable onChanged;
    private final ObservableList<RosterPlayer> available = FXCollections.observableArrayList();
    private RosterPlayer selectedPlayer;
    private boolean editingRole;
    @FXML private VBox rosterRoot, pickerRoot, roleRoot;
    @FXML private Label rosterTitle, rosterStatus, pickerCaption, pickerStatus, pickerPlaceholder, roleTitle, rolePrompt, roleStatus, roleCaption;
    @FXML private TableView<RosterPlayer> rosterTable, playerTable;
    @FXML private TableColumn<RosterPlayer, String> nicknameColumn, nameColumn, countryColumn, ratingColumn, roleColumn;
    @FXML private TextField search;
    @FXML private Button choose, pickerRefresh, editRole, confirmRoleButton, roleBack, transferPlayer, releasePlayer;
    @FXML private ComboBox<String> role;

    public TeamRosterDialog(Team team, TeamDAO dao, Runnable onChanged) {
        this.team = team; this.dao = dao; this.onChanged = onChanged;
    }

    @FXML public void showRoster() {
        var view = FxmlViews.load("TeamRosterModal.fxml", this);
        bindColumns(true);
        rosterTitle.setText(team.name());
        editRole.disableProperty().bind(rosterTable.getSelectionModel().selectedItemProperty().isNull());
        transferPlayer.disableProperty().bind(rosterTable.getSelectionModel().selectedItemProperty().isNull());
        releasePlayer.disableProperty().bind(rosterTable.getSelectionModel().selectedItemProperty().isNull());
        ModalManager.getInstance().showModal(view.root());
        TableView<RosterPlayer> currentTable = rosterTable;
        Label currentStatus = rosterStatus;
        BackgroundTask.run(() -> dao.getRosterMembers(team.teamId()), players -> {
            currentTable.getItems().setAll(players);
            currentStatus.setText(players.size() + " players · Roles belong to the current team");
        }, error -> currentStatus.setText(errorMessage(error)));
    }

    @FXML public void showPlayerPicker() {
        var view = FxmlViews.load("AvailablePlayersModal.fxml", this);
        bindColumns(false);
        pickerCaption.setText("ADD PLAYER · " + team.name());
        FilteredList<RosterPlayer> filtered = new FilteredList<>(available);
        SortedList<RosterPlayer> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(playerTable.comparatorProperty());
        playerTable.setItems(sorted);
        search.textProperty().addListener((obs, old, value) -> {
            String query = value.trim().toLowerCase(Locale.ROOT);
            filtered.setPredicate(player -> (player.nickname() + " " + player.fullName() + " " + display(player.country()))
                    .toLowerCase(Locale.ROOT).contains(query));
        });
        choose.disableProperty().bind(playerTable.getSelectionModel().selectedItemProperty().isNull());
        ModalManager.getInstance().showModal(view.root());
        loadAvailable();
        search.requestFocus();
    }

    @FXML private void loadAvailable() {
        available.clear();
        pickerPlaceholder.setText("Loading players…");
        pickerRefresh.setDisable(true);
        pickerStatus.setText("Loading available players…");
        BackgroundTask.run(dao::getAvailablePlayers, players -> {
            available.setAll(players);
            pickerPlaceholder.setText("No available players match your search.");
            pickerStatus.setText(players.size() + " free agents · Click a player to choose their role");
            pickerRefresh.setDisable(false);
        }, error -> {
            pickerPlaceholder.setText("Could not load players. Try Refresh.");
            pickerStatus.setText(errorMessage(error));
            pickerRefresh.setDisable(false);
        });
    }

    @FXML private void selectClickedPlayer(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 1) return;
        Node target = event.getTarget() instanceof Node node ? node : null;
        while (target != null && target != playerTable) {
            if (target instanceof TableRow<?> row) {
                if (!row.isEmpty()) showRolePrompt((RosterPlayer) row.getItem());
                return;
            }
            target = target.getParent();
        }
    }
    @FXML private void onPlayerKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) { event.consume(); chooseSelectedPlayer(); }
    }
    @FXML private void chooseSelectedPlayer() { showRolePrompt(playerTable.getSelectionModel().getSelectedItem()); }

    private void showRolePrompt(RosterPlayer player) {
        showRolePrompt(player, false);
    }

    @FXML private void editSelectedRole() {
        showRolePrompt(rosterTable.getSelectionModel().getSelectedItem(), true);
    }

    private void showRolePrompt(RosterPlayer player, boolean edit) {
        if (player == null) return;
        selectedPlayer = player;
        editingRole = edit;
        var view = FxmlViews.load("PlayerRoleModal.fxml", this);
        roleTitle.setText(player.nickname());
        roleCaption.setText(edit ? "EDIT TEAM ROLE" : "ASSIGN ROLE");
        rolePrompt.setText(edit ? "Update " + player.nickname() + "'s role in " + team.name() + "."
                : "What role will " + player.nickname() + " play in " + team.name() + "?");
        confirmRoleButton.setText(edit ? "Save role  →" : "Add to team  →");
        roleBack.setText(edit ? "Back to roster" : "Back to players");
        ModalManager.getInstance().showModal(view.root());
        if (edit) {
            role.setValue(player.role());
            role.getEditor().setText(player.role() == null ? "" : player.role());
        }
        role.requestFocus();
    }
    @FXML private void confirmRole() {
        final String selectedRole;
        try { selectedRole = CreatorValidation.text(role.getEditor().getText(), "Role", 50, true); }
        catch (IllegalArgumentException error) { roleStatus.setText(error.getMessage()); return; }
        RosterPlayer player = selectedPlayer;
        boolean edit = editingRole;
        roleRoot.setDisable(true);
        roleStatus.setText(edit ? "Saving role…" : "Adding player…");
        BackgroundTask.run(() -> {
            if (edit) dao.updatePlayerRole(team.teamId(), player.playerId(), player.role(), selectedRole);
            else dao.assignPlayer(team.teamId(), player.playerId(), selectedRole);
            return null;
        }, ignored -> {
            onChanged.run(); showRoster();
        }, error -> {
            roleRoot.setDisable(false);
            roleStatus.setText(errorMessage(error));
        });
    }
    @FXML private void cancelRole() {
        if (editingRole) backToRoster();
        else ModalManager.getInstance().showModal(pickerRoot);
    }
    @FXML private void backToRoster() {
        if (rosterRoot == null) showRoster();
        else ModalManager.getInstance().showModal(rosterRoot);
    }
    @FXML private void transferPlayer() { movePlayer(false); }
    @FXML private void releasePlayer() { movePlayer(true); }
    private void movePlayer(boolean release) {
        var player = rosterTable.getSelectionModel().getSelectedItem();
        if (player != null) new OrganizerEditor(() -> { onChanged.run(); showRoster(); }, this::showRoster).move(team, player, release);
    }
    @FXML private void close() { ModalManager.getInstance().hideModal(); }

    private void bindColumns(boolean withRole) {
        column(nicknameColumn, RosterPlayer::nickname);
        column(nameColumn, RosterPlayer::fullName);
        column(countryColumn, player -> display(player.country()));
        column(ratingColumn, player -> player.rating() == null ? "—" : String.format(Locale.ROOT, "%.2f", player.rating()));
        if (withRole) column(roleColumn, player -> player.role() == null ? "Unassigned" : player.role());
    }
    private void column(TableColumn<RosterPlayer, String> column, Function<RosterPlayer, String> value) {
        column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue())));
    }
    private String display(String text) { return text == null ? "—" : text; }
    private String errorMessage(Throwable error) {
        if (error instanceof SQLException sql) {
            if ("42703".equals(sql.getSQLState())) return "Team roles are not set up in the database yet. Please apply the team-role migration.";
            if ("P0001".equals(sql.getSQLState())) return sql.getMessage();
            if ("23503".equals(sql.getSQLState())) return "The team or player no longer exists. Close this dialog and refresh the teams.";
        }
        return "Could not update the roster. Check the database connection and try again.";
    }
}
