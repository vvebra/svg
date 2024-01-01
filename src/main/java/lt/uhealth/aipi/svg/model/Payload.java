package lt.uhealth.aipi.svg.model;

import java.util.Map;
import java.util.stream.Collectors;

public record Payload(String payload, Map<String, String> responses) {

    public static Payload fromMagicItemWithNotes(MagicItemWithNotes magicItemWithNotes){

        Map<String, String> prevAnswers = magicItemWithNotes.allMagicItemWithNotes().get().values().stream()
                .filter(m -> m.answer().get() != null)
                .collect(Collectors.toMap(m -> String.valueOf(m.magicItem().index()), m -> m.answer().get().payload()));

        return new Payload(magicItemWithNotes.magicItemString(), prevAnswers);
    }
}
