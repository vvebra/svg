package lt.uhealth.aipi.svg.model;

import lt.uhealth.aipi.svg.util.JsonReader;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public boolean isMissingDependencies(){
        return issues != null && issues.stream().anyMatch(Issue::isMissingDependencies);
    }

    public Set<Integer> getMissingDependencies(){
        return Stream.ofNullable(issues)
                .flatMap(Collection::stream)
                .filter(Issue::isMissingDependencies)
                .map(Issue::getMissingDependency)
                .collect(Collectors.toSet());
    }

    public static RestError fromErrorString(String errorString){
        return JsonReader.readValue(errorString, RestError.class);
    }
}
