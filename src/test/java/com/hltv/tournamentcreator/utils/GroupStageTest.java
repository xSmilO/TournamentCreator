package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class GroupStageTest {
    private List<TournamentParticipant> teams(int count) {
        return IntStream.rangeClosed(1,count).mapToObj(i -> new TournamentParticipant(UUID.randomUUID(),"Team "+i,i)).toList();
    }
    private TournamentMatch result(TournamentGroup group,int a,int b,int score1,int score2) {
        var first=group.teams().get(a); var second=group.teams().get(b);
        return new TournamentMatch(UUID.randomUUID(),UUID.randomUUID(),first.teamId(),first.name(),second.teamId(),second.name(),
                "Bo3","Completed",null,GroupStage.STAGE,group.name(),score1>score2?first.teamId():second.teamId(),score1,score2,null,null,group.groupId());
    }
    private List<TournamentMatch> completed(List<TournamentGroup> groups) {
        List<TournamentMatch> matches=new ArrayList<>();
        for(var group:groups) for(var f:GroupStage.fixtures(group)) {
            var first=group.teams().stream().filter(t -> t.teamId().equals(f.team1())).findFirst().orElseThrow();
            var second=group.teams().stream().filter(t -> t.teamId().equals(f.team2())).findFirst().orElseThrow();
            boolean firstWins=first.seed()<second.seed();
            matches.add(new TournamentMatch(f.matchId(),UUID.randomUUID(),f.team1(),first.name(),f.team2(),second.name(),"Bo3","Completed",null,
                    GroupStage.STAGE,"Round "+f.round(),firstWins?f.team1():f.team2(),firstWins?2:0,firstWins?0:2,null,null,group.groupId()));
        }
        return matches;
    }

    @Test void snakeDrawBalancesSeedsAndAssignsEveryTeamExactlyOnce() {
        var teams=teams(8); var groups=GroupStage.draw(teams);
        assertEquals(List.of(1,4,5,8),groups.get(0).teams().stream().map(TournamentParticipant::seed).toList());
        assertEquals(List.of(2,3,6,7),groups.get(1).teams().stream().map(TournamentParticipant::seed).toList());
        assertEquals(8,groups.stream().flatMap(g -> g.teams().stream()).map(TournamentParticipant::teamId).distinct().count());
    }
    @Test void eachGroupHasSixUniqueMatchesAndThreeMatchesPerTeam() {
        var group=GroupStage.draw(teams(4)).get(0); var fixtures=GroupStage.fixtures(group);
        assertEquals(6,fixtures.size()); Set<Set<UUID>> pairs=new HashSet<>();
        for(var f:fixtures) { assertTrue(pairs.add(Set.of(f.team1(),f.team2()))); assertEquals(group.groupId(),f.groupId()); }
        for(var team:group.teams()) assertEquals(3,fixtures.stream().filter(f -> f.team1().equals(team.teamId())||f.team2().equals(team.teamId())).count());
        for(int round=1;round<=3;round++) {
            final int r=round;
            assertEquals(4,fixtures.stream().filter(f -> f.round()==r).flatMap(f -> java.util.stream.Stream.of(f.team1(),f.team2())).distinct().count());
        }
    }
    @Test void largeTournamentCreatesSixtyFourGroupsAnd384Matches() {
        var groups=GroupStage.draw(teams(256));
        assertEquals(64,groups.size()); assertEquals("Group BL",groups.get(63).name());
        assertEquals(384,groups.stream().mapToInt(g -> GroupStage.fixtures(g).size()).sum());
    }
    @Test void standingsAwardThreePointsPerWinAndIgnoreUnplayedMatches() {
        var group=GroupStage.draw(teams(4)).get(0);
        var table=GroupStage.standings(group,List.of(result(group,0,1,2,0)));
        assertEquals(group.teams().get(0).teamId(),table.get(0).teamId()); assertEquals(3,table.get(0).points());
        assertEquals(1,table.get(0).played()); assertEquals(2,table.get(0).mapDifference());
        assertEquals(2,table.stream().mapToInt(GroupStanding::played).sum());
    }
    @Test void headToHeadBreaksEqualPointsBeforeOverallMapDifference() {
        var group=GroupStage.draw(teams(4)).get(0);
        var matches=List.of(result(group,0,1,2,1),result(group,0,2,0,2),result(group,0,3,2,0),
                result(group,1,2,2,0),result(group,1,3,2,0),result(group,2,3,0,2));
        var table=GroupStage.standings(group,matches);
        assertEquals(List.of(group.teams().get(0).teamId(),group.teams().get(1).teamId(),group.teams().get(3).teamId(),group.teams().get(2).teamId()),
                table.stream().map(GroupStanding::teamId).toList());
        assertTrue(table.get(0).mapDifference()<table.get(1).mapDifference());
    }
    @Test void threeWayHeadToHeadTieUsesMapDifferenceThenSeed() {
        var group=GroupStage.draw(teams(4)).get(0);
        var matches=List.of(result(group,0,1,2,1),result(group,1,2,2,0),result(group,2,0,2,0),
                result(group,0,3,2,0),result(group,1,3,2,0),result(group,2,3,2,0));
        var table=GroupStage.standings(group,matches);
        assertEquals(group.teams().get(1).teamId(),table.get(0).teamId());
        assertEquals(group.teams().get(2).teamId(),table.get(1).teamId());
        var tied=List.of(result(group,0,1,2,0),result(group,1,2,2,0),result(group,2,0,2,0),
                result(group,0,3,2,0),result(group,1,3,2,0),result(group,2,3,2,0));
        assertEquals(group.teams().get(0).teamId(),GroupStage.standings(group,tied).get(0).teamId());
    }
    @Test void qualifiersAlwaysGoIntoOppositeBracketHalvesIncludingOddGroupCounts() {
        for(int n=4;n<=256;n+=4) {
            var groups=GroupStage.draw(teams(n)); var matches=completed(groups);
            var seeds=GroupStage.playoffSeeds(groups,matches);
            assertEquals(groups.size()*2,seeds.size()); assertEquals(seeds.size(),new HashSet<>(seeds).size());
            int size=2; while(size<seeds.size())size*=2;
            var positions=SingleElimination.seedOrder(size);
            for(var group:groups) {
                var table=GroupStage.standings(group,matches);
                int winnerPosition=positions.indexOf(seeds.indexOf(table.get(0).teamId())+1);
                int runnerPosition=positions.indexOf(seeds.indexOf(table.get(1).teamId())+1);
                assertNotEquals(winnerPosition<size/2,runnerPosition<size/2);
            }
            assertDoesNotThrow(() -> SingleElimination.generate(seeds));
        }
    }
    @Test void playoffsCannotBeGeneratedUntilEveryFixtureIsCompleted() {
        var groups=GroupStage.draw(teams(8)); var matches=completed(groups);
        assertTrue(GroupStage.complete(groups,matches));
        assertFalse(GroupStage.complete(groups,matches.subList(0,matches.size()-1)));
        assertThrows(IllegalArgumentException.class, () -> GroupStage.playoffSeeds(groups,List.of()));
    }
    @Test void rejectsDuplicateFixturesAndInconsistentResults() {
        var group=GroupStage.draw(teams(4)).get(0); var match=result(group,0,1,2,0);
        assertThrows(IllegalArgumentException.class, () -> GroupStage.standings(group,List.of(match,match)));
        assertThrows(IllegalArgumentException.class, () -> GroupStage.standings(group,List.of(result(group,0,1,1,0))));
    }
    @Test void requiresFourTeamGroupsAndUniqueEntrants() {
        assertThrows(IllegalArgumentException.class, () -> GroupStage.draw(teams(3)));
        assertThrows(IllegalArgumentException.class, () -> GroupStage.draw(teams(6)));
        var one=teams(1).get(0);
        assertThrows(IllegalArgumentException.class, () -> GroupStage.draw(List.of(one,one,one,one)));
    }
}
