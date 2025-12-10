package com.makersacademy.acebook.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
// JPA will map this @Entity class to the db table
@Entity
//
@Getter @Setter @NoArgsConstructor
// this matches the name crated on the migration
@Table(name = "comment_likes")
public class CommentLike {

    // primary key id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fk user_id to match the user that likes this comment
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    //fk comment_id to match the like on the comment being liked
    @ManyToOne
    @JoinColumn(name = "comment_id")
    private  Comment comment;

    // this automatically sets the timestamp now
    private LocalDateTime createdAt = LocalDateTime.now();
}
