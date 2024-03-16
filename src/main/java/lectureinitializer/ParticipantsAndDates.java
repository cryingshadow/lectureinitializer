package lectureinitializer;

import java.io.*;

public record ParticipantsAndDates(String[] participants, String[] dates) {

    public static ParticipantsAndDates fromFile(final File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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
    }

}
