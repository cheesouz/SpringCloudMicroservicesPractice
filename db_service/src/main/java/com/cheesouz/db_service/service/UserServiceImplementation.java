package com.cheesouz.db_service.service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.cheesouz.db_service.dto.UserCreateRequest;
import com.cheesouz.db_service.dto.UserResponse;
import com.cheesouz.db_service.entity.User;
import com.cheesouz.db_service.exception.InvalidSortException;
import com.cheesouz.db_service.repository.UserRepository;

@Service
public class UserServiceImplementation implements UserService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    public UserServiceImplementation(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Page<UserResponse> findPage(int page, int size, String sortBy, String sortDir, String lastName, LocalDate dateOfBirth) {
     
        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDir)
                .orElseThrow(() -> new InvalidSortException("Unsupported sort direction: " + sortDir));

        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, sortBy));

        return userRepository.findAll(buildFilter(lastName, dateOfBirth), pageable)
                .map(UserResponse::from);
    }

    private Specification<User> buildFilter(String lastName, LocalDate dateOfBirth) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (lastName != null && !lastName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("lastName")),
                        "%" + lastName.toLowerCase(Locale.ROOT) + "%"));
            }
            if (dateOfBirth != null) {
                predicates.add(cb.equal(root.get("dateOfBirth"), dateOfBirth));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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