package util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Custom Deserializer for ZonedDateTime that attempts to parse both
 * ISO_OFFSET_DATE_TIME and flexible ISO_LOCAL_DATE_TIME formats.
 * Assumes system default timezone if parsing as LocalDateTime.
 */
public class LenientZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {

    // Formatter for standard ZonedDateTime (e.g., 2025-10-26T19:47:49.000+00:00)
    private static final DateTimeFormatter ZONED_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // Custom flexible formatter for LocalDateTime (e.g., 2025-10-26T23:27:30.7089766)
    // Accepts 0-9 nano digits
    private static final DateTimeFormatter FLEXIBLE_LOCAL_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE) // yyyy-MM-dd
            .appendLiteral('T')                      // T
            .append(DateTimeFormatter.ISO_LOCAL_TIME) // HH:mm:ss[.SSSSSSSSS]
            .optionalStart()                         // Optional nano part start
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true) // 0-9 nano digits, requires decimal point
            .optionalEnd()                           // Optional nano part end
            .toFormatter();

    // Standard local date time formatter as a fallback
    private static final DateTimeFormatter STANDARD_LOCAL_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;


    @Override
    public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        // 1. Try parsing as standard ZonedDateTime first
        try {
            return ZonedDateTime.parse(dateString, ZONED_FORMATTER);
        } catch (DateTimeParseException e1) {
            // Ignore and try next format
        }

        // 2. Try parsing as flexible LocalDateTime
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateString, FLEXIBLE_LOCAL_FORMATTER);
            return ldt.atZone(ZoneId.systemDefault()); // Assume system timezone
        } catch (DateTimeParseException e2) {
            // Ignore and try next format
        }

        // 3. Try parsing as standard LocalDateTime as a final attempt
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateString, STANDARD_LOCAL_FORMATTER);
            return ldt.atZone(ZoneId.systemDefault()); // Assume system timezone
        } catch (DateTimeParseException e3) {
            // If all attempts fail, log error and return null (more resilient than throwing exception)
            System.err.println("Failed to parse timestamp flexibly: '" + dateString + "'. Tried ZonedDateTime and multiple LocalDateTime formats. Error: " + e3.getMessage());
            // You could throw IOException here if strict parsing is required:
            // throw new IOException("Cannot parse date/time string: " + dateString, e3);
            return null; // Return null on failure
        }
    }
}