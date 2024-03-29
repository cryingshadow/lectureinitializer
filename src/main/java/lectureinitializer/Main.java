package lectureinitializer;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

public class Main {

    public static void main(final String[] args) throws IOException {
        if (args == null || args.length != 1) {
            System.out.println("Aufruf mit FILE");
            return;
        }
        final File file = new File(args[0]);
        if (file.getName().equals("assignment.txt")) {
            Main.prepareTalk(file);
        } else {
            Main.writeParticipantsLists(file);
        }
    }

    private static void prepareTalk(final File file) throws IOException {
        final Path root = file.getAbsoluteFile().toPath().getParent();
        final Path protocols = root.resolve("protocols");
        protocols.toFile().mkdir();
        final File metaFile = root.getParent().resolve("meta.txt").toFile();
        final Subject subject = Subject.fromFile(metaFile);
        final TalkMode talkMode = TalkMode.fromFile(metaFile);
        final String place =
            root.toFile().getName().substring(3).toLowerCase().startsWith("m") ? "Mettmann" : "Bergisch Gladbach";
        final List<LocalDate> dates = Main.toDates(root);
        final List<Assignment> assignments = Main.toAssignments(file, dates);
        final File build = protocols.resolve("build.sh").toFile();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(build))) {
            writer.write("#!/bin/bash\n\n");
            writer.write("for i in *.tex; do\n");
            writer.write("    pdflatex \"$i\"\n");
            writer.write("    pdflatex \"$i\"\n");
            writer.write("done\n");
        }
        build.setExecutable(true);
        try (
            BufferedWriter solutionsWriter = new BufferedWriter(new FileWriter(root.resolve("solutions.csv").toFile()))
        ) {
            for (final Assignment assignment : assignments) {
                System.out.println(
                    String.format(
                        "%s: %s (%s)",
                        assignment.date().toString(),
                        assignment.topic(),
                        assignment.participant()
                    )
                );
                solutionsWriter.write(assignment.participant());
                solutionsWriter.write(";\n");
                final String[] nameParts = assignment.participant().split(" ");
                final String lastName = nameParts[nameParts.length - 1];
                final String protocol = String.format("protokoll%s%s.tex", subject.shortName(), Main.toASCII(lastName));
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(protocols.resolve(protocol).toFile()))) {
                    writer.write("\\documentclass{article}\n\n");
                    writer.write("\\input{../../../../../../templates/protocol/packages.tex}\n");
                    writer.write("\\newcommand{\\subject}{");
                    writer.write(subject.name());
                    writer.write("}\n");
                    writer.write("\\newcommand{\\student}{");
                    writer.write(assignment.participant());
                    writer.write("}\n");
                    writer.write("\\newcommand{\\presentationtitle}{");
                    writer.write(assignment.topic());
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
                    writer.write("}\n\n");
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
                        writer.write("\\quizpassed{}\n");
                        writer.write("\\quizbonusi{0}\n");
                        writer.write("\\quizbonusii{0}\n");
                        writer.write("\\quizbonusiii{0}\n");
                        writer.write("\\quizparticipantbonusi{0}\n");
                        writer.write("\\quizparticipantbonusii{0}\n");
                        writer.write("\\quizparticipantbonusiii{0}\n");
                        writer.write("\\evaluationpartresult{20}\n");
                        writer.write("}\n\n");
                        writer.write("\\newcommand{\\additionalEvaluation}{%\n");
                        writer.write("\\contributions\n");
                        writer.write("Die individuellen Beiträge umfassten:\n");
                        writer.write("\\begin{itemize}\n");
                        writer.write("\\item \n");
                        writer.write("\\end{itemize}%\n");
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
                            writer.write("\\quizpassed{}\n");
                            writer.write("\\quizbonusi{0}\n");
                            writer.write("\\quizbonusii{0}\n");
                            writer.write("\\quizbonusiii{0}\n");
                            writer.write("\\quizparticipantbonusi{0}\n");
                            writer.write("\\quizparticipantbonusii{0}\n");
                            writer.write("\\quizparticipantbonusiii{0}\n");
                            writer.write("\\evaluationpartresult{10}\n");
                        } else {
                            writer.write("\\newcommand{\\quiz}{%\n");
                            writer.write("\\quizcontentv{}\n");
                            writer.write("\\quizdifficultyv{}\n");
                            writer.write("\\quizpassed{}\n");
                            writer.write("\\quizbonusi{0}\n");
                            writer.write("\\quizbonusii{0}\n");
                            writer.write("\\quizbonusiii{0}\n");
                            writer.write("\\quizparticipantbonusi{0}\n");
                            writer.write("\\quizparticipantbonusii{0}\n");
                            writer.write("\\quizparticipantbonusiii{0}\n");
                            writer.write("\\evaluationpartresult{20}\n");
                        }
                        writer.write("}\n\n");
                        writer.write("\\newcommand{\\additionalEvaluation}{%\n");
                        writer.write("\\contributions\n");
                        writer.write("Die individuellen Beiträge umfassten:\n");
                        writer.write("\\begin{itemize}\n");
                        writer.write("\\item \n");
                        writer.write("\\end{itemize}%\n");
                        writer.write("\\evaluation{}{40}\n");
                        break;
                    }
                    writer.write("}\n\n");
                    writer.write("\\newcommand{\\totalReview}{%\n");
                    writer.write(
                        "Insgesamt wurden \\evaluationpoints{} Punkte erreicht und das Gesamturteil lautet: \\grade\n"
                    );
                    writer.write("}\n\n");
                    writer.write("\\input{../../../../../../templates/protocol/protocol.tex}\n");
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

    private static List<Assignment> toAssignments(final File file, final List<LocalDate> dates) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final List<Assignment> result = new LinkedList<Assignment>();
            String line = reader.readLine();
            int i = dates.size() - 1;
            int num = 0;
            while (line != null && !line.isBlank()) {
                final String[] assignment = line.split("->");
                if (assignment.length != 2) {
                    throw new IOException("File must contain assignments of the form participant -> topic!");
                }
                result.add(new Assignment(assignment[0].strip(), assignment[1].strip(), dates.get(i)));
                num++;
                if (num >= 4) {
                    num = 0;
                    i--;
                    if (i < 0) {
                        i = 0;
                    }
                }
                line = reader.readLine();
            }
            return result;
        }
    }

    private static LocalDate toDate(final Path path) {
        final String fileName = path.toFile().getName();
        return LocalDate.of(
            2000 + Integer.parseInt(fileName.substring(0, 2)),
            Integer.parseInt(fileName.substring(2, 4)),
            Integer.parseInt(fileName.substring(4, 6))
        );
    }

    private static List<LocalDate> toDates(final Path root) throws IOException {
        return Files
            .list(root)
            .filter(path -> path.toFile().getName().matches("[0-9][0-9][0-9][0-9][0-9][0-9]\\.txt"))
            .map(path -> Main.toDate(path))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private static void writeParticipantsLists(final File file) throws IOException {
        final String fileName = file.getName();
        final String classIdentifier = fileName.substring(0, fileName.length() - 4);
        final ParticipantsAndDates participantsAndDates = ParticipantsAndDates.fromFile(file);
        final Path root = file.getAbsoluteFile().toPath().getParent().resolve(classIdentifier);
        if (!root.toFile().mkdir()) {
            throw new IOException("Could not create directory " + root.toFile().getName() + "!");
        }
        for (final String date : participantsAndDates.dates()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(root.resolve(date + ".txt").toFile()))) {
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
            if (thirdLine != null && !thirdLine.isBlank()) {
                final int numOfTopics = Integer.parseInt(reader.readLine());
                final String[] topics = new String[numOfTopics];
                for (int i = 0; i < numOfTopics; i++) {
                    topics[i] = reader.readLine();
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

}
