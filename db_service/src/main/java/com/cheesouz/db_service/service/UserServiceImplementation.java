package com.cheesouz.db_service.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cheesouz.db_service.dto.UserCreateRequest;
import com.cheesouz.db_service.dto.UserResponse;
import com.cheesouz.db_service.entity.User;
import com.cheesouz.db_service.repository.UserRepository;

@Service
public class UserServiceImplementation implements UserService {
    private final UserRepository userRepository;

    public UserServiceImplementation(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from);
    }

    @Override
    public UserResponse save(UserCreateRequest request) {
        return UserResponse.from(userRepository.save(toEntity(request)));
    }

    @Override
    public Optional<UserResponse> update(Long id, UserCreateRequest request) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setFirstName(request.getFirstName());
                    existing.setLastName(request.getLastName());
                    existing.setEmailAdress(request.getEmailAdress());
                    existing.setDateOfBirth(request.getDateOfBirth());
                    return UserResponse.from(userRepository.save(existing));
                });
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    private User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmailAdress(request.getEmailAdress());
        user.setDateOfBirth(request.getDateOfBirth());
        return user;
    }
}