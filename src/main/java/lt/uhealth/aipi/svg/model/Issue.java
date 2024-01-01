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

    public Long tooEarlyByMillis(){
        if (params == null || params.expected() == null
                || params.expected().after() == null
                || params.actual() == null
                || params.expected().after() < params.actual()){
            return null;
        }

        return params.expected().after() - params.actual();
    }

    public boolean isMissingDependencies(){
        return message != null && message.compareToIgnoreCase("Required") == 0
                && code != null && code.equals("invalid_type")
                && path != null && path.size() == 2
                && path.getFirst().equals("responses") && isInteger(path.get(1));
    }

    public Integer getMissingDependency(){
        if (!isMissingDependencies()){
            return null;
        }

        return Integer.parseInt(path.get(1));
    }

    boolean isInteger(String s){
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException nfe){
            return false;
        }
    }
}
