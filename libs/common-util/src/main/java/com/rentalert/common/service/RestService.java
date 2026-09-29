package com.rentalert.common.service;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

public interface RestService {

    <T> T processGetRequest(String uri, Class<T> responseType);

    <T> T processGetRequest(String uri, ParameterizedTypeReference<T> typeRef);

    <T> T processPostRequest(String uri, String body, Class<T> responseType);

    String buildUri(String basePath, Map<String, Object> params);
}
