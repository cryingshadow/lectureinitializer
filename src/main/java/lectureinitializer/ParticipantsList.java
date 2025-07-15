package lectureinitializer;

import java.io.*;
import java.util.*;

public class ParticipantsList extends LinkedHashMap<String, List<String>> {

    private static final long serialVersionUID = 1L;

    private static String extractLecture(final String line, final BufferedReader reader) throws IOException {
        String lectureLine = line;
        while (lectureLine.chars().filter(i -> i == '|').count() != 2) {
            lectureLine += reader.readLine();
        }
        return lectureLine.substring(0, line.lastIndexOf('|') - 1);
    }

    public ParticipantsList(final File participantsList) throws IOException {
        super();
        try (BufferedReader reader = new BufferedReader(new FileReader(participantsList))) {
            String line = reader.readLine();
            String currentLecture = ParticipantsList.extractLecture(line, reader);
            List<String> currentParticipants = new LinkedList<String>();
            line = reader.readLine();
            while (line != null) {
                if (line.isBlank() || !line.startsWith("Seite") && !line.matches("\\w\\w\\w\\w\\d\\d\\d\\w.*")) {
                    line = reader.readLine();
                    continue;
                }
                if (line.startsWith("Seite")) {
                    reader.readLine();
                    line = reader.readLine().substring(1);
                    if (!line.isBlank() && !currentLecture.equals(ParticipantsList.extractLecture(line, reader))) {
                        this.put(currentLecture, currentParticipants);
                        currentLecture = ParticipantsList.extractLecture(line, reader);
                        currentParticipants = new LinkedList<String>();
                    }
                } else {
                    String entryLine = line;
                    while (entryLine.trim().length() < 9) {
                        entryLine += reader.readLine();
                    }
                    currentParticipants.add(entryLine.substring(8).trim());
                }
                line = reader.readLine();
            }
            this.put(currentLecture, currentParticipants);
        }
    }

}
