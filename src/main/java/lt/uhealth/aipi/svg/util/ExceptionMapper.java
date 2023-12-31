package lt.uhealth.aipi.svg.util;

import lt.uhealth.aipi.svg.exception.RestApiException;
import lt.uhealth.aipi.svg.model.RestError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public interface ExceptionMapper {

    Logger LOG = LoggerFactory.getLogger(ExceptionMapper.class);

    static Throwable fromWebClientResponseException(Throwable t){
        if (t instanceof WebClientResponseException we){
            try {
                String errorString = we.getResponseBodyAsString();
                RestError restError = parseError(errorString);
                return new RestApiException(we.getStatusCode().value(), errorString, restError);
            } catch (RuntimeException re){
                LOG.error("Error while converting WebClientResponseException", re);
                return t;
            }
        }

        return t;
    }

    private static RestError parseError(String errorString){
        if (errorString == null || errorString.isEmpty()){
            return null;
        }

        try {
            return RestError.fromErrorString(errorString);
        } catch (RuntimeException re){
            LOG.error("Error while parsing RestError", re);
            return null;
        }
    }
}
