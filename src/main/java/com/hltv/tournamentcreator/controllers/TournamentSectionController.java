package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.TournamentDAO;
import com.hltv.tournamentcreator.models.Tournament;
import com.hltv.tournamentcreator.dao.MatchDAO;
import com.hltv.tournamentcreator.utils.FxmlViews;
import com.hltv.tournamentcreator.utils.Icons;
import com.hltv.tournamentcreator.utils.ModalManager;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class TournamentSectionController extends DatabaseSectionController<Tournament> {
    private final TournamentDAO dao = new TournamentDAO();
    @Override protected String sectionName() { return "Tournaments"; }
    @Override protected List<Tournament> fetchRows() throws Exception { return dao.getAllTournaments(); }
    @Override protected List<String> filterOptions() { return List.of("All tournaments", "Draft", "Registration", "In progress", "Completed", "Cancelled", "Archived"); }
    @Override protected boolean matchesFilter(Tournament t, String filter) {
        return "All tournaments".equals(filter) || state(t).equals(filter);
    }
    private String state(Tournament t) { return t.archived() ? "Archived" : t.state(); }
    @Override protected Node card(Tournament t) {
        var view = FxmlViews.load("components/TournamentCard.fxml");
        view.<VBox>get("art").getStyleClass().add("mark-" + Math.floorMod(t.name().hashCode(), 4));
        view.<StackPane>get("icon").getChildren().setAll(Icons.of("trophy"));
        view.<Label>get("state").setText(state(t).toUpperCase(Locale.ROOT));
        view.<Label>get("name").setText(t.name());
        String dates = (t.startDate() == null ? "Dates to be confirmed" : t.startDate().toString())
                + (t.endDate() == null ? "" : " — " + t.endDate());
        String schedule = t.endDate()!=null && t.endDate().isBefore(LocalDate.now()) ? "Scheduled dates ended"
                : t.startDate()!=null && t.startDate().isAfter(LocalDate.now()) ? "Scheduled upcoming" : "Schedule";
        view.<Label>get("summary").setText(t.participantCount() + " / " + t.teamLimit() + " teams  ·  " + schedule + ": " + dates);
        view.<Label>get("meta").setText((t.tier() == null ? "Unspecified tier" : t.tier())
                + (t.location() == null ? "" : "  ·  " + t.location())
                + (t.prizePool() == null ? "" : "  ·  Prize pool " + t.prizePool().toPlainString()));
        view.<Button>get("open").setOnAction(event -> new TournamentManagerController(t, dao, new MatchDAO(), this::loadData).show());
        view.<Button>get("delete").setOnAction(event -> new DeleteTournamentDialog(t, dao, this::loadData).show());
        view.<Button>get("edit").setOnAction(event -> new OrganizerEditor(() -> { ModalManager.getInstance().hideModal(); loadData(); },
                () -> ModalManager.getInstance().hideModal()).tournament(t));
        return view.root();
    }
    @Override protected void configureColumns() { /* Tournament results use the FXML card template. */ }
    @Override protected void openCreator() {
        ModalManager.getInstance().showModal("/com/hltv/tournamentcreator/TournamentCreatorModal.fxml", this::loadData);
    }
}
