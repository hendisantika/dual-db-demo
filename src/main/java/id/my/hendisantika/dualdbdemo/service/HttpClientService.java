package id.my.hendisantika.dualdbdemo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;

/**
 * Service demonstrating RestClient usage with connection pooling.
 * The RestClient is configured with Apache HttpClient 5 connection pool
 * for high-traffic scenarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpClientService {

    private final RestClient restClient;
    private final RestClient.Builder restClientBuilder;

    /**
     * Simple GET request example.
     */
    public <T> T get(String url, Class<T> responseType) {
        log.debug("Executing GET request to: {}", url);

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(responseType);
    }

    /**
     * GET request with path variables.
     */
    public <T> T get(String url, Class<T> responseType, Object... uriVariables) {
        log.debug("Executing GET request to: {} with variables", url);

        return restClient.get()
                .uri(url, uriVariables)
                .retrieve()
                .body(responseType);
    }

    /**
     * GET request with query parameters.
     */
    public <T> T getWithParams(String url, Class<T> responseType, Map<String, String> params) {
        log.debug("Executing GET request to: {} with params: {}", url, params);

        return restClient.get()
                .uri(url, uriBuilder -> {
                    params.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .retrieve()
                .body(responseType);
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
}
