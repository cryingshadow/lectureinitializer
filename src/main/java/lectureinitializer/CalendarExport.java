package lectureinitializer;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

import ocp.*;

public class CalendarExport extends LinkedHashMap<String, List<OCEntry>>{

    private static final long serialVersionUID = 1L;

    public static void createClassFiles(final File participantsList, final File calendarExport) throws IOException {
        final Map<String, List<String>> participantsByEvent = new ParticipantsList(participantsList);
        final CalendarExport calendarEntriesByEvent = CalendarExport.parseCalendarExport(calendarExport);
        for (final Map.Entry<String, List<String>> participantsEntries : participantsByEvent.entrySet()) {
            final String lecture = participantsEntries.getKey();
            if (calendarEntriesByEvent.containsKey(lecture)) {
                final List<OCEntry> calendarEntries = calendarEntriesByEvent.get(lecture);
                final File classFile =
                    CalendarExport.computeClassFile(
                        lecture,
                        calendarEntries,
                        participantsList.toPath().toAbsolutePath().getParent()
                    );
                if (classFile.exists()) {
                    continue;
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(classFile))) {
                    writer.write(String.valueOf(participantsEntries.getValue().size()));
                    writer.write("\n");
                    for (final String participant : participantsEntries.getValue()) {
                        writer.write(participant);
                        writer.write("\n");
                    }
                    writer.write(String.valueOf(calendarEntries.size()));
                    writer.write("\n");
                    for (final OCEntry calendarEntry : calendarEntries) {
                        writer.write(CalendarExport.toClassFileLine(calendarEntry));
                        writer.write("\n");
                    }
                }
            }
        }
    }

    public static CalendarExport parseCalendarExport(final File calendarExport) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(calendarExport))) {
            return new CalendarExport(OCEntry.parseAndGroup(reader, LectureExtractor.INSTANCE));
        }
    }

    private static File computeClassFile(
        final String lecture,
        final List<OCEntry> calendarEntries,
        final Path lecturesPath
    ) {
        final String folder = CalendarExport.folderForLecture(lecture.substring(0, lecture.indexOf('|') - 1));
        final String classFileName = CalendarExport.toClassFileName(lecture, calendarEntries);
        return lecturesPath.resolve(folder).resolve("classes").resolve(classFileName).toFile();
    }

    private static String folderForLecture(final String lecture) {
        switch (lecture) {
        case "Advanced Software Engineering":
            return "AdvancedSoftwareEngineering";
        case "Aktuelle Trends in der Programmierung":
            return "AktuelleTrendsInDerProgrammierung";
        case "Algorithmen und Datenstrukturen":
            return "AlgorithmenUndDatenstrukturen";
        case "Geschäftsprozessmanagement":
            return "Geschäftsprozessmanagement";
        case "Grundlagen der Informatik":
            return "GrundlagenDerInformatik";
        case "Operations Research":
            return "OperationsResearch";
        case "Programmierung I":
            return "Programmierung_I";
        case "Programmierung II":
            return "Programmierung_II";
        case "Software Engineering Project":
            return "SoftwareEngineeringProject";
        case "Softwaremodeling & Architecture":
            return "SoftwareModelingAndArchitecture";
        case "Software Testing & DevOps":
            return "SoftwareTestingAndDevOps";
        case "Technologie-Trends":
            return "TechnologieTrends";
        case "Wirtschaftsmathematik":
            return "Wirtschaftsmathematik";
        default:
            throw new IllegalArgumentException(String.format("Lecture %s not found!", lecture));
        }
    }

    private static String toClassFileLine(final OCEntry calendarEntry) {
        final LocalDateTime start = calendarEntry.start();
        return String.format(
            "%02d%02d%02d%02d%02d%d",
            start.getYear() % 100,
            start.getMonthValue(),
            start.getDayOfMonth(),
            start.getHour(),
            start.getMinute(),
            Duration.between(start, calendarEntry.end()).toMinutes() / 45
        );
    }

    private static String toClassFileName(final String lecture, final List<OCEntry> calendarEntries) {
        final String[] groups = lecture.substring(lecture.indexOf('|') + 2).split(", ");
        final TreeSet<String> prefixes = new TreeSet<String>();
        final TreeSet<String> infixes = new TreeSet<String>();
        final TreeSet<Integer> years = new TreeSet<Integer>();
        final TreeSet<String> suffixes = new TreeSet<String>();
        for (final String group : groups) {
            prefixes.add(group.substring(0, 2).toLowerCase());
            infixes.add(group.substring(2, 4).toLowerCase());
            years.add(Integer.parseInt(group.substring(4, 7)));
            suffixes.add(group.substring(7).toLowerCase());
        }
        final StringBuilder result = new StringBuilder();
        result.append(CalendarExport.toQuarter(calendarEntries));
        for (final String prefix : prefixes) {
            result.append(prefix);
        }
        for (final String infix : infixes) {
            result.append(infix);
        }
        for (final Integer year : years) {
            result.append(String.valueOf(year));
        }
        for (final String suffix : suffixes) {
            result.append(suffix);
        }
        result.append(".txt");
        return result.toString();
    }

    private static String toQuarter(final List<OCEntry> calendarEntries) {
        final List<OCEntry> sorted = new ArrayList<OCEntry>(calendarEntries);
        Collections.sort(sorted);
        final OCEntry calendarEntry = sorted.getFirst();
        int year = calendarEntry.start().getYear() % 100;
        int quarter = calendarEntry.start().getMonthValue() / 3 + 1;
        if (quarter > 4) {
            year++;
            quarter -= 4;
        }
        return String.format("%02d%d", year, quarter);
    }

    private CalendarExport(final Map<String, List<OCEntry>> map) {
        super(map);
    }

}
