package lt.uhealth.aipi.svg.model;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MagicItemWithNotesTest {

    private static final Logger LOG = LoggerFactory.getLogger(MagicItemWithNotesTest.class);

    @Test
    void givenMagicString_whenCreate_thenCorrect() {
        //given
        int index = 90;
        String magic = "por";
        String magicString = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpbmRleCI6OTAsInRvdGFsIjo5MywiZXBvY2giOjE3MDM4MDQwOTg0NTUsInNlZWQiOiJNVGN3TXpnd05EQTVPRFExTlE9PSIsIm1hZ2ljIjoicG9yIiwiYmFycmllcnMiOlt7InR5cGUiOiJ0aW1lIiwiZnJvbSI6MjE1OCwidW50aWwiOjQ2NDN9LHsidHlwZSI6ImRlcGVuZGVuY3kiLCJvbiI6Wzc1XX0seyJ0eXBlIjoiZGVwZW5kZW5jeSIsIm9uIjpbNDBdfV0sImlhdCI6MTcwMzgwNDA5OH0.DMK0XY1LrQsg-uuwCCWiHUdOnFdCsG0sOGetT2gTGIk";

        //when
        MagicItemWithNotes magicItemWithNotes = MagicItemWithNotes.create(index, magic, magicString);
        LOG.info("magicItemWithNotes={}", magicItemWithNotes);

        //then
        assertEquals(index, magicItemWithNotes.index());
        assertEquals(magic, magicItemWithNotes.magic());

        assertEquals(90, magicItemWithNotes.magicItem().index());
        assertEquals(93, magicItemWithNotes.magicItem().total());
        assertEquals(3, magicItemWithNotes.magicItem().barriers().size());

        Barrier barrier = magicItemWithNotes.magicItem().barriers().getFirst();
        assertEquals("time", barrier.type());
        assertEquals(2158, barrier.from());
        assertEquals(4643, barrier.until());

        barrier = magicItemWithNotes.magicItem().barriers().get(1);
        assertEquals("dependency", barrier.type());
        assertEquals(1, barrier.on().size());
        assertEquals(75, barrier.on().getFirst());
    }
}