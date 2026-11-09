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
import com.hltv.tournamentcreator.controllers.*;
import com.hltv.tournamentcreator.dao.*;
import com.hltv.tournamentcreator.models.*;
import com.hltv.tournamentcreator.utils.*;

public class LifecycleUiSmoke {
 static StackPane root,overlay;static AtomicReference<Throwable> failure=new AtomicReference<>();
 static AtomicReference<TournamentLifecycle.State> state=new AtomicReference<>(new TournamentLifecycle.State("Draft",false));
 static AtomicInteger changes=new AtomicInteger(),outcomes=new AtomicInteger();
 static UUID event=UUID.randomUUID(),a=UUID.randomUUID(),b=UUID.randomUUID();
 static void fx(Runnable task)throws Exception {var latch=new CountDownLatch(1);Platform.runLater(() -> {try{task.run();}catch(Throwable e){failure.set(e);}finally{latch.countDown();}});if(!latch.await(10,TimeUnit.SECONDS))throw new AssertionError("FX timeout");if(failure.get()!=null)throw new RuntimeException(failure.get());}
 static <T> T get(String id){root.applyCss();root.layout();return (T)overlay.lookup("#"+id);}
 static void check(boolean test){if(!test)throw new AssertionError("UI assertion failed");}
 static void await(BooleanSupplier test)throws Exception {for(int i=0;i<200;i++){var ok=new AtomicBoolean();fx(() -> ok.set(test.getAsBoolean()));if(ok.get())return;Thread.sleep(20);}throw new AssertionError("Async timeout");}
 static void snap(String name){try{ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",new File("target/lifecycle-"+name+".png"));}catch(Exception e){throw new RuntimeException(e);}}
 static void act(String action)throws Exception {
  fx(() -> {LifecycleUiSmoke.<ComboBox<String>>get("action").setValue(action);LifecycleUiSmoke.<TextArea>get("reason").setText("Verified organizer action");LifecycleUiSmoke.<CheckBox>get("confirm").setSelected(true);LifecycleUiSmoke.<Button>get("apply").fire();});
  await(() -> !LifecycleUiSmoke.<VBox>get("creator-form").isDisabled() && LifecycleUiSmoke.<Label>get("status").getText().equals("Action saved."));
 }
 public static void main(String[] args)throws Exception {
  Platform.startup(() -> {});
  try {
   fx(() -> {
    root=new StackPane();overlay=new StackPane();root.getChildren().add(overlay);new Scene(root,1180,760);root.resize(1180,760);ModalManager.initialize(overlay);
    root.getStylesheets().add(LifecycleUiSmoke.class.getResource("/com/hltv/tournamentcreator/css/app.css").toExternalForm());
    var dao=new LifecycleDAO(){
     public Overview overview(UUID id){return new Overview(state.get(),state.get().state().equals("Completed")?List.of(new Placement("Alpha",1),new Placement("Beta",2)):List.of(),List.of(new Audit("2026-11-08","Lifecycle change","Verified organizer action","Draft","Registration")));}
     public void act(UUID id,TournamentLifecycle.State expected,String action,String reason){check(id.equals(event));check(expected.equals(state.get()));check(!reason.isBlank());state.set(switch(action){case "Open registration" -> new TournamentLifecycle.State("Registration",false);case "Start tournament" -> new TournamentLifecycle.State("In progress",false);case "Finish tournament" -> new TournamentLifecycle.State("Completed",false);case "Archive" -> new TournamentLifecycle.State("Completed",true);case "Unarchive" -> new TournamentLifecycle.State("Completed",false);default -> throw new AssertionError(action);});}
    };
    new EventControl(event,dao,changes::incrementAndGet,() -> {}).show("NOVA MASTERS");
   });
   await(() -> !LifecycleUiSmoke.<VBox>get("creator-form").isDisabled());
   fx(() -> {LifecycleUiSmoke.<Button>get("apply").fire();check(LifecycleUiSmoke.<Label>get("status").getText().contains("Confirm"));snap("control");});
   act("Open registration");act("Start tournament");act("Finish tournament");
   fx(() -> {TabPane tabs=(TabPane)overlay.lookup(".tab-pane");tabs.getSelectionModel().select(1);root.applyCss();root.layout();check(LifecycleUiSmoke.<TableView<?>>get("standings").getItems().size()==2);snap("standings");tabs.getSelectionModel().select(2);root.applyCss();root.layout();LifecycleUiSmoke.<TableView<?>>get("history").getSelectionModel().selectFirst();check(LifecycleUiSmoke.<TextArea>get("details").getText().contains("Before:"));snap("history");tabs.getSelectionModel().select(0);});
   act("Archive");act("Unarchive");check(changes.get()==5);
   fx(() -> {
    var m=new TournamentMatch(UUID.randomUUID(),event,a,"Alpha",b,"Beta","Bo3","Upcoming",null,SingleElimination.STAGE,"Semifinal",null,0,0,UUID.randomUUID(),1);
    var dao=new MatchDAO(){public List<MapResult> getMaps(UUID id){return List.of();}public void saveOutcome(TournamentMatch expected,List<MapResult> maps,String outcome,UUID winner,String reason){check(outcome.equals("Forfeit"));check(winner.equals(b));check(reason.equals("Alpha did not appear"));outcomes.incrementAndGet();}};
    new MatchOutcomeEditor(m,dao,"Affected next match: Final (Upcoming).",() -> {},() -> {}).show();
   });
   await(() -> !LifecycleUiSmoke.<VBox>get("creator-form").isDisabled());
   fx(() -> {LifecycleUiSmoke.<Button>get("save").fire();check(LifecycleUiSmoke.<Label>get("status").getText().contains("Confirm"));LifecycleUiSmoke.<ComboBox<?>>get("winner").getSelectionModel().select(1);LifecycleUiSmoke.<TextArea>get("reason").setText("Alpha did not appear");LifecycleUiSmoke.<CheckBox>get("confirm").setSelected(true);snap("outcome");LifecycleUiSmoke.<Button>get("save").fire();});
   await(() -> outcomes.get()==1);
   System.out.println("PASS: event actions, confirmation, standings/history, archive/unarchive and forfeit form; fake DAOs only");
  }finally{Platform.exit();}
 }
}
