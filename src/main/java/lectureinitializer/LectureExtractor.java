package lectureinitializer;

import java.util.function.*;

import ocp.*;

public class LectureExtractor implements Function<OCEntry, String> {

    public static final LectureExtractor INSTANCE = new LectureExtractor();

    private static final String LECTURE_PATTERN = ".+ \\((Vorlesung|E-Learning)\\) \\| (\\w|\\s)+";

    private LectureExtractor() {}

    @Override
    public String apply(final OCEntry entry) {
        final String subject = entry.subject();
        if (!subject.matches(LectureExtractor.LECTURE_PATTERN)) {
            return "other";
        }
        return subject.substring(0, subject.indexOf(" (")) + subject.substring(subject.indexOf(") ") + 1);
    }

}
