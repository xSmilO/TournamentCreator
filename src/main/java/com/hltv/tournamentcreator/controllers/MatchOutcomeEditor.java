package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.MatchDAO;
import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.util.*;

public final class MatchOutcomeEditor {
    private final TournamentMatch match;
    private final MatchDAO dao;
    private final Runnable onSaved,onBack;
    private final String downstream;
    private List<MapResult> original=List.of();
    @FXML private VBox root;
    @FXML private Label title,impact,status;
    @FXML private ComboBox<String> action;
    @FXML private ComboBox<TournamentParticipant> winner;
    @FXML private TextArea reason;
    @FXML private CheckBox confirm;
    @FXML private Button save;
    public MatchOutcomeEditor(TournamentMatch match,MatchDAO dao,String downstream,Runnable onSaved,Runnable onBack) {
        this.match=match;this.dao=dao;this.downstream=downstream;this.onSaved=onSaved;this.onBack=onBack;
    }
    public void show() {
        var view=FxmlViews.load("MatchOutcome.fxml",this);title.setText(match.opponents());
        impact.setText("Current outcome: "+match.outcome()+". "+downstream+" Reset result clears map scores and removes the advanced winner. A started downstream match blocks this change; reset later matches first. Group results remain locked after playoffs. Withdrawal awards this match to the opponent; it does not withdraw a team from the whole event.");
        action.getItems().setAll("Forfeit","Withdrawal","Cancelled","Reset result");action.getSelectionModel().selectFirst();
        winner.setConverter(new StringConverter<>() {
            public String toString(TournamentParticipant t){return t==null?"":t.name();}
            public TournamentParticipant fromString(String s){return null;}
        });
        if(match.team1Id()!=null)winner.getItems().add(new TournamentParticipant(match.team1Id(),match.team1Name(),null));
        if(match.team2Id()!=null)winner.getItems().add(new TournamentParticipant(match.team2Id(),match.team2Name(),null));
        action.valueProperty().addListener((o,a,b) -> {confirm.setSelected(false);winner.setDisable("Cancelled".equals(b)||"Reset result".equals(b));});
        ModalManager.getInstance().showModal(view.root());root.setDisable(true);status.setText("Loading saved results…");
        BackgroundTask.run(() -> dao.getMaps(match.matchId()),maps -> {original=maps;root.setDisable(false);status.setText(maps.size()+" saved map records. Played maps must be reset before an awarded outcome.");},
            e -> {root.setDisable(false);save.setDisable(true);status.setText(EventControl.error(e));});
    }
    @FXML private void back(){onBack.run();}
    @FXML private void save() {
        if(!confirm.isSelected()){status.setText("Confirm the result change first.");return;}
        final String why;
        try{why=CreatorValidation.text(reason.getText(),"Reason",500,true);}catch(IllegalArgumentException e){status.setText(e.getMessage());return;}
        String outcome=action.getValue();UUID team=winner.getValue()==null?null:winner.getValue().teamId();
        if(("Forfeit".equals(outcome)||"Withdrawal".equals(outcome))&&team==null){status.setText("Choose the team awarded the win.");return;}
        root.setDisable(true);status.setText("Saving outcome…");
        BackgroundTask.run(() -> {dao.saveOutcome(match,original,outcome,team,why);return null;},ignored -> onSaved.run(),
            e -> {root.setDisable(false);status.setText(EventControl.error(e));});
    }
}
