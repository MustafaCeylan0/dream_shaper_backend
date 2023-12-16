package com.seng.comfy.backend.repository;

import com.seng.comfy.backend.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByEmail(String email);

    Optional<UserAuth> findByUsername(String username);
}
