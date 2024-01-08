package com.seng.comfy.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profile")
@Setter
@Getter
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;


    @JsonBackReference // This will omit the user field during serialization
    @OneToOne( fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAuth user;

    @Column(length = 200)
    private String ppPath;

    @Column(length = 2000)
    private String bio;

    @Column
    private Timestamp timeGenerate;

    @Column
    private Timestamp timeLastLogin;

    @Column(nullable = false)
    private  int credits;
    public UserProfile() {
        this.timeGenerate = new Timestamp(System.currentTimeMillis());
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    // Add OneToMany relationship for comments
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();
}