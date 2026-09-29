package com.rentalert.common.service.impl;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.common.service.RestService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class RestServiceImpl implements RestService {

    private final RestClient restClient;

    @Override
    public <T> T processGetRequest(String uri, Class<T> responseType) {
        return restClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> throwUpstreamError("GET", uri, response)
            )
            .body(responseType);
    }

    @Override
    public <T> T processGetRequest(String uri, ParameterizedTypeReference<T> typeRef) {
        return restClient.get()
            .uri(uri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> throwUpstreamError("GET", uri, response)
            )
            .body(typeRef);
    }

    @Override
    public <T> T processPostRequest(String uri, String body, Class<T> responseType) {
        return restClient.post()
            .uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .onStatus(
                HttpStatusCode::isError,
                (request, response) -> throwUpstreamError("POST", uri, response)
            )
            .body(responseType);
    }

    private static void throwUpstreamError(String method, String uri, ClientHttpResponse response) throws IOException {
        int status = response.getStatusCode().value();
        throw new UpstreamHttpException(
            "Error during " + method + " request: " + uri + ", status=" + status,
            status,
            parseRetryAfterSeconds(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
        );
    }

    private static Long parseRetryAfterSeconds(String header) {
        if (header == null) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            // HTTP-date формат не поддерживаем — вызывающий код применит свой backoff.
            return null;
        }
    }

    @Override
    public String buildUri(String basePath, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(basePath);
        params.forEach(builder::queryParam);
        return builder.build().toUriString();
    }
}
