package com.cheesouz.api_service.dto;

import java.time.LocalDate;

public record UserResponse(
     Long id,
     String firstName,
     String lastName,
     String emailAdress,
     LocalDate dateOfBirth
)
{}