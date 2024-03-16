package lectureinitializer;

import java.io.*;

public record Subject(String name, String shortName) {

    public static Subject fromFile(final File metaFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            return new Subject(reader.readLine(), reader.readLine());
        }
    }

}
