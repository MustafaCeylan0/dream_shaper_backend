package com.seng.comfy.backend.repository;

import com.seng.comfy.backend.entity.Image;
import com.seng.comfy.backend.entity.UserAuth;
import com.seng.comfy.backend.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByUser_username(String username);


}
