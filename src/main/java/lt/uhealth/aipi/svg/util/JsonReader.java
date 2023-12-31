package lt.uhealth.aipi.svg.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lt.uhealth.aipi.svg.exception.AppRuntimeException;

import java.io.IOException;

public interface JsonReader {

    ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    static <T> T readValue(byte[] json, Class<T> valueType){
        try {
            return OBJECT_MAPPER.readValue(json, valueType);
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
    }

    static <T> T readValue(String json, Class<T> valueType){
        try {
            return OBJECT_MAPPER.readValue(json, valueType);
        } catch (IOException e) {
            throw new AppRuntimeException(e);
        }
    }
}
