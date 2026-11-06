package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.TeamDAO;
import com.hltv.tournamentcreator.dao.TournamentDAO;
import com.hltv.tournamentcreator.models.Team;
import com.hltv.tournamentcreator.models.Tournament;
import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TournamentCreatorController extends CreatorController {
    @FXML private TextField name, prize, locationField, format, teamLimit;
    @FXML private ComboBox<String> tier;
    @FXML private DatePicker start, end;
    private Set<Team> selected;
    @FXML public void initialize() { selected = loadChoices(() -> new TeamDAO().getAllTeams(), Team::name); }
    @Override protected BackgroundTask.Work<Void> validatedSave() {
        String validName = CreatorValidation.text(name.getText(), "Tournament name", 200, true);
        int capacity = CreatorValidation.teamLimit(teamLimit.getText());
        LocalDate startDate = readDate(start), endDate = readDate(end);
        CreatorValidation.dates(startDate, endDate);
        BigDecimal prizePool = CreatorValidation.prizePool(prize.getText());
        String validLocation = CreatorValidation.text(locationField.getText(), "Location", 100, false);
        String validFormat = CreatorValidation.text(format.getText(), "Format", 50, false);
        List<UUID> teamIds = selected.stream().map(Team::teamId).toList();
        if (teamIds.size() > capacity) throw new IllegalArgumentException("Selected teams exceed the team limit of " + capacity + ".");
        Tournament tournament = new Tournament(UUID.randomUUID(), validName,
                "Unspecified".equals(tier.getValue()) ? null : tier.getValue(), startDate, endDate,
                prizePool, validLocation, validFormat, teamIds.size(), capacity);
        return () -> { new TournamentDAO().createTournament(tournament, teamIds); return null; };
    }
    private LocalDate readDate(DatePicker picker) {
        String text = picker.getEditor().getText();
        if (text == null || text.isBlank()) return null;
        try { return picker.getConverter().fromString(text.trim()); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Enter a valid date or choose one from the calendar."); }
    }
}
