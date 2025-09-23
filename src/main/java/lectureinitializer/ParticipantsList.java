package lectureinitializer;

import java.io.*;
import java.util.*;

public class ParticipantsList extends LinkedHashMap<Lecture, List<String>> {

    private static final long serialVersionUID = 1L;

    private static Lecture extractLecture(final String line, final BufferedReader reader) throws IOException {
        String lectureLine = line;
        while (lectureLine.chars().filter(i -> i == '|').count() != 2) {
            lectureLine += reader.readLine();
        }
        return Lecture.parse(lectureLine.substring(0, lectureLine.lastIndexOf('|') - 1));
    }

    public ParticipantsList(final File participantsList) throws IOException {
        super();
        try (BufferedReader reader = new BufferedReader(new FileReader(participantsList))) {
            String line = reader.readLine();
            Lecture currentLecture = ParticipantsList.extractLecture(line, reader);
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
                    if (!line.isBlank()) {
                        Lecture lectureOnPage = ParticipantsList.extractLecture(line, reader);
                        if (!currentLecture.equals(lectureOnPage)) {
                            this.put(currentLecture, currentParticipants);
                            currentLecture = lectureOnPage;
                            currentParticipants = new LinkedList<String>();
                        }
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
