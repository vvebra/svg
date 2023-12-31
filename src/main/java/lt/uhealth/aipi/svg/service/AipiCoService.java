package lt.uhealth.aipi.svg.service;

import lt.uhealth.aipi.svg.client.AipiCoClient;
import lt.uhealth.aipi.svg.exception.AppRuntimeException;
import lt.uhealth.aipi.svg.exception.RestApiException;
import lt.uhealth.aipi.svg.model.MagicItemWithNotes;
import lt.uhealth.aipi.svg.model.Payload;
import lt.uhealth.aipi.svg.util.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AipiCoService {

    private static final Logger LOG = LoggerFactory.getLogger(AipiCoService.class);

    private final AipiCoClient aipiCoClient;

    @Autowired
    public AipiCoService(AipiCoClient aipiCoClient){
        this.aipiCoClient = aipiCoClient;
    }

    public Mono<List<MagicItemWithNotes>> getMagic(String magic){
        return Mono.just(magic)
                .doOnNext(m -> LOG.debug("Requesting getMagic with {}", m))
                .flatMap(aipiCoClient::getMagic)
                .onErrorMap(ExceptionMapper::fromWebClientResponseException)
                .doOnError(t -> LOG.error("Error while getMagic {}: {}: {}", magic, t.getClass(), t.getMessage()))
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(200))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .map(l -> toMagicItemsWithNotes(magic, l))
                .doOnNext(rez -> LOG.debug("getMagic result size is {}", rez.size()));
    }

    public Flux<MagicItemWithNotes> processMagicItems(List<MagicItemWithNotes> magicItemWithNotes){
        return Mono.just(magicItemWithNotes)
                .map(this::enrichMagicItems)
                .flatMapIterable(this::getIndependentMagicItems)
                .flatMap(this::processMagic)
                .doOnComplete(() -> LOG.debug("processMagic completed"))
                .thenMany(Flux.fromIterable(magicItemWithNotes));
    }

    public Flux<MagicItemWithNotes> processMagic(MagicItemWithNotes magicItemWithNotes){
        LOG.debug("processMagic index: {}", magicItemWithNotes.magicItem().index());
        return postMagic(magicItemWithNotes)
                .flatMapIterable(MagicItemWithNotes::findReadyDependents)
                .filter(m -> !m.pickedForRequest().getAndSet(true))
                .flatMap(this::processMagic);
    }

    public Mono<MagicItemWithNotes> postMagic(MagicItemWithNotes magicItemWithNotes){
        return postMagic(magicItemWithNotes, null);
    }

    Mono<MagicItemWithNotes> postMagic(MagicItemWithNotes magicItemWithNotes, Throwable prevThrowable){
        Mono<MagicItemWithNotes> mono = Mono.just(magicItemWithNotes);
        if (prevThrowable != null){
            if (prevThrowable instanceof RestApiException rae && rae.isTooEarly()){
                long tooEarlyBy = rae.tooEarlyBy();
                mono = mono
                        .doOnNext(m -> LOG.debug("Delaying postMagic with index {}: {} ms",
                                m.magicItem().index(), tooEarlyBy))
                        .delayElement(Duration.ofMillis(tooEarlyBy));
            } else {
                return Mono.error(prevThrowable);
            }
        }

        Mono<MagicItemWithNotes> mono2 = Mono.just(magicItemWithNotes)
                .doOnNext(m -> LOG.debug("Requesting postMagic with index: {}", m.magicItem().index()))
                .flatMap(m -> aipiCoClient.postMagic(m.magic(), Payload.fromMagicItemWithNotes(m)))
                .onErrorMap(ExceptionMapper::fromWebClientResponseException)
                .doOnError(t -> LOG.error("Error while postMagic {}: {}: {}",
                        magicItemWithNotes.magicItem().index(), t.getClass(), t.getMessage()))
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(10))
                        .filter(t -> !tooEarlyOrTooLate(t))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .map(magicItemWithNotes::withAnswer)
                .doOnNext(m -> LOG.debug("Answer from postMagic {}: {}", m.magicItem().index(), m.answer()))
                .onErrorResume(ignored -> prevThrowable == null, t -> postMagic(magicItemWithNotes, t));

        return mono.flatMap(ignored -> mono2);
    }

    boolean tooEarlyOrTooLate(Throwable t){
        return (t instanceof RestApiException rae) && (rae.isTooEarly() || rae.isTooLate());
    }

    public List<MagicItemWithNotes> enrichMagicItems(List<MagicItemWithNotes> magicItemsWithNotes) {
        Map<Integer, MagicItemWithNotes> magicItemsWithNotesMap = magicItemsWithNotes.stream()
                .map(m -> m.withDependents(new HashMap<>()))
                .collect(Collectors.toMap(m -> m.magicItem().index(), Function.identity()));

        magicItemsWithNotesMap.values().forEach(m -> {
            Set<Integer> dependencies = m.getDependencies();
            dependencies.forEach(d -> magicItemsWithNotesMap.get(d).dependents().get().put(m.magicItem().index(), m));
        });

        magicItemsWithNotesMap.values().forEach(MagicItemWithNotes::withImmutableDependents);

        if (magicItemsWithNotesMap.size() != magicItemsWithNotes.size()) {
            throw new AppRuntimeException(
                    "Size of magicItemsWithNotesMap %s is not equals to the size of magicItemsWithNotes %s"
                            .formatted(magicItemsWithNotesMap.size(), magicItemsWithNotes.size()));
        }

        magicItemsWithNotes.forEach(m -> m.withDependsOn(magicItemsWithNotesMap));

        return magicItemsWithNotes;
    }

    List<MagicItemWithNotes> getIndependentMagicItems(List<MagicItemWithNotes> magicItems){
        return magicItems.stream()
                .filter(MagicItemWithNotes::isIndependant)
                .toList();
    }

    List<MagicItemWithNotes> toMagicItemsWithNotes(String magic, List<String> magicItemStrings){
        MagicItemWithNotes[] magicItemsWithNotes = new MagicItemWithNotes[magicItemStrings.size()];
        for (int i = 0; i < magicItemStrings.size(); i++) {
            magicItemsWithNotes[i] = MagicItemWithNotes.create(i, magic, magicItemStrings.get(i));
        }

        return List.of(magicItemsWithNotes);
    }
}
