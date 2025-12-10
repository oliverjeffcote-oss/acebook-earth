package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.CommentLike;
import com.makersacademy.acebook.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CommentLikeRepository extends CrudRepository<CommentLike, Long> {

    long countByComment(Comment comment);
    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    void deleteByUserAndComment(User user, Comment comment);
}
