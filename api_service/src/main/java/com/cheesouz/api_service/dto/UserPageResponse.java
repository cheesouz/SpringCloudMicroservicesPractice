package com.cheesouz.api_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPageResponse {
    private List<UserResponse> content;
    private int size;
    private int number;
    private long totalElements;
    private int totalPages;
}