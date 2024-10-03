package lectureinitializer;

import java.io.*;

import org.testng.*;
import org.testng.annotations.*;

public class ParticipantsAndDatesTest {

    @DataProvider
    public Object[][] fromReaderData() {
        return new Object[][] {
            {"0\n0", new ParticipantsAndDates(new String[] {}, new String[] {})},
            {
                "1\nMax Mustermann\n1\n24101109454",
                new ParticipantsAndDates(new String[] {"Max Mustermann"}, new String[] {"24101109454"})
            },
            {
                "2\nMax Mustermann\nMelanie Musterfrau\n3\n24101109454\n24101209454\n24111113454",
                new ParticipantsAndDates(
                    new String[] {"Max Mustermann", "Melanie Musterfrau"},
                    new String[] {"24101109454", "24101209454", "24111113454"}
                )
            }
        };
    }

    @Test(dataProvider="fromReaderData")
    public void fromReaderTest(final String content, final ParticipantsAndDates expected) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            Assert.assertEquals(ParticipantsAndDates.fromReader(reader), expected);
        }
    }

}
