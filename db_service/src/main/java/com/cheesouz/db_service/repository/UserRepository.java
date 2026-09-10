package com.cheesouz.db_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cheesouz.db_service.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
