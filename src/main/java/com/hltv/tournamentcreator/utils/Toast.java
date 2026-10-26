package com.hltv.tournamentcreator.utils;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Toast {

    public enum ToastType {
        SUCCESS, ERROR, WARNING, INFO
    }

    public static void show(Stage ownerStage, String message, ToastType type) {
        Popup popup = new Popup();

        String backgroundColor;
        String textColor = "white";

        switch (type) {
            case SUCCESS -> backgroundColor = "#4CAF50";
            case ERROR -> backgroundColor = "#F44336";
            case WARNING -> backgroundColor = "#FF9800";
            default -> backgroundColor = "rgba(40, 40, 40, 0.9)";
        }

        // Create the toast UI
        Label label = new Label(message);
        label.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: %s;
            -fx-padding: 10px 20px;
            -fx-background-radius: 5px;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 5);
        """, backgroundColor, textColor));

        popup.getContent().add(label);
        popup.show(ownerStage);

        double windowX = ownerStage.getX();
        double windowY = ownerStage.getY();
        double windowWidth = ownerStage.getWidth();
        double windowHeight = ownerStage.getHeight();

        popup.setX(windowX + (windowWidth / 2) - (label.getWidth() / 2));
        popup.setY(windowY + windowHeight - 100);

        // Create the fade-out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(2500));
        fadeOut.setOnFinished(e -> popup.hide());
        fadeOut.play();
    }

    public static void show(Stage ownerStage, String message) {
        show(ownerStage, message, ToastType.INFO);
    }
}