package com.cheesouz.api_service.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "first name is required")
        @Size(max = 100, message = "first name must be at most 100 characters")
        String firstName,
        @NotBlank(message = "last name is required")
        @Size(max = 100, message = "last name must be at most 100 characters")
        String lastName,
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 254, message = "email must be at most 254 characters")
        String emailAdress,
        @NotNull(message = "date of birth is required")
        LocalDate dateOfBirth) {}