package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.LifecycleDAO;
import com.hltv.tournamentcreator.utils.*;
import javafx.fxml.FXML;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.*;

public final class EventControl {
    private final UUID tournament;
    private final LifecycleDAO dao;
    private final Runnable onChanged, onBack;
    private TournamentLifecycle.State current;
    @FXML private VBox root;
    @FXML private Label title,stateLabel,explanation,status;
    @FXML private ComboBox<String> action;
    @FXML private TextArea reason,details;
    @FXML private CheckBox confirm;
    @FXML private Button apply;
    @FXML private TableView<LifecycleDAO.Placement> standings;
    @FXML private TableColumn<LifecycleDAO.Placement,String> placeColumn,teamColumn;
    @FXML private TableView<LifecycleDAO.Audit> history;
    @FXML private TableColumn<LifecycleDAO.Audit,String> timeColumn,actionColumn,reasonColumn;
    public EventControl(UUID tournament,LifecycleDAO dao,Runnable onChanged,Runnable onBack) {
        this.tournament=tournament;this.dao=dao;this.onChanged=onChanged;this.onBack=onBack;
    }
    public void show(String name) {
        var view=FxmlViews.load("EventControl.fxml",this);title.setText(name);
        placeColumn.setCellValueFactory(v -> new SimpleStringProperty(Integer.toString(v.getValue().place())));
        teamColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().team()));
        timeColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().time()));
        actionColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().action()));
        reasonColumn.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().reason()));
        history.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> details.setText(b==null?"":"Before:\n"+b.before()+"\n\nAfter:\n"+b.after()));
        action.valueProperty().addListener((o,a,b) -> {confirm.setSelected(false);explanation.setText(explain(b));});
        ModalManager.getInstance().showModal(view.root());refresh();
    }
    @FXML private void refresh(){load(null);}
    private void load(String message) {
        root.setDisable(true);status.setText("Loading event control…");
        BackgroundTask.run(() -> dao.overview(tournament), data -> {
            current=data.state();stateLabel.setText(current.state()+(current.archived()?" · Archived":""));
            standings.getItems().setAll(data.placements());history.getItems().setAll(data.audit());details.clear();
            List<String> actions=current.archived()?List.of("Unarchive") : switch(current.state()) {
                case "Draft" -> List.of("Open registration","Start tournament","Reset draw","Cancel tournament");
                case "Registration" -> List.of("Close registration","Start tournament","Reset draw","Cancel tournament");
                case "In progress" -> List.of("Finish tournament","Cancel tournament");
                default -> List.of("Reopen tournament","Archive");
            };
            action.getItems().setAll(actions);action.getSelectionModel().selectFirst();confirm.setSelected(false);
            root.setDisable(false);apply.setDisable(false);status.setText(message==null?"":message);
        },e -> {root.setDisable(false);apply.setDisable(true);status.setText(error(e));});
    }
    private String explain(String action) {
        if(action==null)return "";
        return switch(action) {
            case "Reset draw" -> "Removes all fixtures, schedules and groups. Registrations and seeds remain. Allowed only before the event starts, with no played or awarded results; automatic byes are allowed. Draw again afterwards.";
            case "Reopen tournament" -> "Clears published placements and unlocks the event for corrections. Downstream match and playoff qualification safeguards still apply. Finish again to republish standings.";
            case "Cancel tournament" -> "Closes the event without a champion or placements. Saved matches remain for history and cannot be edited until reopening.";
            case "Finish tournament" -> "Validates all fixtures and the bracket champion, then saves final placements and locks competition edits.";
            case "Archive" -> "Hides editing while preserving results and history. Unarchive before reopening.";
            case "Open registration" -> "Allows adding/removing teams. Requires a draft with no existing fixtures or groups.";
            case "Close registration" -> "Returns to Draft and prevents team registration. Existing teams remain registered.";
            case "Start tournament" -> "Closes registration and enables score/outcome entry. Requires at least two teams and existing fixtures.";
            default -> "Restores access to the event without changing its competition state.";
        };
    }
    @FXML private void apply() {
        if(current==null)return;
        if(!confirm.isSelected()){status.setText("Confirm the selected action first.");return;}
        final String why;
        try{why=CreatorValidation.text(reason.getText(),"Reason",500,Set.of("Reset draw","Reopen tournament","Cancel tournament").contains(action.getValue()));}catch(IllegalArgumentException e){status.setText(e.getMessage());return;}
        var expected=current;String selected=action.getValue();root.setDisable(true);status.setText("Applying action…");
        BackgroundTask.run(() -> {dao.act(tournament,expected,selected,why);return null;},ignored -> {
            reason.clear();onChanged.run();load("Action saved.");
        },e -> {root.setDisable(false);status.setText(error(e));});
    }
    @FXML private void back(){onBack.run();}
    static String error(Throwable e){
        if(e instanceof IllegalArgumentException)return e.getMessage();
        if(e instanceof java.sql.SQLException sql){
            if("P0001".equals(sql.getSQLState()))return sql.getMessage();
            if(Set.of("42703","42P01").contains(sql.getSQLState()))return "Apply database migration 005_tournament_lifecycle.sql, then refresh.";
        }
        return "Could not save or load event data. Check the database connection and retry.";
    }
}
