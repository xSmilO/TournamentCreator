import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.time.*;
import com.hltv.tournamentcreator.controllers.*;
import com.hltv.tournamentcreator.dao.*;
import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;

public class OrganizerUiSmoke {
 static StackPane root, overlay; static OrganizerEditor editor;
 static AtomicReference<Throwable> error=new AtomicReference<>();
 static AtomicReference<String> saved=new AtomicReference<>();
 static AtomicInteger done=new AtomicInteger(), cancelled=new AtomicInteger();
 static UUID id=UUID.randomUUID();
 static Team a=new Team(id,"Alpha",1), b=new Team(UUID.randomUUID(),"Beta",2);
 static Country country=new Country(1,"Poland","PL");
 static void fx(Runnable action) throws Exception {
  var latch=new CountDownLatch(1); Platform.runLater(() -> {try {action.run();}catch(Throwable t){error.set(t);}finally{latch.countDown();}});
  if(!latch.await(10,TimeUnit.SECONDS)) throw new AssertionError("FX timeout");
  if(error.get()!=null) throw new RuntimeException(error.get());
 }
 static <T> T node(String id) {root.applyCss();root.layout();return (T)overlay.lookup("#"+id);}
 static void click(String id){((Button)node(id)).fire();}
 static void field(String id,String value){((TextField)node(id)).setText(value);}
 static void check(boolean value){if(!value)throw new AssertionError("Check failed");}
 static void await(BooleanSupplier ready) throws Exception {
  for(int i=0;i<150;i++){var result=new AtomicBoolean();fx(() -> result.set(ready.getAsBoolean()));if(result.get())return;Thread.sleep(20);}throw new AssertionError("Async timeout");
 }
 static void complete(String expected) throws Exception {
  await(() -> ((Label)node("status")).getText().equals("Saved successfully."));
  fx(() -> {check(expected.equals(saved.get()));click("cancel");});
 }
 static void snapshot(String name) {
  try {root.applyCss();root.layout();ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",new File("target/organizer-"+name+".png"));}catch(Exception e){throw new RuntimeException(e);}
 }
 public static void main(String[] args)throws Exception {
  Platform.startup(() -> {});
  try {
   fx(() -> {
    root=new StackPane();overlay=new StackPane();root.getChildren().add(overlay);new Scene(root,1180,760);root.resize(1180,760);
    root.getStylesheets().add(OrganizerUiSmoke.class.getResource("/com/hltv/tournamentcreator/css/app.css").toExternalForm());ModalManager.initialize(overlay);
    var dao=new OrganizerDAO(){
     public void updatePlayer(Player p,String n,String f,String l,Country c,LocalDate d){check(c.equals(country));check(n.equals("Updated"));check(d.equals(LocalDate.of(2000,1,1)));saved.set("player");}
     public void updateTeam(Team t,String n,Integer r){check(n.equals("Renamed"));check(r==null);saved.set("team");}
     public void updateTournament(Tournament o,Tournament t){check(t.name().equals("Updated cup"));check(t.startDate().equals(LocalDate.of(2027,1,1)));saved.set("tournament");}
     public void movePlayer(UUID source,RosterPlayer p,UUID target,String role){check(source.equals(id));if(target==null){check(role==null);saved.set("release");}else{check(target.equals(b.teamId()));check(role.equals("AWPer"));saved.set("transfer");}}
     public void updateSeed(UUID t,TournamentParticipant p,int seed){check(seed==7);saved.set("seed");}
     public void updateSchedule(TournamentMatch m,LocalDateTime date){check(date.equals(LocalDateTime.of(2027,1,2,18,30)));saved.set("schedule");}
    };
    editor=new OrganizerEditor(dao,new CountryDAO(){public List<Country> getAllCountries(){return List.of(country);}},new TeamDAO(){public List<Team> getAllTeams(){return List.of(a,b);}},done::incrementAndGet,cancelled::incrementAndGet);
    editor.team(a);check(((TextField)node("name")).getText().equals("Alpha"));field("name","Never saved");click("cancel");check(saved.get()==null&&cancelled.get()==1);
    editor.team(a);field("name","Renamed");field("ranking","");click("save");
   }); complete("team");
   fx(() -> editor.player(new Player(id,"nick","First","Last",country,LocalDate.of(2000,1,1),a,null,null,null)));
   await(() -> !((Button)node("save")).isDisabled());
   fx(() -> {field("name","Updated");snapshot("player");click("save");});complete("player");
   fx(() -> {editor.tournament(new Tournament(id,"Cup",null,LocalDate.of(2027,1,1),null,null,null,"Custom",0,16));field("name","Updated cup");snapshot("tournament");click("save");});complete("tournament");
   var player=new RosterPlayer(UUID.randomUUID(),"nick",null,null,null,null,"IGL");
   fx(() -> editor.move(a,player,false));await(() -> !((Button)node("save")).isDisabled());
   fx(() -> {ComboBox<Team> box=node("destination");check(box.getItems().equals(List.of(b)));box.setValue(b);((ComboBox<?>)node("role")).getEditor().setText("AWPer");snapshot("transfer");click("save");});complete("transfer");
   fx(() -> {editor.move(a,player,true);click("save");});complete("release");
   fx(() -> {editor.seed(id,new TournamentParticipant(a.teamId(),"Alpha",1));field("seed","0");click("save");check(((Label)node("status")).getText().contains("positive"));field("seed","7");click("save");});complete("seed");
   fx(() -> {editor.schedule(new TournamentMatch(UUID.randomUUID(),id,a.teamId(),"Alpha",b.teamId(),"Beta","Bo3","Upcoming",LocalDateTime.of(2027,1,2,18,30),"Single elimination","Final",null,0,0,null,null));field("time","25:00");click("save");check(((Label)node("status")).getText().contains("HH:mm"));field("time","18:30");snapshot("schedule");click("save");});complete("schedule");
   check(done.get()==7);
   System.out.println("PASS: seven organizer forms, prefill, validation, cancel, save, and return callbacks; no database access");
  } finally {Platform.exit();}
 }
}
