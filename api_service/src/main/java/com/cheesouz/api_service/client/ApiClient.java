package com.cheesouz.api_service.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.cheesouz.api_service.dto.UserCreateRequest;
import com.cheesouz.api_service.dto.UserPageResponse;
import com.cheesouz.api_service.dto.UserResponse;
import com.cheesouz.api_service.exception.ServiceUnavailableException;
import com.cheesouz.api_service.exception.UpstreamTimeoutException;
import com.cheesouz.api_service.exception.UserNotFoundException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@Component
public class ApiClient {

    private final WebClient webClient;
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ApiClient(
        WebClient.Builder loadBalancedWebClientBuilder, 
        RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
                this.webClient = loadBalancedWebClientBuilder
                        .baseUrl("http://db-service")
                        .build();
                this.circuitBreakerRegistry = circuitBreakerRegistry;
                this.retryRegistry = retryRegistry;
    }

    public UserPageResponse getUsersPage(int page, int size, String sort, String lastName, LocalDate dateOfBirth) {
        return withResilience( webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/users")
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .queryParam("sort", sort);
                    if (lastName != null && !lastName.isBlank()) {
                        uriBuilder.queryParam("lastName", lastName);
                    }
                    if (dateOfBirth != null) {
                        uriBuilder.queryParam("dateOfBirth", dateOfBirth);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(UserPageResponse.class), true)
                .block();
    }

    public UserResponse getUserById(Long id) {
        return withResilience(webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new UserNotFoundException(id)))
                .bodyToMono(UserResponse.class), true)
                .block();
    }

    public UserResponse createUser(UserCreateRequest request) {
        return withResilience(webClient.post()
                .uri("/users")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class), false)
                .block();
    }

    public UserResponse updateUser(Long id, UserCreateRequest request) {
        return withResilience(webClient.put()
                .uri("/users/{id}", id)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new UserNotFoundException(id)))
                .bodyToMono(UserResponse.class), true)
                .block();
    }

    public void deleteUser(Long id) {
        withResilience(webClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new UserNotFoundException(id)))
                .toBodilessEntity(), true)
                .block();
    }

    public ResponseEntity<String> chaos(int delayMs, double errorRate) {
        return withResilience( webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/dev/chaos")
                        .queryParam("delayMs", delayMs)
                        .queryParam("errorRate", errorRate)
                        .build())
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(body -> {
                            if (response.statusCode().isError()) {
                                int statusValue = response.statusCode().value();
                                HttpStatus httpStatus = HttpStatus.resolve(statusValue);
                                String statusText = httpStatus != null ? httpStatus.getReasonPhrase() : "Unknown Error";
                                return Mono.error(WebClientResponseException.create(
                                        statusValue,
                                        statusText,
                                        response.headers().asHttpHeaders(),
                                        body.getBytes(StandardCharsets.UTF_8),
                                        StandardCharsets.UTF_8));
                            }
                            return Mono.just(ResponseEntity.status(response.statusCode()).body(body));
                        })), true)
                .block();
    }

    private static final String TARGET_SERVICE = "db-service";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private <T> Mono<T> withResilience(Mono<T> mono, boolean withRetry) {
        Mono<T> resilient = mono.timeout(TIMEOUT);

        if (withRetry) {
            resilient = resilient.transformDeferred(
                    RetryOperator.of(retryRegistry.retry("dbService")));
        }

        return resilient
                .transformDeferred(
                        CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("dbService")))
                .onErrorResume(ApiClient::translateDownstreamError);
    }

    private static <T> Mono<T> translateDownstreamError(Throwable error) {
        Throwable cause = Exceptions.unwrap(error);

        if (cause instanceof TimeoutException) {
            return Mono.error(new UpstreamTimeoutException(
                    "Call to " + TARGET_SERVICE + " timed out after " + TIMEOUT.toMillis() + "ms."));
        }
        if (cause instanceof CallNotPermittedException) {
            return Mono.error(new ServiceUnavailableException(
                    "Circuit breaker for " + TARGET_SERVICE
                            + " is OPEN; the service is temporarily unavailable."));
        }
        if (cause instanceof IOException || cause instanceof WebClientRequestException) {
            return Mono.error(new ServiceUnavailableException(
                    "Call to " + TARGET_SERVICE
                            + " failed after retries were exhausted: " + cause.getMessage()));
        }
        return Mono.error(error);
    }
}