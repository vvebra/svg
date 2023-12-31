package lt.uhealth.aipi.svg.model;

import java.util.List;

public record Barrier (String type,
                       Long from,
                       Long until,
                       List<Integer> on){

    List<Integer> getDependencies(){
        return "dependency".equals(type) && on != null ? on : List.of();
    }
}
