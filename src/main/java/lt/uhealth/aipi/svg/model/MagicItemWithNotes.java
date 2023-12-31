package lt.uhealth.aipi.svg.model;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import lt.uhealth.aipi.svg.util.JsonReader;

public record MagicItemWithNotes(int index,
                                 String magic,
                                 MagicItem magicItem,
                                 String magicItemString,
                                 AtomicReference<Map<Integer, MagicItemWithNotes>> dependsOn,
                                 AtomicReference<Map<Integer, MagicItemWithNotes>> dependents,
                                 AtomicBoolean pickedForRequest,
                                 AtomicReference<Answer> answer) {

    public boolean isIndependant(){
        return this.magicItem.isIndependant();
    }

    public MagicItemWithNotes withAnswer(String answerString) {
        this.answer.set(Answer.fromAnswerString(answerString));
        return this;
    }

    public Set<Integer> getDependencies() {
        return magicItem.getDependencies();
    }

    public MagicItemWithNotes withDependsOn(Map<Integer, MagicItemWithNotes> dependsOn) {
        this.dependsOn.set(getDependencies().stream()
                .map(dependsOn::get)
                .collect(Collectors.toUnmodifiableMap(m -> m.magicItem.index(), Function.identity())));

        return this;
    }

    public MagicItemWithNotes withDependents(Map<Integer, MagicItemWithNotes> dependents) {
        this.dependents.set(dependents);
        return this;
    }

    public MagicItemWithNotes withImmutableDependents(){
        this.dependents.set(Map.copyOf(this.dependents.get()));
        return this;
    }

    boolean isReady(){
        return dependsOn.get().values().stream().allMatch(m -> m.answer().get() != null);
    }

    public List<MagicItemWithNotes> findReadyDependents(){
        return dependents.get().values().stream().filter(MagicItemWithNotes::isReady).toList();
    }

    public static MagicItemWithNotes create(int index, String magic, String magicItemString) {
        int posOfFirstDot = magicItemString.indexOf('.');
        int posOfLastDot = magicItemString.lastIndexOf('.');

        String magicItemStringPart = posOfFirstDot >= 0 && posOfLastDot >= 0
                ? magicItemString.substring(posOfFirstDot + 1, posOfLastDot)
                : magicItemString;

        byte[] json = Base64.getUrlDecoder().decode(magicItemStringPart);

        MagicItem magicItem = JsonReader.readValue(json, MagicItem.class);
        return new MagicItemWithNotes(index, magic, magicItem, magicItemString,
                new AtomicReference<>(), new AtomicReference<>(), new AtomicBoolean(false),
                new AtomicReference<>());
    }
}
