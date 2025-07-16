package lectureinitializer;

import java.io.*;
import java.lang.reflect.*;
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
                TalkAssignments.prepareTalk(new File(options.get(Flag.ASSIGNMENT)), classFile);
            } else {
                ParticipantsAndDates.writeParticipantsLists(classFile);
            }
        } else if (options.containsKey(Flag.PARTICIPANTS)) {
            CalendarExport.createClassFiles(
                new File(options.get(Flag.PARTICIPANTS)),
                new File(options.get(Flag.EXPORT))
            );
        } else if (options.containsKey(Flag.ATTENDANCE)) {
            AttendanceListUpdater.updateAttendanceList(
                new File(options.get(Flag.ATTENDANCE)),
                new File(options.get(Flag.EXPORT))
            );
        } else {
            QuizQuestions.transformQuizFile(new File(options.get(Flag.QUIZ)), new File(options.get(Flag.OUTPUT)));
        }
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
