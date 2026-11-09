package com.hltv.tournamentcreator.dao;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
class TournamentDeletionTest {
 private final UUID id=UUID.randomUUID();
 private MatchPersistenceTest.Jdbc jdbc(){var j=new MatchPersistenceTest.Jdbc();j.query=sql -> {assertTrue(sql.endsWith("FOR UPDATE"));return List.of(Map.of("name","Cup"));};return j;}
 @Test void deletesOnlySelectedTournamentAndCommits()throws Exception{var j=jdbc();new TournamentDAO(j::connection).deleteTournament(id,"Cup");assertTrue(j.committed);assertEquals(1,j.updates.size());assertEquals("DELETE FROM winit.tournaments WHERE tournament_id=?",j.updates.getFirst().sql());assertEquals(id,j.updates.getFirst().parameters().get(1));}
 @Test void renamedTournamentCannotBeDeletedFromStaleDialog(){var j=jdbc();assertThrows(SQLException.class,() -> new TournamentDAO(j::connection).deleteTournament(id,"Old cup"));assertTrue(j.rolledBack);assertTrue(j.updates.isEmpty());}
 @Test void missingTournamentIsRejected(){var j=jdbc();j.query=sql -> List.of();assertThrows(SQLException.class,() -> new TournamentDAO(j::connection).deleteTournament(id,"Cup"));assertTrue(j.rolledBack);assertTrue(j.updates.isEmpty());}
 @Test void failureRollsBack(){var j=jdbc();j.failUpdate="DELETE";assertThrows(SQLException.class,() -> new TournamentDAO(j::connection).deleteTournament(id,"Cup"));assertTrue(j.rolledBack);assertFalse(j.committed);}
}
