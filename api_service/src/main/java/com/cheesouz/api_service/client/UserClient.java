package com.cheesouz.api_service.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.cheesouz.api_service.dto.UserCreateRequest;
import com.cheesouz.api_service.dto.UserResponse;

@Component
public class UserClient {

    private final WebClient webClient;

    public UserClient(WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("http://db-service")
                .build();
    }

    public List<UserResponse> getAllUsers() {
        return webClient.get()
                .uri("/users")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UserResponse>>() {})
                .block();
    }

    public UserResponse getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
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
                .bodyToMono(UserResponse.class)
                .block();
    }

    public void deleteUser(Long id) {
        webClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}