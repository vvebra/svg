package lt.uhealth.alpi.svg.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = { RuntimeException.class })
    protected Mono<ResponseEntity<Object>> handleRuntimeException(RuntimeException re, ServerWebExchange exchange) {
        LOG.error("Handling exception from controller", re);
        return handleExceptionInternal(re, "", new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, exchange);
    }

}
