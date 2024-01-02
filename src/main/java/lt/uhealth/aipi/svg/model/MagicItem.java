package lt.uhealth.aipi.svg.model;

import java.util.List;

public record MagicItem (String magicString,
                        Integer index,
                        Integer total,
                        Long epoch,
                        String seed,
                        String magic,
                        List<Barrier> barriers,
                        Long iat){

}
