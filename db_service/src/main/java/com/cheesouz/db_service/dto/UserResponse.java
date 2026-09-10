package com.cheesouz.db_service.dto;

import java.time.LocalDate;

import com.cheesouz.db_service.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String emailAdress;
    private LocalDate dateOfBirth;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmailAdress(),
                user.getDateOfBirth());
    }
}