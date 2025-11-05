package lectureinitializer;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public record ParticipantsAndDates(String[] participants, String[] dates) {

    public static ParticipantsAndDates fromFile(final File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return ParticipantsAndDates.fromReader(reader);
        }
    }

    public static ParticipantsAndDates fromReader(final BufferedReader reader) throws IOException {
        final int numOfParticipants = Integer.parseInt(reader.readLine());
        final String[] participants = new String[numOfParticipants];
        for (int i = 0; i < numOfParticipants; i++) {
            participants[i] = reader.readLine();
        }
        final int numOfDates = Integer.parseInt(reader.readLine());
        final String[] dates = new String[numOfDates];
        for (int i = 0; i < numOfDates; i++) {
            dates[i] = reader.readLine();
        }
        return new ParticipantsAndDates(participants, dates);
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof ParticipantsAndDates)) {
            return false;
        }
        final ParticipantsAndDates other = (ParticipantsAndDates)o;
        return Arrays.deepEquals(this.participants(), other.participants())
            && Arrays.deepEquals(this.dates(), other.dates());
    }

    @Override
    public int hashCode() {
        return this.participants().hashCode() * 3 + this.dates().hashCode() * 5;
    }

    public static void writeParticipantsLists(final File classFile) throws IOException {
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
        ParticipantsAndDates.writeExecutableScript(
            root,
            "groups.sh",
            List.of(
                "#!/bin/bash",
                "",
                "if [[ $# -eq 2 ]]; then",
                "  java -jar ../../../../nameandgrouppicker.jar GROUPS -n $1.txt --count $2",
                " else if [[ $# -eq 3 ]]; then",
                "  java -jar ../../../../nameandgrouppicker.jar GROUPS -n $1.txt --min $2 --max $3",
                "else",
                "  echo \"Illegal number of parameters\" >&2",
                "  exit 2",
                "fi",
                "fi"
            )
        );
        ParticipantsAndDates.writeExecutableScript(
            root,
            "pick.sh",
            List.of(
                "#!/bin/bash",
                "",
                "java -jar ../../../../nameandgrouppicker.jar PICK -n $1.txt -f frequencies.txt"
            )
        );
        final File metaFile = root.getParent().resolve("meta.txt").toFile();
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            reader.readLine();
            reader.readLine();
            final String thirdLine = reader.readLine();
            if (thirdLine != null && !thirdLine.isBlank()) {
                if ("EXAM".equals(thirdLine)) {
                    Path exercisesPath = root.resolve("exercises");
                    exercisesPath.toFile().mkdir();
                    ParticipantsAndDates.writeExecutableScript(
                        exercisesPath,
                        "exgen.sh",
                        List.of(
                            "#!/bin/bash",
                            "",
                            "cd ../../../exercises",
                            "",
                            "for d in */ ; do",
                            "  cd $d",
                            "  . build.sh",
                            "  cd ..",
                            "done",
                            "",
                            String.format("cd ../classes/%s/exercises", root.getFileName().toString())
                        )
                    );
                    ParticipantsAndDates.writeExecutableScript(
                        exercisesPath,
                        "build.sh",
                        List.of(
                            "#!/bin/bash",
                            "",
                            ". exgen.sh",
                            "",
                            "for i in exercise*.tex; do",
                            "    pdflatex \"$i\"",
                            "    pdflatex \"$i\"",
                            "done",
                            "",
                            "for i in solution*.tex; do",
                            "    pdflatex \"$i\"",
                            "    pdflatex \"$i\"",
                            "done",
                            "",
                            "for i in exampleExam*.tex; do",
                            "    pdflatex \"$i\"",
                            "    pdflatex \"$i\"",
                            "done"
                        )
                    );
                } else {
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
    }

    private static void writeExecutableScript(
        final Path root,
        final String name,
        final List<String> lines
    ) throws IOException {
        final File script = root.resolve(name).toFile();
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(script))) {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        }
        script.setExecutable(true);
    }

}
