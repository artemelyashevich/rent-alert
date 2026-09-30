package com.rentalert.notification_ms.config;

import com.rentalert.common.interceptor.OutgoingRequestLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class HttpClientConfiguration {

    @Bean
    public PoolingHttpClientConnectionManager poolingConnectionManager(
        HttpClientProperties properties
    ) {
        SocketConfig socketConfig = SocketConfig.custom()
            .setSoTimeout(Timeout.ofMilliseconds(properties.getReadTimeoutMs()))
            .build();
        return PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(properties.getMaxConnections())
            .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
            .setConnectionTimeToLive(TimeValue.ofMinutes(properties.getConnectionTtlMinutes()))
            .setValidateAfterInactivity(TimeValue.ofSeconds(properties.getValidateAfterInactivitySeconds()))
            .setDefaultSocketConfig(socketConfig)
            .build();
    }

    @Bean
    public ConnectionKeepAliveStrategy keepAliveStrategy() {
        return (response, context) -> {
            BasicHeaderElementIterator iterator = new BasicHeaderElementIterator(response.headerIterator("Keep-Alive"));

            while (iterator.hasNext()) {
                HeaderElement element = iterator.next();
                if ("timeout".equalsIgnoreCase(element.getName()) && element.getValue() != null) {
                    try {
                        return TimeValue.ofSeconds(
                            Long.parseLong(element.getValue())
                        );
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return TimeValue.ofSeconds(30);
        };
    }

    @Bean
    public CloseableHttpClient apacheHttpClient(
        PoolingHttpClientConnectionManager connectionManager,
        ConnectionKeepAliveStrategy keepAliveStrategy,
        HttpClientProperties properties
    ) {

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.getConnectionRequestTimeoutMs()))
            .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMs()))
            .setResponseTimeout(Timeout.ofMilliseconds(properties.getReadTimeoutMs()))
            .build();

        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setKeepAliveStrategy(keepAliveStrategy)
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofSeconds(properties.getIdleEvictSeconds()))
            .disableConnectionState()
            .build();
    }

    @Bean
    public RestClient restClient(CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder()
            .requestFactory(factory)
            .requestInterceptor(new OutgoingRequestLoggingInterceptor())
            .build();
    }

    @Bean("frankfurterRestClient")
    public RestClient frankfurterRestClient() {
        SocketConfig socketConfig = SocketConfig.custom()
            .setSoTimeout(Timeout.ofMilliseconds(30000))
            .build();
        PoolingHttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(10)
            .setMaxConnPerRoute(5)
            .setDefaultSocketConfig(socketConfig)
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.ofMilliseconds(10000))
            .setConnectTimeout(Timeout.ofMilliseconds(10000))
            .setResponseTimeout(Timeout.ofMilliseconds(30000))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connManager)
            .setDefaultRequestConfig(requestConfig)
            .evictExpiredConnections()
            .evictIdleConnections(TimeValue.ofSeconds(30))
            .disableConnectionState()
            .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder()
            .requestFactory(factory)
            .requestInterceptor(new OutgoingRequestLoggingInterceptor())
            .build();
    }
}