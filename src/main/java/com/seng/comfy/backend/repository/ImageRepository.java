package com.seng.comfy.backend.repository;

import com.seng.comfy.backend.entity.Image;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    Optional<Image> findByImageId(String imageId);

    //List<Optional<Image>> findByUserId(Long userId);

    List<Image> findByUser_userId(Long userId);


    List<Image> findAllByVisible(boolean b, Pageable pageable);

    List<Image> findAllByVisibleAndUser_userId(boolean b, Long userId, Pageable pageable);

    @Query("SELECT img FROM Image img JOIN img.user.likes lk WHERE lk.user.userId = :userId ")
    List<Image> findLikedImagesByUserId(@Param("userId") Long userId);

}
