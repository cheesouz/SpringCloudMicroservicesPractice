package com.cheesouz.api_service.client;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.cheesouz.api_service.dto.UserCreateRequest;
import com.cheesouz.api_service.dto.UserPageResponse;
import com.cheesouz.api_service.dto.UserResponse;
import com.cheesouz.api_service.exception.UserNotFoundException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
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
                        .map(body -> ResponseEntity.status(response.statusCode()).body(body))), true)
                .block();
    }

    private <T> Mono<T> withResilience(Mono<T> mono, boolean withRetry) {
    Mono<T> resilient = mono.timeout(Duration.ofSeconds(2));

    if (withRetry) {
        resilient = resilient.transformDeferred(
            RetryOperator.of(retryRegistry.retry("dbService")));
    }

    return resilient.transformDeferred(
        CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker("dbService")));
}
}