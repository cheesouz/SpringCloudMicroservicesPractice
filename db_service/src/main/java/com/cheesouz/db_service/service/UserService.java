package com.cheesouz.db_service.service;

import java.util.List;
import java.util.Optional;

import com.cheesouz.db_service.dto.UserCreateRequest;
import com.cheesouz.db_service.dto.UserResponse;

public interface UserService {
    List<UserResponse> findAll();
    Optional<UserResponse> findById(Long id);
    UserResponse save(UserCreateRequest request);
    Optional<UserResponse> update(Long id, UserCreateRequest request);
    void deleteById(Long id);
}