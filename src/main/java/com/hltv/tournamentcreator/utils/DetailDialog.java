package com.hltv.tournamentcreator.utils;

import javafx.scene.control.*;
import java.util.List;

public final class DetailDialog {
    private DetailDialog() {}
    public static void show(String title, String caption, BackgroundTask.Work<List<String>> fetch) {
        var view = FxmlViews.load("DetailModal.fxml");
        view.<Label>get("caption").setText(caption);
        view.<Label>get("heading").setText(title);
        ListView<String> list = view.get("list");
        Label placeholder = view.get("placeholder");
        view.<Button>get("close").setOnAction(event -> ModalManager.getInstance().hideModal());
        ModalManager.getInstance().showModal(view.root());
        BackgroundTask.run(fetch, rows -> {
            list.getItems().setAll(rows);
            placeholder.setText("No entries yet.");
        }, error -> placeholder.setText("Could not load entries. Close and try again."));
    }
}
