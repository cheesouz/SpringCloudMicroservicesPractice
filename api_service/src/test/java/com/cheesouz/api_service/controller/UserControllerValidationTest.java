package com.cheesouz.api_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.cheesouz.api_service.client.ApiClient;
import com.cheesouz.api_service.dto.UserResponse;
import com.cheesouz.api_service.exception.GlobalExceptionHandler;

class UserControllerValidationTest {

    private final ApiClient apiClient = org.mockito.Mockito.mock(ApiClient.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(apiClient))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void createUserWithInvalidPayloadReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "Doe",
                                  "emailAdress": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed for the request."))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[?(@.field == 'firstName')].message")
                        .value("first name is required"))
                .andExpect(jsonPath("$.errors[?(@.field == 'emailAdress')].message")
                        .value("email must be a valid email address"))
                .andExpect(jsonPath("$.errors[?(@.field == 'dateOfBirth')].message")
                        .value("date of birth is required"));
    }

    @Test
    void createUserWithMalformedDateReturns400() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "emailAdress": "john.doe@example.com",
                                  "dateOfBirth": "not-a-date"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createUserWithValidPayloadReturns201() throws Exception {
        UserResponse response = new UserResponse(1L, "John", "Doe", "john.doe@example.com",
                LocalDate.of(1990, 5, 15));
        when(apiClient.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "emailAdress": "john.doe@example.com",
                                  "dateOfBirth": "1990-05-15"
                                }
                                """))
                .andExpect(status().is(HttpStatus.CREATED.value()))
                .andExpect(jsonPath("$.firstName").value("John"));
    }
}