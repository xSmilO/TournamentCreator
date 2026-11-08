package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.*;
import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/** Shared FXML form for small organizer edits; each mode exposes only its own fields. */
public final class OrganizerEditor {
    private enum Mode { PLAYER, TEAM, TOURNAMENT, TRANSFER, RELEASE, SEED, SCHEDULE }
    private final OrganizerDAO dao;
    private final CountryDAO countries;
    private final TeamDAO teams;
    private final Runnable onSaved, onCancel;
    private Mode mode;
    private Object original;
    private UUID owner;
    private boolean finished;
    @FXML private VBox root, namePane, playerPane, rankingPane, tournamentPane, transferPane, seedPane, schedulePane;
    @FXML private Label title, hint, status, nameLabel;
    @FXML private TextField name, first, last, ranking, prize, venue, format, seed, time;
    @FXML private DatePicker birth, start, end, matchDate;
    @FXML private ComboBox<Country> country;
    @FXML private ComboBox<Team> destination;
    @FXML private ComboBox<String> tier, role;
    @FXML private Button save, cancel;

    public OrganizerEditor(Runnable onSaved, Runnable onCancel) { this(new OrganizerDAO(), onSaved, onCancel); }
    public OrganizerEditor(OrganizerDAO dao, Runnable onSaved, Runnable onCancel) {
        this(dao, new CountryDAO(), new TeamDAO(), onSaved, onCancel);
    }
    public OrganizerEditor(OrganizerDAO dao, CountryDAO countries, TeamDAO teams, Runnable onSaved, Runnable onCancel) {
        this.dao = dao; this.countries = countries; this.teams = teams; this.onSaved = onSaved; this.onCancel = onCancel;
    }
    private void open(Mode mode, Object original, String caption, String explanation) {
        this.mode = mode; this.original = original; finished = false;
        var view = FxmlViews.load("OrganizerEditor.fxml", this);
        for (VBox pane : List.of(namePane, playerPane, rankingPane, tournamentPane, transferPane, seedPane, schedulePane)) {
            pane.setVisible(false); pane.setManaged(false);
        }
        title.setText(caption); hint.setText(explanation);
        root.getStyleClass().add("organizer-" + mode.name().toLowerCase(Locale.ROOT));
        ModalManager.getInstance().showModal(view.root());
    }
    private void show(VBox... panes) { for (var pane : panes) { pane.setVisible(true); pane.setManaged(true); } }
    public void player(Player player) {
        open(Mode.PLAYER, player, "Edit player", "Update profile details. Team membership and statistics are kept separately.");
        show(namePane, playerPane);
        nameLabel.setText("Nickname");
        name.setText(player.nickname()); first.setText(player.firstName()); last.setText(player.lastName()); birth.setValue(player.dateOfBirth());
        country.setConverter(converter(Country::name));
        load(countries::getAllCountries, values -> { country.getItems().setAll(values); country.setValue(player.country()); });
    }
    public void team(Team team) {
        open(Mode.TEAM, team, "Edit team", "Ranking is manually managed; leave it empty for an unranked team.");
        show(namePane, rankingPane); name.setText(team.name()); ranking.setText(text(team.worldRanking()));
    }
    public void tournament(Tournament tournament) {
        open(Mode.TOURNAMENT, tournament, "Edit tournament", "Dates and format lock after the start date, a draw, or a started match. Name, tier, location and prize pool remain editable.");
        show(namePane, tournamentPane); name.setText(tournament.name());
        tier.getItems().setAll("Unspecified", "S-Tier", "A-Tier", "B-Tier", "C-Tier");
        tier.setValue(tournament.tier() == null ? "Unspecified" : tournament.tier());
        start.setValue(tournament.startDate()); end.setValue(tournament.endDate());
        prize.setText(text(tournament.prizePool())); venue.setText(tournament.location()); format.setText(tournament.format());
    }
    public void move(Team source, RosterPlayer player, boolean release) {
        owner = source.teamId();
        open(release ? Mode.RELEASE : Mode.TRANSFER, player, (release ? "Release " : "Transfer ") + player.nickname(),
            release ? "Release this player from " + source.name() + "? Their team role will be cleared."
                : "Move this player from " + source.name() + " and assign their new role. Historical lineups are not yet tracked.");
        if (release) { save.setText("Release player"); return; }
        show(transferPane); destination.setConverter(converter(Team::name));
        role.getItems().setAll("IGL", "AWPer", "Entry fragger", "Lurker", "Support", "Rifler", "Coach");
        role.setValue(player.role());
        load(teams::getAllTeams, values -> {
            destination.getItems().setAll(values.stream().filter(t -> !t.teamId().equals(source.teamId())).toList());
            if (destination.getItems().isEmpty()) { status.setText("Create another team before transferring a player."); save.setDisable(true); }
        });
    }
    public void seed(UUID tournament, TournamentParticipant player) {
        owner = tournament; open(Mode.SEED, player, "Edit seed · " + player.name(), "Seeds must be unique. To swap two teams, first move one to an unused seed. Seeds lock after a draw or started match.");
        show(seedPane); seed.setText(text(player.seed()));
    }
    public void schedule(TournamentMatch match) {
        open(Mode.SCHEDULE, match, "Schedule · " + match.opponents(), "Only upcoming matches can be rescheduled. Clear both fields to leave the time undecided.");
        show(schedulePane);
        if (match.start() != null) { matchDate.setValue(match.start().toLocalDate()); time.setText(match.start().format(DateTimeFormatter.ofPattern("HH:mm"))); }
        if (!"Upcoming".equals(match.status())) { save.setDisable(true); status.setText("This match has already started or completed."); }
    }
    private <T> void load(BackgroundTask.Work<List<T>> fetch, java.util.function.Consumer<List<T>> accept) {
        save.setDisable(true); status.setText("Loading choices…");
        BackgroundTask.run(fetch, values -> { status.setText(""); save.setDisable(false); accept.accept(values); },
            error -> status.setText("Could not load choices. Cancel and reopen to retry."));
    }
    @FXML private void clearCountry() { country.setValue(null); }
    @FXML private void clearSchedule() { matchDate.setValue(null); matchDate.getEditor().clear(); time.clear(); }
    @FXML private void cancel() { if (finished) onSaved.run(); else onCancel.run(); }
    @FXML private void save() {
        final BackgroundTask.Work<Void> work;
        try { work = validatedWork(); }
        catch (IllegalArgumentException e) { status.setText(e.getMessage()); return; }
        root.setDisable(true); status.setText("Saving…");
        BackgroundTask.run(work, ignored -> {
            root.setDisable(false); finished = true; save.setDisable(true); cancel.setText("Done");
            status.setText("Saved successfully.");
            // Keep the confirmation visible; Done returns to the refreshed originating view.
        }, error -> {
            root.setDisable(false);
            status.setText(error instanceof IllegalArgumentException ? error.getMessage()
                : error instanceof SQLException sql && "P0001".equals(sql.getSQLState()) ? sql.getMessage()
                : error instanceof SQLException sql && "23503".equals(sql.getSQLState()) ? "A selected entry no longer exists. Cancel, refresh, and try again."
                : "Could not save. Check the database connection and try again.");
        });
    }
    private BackgroundTask.Work<Void> validatedWork() {
        switch (mode) {
            case PLAYER -> {
                Player old = (Player) original;
                String nick = CreatorValidation.text(name.getText(), "Nickname", 50, true);
                String f = CreatorValidation.text(first.getText(), "First name", 50, false), l = CreatorValidation.text(last.getText(), "Surname", 50, false);
                LocalDate date = date(birth); Country c = country.getValue();
                return () -> { dao.updatePlayer(old, nick, f, l, c, date); return null; };
            }
            case TEAM -> {
                String n = CreatorValidation.text(name.getText(), "Team name", 100, true);
                Integer r = CreatorValidation.ranking(ranking.getText());
                return () -> { dao.updateTeam((Team) original, n, r); return null; };
            }
            case TOURNAMENT -> {
                Tournament old = (Tournament) original;
                var value = new Tournament(old.tournamentId(), CreatorValidation.text(name.getText(), "Tournament name", 200, true),
                    "Unspecified".equals(tier.getValue()) ? null : tier.getValue(), date(start), date(end), CreatorValidation.prizePool(prize.getText()),
                    CreatorValidation.text(venue.getText(), "Location", 100, false), CreatorValidation.text(format.getText(), "Format", 50, false), old.participantCount(), old.teamLimit());
                CreatorValidation.dates(value.startDate(), value.endDate());
                return () -> { dao.updateTournament(old, value); return null; };
            }
            case TRANSFER, RELEASE -> {
                Team target = destination.getValue();
                if (mode == Mode.TRANSFER && target == null) throw new IllegalArgumentException("Choose a destination team.");
                String r = mode == Mode.RELEASE ? null : CreatorValidation.text(role.getEditor().getText(), "Destination role", 50, true);
                return () -> { dao.movePlayer(owner, (RosterPlayer) original, target == null ? null : target.teamId(), r); return null; };
            }
            case SEED -> {
                int number;
                try { number = Integer.parseInt(seed.getText().trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("Seed must be a positive whole number."); }
                if (number < 1) throw new IllegalArgumentException("Seed must be a positive whole number.");
                return () -> { dao.updateSeed(owner, (TournamentParticipant) original, number); return null; };
            }
            case SCHEDULE -> {
                LocalDate d = date(matchDate); String t = time.getText().trim();
                if ((d == null) != t.isEmpty()) throw new IllegalArgumentException("Enter both date and time, or clear both.");
                LocalDateTime value = null;
                if (d != null) {
                    if (!t.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) throw new IllegalArgumentException("Enter time as HH:mm (00:00–23:59).");
                    value = d.atTime(LocalTime.parse(t));
                }
                LocalDateTime schedule = value;
                return () -> { dao.updateSchedule((TournamentMatch) original, schedule); return null; };
            }
        }
        throw new IllegalStateException("Unknown edit mode");
    }
    private static LocalDate date(DatePicker picker) {
        if (picker.getEditor().getText().isBlank()) return null;
        try { return picker.getConverter().fromString(picker.getEditor().getText().trim()); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Enter a valid date or use the calendar."); }
    }
    private static String text(Object value) { return value == null ? "" : value.toString(); }
    private static <T> StringConverter<T> converter(Function<T,String> name) {
        return new StringConverter<>() {
            public String toString(T value) { return value == null ? "" : name.apply(value); }
            public T fromString(String text) { return null; }
        };
    }
}
