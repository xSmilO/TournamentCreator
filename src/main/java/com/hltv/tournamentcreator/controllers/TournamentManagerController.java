package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.MatchDAO;
import com.hltv.tournamentcreator.dao.TournamentDAO;
import com.hltv.tournamentcreator.dao.GroupStageDAO;
import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;

/** Owns the tournament-management flow; all layouts are FXML views. */
public final class TournamentManagerController {
    private final Tournament tournament;
    private final TournamentDAO tournaments;
    private final MatchDAO matches;
    private final GroupStageDAO groups;
    private final Runnable onChanged;
    private List<TournamentParticipant> participants = List.of();
    private List<TournamentMatch> currentMatches = List.of();
    private List<TournamentGroup> currentGroups = List.of();
    private Tab resultReturnTab;
    private int capacity;
    private TournamentMatch editingMatch;
    private List<MapResult> originalMaps = List.of();
    private final List<ScoreRow> scoreRows = new ArrayList<>();
    private record ScoreRow(UUID id, int order, FxmlViews.Fragment view) {}
    private record ScoreData(List<MapResult> maps, List<String> names) {}
    private record Snapshot(List<TournamentParticipant> teams, List<TournamentMatch> matches, int limit, List<TournamentGroup> groups, boolean registrationOpen) {}
    private final javafx.beans.property.BooleanProperty registrationOpen = new javafx.beans.property.SimpleBooleanProperty(false);
    @FXML private Button removeTeam, editSeed, editSchedule, editOutcome;

    @FXML private VBox managerRoot, teamPickerRoot, matchFormRoot, scoreRoot, mapRows;
    @FXML private Label title, managerStatus, champion, teamPickerStatus, matchFormStatus, scoreTitle, scoreStatus, seriesSummary, capacityLabel;
    @FXML private TableView<TournamentParticipant> participantsTable;
    @FXML private TableColumn<TournamentParticipant,String> participantSeed, participantName;
    @FXML private TableView<TournamentMatch> matchesTable;
    @FXML private TableColumn<TournamentMatch,String> matchOpponents, matchScore, matchStatus, matchRound, matchStart;
    @FXML private TableView<Team> eligibleTable;
    @FXML private TableColumn<Team,String> eligibleName, eligibleRanking;
    @FXML private TextField teamSearch, timeField, stageField, roundField, teamLimitField, correctionReason;
    @FXML private Button refresh, addTeams, addMatch, generate, register, editScores, createMatch, saveLimit;
    @FXML private ComboBox<String> bracketFormat, matchFormat;
    @FXML private CheckBox randomDraw;
    @FXML private ComboBox<TournamentParticipant> firstTeam, secondTeam;
    @FXML private DatePicker matchDate;
    @FXML private TabPane tabs;
    @FXML private Tab matchesTab;
    @FXML private HBox bracketColumns;
    @FXML private VBox groupPanels;
    @FXML private Label groupProgress;
    @FXML private Button drawGroups, qualifyGroups;
    @FXML private ComboBox<String> groupFormat, playoffFormat;

    public TournamentManagerController(Tournament tournament, TournamentDAO tournaments, MatchDAO matches, Runnable onChanged) {
        this(tournament,tournaments,matches,new GroupStageDAO(),onChanged);
    }

    public TournamentManagerController(Tournament tournament, TournamentDAO tournaments, MatchDAO matches, GroupStageDAO groups, Runnable onChanged) {
        this.tournament = tournament; this.tournaments = tournaments; this.matches = matches; this.groups = groups; this.onChanged = onChanged;
    }

