package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.CreatorValidation;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Organizer edits use original field values to reject stale windows. */
public class OrganizerDAO {
    private final ConnectionSource connections;
    public OrganizerDAO() { this(DatabaseManager::getConnection); }
    OrganizerDAO(ConnectionSource connections) { this.connections = connections; }

    public void updatePlayer(Player old, String nickname, String first, String last, Country country, LocalDate birth) throws SQLException {
        nickname = CreatorValidation.text(nickname, "Nickname", 50, true);
        first = CreatorValidation.text(first, "First name", 50, false);
        last = CreatorValidation.text(last, "Surname", 50, false);
        if (birth != null && birth.isAfter(LocalDate.now())) throw new IllegalArgumentException("Birth date cannot be in the future.");
        try (var c = connections.open()) {
            update(c, """
                UPDATE winit.players SET nickname=?, first_name=?, last_name=?, country_id=?, date_of_birth=?
                WHERE player_id=? AND nickname IS NOT DISTINCT FROM ? AND first_name IS NOT DISTINCT FROM ?
                AND last_name IS NOT DISTINCT FROM ? AND country_id IS NOT DISTINCT FROM ? AND date_of_birth IS NOT DISTINCT FROM ?
                """, nickname, first, last, country == null ? null : country.countryId(), birth,
                old.playerId(), old.nickname(), old.firstName(), old.lastName(), old.country() == null ? null : old.country().countryId(), old.dateOfBirth());
        }
    }

    public void updateTeam(Team old, String name, Integer ranking) throws SQLException {
        name = CreatorValidation.text(name, "Team name", 100, true);
        CreatorValidation.ranking(ranking == null ? "" : ranking.toString());
        try (var c = connections.open()) {
            update(c, "UPDATE winit.teams SET name=?, world_ranking=? WHERE team_id=? AND name IS NOT DISTINCT FROM ? AND world_ranking IS NOT DISTINCT FROM ?",
                name, ranking, old.teamId(), old.name(), old.worldRanking());
        }
    }

    public void movePlayer(UUID sourceTeam, RosterPlayer player, UUID destinationTeam, String role) throws SQLException {
        if (sourceTeam.equals(destinationTeam)) throw new IllegalArgumentException("Choose a different destination team.");
        String newRole = destinationTeam == null ? null : CreatorValidation.text(role, "Destination role", 50, true);
        // A single UPDATE atomically moves the team and role; no intermediate free-agent state.
        try (var c = connections.open()) {
            update(c, "UPDATE winit.players SET team_id=?, team_role=? WHERE player_id=? AND team_id=? AND team_role IS NOT DISTINCT FROM ?",
                destinationTeam, newRole, player.playerId(), sourceTeam, player.role());
        }
    }

    public void updateTournament(Tournament old, Tournament value) throws SQLException {
        String name = CreatorValidation.text(value.name(), "Tournament name", 200, true);
        String location = CreatorValidation.text(value.location(), "Location", 100, false);
        String format = CreatorValidation.text(value.format(), "Format", 50, false);
        CreatorValidation.dates(value.startDate(), value.endDate());
        CreatorValidation.prizePool(value.prizePool() == null ? "" : value.prizePool().toPlainString());
        if (value.tier() != null && !Set.of("S-Tier", "A-Tier", "B-Tier", "C-Tier").contains(value.tier()))
            throw new IllegalArgumentException("Choose a valid tier.");
        transaction(c -> {
            MatchDAO.lockTournament(c, old.tournamentId());
            boolean structureChanged = !Objects.equals(old.startDate(), value.startDate())
                || !Objects.equals(old.endDate(), value.endDate()) || !Objects.equals(old.format(), format);
            if (structureChanged) {
                boolean datesStarted = old.startDate() != null && !old.startDate().isAfter(LocalDate.now())
                    || old.endDate() != null && old.endDate().isBefore(LocalDate.now());
                if (datesStarted || competitionExists(c, old.tournamentId()))
                    throw rule("Dates and format are locked after the start date, a draw, or a started match. Other details can still be edited.");
            }
            update(c, """
                UPDATE winit.tournaments SET name=?, tier=?, start_date=?, end_date=?, prize_pool=?, location=?, format=?
                WHERE tournament_id=? AND name IS NOT DISTINCT FROM ? AND tier IS NOT DISTINCT FROM ?
                AND start_date IS NOT DISTINCT FROM ? AND end_date IS NOT DISTINCT FROM ?
                AND prize_pool IS NOT DISTINCT FROM ? AND location IS NOT DISTINCT FROM ? AND format IS NOT DISTINCT FROM ?
                """, name, value.tier(), value.startDate(), value.endDate(), value.prizePool(), location, format,
                old.tournamentId(), old.name(), old.tier(), old.startDate(), old.endDate(), old.prizePool(), old.location(), old.format());
        });
    }

    public void updateSeed(UUID tournament, TournamentParticipant old, int seed) throws SQLException {
        if (seed < 1) throw new IllegalArgumentException("Seed must be a positive whole number.");
        transaction(c -> {
            MatchDAO.lockTournament(c, tournament);
            if (competitionExists(c, tournament)) throw rule("Seeds are locked after groups/a bracket are drawn or a match starts.");
            try (var s = c.prepareStatement("SELECT 1 FROM winit.tournament_participants WHERE tournament_id=? AND seed_number=? AND team_id<>?")) {
                bind(s, tournament, seed, old.teamId());
                try (var rs = s.executeQuery()) { if (rs.next()) throw rule("That seed is already used. Choose an unused seed."); }
            }
            update(c, "UPDATE winit.tournament_participants SET seed_number=? WHERE tournament_id=? AND team_id=? AND seed_number IS NOT DISTINCT FROM ?",
                seed, tournament, old.teamId(), old.seed());
        });
    }

    public void updateSchedule(TournamentMatch old, LocalDateTime start) throws SQLException {
        transaction(c -> {
            MatchDAO.lockTournament(c, old.tournamentId());
            update(c, """
                UPDATE winit.matches SET start_time=? WHERE match_id=? AND tournament_id=?
                AND start_time IS NOT DISTINCT FROM ? AND status='Upcoming'
                """, start, old.matchId(), old.tournamentId(), old.start());
        });
    }

    private boolean competitionExists(Connection c, UUID id) throws SQLException {
        try (var s = c.prepareStatement("""
            SELECT 1 FROM winit.matches WHERE tournament_id=?
            AND (stage IN ('Single elimination', 'Group stage') OR status IN ('Live', 'Completed'))
            UNION ALL SELECT 1 FROM winit.tournament_groups WHERE tournament_id=? LIMIT 1
            """)) {
            bind(s, id, id);
            try (var rs = s.executeQuery()) { return rs.next(); }
        }
    }
    private static void bind(PreparedStatement s, Object... args) throws SQLException {
        for (int i=0; i<args.length; i++) s.setObject(i+1, args[i]);
    }
    private static void update(Connection c, String sql, Object... args) throws SQLException {
        try (var s = c.prepareStatement(sql)) {
            bind(s, args);
            if (s.executeUpdate() != 1) throw rule("This entry changed, was removed, or is no longer editable. Close this form, refresh, and try again.");
        }
    }
    private static SQLException rule(String message) { return new SQLException(message, "P0001"); }
    @FunctionalInterface private interface Work { void run(Connection c) throws SQLException; }
    private void transaction(Work work) throws SQLException {
        try (var c = connections.open()) {
            c.setAutoCommit(false);
            try { work.run(c); c.commit(); }
            catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
