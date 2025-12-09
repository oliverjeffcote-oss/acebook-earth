package com.makersacademy.acebook.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;

@Data
@Entity @Getter
@Setter
@Table(name = "USERS")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "email_address")
    private String email;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "description")
    private String description;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "location")
    private String location;

    @Column(name = "education")
    private String education;

    @Column(name = "birthplace")
    private String birthplace;

    @Column(name = "employment")
    private String employment;

    @OneToMany(mappedBy = "user")
    private List<Post> posts;

    @OneToMany(mappedBy = "user")
    private List<Like> likes;

    @OneToMany(mappedBy = "user")
    private List<Comment> comments;

    // Friendships this user STARTED
    @OneToMany(mappedBy = "requester")
    private List<Relationship> sentFriendRequests;

    // Friendships where this user is the TARGET (the friend)
    @OneToMany(mappedBy = "receiver")
    private List<Relationship> receivedFriendRequests;

    public User() {
        this.enabled = TRUE;
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.enabled = TRUE;
    }

    public User(String username, boolean enabled, String email) {
        this.username = username;
        this.email = email;
        this.enabled = enabled;
    }
}
