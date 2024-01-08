package com.seng.comfy.backend.repository;

import com.seng.comfy.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByImage_imageId(String imageId);
}