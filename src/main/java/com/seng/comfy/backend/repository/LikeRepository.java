package com.seng.comfy.backend.repository;

import com.seng.comfy.backend.entity.Image;
import com.seng.comfy.backend.entity.Like;
import com.seng.comfy.backend.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserAndImage(UserProfile user, Image image);

    Optional<Like> findByUserAndImage(UserProfile user, Image image);
    void deleteByUserAndImage(UserProfile user, Image image);
}