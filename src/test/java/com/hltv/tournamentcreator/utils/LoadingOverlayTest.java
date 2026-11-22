package com.hltv.tournamentcreator.utils;

import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class LoadingOverlayTest {
    @BeforeAll static void toolkit() throws Exception {
        CompletableFuture<Void> ready = new CompletableFuture<>();
        Platform.startup(() -> ready.complete(null));
        ready.get(10, TimeUnit.SECONDS);
    }

    @Test void repeatedAndOverlappingLoadsPreserveTableAndLayout() throws Exception {
        onFx(() -> {
            TableView<String> table = new TableView<>();
            table.getItems().add("Saved row");
            VBox parent = new VBox(table);
            VBox.setVgrow(table, Priority.ALWAYS);
            Runnable first = LoadingOverlay.show(table);
            StackPane host = (StackPane) parent.getChildren().getFirst();
            assertEquals(Priority.ALWAYS, VBox.getVgrow(host));
            var mask = host.getChildren().get(1);
            assertTrue(mask.isVisible());
            Runnable second = LoadingOverlay.show(table);
            first.run();
            assertTrue(mask.isVisible());
            second.run();
            assertFalse(mask.isVisible());
            assertFalse(mask.isManaged());
            assertEquals("Saved row", table.getItems().getFirst());
            Runnable refresh = LoadingOverlay.show(table);
            assertSame(host, table.getParent());
            assertTrue(mask.isVisible());
            refresh.run();
        });
    }

    @Test void failedFetchRemovesMaskAndRunsErrorCallback() throws Exception {
        CompletableFuture<StackPane> completed = new CompletableFuture<>();
        onFx(() -> {
            TableView<String> table = new TableView<>();
            VBox parent = new VBox(table);
            BackgroundTask.run(table, () -> { throw new IllegalStateException("Expected test failure"); },
                    ignored -> completed.completeExceptionally(new AssertionError("Expected failure")),
                    error -> completed.complete((StackPane) parent.getChildren().getFirst()));
        });
        StackPane host = completed.get(10, TimeUnit.SECONDS);
        onFx(() -> assertFalse(host.getChildren().get(1).isVisible()));
    }

    private static void onFx(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try { action.run(); done.complete(null); }
            catch (Throwable error) { done.completeExceptionally(error); }
        });
        done.get(10, TimeUnit.SECONDS);
    }
}
