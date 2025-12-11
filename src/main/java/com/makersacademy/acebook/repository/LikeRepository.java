package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.model.Like;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepository extends CrudRepository<Like, Long> {

    long countByPost(Post post);
    Optional<Like> findByPostAndUser(Post post, User user);

    void deleteByPostAndUser(Post post, User user);

    // this will count all likes across all posts created by the user
    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.user = :user")
    long countLikesReceivedByUser(@Param("user") User user);
}
