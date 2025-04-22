package lectureinitializer;

import java.util.*;

public record QuizQuestion(String question, String correctAnswer, List<String> wrongAnswers) {}
