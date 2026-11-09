package com.hltv.tournamentcreator.utils;

import com.hltv.tournamentcreator.models.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class TournamentLifecycleTest {
    private final UUID event=UUID.randomUUID();
    private List<UUID> teams(int count){return IntStream.range(0,count).mapToObj(i -> UUID.randomUUID()).toList();}
    private List<TournamentMatch> completed(List<UUID> entrants){
        var draw=SingleElimination.generate(entrants);Map<UUID,UUID[]> pairs=new HashMap<>();
        for(var m:draw)pairs.put(m.matchId(),new UUID[]{m.team1Id(),m.team2Id()});
        List<TournamentMatch> result=new ArrayList<>();
        for(var m:draw){
            UUID a=pairs.get(m.matchId())[0],b=pairs.get(m.matchId())[1],winner=a==null?b:a;
            boolean bye=a==null||b==null;
            result.add(new TournamentMatch(m.matchId(),event,a,"A",b,"B","Bo1","Completed",null,SingleElimination.STAGE,m.round(),winner,bye?0:1,0,m.nextMatchId(),m.nextSlot(),null,bye?"Bye":"Played",null));
            if(m.nextMatchId()!=null)pairs.get(m.nextMatchId())[m.nextSlot()-1]=winner;
        }
        return result;
    }
    @Test void sharedPlacesForEightTeams(){
        var teams=teams(8);var places=TournamentLifecycle.placements(teams,List.of(),completed(teams));
        assertEquals(1,Collections.frequency(new ArrayList<>(places.values()),1));
        assertEquals(1,Collections.frequency(new ArrayList<>(places.values()),2));
        assertEquals(2,Collections.frequency(new ArrayList<>(places.values()),3));
        assertEquals(4,Collections.frequency(new ArrayList<>(places.values()),5));
    }
    @Test void byesAndMaximumFieldProduceCompletePlacements(){
        for(int n:List.of(2,3,5,12,256)){var entrants=teams(n);assertEquals(n,TournamentLifecycle.placements(entrants,List.of(),completed(entrants)).size());}
    }
    @Test void groupEliminationsSharePlaceBelowPlayoffQualifiers(){
        var ids=teams(8);var entrants=IntStream.range(0,8).mapToObj(i -> new TournamentParticipant(ids.get(i),"T"+i,i+1)).toList();
        var groups=GroupStage.draw(entrants);List<TournamentMatch> matches=new ArrayList<>();
        for(var group:groups)for(var f:GroupStage.fixtures(group))matches.add(new TournamentMatch(f.matchId(),event,f.team1(),"A",f.team2(),"B","Bo1","Completed",null,GroupStage.STAGE,"Round",f.team1(),1,0,null,null,group.groupId()));
        var qualifiers=GroupStage.playoffSeeds(groups,matches);matches.addAll(completed(qualifiers));
        var places=TournamentLifecycle.placements(ids,groups,matches);
        assertEquals(4,Collections.frequency(new ArrayList<>(places.values()),5));assertEquals(8,places.size());
    }
    @Test void incompleteTournamentCannotFinish(){
        var ids=teams(2);var m=completed(ids).getFirst();
        var upcoming=new TournamentMatch(m.matchId(),event,m.team1Id(),"A",m.team2Id(),"B","Bo1","Upcoming",null,m.stage(),m.round(),null,0,0,null,null);
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.placements(ids,List.of(),List.of(upcoming)));
    }
    @Test void awardedFinalHasChampionWithoutMapWins(){
        var ids=teams(2);var m=completed(ids).getFirst();
        var award=new TournamentMatch(m.matchId(),event,m.team1Id(),"A",m.team2Id(),"B","Bo1","Completed",null,m.stage(),m.round(),m.team2Id(),0,0,null,null,null,"Forfeit","No show");
        assertEquals(1,TournamentLifecycle.placements(ids,List.of(),List.of(award)).get(m.team2Id()));
    }
    @Test void invalidPlayedFinalCannotFinish(){
        var ids=teams(2);var m=completed(ids).getFirst();
        var invalid=new TournamentMatch(m.matchId(),event,m.team1Id(),"A",m.team2Id(),"B","Bo1","Completed",null,m.stage(),m.round(),m.team1Id(),0,0,null,null);
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.placements(ids,List.of(),List.of(invalid)));
    }
    @Test void multipleFinalsAndUnregisteredEntrantsAreRejected(){
        var ids=teams(4);var matches=completed(ids);
        var extra=new ArrayList<>(matches);extra.addAll(completed(teams(2)));
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.placements(ids,List.of(),extra));
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.placements(teams(4),List.of(),matches));
    }
    @Test void wrongAdvancementIsRejected(){
        var ids=teams(4);var matches=new ArrayList<>(completed(ids));var last=matches.removeLast();
        matches.add(new TournamentMatch(last.matchId(),event,ids.get(3),"A",last.team2Id(),"B","Bo1","Completed",null,last.stage(),last.round(),ids.get(3),1,0,null,null));
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.placements(ids,List.of(),matches));
    }
    @Test void forfeitAndWithdrawalCountPointsButNoMaps(){
        var ids=teams(4);var group=GroupStage.draw(IntStream.range(0,4).mapToObj(i -> new TournamentParticipant(ids.get(i),"T"+i,i+1)).toList()).getFirst();
        for(String outcome:List.of("Forfeit","Withdrawal")){
            var m=new TournamentMatch(UUID.randomUUID(),event,ids.get(0),"A",ids.get(1),"B","Bo3","Completed",null,GroupStage.STAGE,"Round",ids.get(0),0,0,null,null,group.groupId(),outcome,"Reason");
            var table=GroupStage.standings(group,List.of(m));
            assertEquals(3,table.getFirst().points());assertEquals(0,table.getFirst().mapsWon());assertEquals(1,table.getFirst().wins());
        }
    }
    @Test void closedAndArchivedStatesRejectEdits(){
        for(String state:List.of("Completed","Cancelled"))assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.requireEditable(new TournamentLifecycle.State(state,false)));
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.requireEditable(new TournamentLifecycle.State("In progress",true)));
        assertThrows(IllegalArgumentException.class,() -> TournamentLifecycle.requireRunning(new TournamentLifecycle.State("Draft",false)));
        assertDoesNotThrow(() -> TournamentLifecycle.requireRunning(new TournamentLifecycle.State("In progress",false)));
    }
}
