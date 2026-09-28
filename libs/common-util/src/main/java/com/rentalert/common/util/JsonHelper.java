package com.rentalert.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rentalert.common.exception.AppException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@UtilityClass
public class JsonHelper {

    public static final ObjectMapper mapper = createMapper();

    private static final String FAILED = "Failed to convert object to json string: {}";

    private static ObjectMapper createMapper() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .defaultDateFormat(sdf)
                .build();
    }

    @SuppressWarnings("unchecked")
    public static Object fromJson(String json, Class valueClass) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, valueClass);
        } catch (Exception var4) {
            return null;
        }
    }

    public static String toJson(Object o) {
        if (o == null) {
            return "null";
        }
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception ex) {
            log.warn(FAILED, ex.getMessage());
            return "null";
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return mapper.readValue(json, typeReference);
        } catch (JacksonException e) {
            throw new AppException("Failed to deserialize JSON", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
