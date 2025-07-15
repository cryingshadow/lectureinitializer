package lectureinitializer;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public record TalkAssignment(TopicAssignment topicAssignment, LocalDateTime date) {

    private static List<LocalDateTime> toDates(final File classFile) throws IOException {
        return
            Arrays
            .stream(ParticipantsAndDates.fromFile(classFile).dates())
            .flatMap(Main::toLocalDateTime)
            .toList();
    }

    private static String extractTopic(final String topicEntry) {
        if (topicEntry.contains(")")) {
            return topicEntry.substring(topicEntry.indexOf(')') + 2).strip();
        }
        return topicEntry.strip();
    }

    public static void prepareTalk(final File assignmentFile, final File classFile) throws IOException {
        final Path root = assignmentFile.getAbsoluteFile().toPath().getParent();
        final Path protocols = root.resolve("protocols");
        protocols.toFile().mkdir();
        final Path quizAnswers = root.resolve("quizAnswers");
        quizAnswers.toFile().mkdir();
        final File metaFile = root.getParent().resolve("meta.txt").toFile();
        final Subject subject = Subject.fromFile(metaFile);
        final TalkMode talkMode = TalkMode.fromFile(metaFile);
        final int numberOfTopics = Integer.parseInt(Files.lines(metaFile.toPath()).skip(3).findFirst().get());
        final String place =
            root.toFile().getName().substring(3).toLowerCase().startsWith("m") ? "Mettmann" : "Bergisch Gladbach";
        final List<LocalDateTime> dates = TalkAssignment.toDates(classFile);
        final List<TalkAssignment> assignments = TalkAssignment.toAssignments(assignmentFile, dates);
        TalkAssignment.writeBuildFile(protocols);
        try (
            BufferedWriter solutionsWriter =
                new BufferedWriter(new FileWriter(root.resolve("quizSolutions.csv").toFile()))
        ) {
            LocalDate current = LocalDate.MIN;
            int numOfTalksWithoutBreak = 0;
            for (final TalkAssignment assignment : assignments) {
                if (!assignment.date().toLocalDate().equals(current)) {
                    current = assignment.date().toLocalDate();
                    TalkAssignment.writeDateLineToConsole(current);
                    numOfTalksWithoutBreak = 0;
                } else if (numOfTalksWithoutBreak > 1) {
                    TalkAssignment.writeBreakLineToConsole(assignment);
                    numOfTalksWithoutBreak = 0;
                }
                TalkAssignment.writeAnnouncementLineToConsole(assignment);
                numOfTalksWithoutBreak++;
                TalkAssignment.writeLineToSolutionFile(solutionsWriter, assignment, metaFile, numberOfTopics);
                ProtocolFileWriter.writeProtocolFile(protocols, talkMode, subject, place, assignment);
                final String[] nameParts = assignment.topicAssignment().participant().split(" ");
                final String lastName = nameParts[nameParts.length - 1].toLowerCase();
                try (
                    BufferedWriter quizAnswersWriter =
                        new BufferedWriter(new FileWriter(quizAnswers.resolve(lastName + ".csv").toFile()))
                ) {
                    quizAnswersWriter.write(assignment.topicAssignment().participant());
                    quizAnswersWriter.write("\n");
                    for (final TalkAssignment assignment2 : assignments) {
                        if (assignment2.equals(assignment)) {
                            continue;
                        }
                        quizAnswersWriter.write(assignment2.topicAssignment().participant());
                        quizAnswersWriter.write(";\n");
                    }
                }
            }
        }
    }

    private static List<TalkAssignment> toAssignments(final File file, final List<LocalDateTime> dates) throws IOException {
        final List<TopicAssignment> assignmentsWithoutDates = new ArrayList<TopicAssignment>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null && !line.isBlank()) {
                final String[] assignment = line.split("->");
                if (assignment.length != 2) {
                    throw new IOException("File must contain assignments of the form participant -> topic!");
                }
                assignmentsWithoutDates.add(
                    new TopicAssignment(assignment[0].strip(), TalkAssignment.extractTopic(assignment[1]))
                );
                line = reader.readLine();
            }
        }
        return Main.toAssignmentsWithDates(assignmentsWithoutDates, dates);
    }

    private static void writeAnnouncementLineToConsole(final TalkAssignment assignment) {
        System.out.println(
            String.format(
                "%s-%s: %s (%s)",
                assignment.date().format(DateTimeFormatter.ofPattern("HH:mm")),
                assignment.date().plusMinutes(45).format(DateTimeFormatter.ofPattern("HH:mm")),
                assignment.topicAssignment().topic(),
                assignment.topicAssignment().participant()
            )
        );
    }

    private static void writeBreakLineToConsole(final TalkAssignment assignment) {
        System.out.println(
            String.format(
                "%s-%s: Pause",
                assignment.date().minusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm")),
                assignment.date().format(DateTimeFormatter.ofPattern("HH:mm"))
            )
        );
    }

    private static void writeBuildFile(final Path protocols) throws IOException {
        final File build = protocols.resolve("build.sh").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(build))) {
            writer.write("#!/bin/bash\n\n");
            writer.write("for i in *.tex; do\n");
            writer.write("    pdflatex \"$i\"\n");
            writer.write("    pdflatex \"$i\"\n");
            writer.write("done\n");
        }
        build.setExecutable(true);
    }

    private static void writeDateLineToConsole(final LocalDate date) {
        System.out.println();
        System.out.println(String.format("%s:", date.format(DateTimeFormatter.ofPattern("dd.MM.uuuu"))));
    }

    private static void writeLineToSolutionFile(
        final BufferedWriter solutionsWriter,
        final TalkAssignment assignment,
        final File metaFile,
        final int numberOfTopics
    ) throws IOException {
        solutionsWriter.write(assignment.topicAssignment().participant());
        solutionsWriter.write(";");
        final Optional<String> solution =
            TalkAssignment.getSolution(assignment.topicAssignment().topic(), metaFile, numberOfTopics);
        solutionsWriter.write(solution.orElseGet(() -> ""));
        solutionsWriter.write("\n");
    }

    private static Optional<String> getSolution(
        final String topic,
        final File metaFile,
        final int numberOfTopics
    ) throws IOException {
        final Path path = metaFile.toPath();
        final Optional<Path> solutionFile =
            Files
            .lines(path)
            .skip(4)
            .limit(numberOfTopics)
            .map(line -> line.split(";"))
            .filter(split -> split.length == 2)
            .filter(split -> topic.equals(split[0]))
            .map(split -> path.getParent().resolve(split[1]))
            .findAny();
        return solutionFile.isEmpty() ?
            Optional.empty() :
                Optional.of(Files.lines(solutionFile.get()).findFirst().get().substring(1));
    }

}
