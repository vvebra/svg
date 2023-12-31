package lt.uhealth.aipi.svg.client;

import lt.uhealth.aipi.svg.model.Payload;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AipiCoClient {

    @GetExchange(value = "/{magic}", accept= MediaType.APPLICATION_JSON_VALUE)
    Mono<List<String>> getMagic(@PathVariable String magic);

    @PostExchange(value = "/{magic}", contentType = MediaType.APPLICATION_JSON_VALUE)
    Mono<String> postMagic(@PathVariable String magic, @RequestBody Payload magicValue);
}
