package lt.uhealth.alpi.svg;

import lt.uhealth.alpi.svg.model.MagicItemWithNotes;
import lt.uhealth.alpi.svg.service.AlpiCoService;
import lt.uhealth.alpi.svg.util.Base64Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Comparator;

@RestController
public class SvgController {

    private static final Logger LOG = LoggerFactory.getLogger(SvgController.class);

    private final AlpiCoService alpiCoService;

    @Autowired
    public SvgController(AlpiCoService alpiCoService){
        this.alpiCoService = alpiCoService;
    }

    @GetMapping(path = "/{magic}", produces = "image/svg")
    public Mono<String> getMagic(@PathVariable String magic){
        LOG.info("getMagic(magic={}) invoked", magic);

        return alpiCoService.getMagic(magic)
                .flux()
                .flatMap(alpiCoService::processMagicItems)
                .sort(Comparator.comparingInt(MagicItemWithNotes::index))
                .map(m -> m.answer().get().payload())
                .reduce("", String::concat)
                .map(Base64Decoder::decode);
    }
}
