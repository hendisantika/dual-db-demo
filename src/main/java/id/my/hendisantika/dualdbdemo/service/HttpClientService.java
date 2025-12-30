package id.my.hendisantika.dualdbdemo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

/**
 * Service demonstrating RestClient usage with connection pooling.
 * The RestClient is configured with Apache HttpClient 5 connection pool
 * for high-traffic scenarios.
 * <p>
 * Includes Redis fallback: if HTTP request fails, attempts to retrieve
 * cached data from Redis.
 */
@Slf4j
@Service
public class HttpClientService {

    private static final String CACHE_KEY_PREFIX = "http:cache:";

    private final RestClient restClient;
    private final RestClient.Builder restClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${http.client.fallback.enabled:true}")
    private boolean fallbackEnabled;

    @Value("${http.client.fallback.cache-ttl-minutes:30}")
    private long cacheTtlMinutes;

    public HttpClientService(RestClient restClient,
                             RestClient.Builder restClientBuilder,
                             RedisTemplate<String, Object> redisTemplate,
                             ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.restClientBuilder = restClientBuilder;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Simple GET request with Redis fallback.
     * If HTTP request fails, attempts to retrieve cached data from Redis.
     */
    public <T> T get(String url, Class<T> responseType) {
        log.debug("Executing GET request to: {}", url);
        String cacheKey = generateCacheKey(url);

        try {
            T result = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(responseType);

            // Cache successful response
            cacheResponse(cacheKey, result);
            return result;

        } catch (RestClientException e) {
            log.warn("HTTP request failed for URL: {}. Error: {}", url, e.getMessage());
            return getFromCacheOrThrow(cacheKey, responseType, e);
        }
    }

    /**
     * GET request with path variables and Redis fallback.
     */
    public <T> T get(String url, Class<T> responseType, Object... uriVariables) {
        log.debug("Executing GET request to: {} with variables", url);
        String cacheKey = generateCacheKey(url, uriVariables);

        try {
            T result = restClient.get()
                    .uri(url, uriVariables)
                    .retrieve()
                    .body(responseType);

            cacheResponse(cacheKey, result);
            return result;

        } catch (RestClientException e) {
            log.warn("HTTP request failed for URL: {} with variables. Error: {}", url, e.getMessage());
            return getFromCacheOrThrow(cacheKey, responseType, e);
        }
    }

    /**
     * GET request with query parameters and Redis fallback.
     */
    public <T> T getWithParams(String url, Class<T> responseType, Map<String, String> params) {
        log.debug("Executing GET request to: {} with params: {}", url, params);
        String cacheKey = generateCacheKey(url, params);

        try {
            T result = restClient.get()
                    .uri(url, uriBuilder -> {
                        params.forEach(uriBuilder::queryParam);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(responseType);

            cacheResponse(cacheKey, result);
            return result;

        } catch (RestClientException e) {
            log.warn("HTTP request failed for URL: {} with params: {}. Error: {}", url, params, e.getMessage());
            return getFromCacheOrThrow(cacheKey, responseType, e);
        }
    }

    /**
     * POST request with JSON body.
     */
    public <T, R> R post(String url, T body, Class<R> responseType) {
        log.debug("Executing POST request to: {}", url);

        return restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * PUT request with JSON body.
     */
    public <T, R> R put(String url, T body, Class<R> responseType) {
        log.debug("Executing PUT request to: {}", url);

        return restClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
    }

    /**
     * DELETE request.
     */
    public void delete(String url) {
        log.debug("Executing DELETE request to: {}", url);

        restClient.delete()
                .uri(url)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Parallel requests using Structured Concurrency.
     * Demonstrates how to make multiple HTTP requests concurrently
     * while leveraging the connection pool.
     */
    public <T> List<T> getMultipleInParallel(List<String> urls, Class<T> responseType) {
        log.debug("Executing {} parallel GET requests", urls.size());

        try (var scope = StructuredTaskScope.open()) {
            List<StructuredTaskScope.Subtask<T>> subtasks = urls.stream()
                    .map(url -> scope.fork(() -> get(url, responseType)))
                    .toList();

            scope.join();

            return subtasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel requests interrupted", e);
        } catch (StructuredTaskScope.FailedException e) {
            throw new RuntimeException("One or more parallel requests failed", e.getCause());
        }
    }

    /**
     * Creates a custom RestClient for a specific base URL.
     * Useful when you need different configurations per external service.
     */
    public RestClient createClientForBaseUrl(String baseUrl) {
        return restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Creates a custom RestClient with authentication header.
     */
    public RestClient createAuthenticatedClient(String baseUrl, String token) {
        return restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    // ==================== Redis Cache Helper Methods ====================

    /**
     * Generates a cache key for a URL.
     */
    private String generateCacheKey(String url) {
        return CACHE_KEY_PREFIX + url.hashCode();
    }

    /**
     * Generates a cache key for a URL with path variables.
     */
    private String generateCacheKey(String url, Object... uriVariables) {
        StringBuilder keyBuilder = new StringBuilder(url);
        for (Object var : uriVariables) {
            keyBuilder.append(":").append(var);
        }
        return CACHE_KEY_PREFIX + keyBuilder.toString().hashCode();
    }

    /**
     * Generates a cache key for a URL with query parameters.
     */
    private String generateCacheKey(String url, Map<String, String> params) {
        StringBuilder keyBuilder = new StringBuilder(url);
        params.forEach((k, v) -> keyBuilder.append(":").append(k).append("=").append(v));
        return CACHE_KEY_PREFIX + keyBuilder.toString().hashCode();
    }

    /**
     * Caches the response in Redis with configured TTL.
     */
    private <T> void cacheResponse(String cacheKey, T response) {
        if (!fallbackEnabled || response == null) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(cacheTtlMinutes));
            log.debug("Cached response for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Failed to cache response for key: {}. Error: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * Attempts to retrieve cached data from Redis, or throws the original exception.
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromCacheOrThrow(String cacheKey, Class<T> responseType, RestClientException originalException) {
        if (!fallbackEnabled) {
            log.debug("Fallback disabled, rethrowing original exception");
            throw originalException;
        }

        try {
            Object cachedValue = redisTemplate.opsForValue().get(cacheKey);

            if (cachedValue != null) {
                log.info("HTTP request failed, returning cached data for key: {}", cacheKey);

                // Handle type conversion if needed
                if (responseType.isInstance(cachedValue)) {
                    return (T) cachedValue;
                }

                // Convert using ObjectMapper for complex types
                String json = objectMapper.writeValueAsString(cachedValue);
                return objectMapper.readValue(json, responseType);
            }

            log.warn("No cached data available for key: {}", cacheKey);
            throw originalException;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached data for key: {}", cacheKey, e);
            throw originalException;
        } catch (Exception e) {
            if (e == originalException) {
                throw originalException;
            }
            log.error("Failed to retrieve cached data for key: {}. Error: {}", cacheKey, e.getMessage());
            throw originalException;
        }
    }

    /**
     * Manually cache a value (useful for pre-populating cache).
     */
    public <T> void cacheValue(String url, T value) {
        String cacheKey = generateCacheKey(url);
        cacheResponse(cacheKey, value);
    }

    /**
     * Invalidate a cached entry.
     */
    public void invalidateCache(String url) {
        String cacheKey = generateCacheKey(url);
        try {
            redisTemplate.delete(cacheKey);
            log.debug("Invalidated cache for key: {}", cacheKey);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for key: {}. Error: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * Check if fallback is enabled.
     */
    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }
}
