package lectureinitializer;

import java.util.*;
import java.util.stream.*;

public record Lecture(String title, Set<String> groups) {

    public static Lecture parse(String line) {
        String[] parts = line.split("\\|");
        return new Lecture(
            parts[0].trim(),
            Arrays.stream(parts[1].split(",")).map(String::trim).collect(Collectors.toSet())
        );
    }
    
}
