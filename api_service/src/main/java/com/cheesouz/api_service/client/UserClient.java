package com.cheesouz.api_service.client;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.cheesouz.api_service.dto.UserCreateRequest;
import com.cheesouz.api_service.dto.UserPageResponse;
import com.cheesouz.api_service.dto.UserResponse;
import com.cheesouz.api_service.exception.UserNotFoundException;

import reactor.core.publisher.Mono;

@Component
public class UserClient {

    private final WebClient webClient;

    public UserClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("http://db-service")
                .build();
    }

    public UserPageResponse getUsersPage(int page, int size, String sort, String lastName, LocalDate dateOfBirth) {
        return webClient.get()
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
                .bodyToMono(UserPageResponse.class)
                .block();
    }

    public UserResponse getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new UserNotFoundException(id)))
                .bodyToMono(UserResponse.class)
                .block();
    }

    public UserResponse createUser(UserCreateRequest request) {
        return webClient.post()
                .uri("/users")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .block();
    }

    public UserResponse updateUser(Long id, UserCreateRequest request) {
        return webClient.put()
                .uri("/users/{id}", id)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new UserNotFoundException(id)))
                .bodyToMono(UserResponse.class)
                .block();
    }

    public void deleteUser(Long id) {
        webClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new UserNotFoundException(id)))
                .toBodilessEntity()
                .block();
    }
}