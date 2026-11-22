package com.hltv.tournamentcreator.utils;

import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public final class BackgroundTask {
    private BackgroundTask() {}

    @FunctionalInterface
    public interface Work<T> { T run() throws Exception; }

    public static <T> void run(javafx.scene.Node content, Work<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Runnable hide = LoadingOverlay.show(content);
        run(work, value -> {
            try { success.accept(value); } finally { hide.run(); }
        }, error -> {
            try { failure.accept(error); } finally { hide.run(); }
        });
    }

    public static <T> void run(Work<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        CompletableFuture.supplyAsync(() -> {
            try { return work.run(); }
            catch (Exception e) { throw new CompletionException(e); }
        }).whenComplete((value, error) -> Platform.runLater(() -> {
            if (error == null) success.accept(value);
            else {
                Throwable cause = error instanceof CompletionException && error.getCause() != null
                        ? error.getCause() : error;
                cause.printStackTrace();
                failure.accept(cause);
            }
        }));
    }
}
