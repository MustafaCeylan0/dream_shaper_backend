package com.seng.comfy.backend.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "images")
@Getter
@Setter
public class Image {

    @Id
    @Setter(AccessLevel.NONE)
    @Column(name = "image_id")
    private String imageId;

    @Column(length = 200)
    private String imagePath;

    @Column(length = 200)
    private String lqImagePath;


    @Column(length = 1000)
    private String description;

    @Column(length = 100)
    private String model;

    @Column(length = 1000)
    private String prompt;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private UserProfile user;

    private Integer likes;

    @Column
    private Timestamp time;

    @Column
    private boolean visible;

    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();


    public Image(String imageId, String imagePath, String lgImagePath, String description, String model, String prompt, UserProfile user, Integer likes, Timestamp time, boolean visible) {
        this.imageId = imageId;
        this.imagePath = imagePath;
        this.lqImagePath = lgImagePath;
        this.description = description;
        this.model = model;
        this.prompt = prompt;
        this.user = user;
        this.likes = likes;
        this.time = time;
        this.visible = visible;
        this.comments = new ArrayList<>();
    }

    public Image() {
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setImage(this);
    }
    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setImage(null);
    }
}