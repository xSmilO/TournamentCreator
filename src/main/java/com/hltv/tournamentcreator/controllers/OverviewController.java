package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.DashboardDAO;
import com.hltv.tournamentcreator.models.Team;
import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.FxmlViews;
import com.hltv.tournamentcreator.utils.Icons;
import com.hltv.tournamentcreator.utils.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OverviewController {
    @FXML private Label tournamentTotal, teamTotal, playerTotal, matchTotal, date, status, scheduleEmpty, rankingEmpty;
    @FXML private VBox schedule, ranking;
    @FXML private Button refresh;
    @FXML private StackPane tournamentIcon, teamIcon, playerIcon, matchIcon;

    @FXML public void initialize() {
        date.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)));
        tournamentIcon.getChildren().setAll(Icons.of("trophy"));
        teamIcon.getChildren().setAll(Icons.of("users"));
        playerIcon.getChildren().setAll(Icons.of("user"));
        matchIcon.getChildren().setAll(Icons.of("chart"));
        loadData();
    }
    protected DashboardDAO.Snapshot fetchData() throws Exception { return new DashboardDAO().load(); }
    @FXML private void loadData() {
        refresh.setDisable(true);
        BackgroundTask.run(this::fetchData, data -> {
            tournamentTotal.setText(count(data.tournaments())); teamTotal.setText(count(data.teams()));
            playerTotal.setText(count(data.players())); matchTotal.setText(count(data.played()));
            schedule.getChildren().setAll(data.matches().stream().map(this::matchRow).toList());
            empty(scheduleEmpty, data.matches().isEmpty(), "No upcoming matches. Your scheduled matches will appear here.");
            ranking.getChildren().setAll(data.topTeams().stream().map(this::teamRow).toList());
            empty(rankingEmpty, data.topTeams().isEmpty(), "No ranked teams yet. Add a world ranking when creating a team.");
            status.setText("Workspace up to date  ·  " + data.scheduled() + " matches scheduled");
            refresh.setDisable(false);
        }, error -> {
            status.setText("Could not load the overview. Check your database connection and try Refresh.");
            schedule.getChildren().clear(); ranking.getChildren().clear();
            empty(scheduleEmpty, true, "Schedule unavailable."); empty(rankingEmpty, true, "Rankings unavailable.");
            refresh.setDisable(false);
        });
    }
    private String count(int count) { return String.format(Locale.ROOT, "%02d", count); }
    private void empty(Label label, boolean visible, String text) {
        label.setText(text); label.setVisible(visible); label.setManaged(visible);
    }
    private Node matchRow(DashboardDAO.ScheduledMatch match) {
        var row = FxmlViews.load("components/MatchRow.fxml");
        row.<Label>get("badge").setText(match.status().toUpperCase(Locale.ROOT));
        row.<Label>get("badge").getStyleClass().add("Live".equals(match.status()) ? "accent" : "muted");
        row.<Label>get("time").setText(match.start() == null ? "TIME TBD" : match.start().format(DateTimeFormatter.ofPattern("MMM dd · HH:mm", Locale.ENGLISH)));
        row.<Label>get("teams").setText(match.team1() + "   vs   " + match.team2());
        row.<Label>get("meta").setText(match.tournament() + (match.stage() == null ? "" : "  ·  " + match.stage())
                + (match.format() == null ? "" : "  ·  " + match.format()));
        return row.root();
    }
    private Node teamRow(Team team) {
        var row = FxmlViews.load("components/RankingRow.fxml");
        StackPane mark = row.get("mark");
        mark.getChildren().setAll(Icons.of("bolt"));
        mark.getStyleClass().add("mark-" + Math.floorMod(team.name().hashCode(), 4));
        row.<Label>get("rank").setText(String.format(Locale.ROOT, "%02d", team.worldRanking()));
        row.<Label>get("name").setText(team.name());
        row.<Label>get("players").setText(team.playerCount() + " players");
        return row.root();
    }
    @FXML private void viewTeams() { ViewManager.getInstance().navigate("Teams"); }
    @FXML private void viewTournaments() { ViewManager.getInstance().navigate("Tournaments"); }
    @FXML private void createTournament() { viewTournaments(); ViewManager.getInstance().openCreator(); }
}
