package lectureinitializer;

import java.util.*;
import java.util.function.*;

import ocp.*;

public class LectureExtractor implements Function<OCEntry, Lecture> {

    public static final LectureExtractor INSTANCE = new LectureExtractor();

    private static final String LECTURE_PATTERN = ".+ \\((Vorlesung|E-Learning|Hybrid-Vorlesung)\\) \\| (\\w|\\s|,)+";

    private LectureExtractor() {}

    @Override
    public Lecture apply(final OCEntry entry) {
        final String subject = entry.subject();
        if (!subject.matches(LectureExtractor.LECTURE_PATTERN)) {
            return new Lecture("other", Set.of());
        }
        return Lecture.parse(
            subject.substring(0, subject.indexOf(" (")) + subject.substring(subject.indexOf(") ") + 1)
        );
    }

}
