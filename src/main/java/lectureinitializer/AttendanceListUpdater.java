package lectureinitializer;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class AttendanceListUpdater {

    public static void updateAttendanceList(
        final File attendanceList,
        final File teamsAttendeesExport
    ) throws IOException {
        final Set<String> actualAttendees =
            new LinkedHashSet<String>(
                Files.lines(teamsAttendeesExport.toPath(), StandardCharsets.UTF_16LE)
                .skip(1)
                .filter(line -> !line.isBlank())
                .map(line -> line.split("\t")[0].split("/")[0].trim())
                .toList()
            );
        final List<String> expectedAttendees =
            Files.lines(attendanceList.toPath()).filter(line -> !line.isBlank()).map(String::trim).toList();
        final List<String> result =
            expectedAttendees.stream()
            .map(name -> actualAttendees.contains(name) ? name : "//" + name)
            .collect(Collectors.toCollection(ArrayList::new));
        if (!expectedAttendees.containsAll(actualAttendees)) {
            result.add("");
            for (final String name : actualAttendees) {
                if (!expectedAttendees.contains(name)) {
                    result.add("//" + name);
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(attendanceList))) {
            for (final String line : result) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

}
