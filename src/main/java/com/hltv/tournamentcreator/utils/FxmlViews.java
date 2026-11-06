package com.hltv.tournamentcreator.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/** Loads full views and small, reusable FXML templates. */
public final class FxmlViews {
    private FxmlViews() {}

    public record Fragment(Parent root, Map<String, Object> namespace, Object controller) {
        @SuppressWarnings("unchecked")
        public <T> T get(String id) { return (T) Objects.requireNonNull(namespace.get(id), "Missing fx:id: " + id); }
    }

    public static Fragment load(String name) { return load(name, null); }

    public static Fragment load(String name, Object controller) {
        var resource = Objects.requireNonNull(FxmlViews.class.getResource("/com/hltv/tournamentcreator/" + name), "Missing FXML: " + name);
        FXMLLoader loader = new FXMLLoader(resource);
        if (controller != null) loader.setController(controller);
        try { return new Fragment(loader.load(), loader.getNamespace(), loader.getController()); }
        catch (IOException error) { throw new IllegalStateException("Could not load " + name, error); }
    }
}
