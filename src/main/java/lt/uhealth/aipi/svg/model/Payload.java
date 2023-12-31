package lt.uhealth.aipi.svg.model;

import lt.uhealth.aipi.svg.exception.AppRuntimeException;

import java.util.Map;
import java.util.stream.Collectors;

public record Payload(String payload, Map<String, String> responses) {

    public static Payload fromMagicItemWithNotes(MagicItemWithNotes magicItemWithNotes){

        Map<String, String> prevAnswers = magicItemWithNotes.dependsOn().get().values().stream()
                .map(Payload::checkAnswerExists)
                .collect(Collectors.toMap(m -> String.valueOf(m.magicItem().index()), m -> m.answer().get().payload()));

        return new Payload(magicItemWithNotes.magicItemString(), prevAnswers);
    }

    private static MagicItemWithNotes checkAnswerExists(MagicItemWithNotes magicItemWithNotes){
        if (magicItemWithNotes.answer().get() == null){
            throw new AppRuntimeException("Magic item with index=%s does not have an answer"
                    .formatted(magicItemWithNotes.magicItem().index()));
        }

        return magicItemWithNotes;
    }
}
