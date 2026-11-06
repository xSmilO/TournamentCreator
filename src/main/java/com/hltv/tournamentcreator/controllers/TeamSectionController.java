package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.TeamDAO;
import com.hltv.tournamentcreator.models.Team;
import com.hltv.tournamentcreator.utils.ModalManager;
import java.util.List;

public class TeamSectionController extends DatabaseSectionController<Team> {
    private final TeamDAO dao = new TeamDAO();
    @Override protected String sectionName() { return "Teams"; }
    @Override protected List<Team> fetchRows() throws Exception { return dao.getAllTeams(); }
    @Override protected void configureColumns() {
        identityColumn("TEAM", Team::name, t -> t.worldRanking() == null ? "Unranked" : "World rank #" + t.worldRanking(), "users");
        column("WORLD RANKING", Team::worldRanking);
        column("PLAYERS", Team::playerCount);
        actionColumn("View roster  →", team -> new TeamRosterDialog(team, dao, this::loadData).showRoster());
        actionColumn("＋  Add player", team -> new TeamRosterDialog(team, dao, this::loadData).showPlayerPicker());
    }
    @Override protected List<String> filterOptions() { return List.of("All teams", "Ranked", "Unranked"); }
    @Override protected boolean matchesFilter(Team team, String filter) {
        return "Ranked".equals(filter) ? team.worldRanking() != null
                : !"Unranked".equals(filter) || team.worldRanking() == null;
    }
    @Override protected void openCreator() {
        ModalManager.getInstance().showModal("/com/hltv/tournamentcreator/TeamCreatorModal.fxml", this::loadData);
    }
}
