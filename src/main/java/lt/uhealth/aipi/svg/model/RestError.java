package lt.uhealth.aipi.svg.model;

import lt.uhealth.aipi.svg.util.JsonReader;

import java.util.List;
import java.util.Objects;

public record RestError(String name, String message, List<Issue> issues) {

    public boolean isTooLate(){
        return issues != null && issues.stream().anyMatch(Issue::isTooLate);
    }

    public boolean isTooEarly(){
        return issues != null && issues.stream().anyMatch(Issue::isTooEarly);
    }

    public Long tooEarlyByMillis(){
        if (issues == null){
            return null;
        }

        return issues.stream()
                .filter(Issue::isTooEarly)
                .map(Issue::tooEarlyByMillis)
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    public static RestError fromErrorString(String errorString){
        return JsonReader.readValue(errorString, RestError.class);
    }
}
