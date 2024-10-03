package lectureinitializer;

import java.io.*;
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

}
