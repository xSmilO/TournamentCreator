package com.hltv.tournamentcreator.dao;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    private Map<String, String> settings() {
        return new HashMap<>(Map.of("DB_URL", "jdbc:postgresql://localhost/test",
                "DB_USER", " app ", "DB_PASS", " password with spaces "));
    }

    @Test void missingSettingsProduceActionableSqlErrorsWithoutLeakingCredentials() {
        for (String key : new String[] {"DB_URL", "DB_USER", "DB_PASS"}) {
            var values = settings();
            values.remove(key);
            SQLException error = assertThrows(SQLException.class,
                    () -> DatabaseManager.connectionProperties(values::get));
            assertEquals("08001", error.getSQLState());
            assertTrue(error.getMessage().contains(key));
            assertFalse(error.getMessage().contains("password with spaces"));
        }
    }

    @Test void blankSettingsAreRejected() {
        var values = settings();
        values.put("DB_URL", "  ");
        assertThrows(SQLException.class, () -> DatabaseManager.connectionProperties(values::get));
    }

    @Test void credentialsPreservePasswordWhitespaceAndConnectionsHaveTimeouts() throws Exception {
        var properties = DatabaseManager.connectionProperties(settings()::get);
        assertEquals("app", properties.getProperty("user"));
        assertEquals(" password with spaces ", properties.getProperty("password"));
        assertEquals("10", properties.getProperty("connectTimeout"));
        assertEquals("60", properties.getProperty("socketTimeout"));
    }
}
