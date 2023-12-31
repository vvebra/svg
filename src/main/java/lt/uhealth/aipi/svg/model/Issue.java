package lt.uhealth.aipi.svg.model;

import java.util.List;

public record Issue(String code, String message, String expected, String received, Params params, List<String> path) {

    public boolean isTooLate(){
        return message != null && message.contains("too late")
                && params != null && params.expected() != null
                && params.expected().before() != null
                && params.actual() != null
                && params.actual() >= params.expected().before();
    }

    public boolean isTooEarly(){
        return message != null && message.contains("too early")
                && params != null && params.expected() != null
                && params.expected().after() != null
                && params.actual() != null
                && params.actual() <= params.expected().after();
    }

    public Long tooEarly(){
        if (params == null || params.expected() == null
                || params.expected().after() == null
                || params.actual() == null
                || params.expected().after() < params.actual()){
            return null;
        }

        return params.expected().after() - params.actual();
    }
}
