package lt.uhealth.aipi.svg.exception;

import lt.uhealth.aipi.svg.model.RestError;

import java.util.Optional;

public class RestApiException extends RuntimeException {

    final int status;
    final String responseBody;
    final transient RestError restError;

    public RestApiException(int status, String responseBody, RestError restError){
        super("%s: %s".formatted(status, responseBody));
        this.status = status;
        this.responseBody = responseBody;
        this.restError = restError;
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isTooLate() {
        return restError != null && restError.isTooLate();
    }

    public boolean isTooEarly() {
        return restError != null && restError.isTooEarly();
    }

    public Long tooEarlyBy() {
        return Optional.ofNullable(restError).map(RestError::tooEarly).orElse(null);
    }

    public RestError getRestError() {
        return restError;
    }
}
