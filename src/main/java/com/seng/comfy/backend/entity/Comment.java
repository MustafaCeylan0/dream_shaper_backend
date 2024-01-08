package com.seng.comfy.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;


@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long commentId;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserProfile user;

    @ManyToOne
    @JoinColumn(name = "image_id", referencedColumnName = "image_id")
    private Image image;

    @Column(length = 1000)
    private String comment;


    @Column
    private Timestamp time;


}