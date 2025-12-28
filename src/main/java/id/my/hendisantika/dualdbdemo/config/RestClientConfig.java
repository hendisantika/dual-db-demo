package id.my.hendisantika.dualdbdemo.config;

import id.my.hendisantika.dualdbdemo.config.properties.HttpClientPoolProperties;
import id.my.hendisantika.dualdbdemo.context.RequestContext;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for RestClient with Apache HttpClient 5 connection pooling.
 * Optimized for high-traffic scenarios with configurable pool settings.
 */
@Configuration
@EnableConfigurationProperties(HttpClientPoolProperties.class)
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    /**
     * Creates a PoolingHttpClientConnectionManager for managing HTTP connections.
     * This is the core component for connection pooling.
     */
    @Bean
    public PoolingHttpClientConnectionManager poolingConnectionManager(HttpClientPoolProperties properties) {
        log.info("Initializing HTTP connection pool - maxTotal: {}, maxPerRoute: {}",
                properties.getMaxTotal(), properties.getMaxPerRoute());

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectionTimeout()))
                .setSocketTimeout(Timeout.ofMilliseconds(properties.getSocketTimeout()))
                .setValidateAfterInactivity(TimeValue.ofMilliseconds(properties.getValidateAfterInactivity()))
                .setTimeToLive(TimeValue.ofMilliseconds(properties.getConnectionTimeToLive()))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(properties.getMaxTotal())
                .setMaxConnPerRoute(properties.getMaxPerRoute())
                .build();

        return connectionManager;
    }

    /**
     * Creates the CloseableHttpClient with connection pooling and request configuration.
     */
    @Bean
    public CloseableHttpClient httpClient(
            PoolingHttpClientConnectionManager connectionManager,
            HttpClientPoolProperties properties) {

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.getConnectionRequestTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(properties.getSocketTimeout()))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(properties.getIdleConnectionTimeout(), TimeUnit.MILLISECONDS))
                .build();

        log.info("HTTP client initialized with connection pooling");
        return httpClient;
    }

    /**
     * Creates Spring's HttpComponentsClientHttpRequestFactory using our pooled HttpClient.
     */
    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory(CloseableHttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * Creates the primary RestClient bean with connection pooling.
     * This RestClient automatically propagates correlation IDs from RequestContext.
     */
    @Bean
    public RestClient restClient(HttpComponentsClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    // Propagate correlation ID if available
                    String correlationId = RequestContext.getCorrelationId();
                    if (!"unknown".equals(correlationId)) {
                        request.getHeaders().add("X-Correlation-ID", correlationId);
                    }

                    // Propagate user ID if available
                    String userId = RequestContext.getUserId();
                    if (!"anonymous".equals(userId)) {
                        request.getHeaders().add("X-User-ID", userId);
                    }

                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Creates a RestClient builder for cases where you need custom configuration
     * per-request or per-service, while still using the pooled connection manager.
     */
    @Bean
    public RestClient.Builder restClientBuilder(HttpComponentsClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}
