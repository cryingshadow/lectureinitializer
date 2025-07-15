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
                TalkAssignment.writeProtocolFile(protocols, talkMode, subject, place, assignment);
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

    private static void writeQuizCommands(final BufferedWriter writer) throws IOException {
        writer.write("\\quizpassed{}\n");
        writer.write("\\quizbonusi{}\n");
        writer.write("\\quizbonusii{}\n");
        writer.write("\\quizbonusiii{}\n");
        writer.write("\\quizparticipantbonusi{}\n");
        writer.write("\\quizparticipantbonusii{}\n");
        writer.write("\\quizparticipantbonusiii{}\n");
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
        final String protocol = String.format("protokoll%s%s.tex", subject.shortName(), TalkAssignment.toASCII(lastName));
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
                TalkAssignment.writeQuizCommands(writer);
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
                    TalkAssignment.writeQuizCommands(writer);
                    writer.write("\\evaluationpartresult{10}\n");
                } else {
                    writer.write("\\newcommand{\\quiz}{%\n");
                    writer.write("\\quizcontentv{}\n");
                    writer.write("\\quizdifficultyv{}\n");
                    TalkAssignment.writeQuizCommands(writer);
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

}
