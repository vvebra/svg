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
import java.util.concurrent.ConcurrentHashMap;
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

    public Flux<MagicItemWithNotes> solveMagicItems(List<MagicItemWithNotes> magicItemWithNotes){
        return Mono.just(magicItemWithNotes)
                .map(this::enrichMagicItems)
                .map(l -> l.stream()
                        .filter(MagicItemWithNotes::isReady)
                        .filter(m -> !m.pickedForRequest().getAndSet(true))
                        .toList())
                .doOnNext(l -> LOG.debug("Starting with magicItems: {}",
                        l.stream().map(m -> m.magicItem().index()).toList()))
                .flatMapIterable(Function.identity())
                .flatMap(this::solveMagicItemWithDependents)
                .doOnComplete(() -> LOG.debug("solveMagicItemsWithDependents completed"))
                .thenMany(Flux.fromIterable(magicItemWithNotes));
    }

    Flux<MagicItemWithNotes> solveMagicItemWithDependents(MagicItemWithNotes magicItemWithNotes){
        LOG.debug("solveMagicItemWithDependents for MagicItem: {}", magicItemWithNotes.magicItem().index());
        return postMagic(magicItemWithNotes)
                .map(MagicItemWithNotes::findReadyDependents)
                .map(l -> l.stream()
                        .filter(m -> !m.pickedForRequest().getAndSet(true))
                        .toList())
                .doOnNext(l -> LOG.debug("Picked ready for request dependents of MagicItem: {}: {}",
                        magicItemWithNotes.magicItem().index(),
                        l.stream().map(m -> m.magicItem().index()).toList()))
                .flatMapIterable(Function.identity())
                .flatMap(this::solveMagicItemWithDependents);
    }

    Mono<MagicItemWithNotes> postMagic(MagicItemWithNotes magicItemWithNotes){
        return postMagic(magicItemWithNotes, null);
    }

    Mono<MagicItemWithNotes> postMagic(MagicItemWithNotes magicItemWithNotes, Throwable prevThrowable){
        Mono<MagicItemWithNotes> mono = Mono.just(magicItemWithNotes);
        if (prevThrowable != null){
            if (prevThrowable instanceof RestApiException rae && rae.isTooEarly()) {
                long tooEarlyByMillis = rae.tooEarlyByMillis();
                mono = mono
                        .doOnNext(m -> LOG.debug("Delaying postMagic for magicItem {}: {} ms",
                                m.magicItem().index(), tooEarlyByMillis))
                        .delayElement(Duration.ofMillis(tooEarlyByMillis));
            } else if (prevThrowable instanceof RestApiException rae && rae.isMissingDependencies()){
                Set<Integer> missingDependencies = rae.getMissingDependencies();
                LOG.debug("Missing dependencies of magicItem: {}: {}",
                        magicItemWithNotes.magicItem().index(), missingDependencies);

                missingDependencies.forEach(magicItemWithNotes::addDependsOn);
                magicItemWithNotes.pickedForRequest().set(false);

                if (!pickForRequestIfReady(magicItemWithNotes)){
                    return Mono.empty();
                }
            } else {
                return Mono.error(prevThrowable);
            }
        }

        Mono<MagicItemWithNotes> mono2 = Mono.just(magicItemWithNotes)
                .doOnNext(m -> LOG.debug("Requesting postMagic for magicItem: {}", m.magicItem().index()))
                .flatMap(m -> aipiCoClient.postMagic(m.magic(), Payload.fromMagicItemWithNotes(m)))
                .onErrorMap(ExceptionMapper::fromWebClientResponseException)
                .doOnError(t -> LOG.error("Error while postMagic for magicItem {}: {}: {}",
                        magicItemWithNotes.magicItem().index(), t.getClass(), t.getMessage()))
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(10))
                        .filter(t -> !tooEarlyOrTooLateOrMissingDependencies(t))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .map(magicItemWithNotes::withAnswer)
                .doOnNext(m -> LOG.debug("Answer from postMagic for magicItem {}: {}",
                        m.magicItem().index(), m.answer()))
                .onErrorResume(t -> postMagic(magicItemWithNotes, t));

        return mono.flatMap(ignored -> mono2);
    }

    boolean pickForRequestIfReady(MagicItemWithNotes magicItemWithNotes){
        boolean isReady = magicItemWithNotes.isReady();
        if (isReady && !magicItemWithNotes.pickedForRequest().getAndSet(true)){
            LOG.debug("magicItem: {} is ready and successfully picked for request",
                    magicItemWithNotes.magicItem().index());
            return true;
        } else {
            if (isReady){
                LOG.debug("magicItem: {} is ready but picked for request by other",
                        magicItemWithNotes.magicItem().index());
            } else {
                LOG.debug("magicItem: {} is not ready", magicItemWithNotes.magicItem().index());
            }

            return false;
        }
    }

    boolean tooEarlyOrTooLateOrMissingDependencies(Throwable t){
        return (t instanceof RestApiException rae)
                && (rae.isTooEarly() || rae.isTooLate() || rae.isMissingDependencies());
    }

    List<MagicItemWithNotes> enrichMagicItems(List<MagicItemWithNotes> magicItemsWithNotes) {
        Map<Integer, MagicItemWithNotes> magicItemsWithNotesMap = magicItemsWithNotes.stream()
                .map(m -> m.withDependents(new ConcurrentHashMap<>()))
                .collect(Collectors.toUnmodifiableMap(m -> m.magicItem().index(), Function.identity()));

        if (magicItemsWithNotesMap.size() != magicItemsWithNotes.size()) {
            throw new AppRuntimeException(
                    "Size of magicItemsWithNotesMap %s is not equals to the size of magicItemsWithNotes %s"
                            .formatted(magicItemsWithNotesMap.size(), magicItemsWithNotes.size()));
        }

        magicItemsWithNotes.forEach(m -> m.withDependsOn(new ConcurrentHashMap<>()));
        magicItemsWithNotes.forEach(m -> m.allMagicItemWithNotes().set(magicItemsWithNotesMap));

        return magicItemsWithNotes;
    }

    List<MagicItemWithNotes> toMagicItemsWithNotes(String magic, List<String> magicItemStrings){
        MagicItemWithNotes[] magicItemsWithNotes = new MagicItemWithNotes[magicItemStrings.size()];
        for (int i = 0; i < magicItemStrings.size(); i++) {
            magicItemsWithNotes[i] = MagicItemWithNotes.create(i, magic, magicItemStrings.get(i));
        }

        return List.of(magicItemsWithNotes);
    }
}
