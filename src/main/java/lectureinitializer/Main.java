package lectureinitializer;

import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import clit.*;

public class Main {

    private static final List<Set<Flag>> ALLOWED_COMBINATIONS =
        List.of(
            Set.of(Flag.CLASSFILE),
            Set.of(Flag.CLASSFILE, Flag.ASSIGNMENT),
            Set.of(Flag.PARTICIPANTS, Flag.EXPORT),
            Set.of(Flag.ATTENDANCE, Flag.EXPORT),
            Set.of(Flag.QUIZ, Flag.OUTPUT)
        );

    public static String escapeForLaTeX(final String text) {
        return text.replaceAll("\\\\", "\\\\textbackslash")
            .replaceAll("([&\\$%\\{\\}_#])", "\\\\$1")
            .replaceAll("~", "\\\\textasciitilde{}")
            .replaceAll("\\^", "\\\\textasciicircum{}")
            .replaceAll("\\\\textbackslash", "\\\\textbackslash{}")
            .replaceAll("([^\\\\])\"", "$1''")
            .replaceAll("^\"", "''");
    }

    public static void main(final String[] args)
    throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final CLITamer<Flag> tamer = new CLITamer<Flag>(Flag.class);
        if (args == null || args.length < 1) {
            System.out.println(tamer.getParameterDescriptions());
            System.out.println(Main.helpText());
            return;
        }
        final Parameters<Flag> options = tamer.parse(args);
        if (!Main.ALLOWED_COMBINATIONS.contains(options.keySet())) {
            System.out.println(tamer.getParameterDescriptions());
            System.out.println(Main.helpText());
            return;
        }
        if (options.containsKey(Flag.CLASSFILE)) {
            final File classFile = new File(options.get(Flag.CLASSFILE));
            if (options.containsKey(Flag.ASSIGNMENT)) {
                TalkAssignment.prepareTalk(new File(options.get(Flag.ASSIGNMENT)), classFile);
            } else {
                ParticipantsAndDates.writeParticipantsLists(classFile);
            }
        } else if (options.containsKey(Flag.PARTICIPANTS)) {
            CalendarExport.createClassFiles(
                new File(options.get(Flag.PARTICIPANTS)),
                new File(options.get(Flag.EXPORT))
            );
        } else {
            QuizQuestions.transformQuizFile(new File(options.get(Flag.QUIZ)), new File(options.get(Flag.OUTPUT)));
        }
    }

    static List<TalkAssignment> toAssignmentsWithDates(
        final List<TopicAssignment> topicAssignments,
        final List<LocalDateTime> dates
    ) {
        final Map<LocalDate, List<LocalDateTime>> datesByDate =
            dates.stream().collect(
                Collectors.toMap(
                    LocalDateTime::toLocalDate,
                    d -> List.of(d),
                    (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).toList()
                )
            );
        int availableDates = dates.size();
        final int neededDates = topicAssignments.size();
        final List<LocalDate> keys = new ArrayList<LocalDate>(datesByDate.keySet());
        Collections.sort(keys);
        for (final LocalDate key : keys) {
            final int datesAtDate = datesByDate.get(key).size();
            if (datesAtDate > availableDates - neededDates) {
                break;
            } else {
                availableDates -= datesAtDate;
                datesByDate.remove(key);
            }
        }
        final List<TalkAssignment> result = new LinkedList<TalkAssignment>();
        int i = 0;
        keys.retainAll(datesByDate.keySet());
        outer: for (final LocalDate key : keys) {
            for (final LocalDateTime date : datesByDate.get(key)) {
                result.add(new TalkAssignment(topicAssignments.get(i), date));
                i++;
                if (i == neededDates) {
                    break outer;
                }
            }
        }
        return result;
    }

    static Stream<LocalDateTime> toLocalDateTime(final String dateString) {
        final int multiplicity = Integer.parseInt(dateString.substring(dateString.length() - 1));
        final LocalDateTime start =
            LocalDate.of(
                2000 + Integer.parseInt(dateString.substring(0, 2)),
                Integer.parseInt(dateString.substring(2, 4)),
                Integer.parseInt(dateString.substring(4, 6))
            ).atTime(Integer.parseInt(dateString.substring(6, 8)), Integer.parseInt(dateString.substring(8, 10)));
        final List<LocalDateTime> result = new LinkedList<LocalDateTime>();
        int numOfBreaks = 0;
        for (int i = 0; i < multiplicity; i++) {
            if (i > 0 && i % 2 == 0) {
                numOfBreaks++;
            }
            result.add(start.plusMinutes(i * 45 + numOfBreaks * 15));
        }
        return result.stream();
    }

    private static String helpText() {
        return String.format(
            "Allowed combinations: %s",
            Main.ALLOWED_COMBINATIONS
            .stream()
            .map(set ->
                set
                .stream()
                .map(flag -> "-" + flag.shortName())
                .collect(Collectors.joining(" and "))
            ).collect(Collectors.joining(", "))
        );
    }

}
