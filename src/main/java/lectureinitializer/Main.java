package lectureinitializer;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import clit.*;
import ocp.*;

public class Main {

    private static final List<Set<Flag>> ALLOWED_COMBINATIONS =
        List.of(
            Set.of(Flag.CLASSFILE),
            Set.of(Flag.CLASSFILE, Flag.ASSIGNMENT),
            Set.of(Flag.PARTICIPANTS, Flag.EXPORT),
            Set.of(Flag.QUIZ, Flag.OUTPUT)
        );

    public static String escapeForLaTeX(final String text) {
        return text.replaceAll("\\\\", "\\\\textbackslash")
            .replaceAll("([&\\$%\\{\\}_#])", "\\\\$1")
            .replaceAll("~", "\\\\textasciitilde{}")
            .replaceAll("\\^", "\\\\textasciicircum{}")
            .replaceAll("\\\\textbackslash", "\\\\textbackslash{}")
            .replaceAll("([^\\\\])\"", "$1''")
            .replaceAll("^\"", "''");
    }

    public static void main(final String[] args)
    throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final CLITamer<Flag> tamer = new CLITamer<Flag>(Flag.class);
        if (args == null || args.length < 1) {
            System.out.println(tamer.getParameterDescriptions());
            System.out.println(Main.helpText());
            return;
        }
        final Parameters<Flag> options = tamer.parse(args);
        if (!Main.ALLOWED_COMBINATIONS.contains(options.keySet())) {
            System.out.println(tamer.getParameterDescriptions());
            System.out.println(Main.helpText());
            return;
        }
        if (options.containsKey(Flag.CLASSFILE)) {
            final File classFile = new File(options.get(Flag.CLASSFILE));
            if (options.containsKey(Flag.ASSIGNMENT)) {
                Main.prepareTalk(new File(options.get(Flag.ASSIGNMENT)), classFile);
            } else {
                Main.writeParticipantsLists(classFile);
            }
        } else if (options.containsKey(Flag.PARTICIPANTS)) {
            Main.createClassFiles(new File(options.get(Flag.PARTICIPANTS)), new File(options.get(Flag.EXPORT)));
        } else {
            Main.transformQuizFile(new File(options.get(Flag.QUIZ)), new File(options.get(Flag.OUTPUT)));
        }
    }

    static List<TalkAssignment> toAssignmentsWithDates(
        final List<TopicAssignment> topicAssignments,
        final List<LocalDateTime> dates
    ) {
        final Map<LocalDate, List<LocalDateTime>> datesByDate =
            dates.stream().collect(
                Collectors.toMap(
                    LocalDateTime::toLocalDate,
                    d -> List.of(d),
                    (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).toList()
                )
            );
        int availableDates = dates.size();
        final int neededDates = topicAssignments.size();
        final List<LocalDate> keys = new ArrayList<LocalDate>(datesByDate.keySet());
        Collections.sort(keys);
        for (final LocalDate key : keys) {
            final int datesAtDate = datesByDate.get(key).size();
            if (datesAtDate > availableDates - neededDates) {
                break;
            } else {
                availableDates -= datesAtDate;
                datesByDate.remove(key);
            }
        }
        final List<TalkAssignment> result = new LinkedList<TalkAssignment>();
        int i = 0;
        keys.retainAll(datesByDate.keySet());
        outer: for (final LocalDate key : keys) {
            for (final LocalDateTime date : datesByDate.get(key)) {
                result.add(new TalkAssignment(topicAssignments.get(i), date));
                i++;
                if (i == neededDates) {
                    break outer;
                }
            }
        }
        return result;
    }

    static Stream<LocalDateTime> toLocalDateTime(final String dateString) {
        final int multiplicity = Integer.parseInt(dateString.substring(dateString.length() - 1));
        final LocalDateTime start =
            LocalDate.of(
                2000 + Integer.parseInt(dateString.substring(0, 2)),
                Integer.parseInt(dateString.substring(2, 4)),
                Integer.parseInt(dateString.substring(4, 6))
            ).atTime(Integer.parseInt(dateString.substring(6, 8)), Integer.parseInt(dateString.substring(8, 10)));
        final List<LocalDateTime> result = new LinkedList<LocalDateTime>();
        int numOfBreaks = 0;
        for (int i = 0; i < multiplicity; i++) {
            if (i > 0 && i % 2 == 0) {
                numOfBreaks++;
            }
            result.add(start.plusMinutes(i * 45 + numOfBreaks * 15));
        }
        return result.stream();
    }

    private static File computeClassFile(
        final String lecture,
        final List<OCEntry> calendarEntries,
        final Path lecturesPath
    ) {
        final String folder = Main.folderForLecture(lecture.substring(0, lecture.indexOf('|') - 1));
        final String classFileName = Main.toClassFileName(lecture, calendarEntries);
        return lecturesPath.resolve(folder).resolve("classes").resolve(classFileName).toFile();
    }

    private static void createClassFiles(final File participantsList, final File calendarExport) throws IOException {
        final Map<String, List<String>> participantsByEvent = Main.parseParticipantsList(participantsList);
        final Map<String, List<OCEntry>> calendarEntriesByEvent = Main.parseCalendarExport(calendarExport);
        for (final Map.Entry<String, List<String>> participantsEntries : participantsByEvent.entrySet()) {
            final String lecture = participantsEntries.getKey();
            if (calendarEntriesByEvent.containsKey(lecture)) {
                final List<OCEntry> calendarEntries = calendarEntriesByEvent.get(lecture);
                final File classFile =
                    Main.computeClassFile(
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
                        writer.write(Main.toClassFileLine(calendarEntry));
                        writer.write("\n");
                    }
                }
            }
        }
    }

    private static String extractLecture(final String line, final BufferedReader reader) throws IOException {
        String lectureLine = line;
        while (lectureLine.chars().filter(i -> i == '|').count() != 2) {
            lectureLine += reader.readLine();
        }
        return lectureLine.substring(0, line.lastIndexOf('|') - 1);
    }

    private static String extractTopic(final String topicEntry) {
        if (topicEntry.contains(")")) {
            return topicEntry.substring(topicEntry.indexOf(')') + 2).strip();
        }
        return topicEntry.strip();
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
        default:
            throw new IllegalArgumentException(String.format("Lecture %s not found!", lecture));
        }
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

    private static String helpText() {
        return String.format(
            "Allowed combinations: %s",
            Main.ALLOWED_COMBINATIONS
            .stream()
            .map(set ->
                set
                .stream()
                .map(flag -> "-" + flag.shortName())
                .collect(Collectors.joining(" and "))
            ).collect(Collectors.joining(", "))
        );
    }

    private static Map<String, List<OCEntry>> parseCalendarExport(final File calendarExport) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(calendarExport))) {
            return OCEntry.parseAndGroup(reader, LectureExtractor.INSTANCE);
        }
    }

    private static Map<String, List<String>> parseParticipantsList(final File participantsList) throws IOException {
        final Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        try (BufferedReader reader = new BufferedReader(new FileReader(participantsList))) {
            String line = reader.readLine();
            String currentLecture = Main.extractLecture(line, reader);
            List<String> currentParticipants = new LinkedList<String>();
            line = reader.readLine();
            while (line != null) {
                if (line.isBlank() || !line.startsWith("Seite") && !line.matches("\\w\\w\\w\\w\\d\\d\\d\\w .+")) {
                    line = reader.readLine();
                    continue;
                }
                if (line.startsWith("Seite")) {
                    reader.readLine();
                    line = reader.readLine().substring(1);
                    if (!line.isBlank() && !currentLecture.equals(Main.extractLecture(line, reader))) {
                        result.put(currentLecture, currentParticipants);
                        currentLecture = Main.extractLecture(line, reader);
                        currentParticipants = new LinkedList<String>();
                    }
                } else {
                    currentParticipants.add(line.substring(9));
                }
                line = reader.readLine();
            }
            result.put(currentLecture, currentParticipants);
        }
        return result;
    }

    private static List<QuizQuestion> parseQuizQuestions(final Iterator<String> iterator) {
        final List<QuizQuestion> questions = new ArrayList<QuizQuestion>();
        int counter = 0;
        String question = null;
        String correctAnswer = null;
        List<String> wrongAnswers = new ArrayList<String>();
        while (iterator.hasNext()) {
            final String line = iterator.next();
            if (counter == 0) {
                question = Main.escapeForLaTeX(line.strip().replaceAll("^(Frage\\s*)?\\d+(\\.|\\)|:|\\s)+", ""));
            } else {
                final String stripped =
                    Main.escapeForLaTeX(line.strip().replaceAll("^(\\d|[abcdABCD])(\\.|\\)|:|\\s)+", ""));
                if (stripped.toLowerCase().endsWith("(richtig)")) {
                    correctAnswer = stripped.substring(0, stripped.length() - 9).strip();
                } else {
                    wrongAnswers.add(stripped);
                }
            }
            counter++;
            if (counter > 4) {
                Collections.shuffle(wrongAnswers);
                questions.add(new QuizQuestion(question, correctAnswer, wrongAnswers));
                wrongAnswers = new ArrayList<String>();
                counter = 0;
            }
        }
        Collections.shuffle(questions);
        return questions;
    }

    private static void prepareTalk(final File assignmentFile, final File classFile) throws IOException {
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
        final List<LocalDateTime> dates = Main.toDates(classFile);
        final List<TalkAssignment> assignments = Main.toAssignments(assignmentFile, dates);
        Main.writeBuildFile(protocols);
        try (
            BufferedWriter solutionsWriter =
                new BufferedWriter(new FileWriter(root.resolve("quizSolutions.csv").toFile()))
        ) {
            LocalDate current = LocalDate.MIN;
            int numOfTalksWithoutBreak = 0;
            for (final TalkAssignment assignment : assignments) {
                if (!assignment.date().toLocalDate().equals(current)) {
                    current = assignment.date().toLocalDate();
                    Main.writeDateLineToConsole(current);
                    numOfTalksWithoutBreak = 0;
                } else if (numOfTalksWithoutBreak > 1) {
                    Main.writeBreakLineToConsole(assignment);
                    numOfTalksWithoutBreak = 0;
                }
                Main.writeAnnouncementLineToConsole(assignment);
                numOfTalksWithoutBreak++;
                Main.writeLineToSolutionFile(solutionsWriter, assignment, metaFile, numberOfTopics);
                Main.writeProtocolFile(protocols, talkMode, subject, place, assignment);
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

    private static String toASCII(final String name) {
        return name
            .replaceAll("ä", "ae")
            .replaceAll("Ä", "Ae")
            .replaceAll("ö", "oe")
            .replaceAll("Ö", "Oe")
            .replaceAll("ü", "ue")
            .replaceAll("Ü", "Ue")
            .replaceAll("ß", "ss")
            .replaceAll("é", "e")
            .replaceAll("[^\\x00-\\x7F]", "");
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
                    new TopicAssignment(assignment[0].strip(), Main.extractTopic(assignment[1]))
                );
                line = reader.readLine();
            }
        }
        return Main.toAssignmentsWithDates(assignmentsWithoutDates, dates);
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
        result.append(Main.toQuarter(calendarEntries));
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

    private static List<LocalDateTime> toDates(final File classFile) throws IOException {
        return
            Arrays
            .stream(ParticipantsAndDates.fromFile(classFile).dates())
            .flatMap(Main::toLocalDateTime)
            .toList();
    }

    private static String toQuarter(final List<OCEntry> calendarEntries) {
        final List<OCEntry> sorted = new ArrayList<OCEntry>(calendarEntries);
        Collections.sort(sorted);
        final OCEntry calendarEntry = sorted.getFirst();
        final int year = calendarEntry.start().getYear() % 100;
        final int quarter =
            calendarEntry.start().getMonthValue() / 3 + (calendarEntry.start().getMonthValue() % 3 == 0 ? 0 : 1);
        return String.format("%02d%d", year, quarter);
    }

    private static void transformQuizFile(final File quiz, final File output) throws IOException {
        final List<String> lines = Files.lines(quiz.toPath()).filter(line -> !line.isBlank()).toList();
        final Iterator<String> iterator = lines.iterator();
        final String topic = iterator.next();
        final List<QuizQuestion> questions = Main.parseQuizQuestions(iterator);
        final Random random = new Random();
        final List<Character> correctAnswers = new LinkedList<Character>();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write("\\documentclass[12pt]{article}\n\n");
            writer.write("\\input{../../../../../../templates/mctests.tex}\n\n");
            writer.write("\\begin{document}\n\n");
            writer.write("\\newtest{");
            writer.write(topic);
            writer.write("}\n\n");
            for (final QuizQuestion question : questions) {
                writer.write("\\question{");
                writer.write(question.question());
                writer.write("}{%\n");
                int skip = random.nextInt(4);
                for (int i = 0; i < 3; i++) {
                    writer.write("\\item ");
//                    writer.write('A' + i + (skip < 0 ? 1 : 0));
//                    writer.write(") ");
                    if (skip == 0) {
                        correctAnswers.add((char)('a' + i));
                        writer.write(question.correctAnswer());
                        writer.write("\n\\item ");
//                        writer.write('A' + i + 1);
//                        writer.write(") ");
                    }
                    writer.write(question.wrongAnswers().get(i));
                    skip--;
                    writer.write("\n");
                }
                if (skip == 0) {
                    correctAnswers.add('d');
                    writer.write("\\item ");
//                    writer.write("D) ");
                    writer.write(question.correctAnswer());
                    writer.write("\n");
                }
                writer.write("}{}\n\n");
            }
            writer.write("% ");
            writer.write(correctAnswers.stream().map(String::valueOf).collect(Collectors.joining(";")));
            writer.write("\n\n\\end{document}\n\n");
        }
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
            Main.getSolution(assignment.topicAssignment().topic(), metaFile, numberOfTopics);
        solutionsWriter.write(solution.orElseGet(() -> ""));
        solutionsWriter.write("\n");
    }

    private static void writeParticipantsLists(final File classFile) throws IOException {
        final String classFileName = classFile.getName();
        final String classIdentifier = classFileName.substring(0, classFileName.length() - 4);
        final ParticipantsAndDates participantsAndDates = ParticipantsAndDates.fromFile(classFile);
        final Path root = classFile.getAbsoluteFile().toPath().getParent().resolve(classIdentifier);
        if (!root.toFile().mkdir()) {
            throw new IOException("Could not create directory " + root.toFile().getName() + "!");
        }
        for (final String date : participantsAndDates.dates()) {
            try (
                BufferedWriter writer =
                    new BufferedWriter(new FileWriter(root.resolve(date.substring(0, 6) + ".txt").toFile()))
            ) {
                for (final String participant : participantsAndDates.participants()) {
                    writer.write(participant);
                    writer.write("\n");
                }
            }
        }
        final File groups = root.resolve("groups.sh").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(groups))) {
            writer.write("#!/bin/bash\n\n");
            writer.write("if [[ $# -eq 2 ]]; then\n");
            writer.write("  java -jar ../../../../nameandgrouppicker.jar GROUPS -n $1.txt --count $2\n");
            writer.write(" else if [[ $# -eq 3 ]]; then\n");
            writer.write("  java -jar ../../../../nameandgrouppicker.jar GROUPS -n $1.txt --min $2 --max $3\n");
            writer.write("else\n");
            writer.write("  echo \"Illegal number of parameters\" >&2\n");
            writer.write("  exit 2\n");
            writer.write("fi\n");
            writer.write("fi\n");
        }
        groups.setExecutable(true);
        final File pick = root.resolve("pick.sh").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pick))) {
            writer.write("#!/bin/bash\n\n");
            writer.write("java -jar ../../../../nameandgrouppicker.jar PICK -n $1.txt -f frequencies.txt\n");
        }
        pick.setExecutable(true);
        final File metaFile = root.getParent().resolve("meta.txt").toFile();
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            reader.readLine();
            reader.readLine();
            final String thirdLine = reader.readLine();
            if (thirdLine != null && !thirdLine.isBlank() && !"EXAM".equals(thirdLine)) {
                final int numOfTopics = Integer.parseInt(reader.readLine());
                final String[] topics = new String[numOfTopics];
                for (int i = 0; i < numOfTopics; i++) {
                    topics[i] = reader.readLine().split(";")[0];
                }
                try (
                    BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve("preferences.txt").toFile()))
                ) {
                    writer.write(String.valueOf(numOfTopics));
                    writer.write("\n");
                    for (final String topic : topics) {
                        writer.write(topic);
                        writer.write("\n");
                    }
                    writer.write(String.valueOf(participantsAndDates.participants().length));
                    writer.write("\n");
                    for (final String participant : participantsAndDates.participants()) {
                        writer.write(participant);
                        writer.write(";\n");
                    }
                }
            }
        }
    }

    private static void writeProtocolFile(
        final Path protocols,
        final TalkMode talkMode,
        final Subject subject,
        final String place,
        final TalkAssignment assignment
    ) throws IOException {
        final String[] nameParts = assignment.topicAssignment().participant().split(" ");
        final String lastName = nameParts[nameParts.length - 1];
        final String protocol = String.format("protokoll%s%s.tex", subject.shortName(), Main.toASCII(lastName));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(protocols.resolve(protocol).toFile()))) {
            writer.write("\\documentclass{article}\n\n");
            writer.write("\\input{../../../../../../templates/protocol/packages.tex}\n");
            writer.write("\\newcommand{\\subject}{");
            writer.write(subject.name());
            writer.write("}\n");
            writer.write("\\newcommand{\\student}{");
            writer.write(assignment.topicAssignment().participant());
            writer.write("}\n");
            writer.write("\\newcommand{\\presentationtitle}{");
            writer.write(assignment.topicAssignment().topic());
            writer.write("}\n");
            writer.write("\\newcommand{\\presentationdate}{");
            writer.write(String.valueOf(assignment.date().getDayOfMonth()));
            writer.write(".\\ ");
            writer.write(assignment.date().getMonth().getDisplayName(TextStyle.FULL, Locale.GERMAN));
            writer.write(" ");
            writer.write(String.valueOf(assignment.date().getYear()));
            writer.write("}\n");
            writer.write("\\newcommand{\\presentationplace}{");
            writer.write(place);
            writer.write("}\n");
            if (talkMode == TalkMode.TALK80QUIZ20) {
                writer.write("\\setboolean{mandatoryhandout}{false}\n");
            }
            writer.write("\n");
            writer.write("\\newcommand{\\presentationContent}{%\n");
            writer.write("Der Vortrag behandelte das Thema \\presentationtitle.\\\\[2ex]\n");
            writer.write("\\notes{%\n");
            writer.write("\\item Start: \n");
            writer.write("\\item \n");
            writer.write("\\item Ende Vortrag: \n");
            writer.write("\\item Prüfer: ?\n");
            writer.write("\\item Ende Diskussion: \n");
            writer.write("}\n}\n\n");
            switch (talkMode) {
            case TALK80QUIZ20:
                writer.write("\\newcommand{\\presentationUnderstandability}{%\n");
                writer.write("\\understandingstructureviii{}\n");
                writer.write("\\understandinglogicviii{}\n");
                writer.write("\\understandingspeechviii{}\n");
                writer.write("\\understandingexamplesviii{}\n");
                writer.write("\\understandingvisualizationviii{}\n");
                writer.write("\\evaluationpartresult{40}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationDepth}{%\n");
                writer.write("\\contenttimeiv{}\n");
                writer.write("\\contentdepthiv{}\n");
                writer.write("\\contentbreadthiv{}\n");
                writer.write("\\contentcorrectnessiv{}\n");
                writer.write("\\contentquestionsiv{}\n");
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationApplication}{%\n");
                writer.write("\\applicationrelevanceviii{}\n");
                writer.write("\\applicationdemonstrationvi{}\n");
                writer.write("\\applicationusersvi{}\n");
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\quiz}{%\n");
                writer.write("\\quizcontentv{}\n");
                writer.write("\\quizdifficultyv{}\n");
                Main.writeQuizCommands(writer);
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\additionalEvaluation}{}\n\n");
                break;
            case TALK40QUIZ10:
            case TALK40QUIZ20:
                writer.write("\\newcommand{\\presentationUnderstandability}{%\n");
                writer.write("\\understandingstructureiv{}\n");
                writer.write("\\understandinglogiciv{}\n");
                writer.write("\\understandingspeechiv{}\n");
                writer.write("\\understandingexamplesiv{}\n");
                writer.write("\\understandingvisualizationiv{}\n");
                writer.write("\\evaluationpartresult{20}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationDepth}{%\n");
                writer.write("\\contenttimeii{}\n");
                writer.write("\\contentdepthii{}\n");
                writer.write("\\contentbreadthii{}\n");
                writer.write("\\contentcorrectnessii{}\n");
                writer.write("\\contentquestionsii{}\n");
                writer.write("\\evaluationpartresult{10}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationApplication}{%\n");
                writer.write("\\applicationrelevanceiv{}\n");
                writer.write("\\applicationdemonstrationiii{}\n");
                writer.write("\\applicationusersiii{}\n");
                writer.write("\\evaluationpartresult{10}\n");
                writer.write("}\n\n");
                if (talkMode == TalkMode.TALK40QUIZ10) {
                    writer.write("\\newcommand{\\handout}{%\n");
                    writer.write("\\handoutdefault{}\n");
                    writer.write("\\handoutamountiii{}\n");
                    writer.write("\\handoutqualityiv{}\n");
                    writer.write("\\handoutformaliii{}\n");
                    writer.write("\\evaluationpartresult{10}\n");
                    writer.write("}\n\n");
                    writer.write("\\newcommand{\\quiz}{%\n");
                    Main.writeQuizCommands(writer);
                    writer.write("\\evaluationpartresult{10}\n");
                } else {
                    writer.write("\\newcommand{\\quiz}{%\n");
                    writer.write("\\quizcontentv{}\n");
                    writer.write("\\quizdifficultyv{}\n");
                    Main.writeQuizCommands(writer);
                    writer.write("\\evaluationpartresult{20}\n");
                }
                writer.write("}\n\n");
                writer.write("\\newcommand{\\additionalEvaluation}{%\n");
                writer.write("\\contributions\n");
                writer.write("Die individuellen Beiträge umfassten:\n");
                writer.write("\\begin{itemize}\n");
                writer.write("\\item \\contributionvalue{0}\n");
                writer.write("\\end{itemize}%\n");
                writer.write("\\evaluationpartresult{40}\n");
                writer.write("}\n\n");
                break;
            case TALK50SCIENCE:
                writer.write("\\newcommand{\\presentationUnderstandability}{%\n");
                writer.write("\\understandingstructurev{}\n");
                writer.write("\\understandinglogicv{}\n");
                writer.write("\\understandingspeechv{}\n");
                writer.write("\\understandingexamplesv{}\n");
                writer.write("\\understandingvisualizationv{}\n");
                writer.write("\\evaluationpartresult{25}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationDepth}{%\n");
                writer.write("\\contenttimeiii{}\n");
                writer.write("\\contentdepthiii{}\n");
                writer.write("\\contentbreadthiii{}\n");
                writer.write("\\contentcorrectnessiii{}\n");
                writer.write("\\contentquestionsiii{}\n");
                writer.write("\\evaluationpartresult{15}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\presentationApplication}{%\n");
                writer.write("\\applicationrelevanceiv{}\n");
                writer.write("\\applicationdemonstrationiii{}\n");
                writer.write("\\applicationusersiii{}\n");
                writer.write("\\evaluationpartresult{10}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\paperEvaluation}{%\n");
                writer.write("\\goali{}\n");
                writer.write("\\contributionsi{}\n");
                writer.write("\\structurequalityi{}\n");
                writer.write("\\basicsmatchingi{}\n");
                writer.write("\\conclusioni{}\n\n");
                writer.write("\\literatureamounti{}\n");
                writer.write("\\literaturequalityii{}\n");
                writer.write("\\relatedamounti{}\n");
                writer.write("\\relatedqualityii{}\n");
                writer.write("\\quotingdensityii{}\n");
                writer.write("\\methodapplicationii{}\n");
                writer.write("\\methodintroi{}\n");
                writer.write("\\objectivityi{}\n");
                writer.write("\\reliabilityi{}\n");
                writer.write("\\validityi{}\n");
                writer.write("\\comprehensibilityi{}\n\n");
                writer.write("Folgende inhaltliche Beiträge wurden für die Arbeit ausgewählt:\n");
                writer.write("\\begin{itemize}\n");
                writer.write("\\item Beitrag 1\n");
                writer.write("\\end{itemize}\n");
                writer.write("\\innovativenessii{}\n");
                writer.write("\\relevanceii{}\n");
                writer.write("\\levelii{}\n");
                writer.write("\\applicabilityii{}\n");
                writer.write("\\valueii{}\n\n");
                writer.write("\\appearancei{}\n");
                writer.write("\\spellingautoi{}\n");
                writer.write("\\languagei{}\n");
                writer.write("\\figuresi{}\n");
                writer.write("\\literaturestylei{}\n\n");
                writer.write("\\evaluationpartresult{35}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\reviewi}{% max 5\n");
                writer.write("\\addevaluationpart{0}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\reviewii}{% max 5\n");
                writer.write("\\addevaluationpart{0}\n");
                writer.write("}\n\n");
                writer.write("\\newcommand{\\reviewiii}{% max 5\n");
                writer.write("\\addevaluationpart{0}\n");
                writer.write("}\n\n");
                break;
            }
            writer.write("\\newcommand{\\totalReview}{%\n");
            writer.write(
                "Insgesamt wurden \\evaluationpoints{} Punkte erreicht und das Gesamturteil lautet: \\grade\n"
            );
            writer.write("}\n\n");
            if (talkMode == TalkMode.TALK50SCIENCE) {
                writer.write("\\input{../../../../../../templates/protocol/protocolScience.tex}\n");
            } else {
                writer.write("\\input{../../../../../../templates/protocol/protocol.tex}\n");
            }
        }
    }

    private static void writeQuizCommands(final BufferedWriter writer) throws IOException {
        writer.write("\\quizpassed{}\n");
        writer.write("\\quizbonusi{}\n");
        writer.write("\\quizbonusii{}\n");
        writer.write("\\quizbonusiii{}\n");
        writer.write("\\quizparticipantbonusi{}\n");
        writer.write("\\quizparticipantbonusii{}\n");
        writer.write("\\quizparticipantbonusiii{}\n");
    }

}
