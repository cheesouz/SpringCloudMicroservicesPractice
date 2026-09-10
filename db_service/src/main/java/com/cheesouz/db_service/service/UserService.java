package com.cheesouz.db_service.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.cheesouz.db_service.dto.UserCreateRequest;
import com.cheesouz.db_service.dto.UserResponse;

public interface UserService {
    Page<UserResponse> findPage(int page, int size, String sortBy, String sortDir, String lastName, LocalDate dateOfBirth);
    Optional<UserResponse> findById(Long id);
    UserResponse save(UserCreateRequest request);
    Optional<UserResponse> update(Long id, UserCreateRequest request);
    void deleteById(Long id);
}