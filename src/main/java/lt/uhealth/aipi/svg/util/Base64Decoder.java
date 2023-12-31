package lt.uhealth.aipi.svg.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public interface Base64Decoder {

    static String decode(String s){
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }
}
