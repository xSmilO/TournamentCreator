module com.example.tournamentcreator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires io.github.cdimascio.dotenv.java;
    requires java.sql;

    opens com.hltv.tournamentcreator to javafx.fxml;
    exports com.hltv.tournamentcreator;
}