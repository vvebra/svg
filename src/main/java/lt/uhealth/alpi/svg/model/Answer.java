package lt.uhealth.alpi.svg.model;

import lt.uhealth.alpi.svg.util.JsonReader;

public record Answer(boolean success, String payload) {

    public static Answer fromAnswerString(String answerString){
        return JsonReader.readValue(answerString, Answer.class);
    }
}
