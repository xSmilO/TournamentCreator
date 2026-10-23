package com.hltv.tournamentcreator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.printAllTeams();

        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("MainScreen-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Tournament Creator");
        stage.setScene(scene);
        stage.show();
    }
}
