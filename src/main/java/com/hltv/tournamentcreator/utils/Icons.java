package com.hltv.tournamentcreator.utils;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

public final class Icons {
    private Icons() {}
    public static StackPane of(String name) {
        var view = FxmlViews.load("components/Icon.fxml");
        SVGPath path = view.get("path");
        path.setContent(switch (name) {
            case "grid" -> "M3 3H10V10H3Z M14 3H21V10H14Z M3 14H10V21H3Z M14 14H21V21H14Z";
            case "trophy" -> "M8 21H16 M12 17V21 M7 4H17V9A5 5 0 0 1 7 9Z M7 6H3V8A4 4 0 0 0 7 12 M17 6H21V8A4 4 0 0 1 17 12";
            case "users" -> "M16 21V19A4 4 0 0 0 12 15H5A4 4 0 0 0 1 19V21 M12.5 7A4 4 0 1 1 4.5 7A4 4 0 1 1 12.5 7 M17 3A4 4 0 0 1 17 11 M23 21V19A4 4 0 0 0 20 15.35";
            case "user" -> "M16 8A4 4 0 1 1 8 8A4 4 0 1 1 16 8 M4 21A8 8 0 0 1 20 21";
            case "chart" -> "M4 19V9 M10 19V5 M16 19V12 M22 19V2";
            case "trash" -> "M3 6H21 M9 6V3H15V6 M5 6L6 21H18L19 6 M10 10V17 M14 10V17";
            case "edit" -> "M15 4L20 9 M4 16L16 4A2 2 0 0 1 20 8L8 20L3 21Z";
            case "arrow-right" -> "M4 12H20 M14 6L20 12L14 18";
            default -> "M13 2L4 14H12L11 22L20 10H12Z";
        });
        return (StackPane) view.root();
    }
}
