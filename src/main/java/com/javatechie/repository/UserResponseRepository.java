package com.javatechie.repository;

import com.javatechie.entity.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserResponseRepository extends JpaRepository<UserResponse, Long> {
}