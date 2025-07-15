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

}
