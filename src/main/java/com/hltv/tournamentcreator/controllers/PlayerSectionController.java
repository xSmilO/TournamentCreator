package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.dao.PlayerDAO;
import com.hltv.tournamentcreator.models.Player;
import com.hltv.tournamentcreator.utils.ModalManager;
import java.util.List;

public class PlayerSectionController extends DatabaseSectionController<Player> {
    private final PlayerDAO dao = new PlayerDAO();
    @Override protected String sectionName() { return "Players"; }
    @Override protected List<Player> fetchRows() throws Exception { return dao.getAllPlayers(); }
    @Override protected void configureColumns() {
        identityColumn("PLAYER", Player::nickname, p -> value(p.firstName()) + " " + value(p.lastName()), "user");
        column("COUNTRY", p -> p.country() == null ? "—" : p.country().name());
        column("TEAM", p -> p.team() == null ? "Free agent" : p.team().name());
        column("HLTV RATING", p -> p.hltvRating() == null ? "—" : String.format(java.util.Locale.ROOT, "%.2f", p.hltvRating()));
        column("HEADSHOTS", p -> p.headshotPercentage() == null ? "—" : String.format(java.util.Locale.ROOT, "%.1f%%", p.headshotPercentage()));
        actionColumn("Edit", player -> new OrganizerEditor(() -> { ModalManager.getInstance().hideModal(); loadData(); },
                () -> ModalManager.getInstance().hideModal()).player(player));
    }
    private String value(String value) { return value == null ? "" : value; }
    @Override protected List<String> filterOptions() { return List.of("All players", "On a team", "Free agents"); }
    @Override protected boolean matchesFilter(Player player, String filter) {
        return "On a team".equals(filter) ? player.team() != null
                : !"Free agents".equals(filter) || player.team() == null;
    }
    @Override protected void openCreator() {
        ModalManager.getInstance().showModal("/com/hltv/tournamentcreator/PlayerCreatorModal.fxml", this::loadData);
    }
}
