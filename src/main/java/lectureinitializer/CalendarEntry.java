package lectureinitializer;

import java.time.*;
import java.time.format.*;
import java.util.*;

public record CalendarEntry(
    String title,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    Optional<LocalDateTime> reminderDateTime,
    String owner,
    String[] requiredParticipants,
    String[] optionalParticipants,
    String description,
    String place,
    CalendarEntryShowType showType
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d.M.yyyy-HH:mm:ss");

    public static CalendarEntry parse(final String entry) {
        final String[] fields = entry.split("\"?,\"?");
        if (fields.length != 22) {
            throw new IllegalArgumentException(
                String.format("Entry does not match expected format (22 vs. %d columns)!", fields.length)
            );
        }
        return new CalendarEntry(
            fields[0],
            LocalDateTime.parse(String.format("%s-%s", fields[1], fields[2]), CalendarEntry.FORMATTER),
            LocalDateTime.parse(String.format("%s-%s", fields[3], fields[4]), CalendarEntry.FORMATTER),
            "Ein".equals(fields[6]) ?
                Optional.of(
                    LocalDateTime.parse(String.format("%s-%s", fields[7], fields[8]), CalendarEntry.FORMATTER)
                ) :
                    Optional.empty(),
            fields[9],
            fields[10].split(";"),
            fields[11].split(";"),
            fields[14],
            fields[16],
            CalendarEntryShowType.parse(fields[21])
        );
    }

    @Override
    public String toString() {
        return String.format(
            "{title:\"%s\",start:\"%s\",end:\"%s\"%s,owner:\"%s\",requiredParticipants:[%s],optionalParticipants:[%s],description:\"%s\",place:\"%s\",showType:\"%s\"}",
            this.title(),
            this.startDateTime().format(CalendarEntry.FORMATTER),
            this.endDateTime().format(CalendarEntry.FORMATTER),
            this.reminderDateTime().isEmpty() ?
                "" :
                    String.format(",reminder:\"%s\"", this.reminderDateTime().get().format(CalendarEntry.FORMATTER)),
            this.owner(),
            String.join(";", this.requiredParticipants()),
            String.join(";", this.optionalParticipants()),
            this.description(),
            this.place(),
            this.showType().name()
        );
    }

}
