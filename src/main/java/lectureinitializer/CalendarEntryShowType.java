package lectureinitializer;

public enum CalendarEntryShowType {

    BLOCKED, ELSEWHERE, FREE, TENTATIVE;

    public static CalendarEntryShowType parse(final String showType) {
        switch (Integer.parseInt(showType)) {//TODO
        case 1:
            return TENTATIVE;
        case 2:
            return BLOCKED;
        case 3:
            return FREE;
        case 4:
            return ELSEWHERE;
        }
        return FREE;
    }

}
