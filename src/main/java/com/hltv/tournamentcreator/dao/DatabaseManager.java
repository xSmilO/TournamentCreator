package com.hltv.tournamentcreator.dao;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Function;

public class DatabaseManager {

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        try {
            // Environment variables also work when no local .env file is present.
            String configDirectory = System.getProperty("tournamentcreator.configDir", ".");
            Dotenv dotenv = Dotenv.configure().directory(configDirectory).ignoreIfMissing().load();
            return openConnection(dotenv::get);
        } catch (io.github.cdimascio.dotenv.DotenvException error) {
            throw new SQLException("Could not read database configuration. Check the .env file.", "08001", error);
        }
    }

    private static Connection openConnection(Function<String, String> settings) throws SQLException {
        Properties properties = connectionProperties(settings);
        String url = settings.apply("DB_URL").trim();
        if (!url.startsWith("jdbc:postgresql:")) {
            throw new SQLException("DB_URL must be a PostgreSQL JDBC URL (jdbc:postgresql://host:5432/database).", "08001");
        }
        return DriverManager.getConnection(url, properties);
    }

    static Properties connectionProperties(Function<String, String> settings) throws SQLException {
        for (String key : new String[] {"DB_URL", "DB_USER", "DB_PASS"}) {
            String value = settings.apply(key);
            if (value == null || value.isBlank()) {
                throw new SQLException("Missing database setting " + key + ". Set it in the environment or .env file.", "08001");
            }
        }
        Properties properties = new Properties();
        properties.setProperty("user", settings.apply("DB_USER").trim());
        properties.setProperty("password", settings.apply("DB_PASS"));
        properties.setProperty("connectTimeout", "10");
        properties.setProperty("socketTimeout", "60");
        return properties;
    }
}
