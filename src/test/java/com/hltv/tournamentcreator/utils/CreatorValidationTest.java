package com.hltv.tournamentcreator.utils;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CreatorValidationTest {
    @Test void teamLimitIsRequiredAndMatchesSupportedBracketRange() {
        assertEquals(2, CreatorValidation.teamLimit("2"));
        assertEquals(16, CreatorValidation.teamLimit(" 16 "));
        assertEquals(256, CreatorValidation.teamLimit("256"));
        for (String value : new String[]{null,"","1","0","-1","16.5","abc","257","2147483648"})
            assertThrows(IllegalArgumentException.class, () -> CreatorValidation.teamLimit(value));
    }
    @Test void namesAreTrimmedAndRequired() {
        assertEquals("Team", CreatorValidation.text("  Team  ", "Name", 100, true));
        assertThrows(IllegalArgumentException.class, () -> CreatorValidation.text("  ", "Name", 100, true));
        assertNull(CreatorValidation.text("  ", "Location", 100, false));
    }

    @Test void namesRespectDatabaseCharacterLimitsIncludingUnicode() {
        String name = "🏆".repeat(100);
        assertEquals(name, CreatorValidation.text(name, "Name", 100, true));
        assertThrows(IllegalArgumentException.class, () -> CreatorValidation.text(name + "x", "Name", 100, true));
    }

    @Test void rankingMustBePositiveAndFitAnInteger() {
        assertNull(CreatorValidation.ranking(""));
        assertEquals(1, CreatorValidation.ranking("1"));
        for (String value : new String[]{"0", "-1", "1.5", "abc", "2147483648"})
            assertThrows(IllegalArgumentException.class, () -> CreatorValidation.ranking(value));
    }

    @Test void prizePoolRespectsNumericTenTwo() {
        assertNull(CreatorValidation.prizePool(""));
        assertEquals(new BigDecimal("99999999.99"), CreatorValidation.prizePool("99999999.99"));
        assertEquals(BigDecimal.ZERO, CreatorValidation.prizePool("0"));
        for (String value : new String[]{"-1", "0.001", "100000000", "NaN"})
            assertThrows(IllegalArgumentException.class, () -> CreatorValidation.prizePool(value));
    }

    @Test void tournamentDatesMayBeOptionalOrEqualButCannotRunBackwards() {
        LocalDate date = LocalDate.of(2026, 11, 3);
        assertDoesNotThrow(() -> CreatorValidation.dates(null, null));
        assertDoesNotThrow(() -> CreatorValidation.dates(date, null));
        assertDoesNotThrow(() -> CreatorValidation.dates(null, date));
        assertDoesNotThrow(() -> CreatorValidation.dates(date, date));
        assertThrows(IllegalArgumentException.class, () -> CreatorValidation.dates(date, date.minusDays(1)));
    }
}
