package com.hltv.tournamentcreator.dao;

import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;
import java.sql.*;
import java.util.*;

public class LifecycleDAO {
    private final ConnectionSource connections;
    public LifecycleDAO() { this(DatabaseManager::getConnection); }
    LifecycleDAO(ConnectionSource connections) { this.connections=connections; }
    public record Placement(String team, int place) {}
    public record Audit(String time, String action, String reason, String before, String after) {}
    public record Overview(TournamentLifecycle.State state, List<Placement> placements, List<Audit> audit) {}
    public Overview overview(UUID id) throws SQLException {
        try(var c=connections.open()) {
            var state=state(c,id,false);
            List<Placement> places=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT t.name,p.placement FROM winit.tournament_participants p JOIN winit.teams t ON t.team_id=p.team_id WHERE p.tournament_id=? AND p.placement IS NOT NULL ORDER BY p.placement,t.name")) {
                s.setObject(1,id); try(var rs=s.executeQuery()) { while(rs.next()) places.add(new Placement(rs.getString("name"),rs.getInt("placement"))); }
            }
            List<Audit> audits=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT * FROM winit.tournament_audit WHERE tournament_id=? ORDER BY created_at DESC,audit_id DESC LIMIT 100")) {
                s.setObject(1,id); try(var rs=s.executeQuery()) { while(rs.next()) audits.add(new Audit(rs.getString("created_at"),rs.getString("action"),rs.getString("reason"),rs.getString("before_details"),rs.getString("after_details"))); }
            }
            return new Overview(state,List.copyOf(places),List.copyOf(audits));
        }
    }
    static TournamentLifecycle.State state(Connection c,UUID id,boolean lock) throws SQLException {
        try(var s=c.prepareStatement("SELECT tournament_id,lifecycle_state,archived FROM winit.tournaments WHERE tournament_id=?"+(lock?" FOR UPDATE":""))) {
            s.setObject(1,id); try(var rs=s.executeQuery()) {
                if(!rs.next()) throw rule("This tournament no longer exists.");
                return new TournamentLifecycle.State(rs.getString("lifecycle_state"),rs.getBoolean("archived"));
            }
        }
    }
    static void running(Connection c,UUID id) throws SQLException {
        try { TournamentLifecycle.requireRunning(state(c,id,false)); }
        catch(IllegalArgumentException e) { throw rule(e.getMessage()); }
    }
    static void audit(Connection c,UUID tournament,UUID match,String action,String reason,String before,String after) throws SQLException {
        try(var s=c.prepareStatement("INSERT INTO winit.tournament_audit (tournament_id,match_id,action,reason,before_details,after_details) VALUES (?,?,?,?,?,?)")) {
            Object[] values={tournament,match,action,reason,before,after};
            for(int i=0;i<values.length;i++) s.setObject(i+1,values[i]);
            s.executeUpdate();
        }
    }
    public void act(UUID id,TournamentLifecycle.State expected,String action,String inputReason) throws SQLException {
        String reason=CreatorValidation.text(inputReason,"Reason",500,Set.of("Reset draw","Reopen tournament","Cancel tournament").contains(action));
        if(reason==null) reason=action;
        try(var c=connections.open()) {
            c.setAutoCommit(false);
            try {
                var old=state(c,id,true);
                if(!old.equals(expected)) throw rule("The event state changed. Refresh Event control and try again.");
                var matches=new MatchDAO(connections).getMatches(c,id);
                String next=old.state(); boolean archived=old.archived();
                if ("Unarchive".equals(action)) {
                    if(!archived) throw rule("This event is not archived."); archived=false;
                } else {
                    if(archived) throw rule("Unarchive this event first.");
                    switch(action) {
                        case "Open registration" -> {
                            if(!"Draft".equals(next) || !matches.isEmpty() || hasGroups(c,id)) throw rule("Registration can only open for a draft without fixtures or groups. Reset an unplayed draw first.");
                            next="Registration";
                        }
                        case "Close registration" -> {
                            if(!"Registration".equals(next)) throw rule("Registration is not open."); next="Draft";
                        }
                        case "Start tournament" -> {
                            if(!Set.of("Draft","Registration").contains(next) || matches.isEmpty() || entrants(c,id).size()<2)
                                throw rule("Register at least two teams and create matches before starting.");
                            next="In progress";
                        }
                        case "Finish tournament" -> {
                            TournamentLifecycle.requireRunning(old);
                            var groups=new GroupStageDAO(connections).getGroups(c,id);
                            var places=TournamentLifecycle.placements(entrants(c,id),groups,matches);
                            clearPlacements(c,id);
                            try(var s=c.prepareStatement("UPDATE winit.tournament_participants SET placement=? WHERE tournament_id=? AND team_id=?")) {
                                for(var place:places.entrySet()) { s.setInt(1,place.getValue());s.setObject(2,id);s.setObject(3,place.getKey());if(s.executeUpdate()!=1)throw rule("A participant changed. Refresh and retry."); }
                            }
                            next="Completed";
                        }
                        case "Cancel tournament" -> {
                            TournamentLifecycle.requireEditable(old); clearPlacements(c,id); next="Cancelled";
                        }
                        case "Reopen tournament" -> {
                            if(!old.terminal()) throw rule("Only completed or cancelled events can be reopened.");
                            clearPlacements(c,id); next=matches.isEmpty()?"Draft":"In progress";
                        }
                        case "Archive" -> {
                            if(!old.terminal()) throw rule("Only completed or cancelled events can be archived."); archived=true;
                        }
                        case "Reset draw" -> {
                            if(!Set.of("Draft","Registration").contains(next)) throw rule("Reset is only available before the tournament starts.");
                            if(matches.stream().anyMatch(m -> !"Upcoming".equals(m.status()) && !"Bye".equals(m.outcome()))) throw rule("A match has already started or been resolved. The draw cannot be reset.");
                            try(var s=c.prepareStatement("SELECT 1 FROM winit.games g JOIN winit.matches m ON m.match_id=g.match_id WHERE m.tournament_id=? AND (g.status<>'Upcoming' OR g.team1_score<>0 OR g.team2_score<>0) LIMIT 1")) {
                                s.setObject(1,id);try(var rs=s.executeQuery()){if(rs.next())throw rule("Map play has started. The draw cannot be reset.");}
                            }
                            audit(c,id,null,"Reset fixtures",reason,matches.toString(),"All fixtures/groups removed; registrations and seeds retained");
                            try(var s=c.prepareStatement("DELETE FROM winit.matches WHERE tournament_id=?")){s.setObject(1,id);s.executeUpdate();}
                            try(var s=c.prepareStatement("DELETE FROM winit.tournament_groups WHERE tournament_id=?")){s.setObject(1,id);s.executeUpdate();}
                            clearPlacements(c,id); next="Draft";
                        }
                        default -> throw new IllegalArgumentException("Choose an event action.");
                    }
                }
                try(var s=c.prepareStatement("UPDATE winit.tournaments SET lifecycle_state=?,archived=? WHERE tournament_id=?")) {
                    s.setString(1,next);s.setObject(2,archived);s.setObject(3,id);s.executeUpdate();
                }
                audit(c,id,null,action,reason,old.toString(),new TournamentLifecycle.State(next,archived).toString());
                c.commit();
            } catch(SQLException|RuntimeException e) {
                try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;
            }
        }
    }
    private boolean hasGroups(Connection c,UUID id)throws SQLException {
        try(var s=c.prepareStatement("SELECT 1 FROM winit.tournament_groups WHERE tournament_id=? LIMIT 1")){s.setObject(1,id);try(var rs=s.executeQuery()){return rs.next();}}
    }
    private List<UUID> entrants(Connection c,UUID id)throws SQLException {
        List<UUID> result=new ArrayList<>();
        try(var s=c.prepareStatement("SELECT team_id FROM winit.tournament_participants WHERE tournament_id=?")){s.setObject(1,id);try(var rs=s.executeQuery()){while(rs.next())result.add(rs.getObject("team_id",UUID.class));}}
        return result;
    }
    private void clearPlacements(Connection c,UUID id)throws SQLException {
        try(var s=c.prepareStatement("UPDATE winit.tournament_participants SET placement=NULL WHERE tournament_id=?")){s.setObject(1,id);s.executeUpdate();}
    }
    static SQLException rule(String message){return new SQLException(message,"P0001");}
}
