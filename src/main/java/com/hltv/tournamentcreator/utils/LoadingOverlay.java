package com.hltv.tournamentcreator.utils;

import javafx.geometry.Pos;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A local loading mask; the rest of the application remains usable. */
final class LoadingOverlay {
    private static final Object KEY = new Object();
    private final StackPane mask;
    private final RotateTransition rotation;
    private int pending;

    private LoadingOverlay(Node content) {
        if (!(content.getParent() instanceof Pane parent))
            throw new IllegalArgumentException("Loading content must be inside a Pane");
        int index = parent.getChildren().indexOf(content);
        StackPane host = new StackPane();
        host.setMinSize(0, 0);
        // Keep the original parent's grow, margin and grid constraints.
        host.getProperties().putAll(content.getProperties());
        parent.getChildren().set(index, host);
        Circle track = new Circle(24, 24, 20, Color.TRANSPARENT);
        track.setStroke(Color.web("#39434e"));
        track.setStrokeWidth(4);
        Arc arc = new Arc(24, 24, 20, 20, 90, 100);
        arc.setType(ArcType.OPEN);
        arc.setFill(Color.TRANSPARENT);
        arc.setStyle("-fx-stroke: -fx-accent;");
        arc.setStrokeWidth(4);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        Pane spinner = new Pane(track, arc);
        spinner.setMinSize(48, 48);
        spinner.setPrefSize(48, 48);
        spinner.setMaxSize(48, 48);
        spinner.setAccessibleText("Loading data");
        rotation = new RotateTransition(Duration.millis(850), spinner);
        rotation.setByAngle(360);
        rotation.setCycleCount(Animation.INDEFINITE);
        rotation.setInterpolator(Interpolator.LINEAR);
        Label caption = new Label("Loading…");
        caption.setStyle("-fx-text-fill: #f4f6f8; -fx-font-size: 15px; -fx-font-weight: bold;");
        VBox badge = new VBox(12, spinner, caption);
        badge.setAlignment(Pos.CENTER);
        badge.setMaxSize(180, 120);
        badge.setStyle("-fx-background-color: #20252d; -fx-background-radius: 14; -fx-padding: 20;");
        mask = new StackPane(badge);
        mask.setStyle("-fx-background-color: rgba(16, 19, 24, 0.72); -fx-background-radius: 8;");
        mask.setPickOnBounds(true);
        mask.sceneProperty().addListener((obs, old, scene) -> updateAnimation());
        mask.visibleProperty().addListener((obs, old, visible) -> updateAnimation());
        host.getChildren().addAll(content, mask);
        // Prevent mouse/keyboard actions on stale data while preserving disable bindings.
        content.addEventFilter(javafx.scene.input.InputEvent.ANY, event -> {
            if (pending > 0) event.consume();
        });
    }

    private void updateAnimation() {
        if (pending > 0 && mask.isVisible() && mask.getScene() != null) rotation.play();
        else rotation.stop();
    }

    static Runnable show(Node content) {
        LoadingOverlay overlay = (LoadingOverlay) content.getProperties().get(KEY);
        if (overlay == null) {
            overlay = new LoadingOverlay(content);
            content.getProperties().put(KEY, overlay);
        }
        overlay.pending++;
        overlay.mask.setManaged(true);
        overlay.mask.setVisible(true);
        overlay.updateAnimation();
        LoadingOverlay current = overlay;
        return () -> {
            if (--current.pending == 0) {
                current.mask.setVisible(false);
                current.mask.setManaged(false);
            }
        };
    }
}
