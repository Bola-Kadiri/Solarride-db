package com.solarride.solarride.repository;

import com.solarride.solarride.domain.user.Role;
import com.solarride.solarride.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndRole(UUID id, Role role);
}