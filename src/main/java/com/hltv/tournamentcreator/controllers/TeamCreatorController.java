package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.TeamDAO;
import com.hltv.tournamentcreator.models.Player;
import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TeamCreatorController extends CreatorController {
    @FXML private TextField name, ranking;
    private final TeamDAO dao = new TeamDAO();
    private Set<Player> selected;
    @FXML public void initialize() { selected = loadChoices(dao::getFreeAgents, Player::nickname); }
    @Override protected BackgroundTask.Work<Void> validatedSave() {
        String validName = CreatorValidation.text(name.getText(), "Team name", 100, true);
        Integer validRanking = CreatorValidation.ranking(ranking.getText());
        List<UUID> playerIds = selected.stream().map(Player::playerId).toList();
        return () -> { dao.createTeam(validName, validRanking, playerIds); return null; };
    }
}
