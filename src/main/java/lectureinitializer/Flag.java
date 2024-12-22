package lectureinitializer;

import clit.*;

public enum Flag implements Parameter {

    ASSIGNMENT("a", "assignment", "File containing the assignment of topics to participants."),

    CLASSFILE("c", "classfile", "File containing the participants and dates of the lecture."),

    EXPORT("e", "export", "CSV-export of the outlook calendar."),

    PARTICIPANTS("p", "participants", "File containing the participants of lectures.");

    private final String description;

    private final String longName;

    private final String shortName;

    private Flag(final String shortName, final String longName, final String description) {
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String longName() {
        return this.longName;
    }

    @Override
    public String shortName() {
        return this.shortName;
    }

}
