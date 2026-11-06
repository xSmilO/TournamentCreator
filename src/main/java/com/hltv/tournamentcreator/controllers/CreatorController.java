package com.hltv.tournamentcreator.controllers;

import com.hltv.tournamentcreator.utils.BackgroundTask;
import com.hltv.tournamentcreator.utils.FxmlViews;
import com.hltv.tournamentcreator.utils.ModalManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public abstract class CreatorController {
    @FXML private VBox creatorRoot, choices;
    @FXML private Label status, choicesStatus;
    @FXML private Button save, retry;
    private Runnable reloadChoices;

    protected abstract BackgroundTask.Work<Void> validatedSave();

    protected <T> Set<T> loadChoices(BackgroundTask.Work<List<T>> fetch, Function<T, String> display) {
        Set<T> selected = new LinkedHashSet<>();
        reloadChoices = () -> {
            save.setDisable(true);
            retry.setVisible(false);
            retry.setManaged(false);
            choicesStatus.setText("Loading…");
            BackgroundTask.run(fetch, values -> {
                selected.clear();
                choices.getChildren().clear();
                for (T value : values) {
                    CheckBox check = (CheckBox) FxmlViews.load("components/ChoiceItem.fxml").root();
                    check.setText(display.apply(value));
                    check.setOnAction(event -> {
                        if (check.isSelected()) selected.add(value);
                        else selected.remove(value);
                    });
                    choices.getChildren().add(check);
                }
                choicesStatus.setText(values.isEmpty() ? "None available. You can create this entry without selections." : "");
                save.setDisable(false);
            }, error -> {
                choicesStatus.setText("Could not load choices. Check the database connection and retry.");
                retry.setVisible(true);
                retry.setManaged(true);
            });
        };
        reloadChoices.run();
        return selected;
    }

    @FXML private void retryChoices() { reloadChoices.run(); }
    @FXML private void handleCancel() { ModalManager.getInstance().hideModal(); }
    @FXML private void handleSave() {
        final BackgroundTask.Work<Void> work;
        try { work = validatedSave(); }
        catch (IllegalArgumentException error) { status.setText(error.getMessage()); return; }
        creatorRoot.setDisable(true);
        status.setText("Saving…");
        BackgroundTask.run(work, ignored -> ModalManager.getInstance().saved(), error -> {
            creatorRoot.setDisable(false);
            String message = "Could not save. Check the database connection and try again.";
            if (error instanceof java.sql.SQLException sql) {
                if ("23503".equals(sql.getSQLState())) message = "A selection no longer exists. Reopen the creator to refresh choices.";
                if (sql.getMessage() != null && sql.getMessage().startsWith("A selected player")) message = sql.getMessage();
            }
            status.setText(message);
        });
    }
}