    public void show() {
        var view = FxmlViews.load("TournamentManager.fxml", this);
        title.setText(tournament.name());
        bind(participantName, TournamentParticipant::name);
        bind(participantSeed, p -> p.seed() == null ? "—" : p.seed().toString());
        bind(matchOpponents, TournamentMatch::opponents);
        bind(matchScore, m -> isBye(m) ? "Bye" : m.score());
        bind(matchStatus, TournamentMatch::status);
        bind(matchRound, m -> text(m.round()));
        bind(matchStart, m -> m.start() == null ? "Unscheduled" : m.start().format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH)));
        editScores.disableProperty().bind(matchesTable.getSelectionModel().selectedItemProperty().isNull());
        editSeed.disableProperty().bind(participantsTable.getSelectionModel().selectedItemProperty().isNull());
        editOutcome.disableProperty().bind(matchesTable.getSelectionModel().selectedItemProperty().isNull());
        editSchedule.disableProperty().bind(matchesTable.getSelectionModel().selectedItemProperty().isNull());
        removeTeam.disableProperty().bind(registrationOpen.not().or(participantsTable.getSelectionModel().selectedItemProperty().isNull()));
        ModalManager.getInstance().showModal(view.root());
        refresh();
    }

    @FXML private void eventControl() {
        new EventControl(tournament.tournamentId(),new com.hltv.tournamentcreator.dao.LifecycleDAO(),onChanged,this::returnFromEdit).show(tournament.name());
    }
    @FXML private void editOutcome() {
        var match=matchesTable.getSelectionModel().getSelectedItem();
        if(match==null)return;
        new MatchOutcomeEditor(match,matches,downstream(match),this::returnFromEdit,this::backToManager).show();
    }
    private String downstream(TournamentMatch match) {
        if(match.nextMatchId()==null)return "No later bracket slot is affected.";
        return currentMatches.stream().filter(m -> m.matchId().equals(match.nextMatchId())).findFirst()
            .map(m -> "Affected next match: "+m.round()+" · "+m.opponents()+" ("+m.status()+").")
            .orElse("The next bracket slot will be updated.");
    }

    @FXML private void editSeed() {
        var team = participantsTable.getSelectionModel().getSelectedItem();
        if (team != null) new OrganizerEditor(this::returnFromEdit, this::backToManager).seed(tournament.tournamentId(), team);
    }
    @FXML private void editSchedule() {
        var match = matchesTable.getSelectionModel().getSelectedItem();
        if (match != null) new OrganizerEditor(this::returnFromEdit, this::backToManager).schedule(match);
    }
    private void returnFromEdit() { backToManager(); refresh(); onChanged.run(); }

    @FXML private void refresh() {
        registrationOpen.set(false);
        refresh.setDisable(true); addTeams.setDisable(true); addMatch.setDisable(true); generate.setDisable(true); saveLimit.setDisable(true);
        drawGroups.setDisable(true); qualifyGroups.setDisable(true);
        managerStatus.setText("Loading tournament…");
        BackgroundTask.run(() -> new Snapshot(tournaments.getRegisteredTeams(tournament.tournamentId()), matches.getMatches(tournament.tournamentId()),
                tournaments.getTeamLimit(tournament.tournamentId()), groups.getGroups(tournament.tournamentId()), tournaments.isRegistrationOpen(tournament.tournamentId())), data -> {
            participants = data.teams(); currentMatches = data.matches();
            currentGroups = data.groups();
            capacity = data.limit();
            teamLimitField.setText(Integer.toString(capacity));
            capacityLabel.setText(participants.size() + " / " + capacity + " teams registered");
            participantsTable.getItems().setAll(participants);
            matchesTable.getItems().setAll(currentMatches);
            boolean bracket = currentMatches.stream().anyMatch(m -> SingleElimination.STAGE.equals(m.stage()));
            registrationOpen.set(data.registrationOpen());
            addTeams.setText(!data.registrationOpen() ? "Registration settings" : participants.size() >= capacity ? "Team limit reached" : "＋  Add teams");
            addTeams.setDisable(data.registrationOpen() && participants.size() >= capacity);
            addMatch.setDisable(bracket || !currentGroups.isEmpty() || participants.size() < 2);
            generate.setDisable(participants.size() < 2 || !currentMatches.isEmpty() || !currentGroups.isEmpty());
            bracketFormat.setDisable(!currentMatches.isEmpty());
            drawGroups.setDisable(!currentGroups.isEmpty() || !currentMatches.isEmpty() || participants.size() < 4 || participants.size() % 4 != 0);
            groupFormat.setDisable(!currentGroups.isEmpty() || !currentMatches.isEmpty());
            playoffFormat.setDisable(bracket);
            try { qualifyGroups.setDisable(bracket || !GroupStage.complete(currentGroups,currentMatches)); }
            catch (IllegalArgumentException invalid) { qualifyGroups.setDisable(true); }
            renderGroups(bracket);
            renderBracket();
            managerStatus.setText(participants.size() + " / " + capacity + " teams · " + currentMatches.size() + " matches"
                    + (data.registrationOpen() ? " · Team changes open" : " · Registration closed — see Event control"));
            refresh.setDisable(false);
            saveLimit.setDisable(false);
        }, error -> { managerStatus.setText(error(error)); refresh.setDisable(false); });
    }

    @FXML private void generateGroups() {
        managerRoot.setDisable(true); groupProgress.setText("Drawing groups and scheduling round-robin matches…");
        String format = groupFormat.getValue();
        BackgroundTask.run(() -> { groups.generateGroups(tournament.tournamentId(),format); return null; }, ignored -> {
            managerRoot.setDisable(false); onChanged.run(); refresh();
        }, error -> { managerRoot.setDisable(false); groupProgress.setText(error(error)); });
    }

    @FXML private void generateGroupPlayoffs() {
        managerRoot.setDisable(true); groupProgress.setText("Qualifying the top two teams from each group…");
        String format = playoffFormat.getValue();
        BackgroundTask.run(() -> { groups.generatePlayoffs(tournament.tournamentId(),format); return null; }, ignored -> {
            managerRoot.setDisable(false); refresh();
        }, error -> { managerRoot.setDisable(false); groupProgress.setText(error(error)); });
    }

    private void renderGroups(boolean playoffs) {
        groupPanels.getChildren().clear();
        if (currentGroups.isEmpty()) {
            groupProgress.setText("Register teams in multiples of four, then draw groups. Each group plays six matches; the top two advance.");
            return;
        }
        long finished = currentMatches.stream().filter(m -> m.groupId()!=null && "Completed".equals(m.status())).count();
        groupProgress.setText(currentGroups.size()+" groups · "+finished+" / "+currentGroups.size()*6+" group matches completed"
                +(playoffs ? " · Playoffs generated; group results are locked" : !qualifyGroups.isDisabled()
                ? " · Ready to generate playoffs" : " · Complete all groups to generate playoffs"));
        for (var group:currentGroups) {
            var view = FxmlViews.load("components/GroupPanel.fxml");
            TitledPane pane = view.get("pane");
            long played = currentMatches.stream().filter(m -> group.groupId().equals(m.groupId()) && "Completed".equals(m.status())).count();
            pane.setText(group.name()+" · "+played+" / 6 matches completed");
            boolean[] populated = {false};
            Runnable populate = () -> {
                if (populated[0]) return;
                populated[0] = true;
                final List<GroupStanding> table;
                try { table=GroupStage.standings(group,currentMatches); }
                catch (IllegalArgumentException invalid) { view.<Label>get("notice").setText(invalid.getMessage()); return; }
                TableView<GroupStanding> standings = view.get("standings");
                bind(view.get("position"), (GroupStanding row) -> Integer.toString(table.indexOf(row)+1)+(table.indexOf(row)<2 ? "  Q" : ""));
                bind(view.get("team"), GroupStanding::name); bind(view.get("played"), (GroupStanding row) -> Integer.toString(row.played()));
                bind(view.get("wins"), (GroupStanding row) -> Integer.toString(row.wins())); bind(view.get("losses"), (GroupStanding row) -> Integer.toString(row.losses()));
                bind(view.get("maps"), (GroupStanding row) -> row.mapsWon()+" : "+row.mapsLost());
                bind(view.get("difference"), (GroupStanding row) -> (row.mapDifference()>0 ? "+" : "")+row.mapDifference());
                bind(view.get("points"), (GroupStanding row) -> Integer.toString(row.points()));
                standings.getItems().setAll(table);
                view.<Label>get("notice").setText(playoffs ? "Q = qualified. Group results are locked." : "Q marks the current top two; qualification is provisional until all group matches finish.");
                VBox fixtures = view.get("fixtures");
                for (var match:currentMatches.stream().filter(m -> group.groupId().equals(m.groupId())).toList()) {
                    var matchView = FxmlViews.load("components/GroupMatchRow.fxml");
                    matchView.<Label>get("teams").setText(match.opponents());
                    matchView.<Label>get("details").setText(match.round()+" · "+match.format());
                    matchView.<Label>get("result").setText(match.score()+" · "+match.status());
                    matchView.<Button>get("open").setDisable(playoffs);
                    matchView.<Button>get("open").setOnAction(event -> showScores(match));
                    fixtures.getChildren().add(matchView.root());
                }
            };
            pane.expandedProperty().addListener((obs, old, expanded) -> { if (expanded) populate.run(); });
            groupPanels.getChildren().add(pane);
            if (group.order()==1) pane.setExpanded(true);
        }
    }

    @FXML private void showTeamPicker() {
        if (!registrationOpen.get()) {
            eventControl();
            return;
        }
        var view = FxmlViews.load("TournamentTeamsModal.fxml", this);
        bind(eligibleName, Team::name);
        bind(eligibleRanking, t -> t.worldRanking() == null ? "Unranked" : t.worldRanking().toString());
        eligibleTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        var selected = eligibleTable.getSelectionModel().getSelectedItems();
        int slots = Math.max(0, capacity - participants.size());
        register.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(selected)
                .or(javafx.beans.binding.Bindings.size(selected).greaterThan(slots)));
        selected.addListener((javafx.collections.ListChangeListener<Team>) change -> teamPickerStatus.setText(
                selected.size() + " selected · " + slots + " slots available"
                        + (selected.size() > slots ? " · Select fewer teams" : "")));
        ModalManager.getInstance().showModal(view.root());
        loadEligible();
    }

    @FXML private void removeTeam() {
        TournamentParticipant selected = participantsTable.getSelectionModel().getSelectedItem();
        if (selected == null || !registrationOpen.get()) return;
        managerRoot.setDisable(true);
        managerStatus.setText("Removing team from tournament…");
        BackgroundTask.run(() -> { tournaments.unregisterTeam(tournament.tournamentId(), selected.teamId()); return null; }, ignored -> {
            managerRoot.setDisable(false); onChanged.run(); refresh();
        }, error -> { managerRoot.setDisable(false); managerStatus.setText(error(error)); });
    }

    @FXML private void loadEligible() {
        teamPickerStatus.setText("Loading teams…");
        BackgroundTask.run(() -> tournaments.getEligibleTeams(tournament.tournamentId()), teams -> {
            var filtered = new FilteredList<>(FXCollections.observableArrayList(teams));
            var sorted = new SortedList<>(filtered);
            sorted.comparatorProperty().bind(eligibleTable.comparatorProperty());
            eligibleTable.setItems(sorted);
            teamSearch.textProperty().addListener((obs, old, query) -> filtered.setPredicate(team -> team.name().toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT))));
            teamPickerStatus.setText(teams.size() + " eligible teams · " + Math.max(0, capacity - participants.size()) + " slots available · Ctrl/Shift-click to select several");
        }, error -> teamPickerStatus.setText(error(error)));
    }

    @FXML private void registerTeams() {
        List<UUID> ids = eligibleTable.getSelectionModel().getSelectedItems().stream().map(Team::teamId).toList();
        if (ids.isEmpty()) { teamPickerStatus.setText("Select at least one team."); return; }
        if (ids.size() > capacity - participants.size()) { teamPickerStatus.setText("Selected teams exceed the remaining slots."); return; }
        teamPickerRoot.setDisable(true);
        teamPickerStatus.setText("Registering teams…");
        BackgroundTask.run(() -> { tournaments.registerTeams(tournament.tournamentId(), ids); return null; }, ignored -> {
            onChanged.run(); backToManager(); refresh();
        }, error -> { teamPickerRoot.setDisable(false); teamPickerStatus.setText(error(error)); });
    }

    @FXML private void updateTeamLimit() {
        final int limit;
        try {
            limit = CreatorValidation.teamLimit(teamLimitField.getText());
            if (limit < participants.size()) throw new IllegalArgumentException("Team limit cannot be smaller than the registered team count.");
        } catch (IllegalArgumentException error) { managerStatus.setText(error.getMessage()); return; }
        saveLimit.setDisable(true);
        managerStatus.setText("Saving team limit…");
        BackgroundTask.run(() -> { tournaments.updateTeamLimit(tournament.tournamentId(), limit); return null; }, ignored -> {
            onChanged.run(); refresh();
        }, error -> { saveLimit.setDisable(false); managerStatus.setText(error(error)); });
    }

    @FXML private void generateBracket() {
        generate.setDisable(true); addTeams.setDisable(true); addMatch.setDisable(true); refresh.setDisable(true);
        boolean random = randomDraw.isSelected();
        managerStatus.setText(random ? "Drawing random pairs and generating bracket…" : "Generating seeded single elimination bracket…");
        String format = bracketFormat.getValue();
        BackgroundTask.run(() -> { matches.generateBracket(tournament.tournamentId(), format, random); return null; }, ignored -> {
            refresh();
        }, error -> { managerStatus.setText(error(error)); refresh.setDisable(false); });
    }

    @FXML private void showMatchCreator() {
        var view = FxmlViews.load("MatchCreatorModal.fxml", this);
        StringConverter<TournamentParticipant> converter = new StringConverter<>() {
            @Override public String toString(TournamentParticipant team) { return team == null ? "" : team.name(); }
            @Override public TournamentParticipant fromString(String value) { return null; }
        };
        firstTeam.setConverter(converter); secondTeam.setConverter(converter);
        firstTeam.getItems().setAll(participants); secondTeam.getItems().setAll(participants);
        ModalManager.getInstance().showModal(view.root());
    }

    @FXML private void createMatch() {
        final UUID a, b;
        final LocalDateTime start;
        final String format = matchFormat.getValue(), stage = stageField.getText(), round = roundField.getText();
        try {
            if (firstTeam.getValue() == null || secondTeam.getValue() == null) throw new IllegalArgumentException("Choose two registered teams.");
            a = firstTeam.getValue().teamId(); b = secondTeam.getValue().teamId();
            MatchResults.evaluate(format, a, b, List.of());
            LocalDate date = matchDate.getEditor().getText().isBlank() ? null : matchDate.getConverter().fromString(matchDate.getEditor().getText());
            String time = timeField.getText().trim();
            if ((date == null) != time.isEmpty()) throw new IllegalArgumentException("Provide both a date and time, or leave both empty.");
            start = date == null ? null : LocalDateTime.of(date, LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT)));
            CreatorValidation.text(stage, "Stage", 50, false); CreatorValidation.text(round, "Round", 50, false);
        } catch (DateTimeParseException error) { matchFormStatus.setText("Enter a valid date and a 24-hour time such as 18:30."); return; }
        catch (IllegalArgumentException error) { matchFormStatus.setText(error.getMessage()); return; }
        matchFormRoot.setDisable(true);
        BackgroundTask.run(() -> { matches.createMatch(tournament.tournamentId(), a, b, format, start, stage, round); return null; }, ignored -> {
            backToManager(); tabs.getSelectionModel().select(matchesTab); refresh();
        }, error -> { matchFormRoot.setDisable(false); matchFormStatus.setText(error(error)); });
    }

    @FXML private void editSelectedScores() { showScores(matchesTable.getSelectionModel().getSelectedItem()); }

    private void showScores(TournamentMatch match) {
        if (match == null) return;
        if (!"Played".equals(match.outcome())) {
            managerStatus.setText("Use Outcome / reset in Matches to clear a non-played outcome before entering scores."); return;
        }
        if (!"Played".equals(match.outcome())) { managerStatus.setText("Use Outcome / reset to clear an awarded, cancelled, or bye outcome before entering scores."); return; }
        if (match.groupId()!=null && currentMatches.stream().anyMatch(m -> SingleElimination.STAGE.equals(m.stage()))) {
            managerStatus.setText("Group results are locked because playoffs have already been generated."); return;
        }
        if (match.team1Id() == null || match.team2Id() == null) {
            managerStatus.setText(isBye(match) ? "This team advanced automatically with a bye." : "Waiting for both teams to qualify for this match.");
            return;
        }
        final int limit;
        try { limit = MatchResults.mapLimit(match.format()); }
        catch (IllegalArgumentException error) { managerStatus.setText(error.getMessage()); return; }
        editingMatch = match;
        resultReturnTab = tabs.getSelectionModel().getSelectedItem();
        var view = FxmlViews.load("MatchScoresModal.fxml", this);
        scoreTitle.setText(match.opponents() + " · " + match.format());
        correctionReason.setPromptText("Completed".equals(match.status()) ? "Correction reason (required)" : "Optional note for event history");
        scoreRows.clear();
        scoreRoot.setDisable(true);
        scoreStatus.setText("Loading saved map scores…");
        ModalManager.getInstance().showModal(view.root());
        BackgroundTask.run(() -> new ScoreData(matches.getMaps(match.matchId()), matches.getMapNames()), data -> {
            List<MapResult> maps = data.maps();
            originalMaps = maps;
            Set<Integer> orders = new HashSet<>();
            for (MapResult map : maps) if (map.order() < 1 || map.order() > limit || !orders.add(map.order())) {
                scoreStatus.setText("Saved maps have invalid order numbers for this match format. Correct the map ordering in the database first.");
                scoreRoot.setDisable(false); scoreRoot.lookup("#save-match-scores").setDisable(true); return;
            }
            for (int i = 1; i <= limit; i++) {
                final int order = i;
                MapResult map = maps.stream().filter(m -> m.order() == order).findFirst().orElse(null);
                var row = FxmlViews.load("components/MapScoreRow.fxml");
                row.<ComboBox<String>>get("mapName").getItems().setAll(data.names());
                row.<Label>get("number").setText("MAP " + i);
                row.<Label>get("firstTeamName").setText(match.team1Name());
                row.<Label>get("secondTeamName").setText(match.team2Name());
                if (map != null) {
                    row.<ComboBox<String>>get("mapName").setValue(map.mapName());
                    row.<TextField>get("firstScore").setText(Integer.toString(map.team1Score()));
                    row.<TextField>get("secondScore").setText(Integer.toString(map.team2Score()));
                    row.<ComboBox<String>>get("mapStatus").setValue(map.status() == null ? "Upcoming" : map.status());
                }
                scoreRows.add(new ScoreRow(map == null ? UUID.randomUUID() : map.gameId(), i, row));
                row.<ComboBox<String>>get("mapName").valueProperty().addListener((obs, old, value) -> updateSummary());
                for (String id : List.of("firstScore", "secondScore"))
                    row.<TextField>get(id).textProperty().addListener((obs, old, value) -> updateSummary());
                row.<ComboBox<String>>get("mapStatus").valueProperty().addListener((obs, old, value) -> updateSummary());
                mapRows.getChildren().add(row.root());
            }
            scoreRoot.setDisable(false); scoreStatus.setText("Completed".equals(match.status()) ? downstream(match) : ""); updateSummary();
        }, error -> {
            scoreRoot.setDisable(false); scoreRoot.lookup("#save-match-scores").setDisable(true); scoreStatus.setText(error(error));
        });
    }

    private List<MapResult> readScores() {
        List<MapResult> results = new ArrayList<>();
        for (ScoreRow row : scoreRows) {
            try {
                results.add(new MapResult(row.id(), row.view().<ComboBox<String>>get("mapName").getValue(), row.order(),
                        Integer.parseInt(row.view().<TextField>get("firstScore").getText().trim()),
                        Integer.parseInt(row.view().<TextField>get("secondScore").getText().trim()), row.view().<ComboBox<String>>get("mapStatus").getValue()));
            } catch (NumberFormatException error) { throw new IllegalArgumentException("Map " + row.order() + " scores must be whole numbers."); }
        }
        return results;
    }

    private void updateSummary() {
        try {
            var result = MatchResults.evaluate(editingMatch.format(), editingMatch.team1Id(), editingMatch.team2Id(), readScores());
            String winner = result.winnerId() == null ? "" : " · Winner: " + (result.winnerId().equals(editingMatch.team1Id()) ? editingMatch.team1Name() : editingMatch.team2Name());
            seriesSummary.setText("Series: " + result.team1Maps() + " : " + result.team2Maps() + " · " + result.status() + winner);
        } catch (IllegalArgumentException error) { seriesSummary.setText("Complete the map details to calculate the series result."); }
    }

    @FXML private void saveScores() {
        final List<MapResult> scores;
        try {
            scores = readScores(); MatchResults.evaluate(editingMatch.format(), editingMatch.team1Id(), editingMatch.team2Id(), scores);
        } catch (IllegalArgumentException error) { scoreStatus.setText(error.getMessage()); return; }
        TournamentMatch match = editingMatch;
        List<MapResult> previous = originalMaps;
        final String reason;
        try { reason = CreatorValidation.text(correctionReason.getText(), "Correction reason", 500, "Completed".equals(match.status())); }
        catch (IllegalArgumentException error) { scoreStatus.setText(error.getMessage()); return; }
        scoreRoot.setDisable(true); scoreStatus.setText("Saving maps and updating the bracket…");
        BackgroundTask.run(() -> { matches.saveScores(match, previous, scores, reason); return null; }, ignored -> {
            backToManager(); tabs.getSelectionModel().select(resultReturnTab == null ? matchesTab : resultReturnTab); refresh();
        }, error -> { scoreRoot.setDisable(false); scoreStatus.setText(error(error)); });
    }

    private void renderBracket() {
        bracketColumns.getChildren().clear();
        List<TournamentMatch> bracket = currentMatches.stream().filter(m -> SingleElimination.STAGE.equals(m.stage())).toList();
        if (bracket.isEmpty()) {
            champion.setText(currentGroups.isEmpty() ? "Register your teams, choose Bo1/Bo3/Bo5, then generate a seeded single elimination bracket."
                    : "Complete every group match, then use Groups → Generate playoffs. The qualified teams will appear here.");
            return;
        }
        TournamentMatch last = bracket.stream().filter(m -> m.nextMatchId() == null).findFirst().orElse(null);
        if (last == null) { champion.setText("The saved bracket is missing its final."); return; }
        Map<UUID,List<TournamentMatch>> feeders = new HashMap<>();
        for (TournamentMatch match : bracket) if (match.nextMatchId() != null) feeders.computeIfAbsent(match.nextMatchId(), id -> new ArrayList<>()).add(match);
        feeders.values().forEach(list -> list.sort(Comparator.comparing(m -> m.nextSlot() == null ? 0 : m.nextSlot())));
        List<List<TournamentMatch>> levels = new ArrayList<>();
        collectRounds(last, feeders, levels, 0, new HashSet<>());
        Collections.reverse(levels);
        for (int r = 0; r < levels.size(); r++) {
            var column = FxmlViews.load("components/BracketRound.fxml");
            column.<Label>get("roundName").setText(levels.get(r).get(0).round());
            VBox slots = column.get("slots");
            double height = 116 * Math.pow(2, r);
            for (TournamentMatch match : levels.get(r)) {
                var slot = FxmlViews.load("components/BracketSlot.fxml");
                slot.<StackPane>get("slot").setPrefHeight(height);
                var card = FxmlViews.load("components/BracketMatch.fxml");
                card.<Label>get("firstName").setText(match.team1Name()); card.<Label>get("secondName").setText(isBye(match) ? "BYE" : match.team2Name());
                card.<Label>get("firstScore").setText(match.awarded() ? (Objects.equals(match.winnerId(),match.team1Id()) ? "W" : "L") : isBye(match) ? "✓" : Integer.toString(match.team1Maps()));
                card.<Label>get("secondScore").setText(match.awarded() ? (Objects.equals(match.winnerId(),match.team2Id()) ? "W" : "L") : isBye(match) ? "—" : Integer.toString(match.team2Maps()));
                card.<Label>get("status").setText((match.awarded() ? match.outcome() : match.status()) + " · " + match.format());
                if (match.winnerId() != null) card.<Label>get(match.winnerId().equals(match.team1Id()) ? "firstName" : "secondName").getStyleClass().add("winner-name");
                card.<Button>get("matchButton").setOnAction(event -> showScores(match));
                card.<Button>get("matchButton").setDisable(match.team1Id() == null || match.team2Id() == null);
                slot.<StackPane>get("slot").getChildren().add(card.root()); slots.getChildren().add(slot.root());
            }
            bracketColumns.getChildren().add(column.root());
            if (r + 1 < levels.size()) {
                var links = FxmlViews.load("components/BracketLinks.fxml");
                StringBuilder path = new StringBuilder();
                for (int i = 0; i < levels.get(r).size() / 2; i++) {
                    double a = (2*i+.5)*height, b = (2*i+1.5)*height, mid = (2*i+1)*height;
                    path.append("M0 ").append(a).append(" H16 V").append(b).append(" H0 M16 ").append(mid).append(" H32 ");
                }
                links.<SVGPath>get("lines").setContent(path.toString()); bracketColumns.getChildren().add(links.root());
            }
        }
        champion.setText(last.winnerId() == null ? "Winners advance automatically when a series is completed. Click a match to enter its map scores."
                : "Champion: " + (last.winnerId().equals(last.team1Id()) ? last.team1Name() : last.team2Name()));
    }

    private void collectRounds(TournamentMatch match, Map<UUID,List<TournamentMatch>> feeders, List<List<TournamentMatch>> levels, int depth, Set<UUID> seen) {
        if (!seen.add(match.matchId())) return;
        while (levels.size() <= depth) levels.add(new ArrayList<>());
        levels.get(depth).add(match);
        for (TournamentMatch feeder : feeders.getOrDefault(match.matchId(), List.of())) collectRounds(feeder, feeders, levels, depth+1, seen);
    }
    private boolean isBye(TournamentMatch match) { return "Completed".equals(match.status()) && (match.team1Id() == null || match.team2Id() == null); }
    @FXML private void backToManager() { ModalManager.getInstance().showModal(managerRoot); }
    @FXML private void close() { ModalManager.getInstance().hideModal(); }
    private <T> void bind(TableColumn<T,String> column, Function<T,String> value) { column.setCellValueFactory(row -> new SimpleStringProperty(value.apply(row.getValue()))); }
    private String text(String value) { return value == null ? "—" : value; }
    private String error(Throwable error) {
        if (error instanceof SQLException sql) {
            if ("23505".equals(sql.getSQLState())) return "One of these teams is already registered. Reopen the team picker to refresh the list.";
            if ("23503".equals(sql.getSQLState())) return "A selected team or tournament no longer exists. Close and refresh the tournament list.";
            if ("P0001".equals(sql.getSQLState())) return sql.getMessage();
            if ("42P01".equals(sql.getSQLState()) || "42703".equals(sql.getSQLState()))
                return "The database needs the latest tournament migrations. Apply the SQL files in db/migrations, then refresh.";
        }
        if (error instanceof IllegalArgumentException) return error.getMessage();
        return "Could not save or load tournament data. Check the database connection and try again.";
    }
}
