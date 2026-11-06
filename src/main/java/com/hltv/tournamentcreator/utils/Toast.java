package com.hltv.tournamentcreator.utils;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.Locale;

public class Toast {
    public enum ToastType { SUCCESS, ERROR, WARNING, INFO }
    public static void show(Stage ownerStage, String message, ToastType type) {
        Popup popup = new Popup();
        Label label = (Label) FxmlViews.load("components/Toast.fxml").root();
        label.setText(message);
        label.getStyleClass().add("toast-" + type.name().toLowerCase(Locale.ROOT));
        popup.getContent().add(label);
        popup.show(ownerStage);
        popup.setX(ownerStage.getX() + ownerStage.getWidth() / 2 - label.getWidth() / 2);
        popup.setY(ownerStage.getY() + ownerStage.getHeight() - 100);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(2500));
        fadeOut.setOnFinished(event -> popup.hide());
        fadeOut.play();
    }
    public static void show(Stage ownerStage, String message) { show(ownerStage, message, ToastType.INFO); }
}
