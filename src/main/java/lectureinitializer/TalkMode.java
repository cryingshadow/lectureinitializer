package lectureinitializer;

import java.io.*;
import java.nio.file.*;

public enum TalkMode {

    TALK40QUIZ10, TALK40QUIZ20, TALK50SCIENCE, TALK80QUIZ20;

    public static TalkMode fromFile(final File metaFile) throws IOException {
        return TalkMode.valueOf(Files.lines(metaFile.toPath()).skip(2).findFirst().get());
    }

}
