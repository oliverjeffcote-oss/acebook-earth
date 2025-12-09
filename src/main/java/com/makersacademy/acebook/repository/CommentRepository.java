package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CommentRepository extends CrudRepository<Comment, Long> {
    @Query("FROM Comment WHERE post = :post ORDER BY COALESCE(editedAt, createdAt) DESC")
    List<Comment> getCommentsNewestFirst(@Param("post") Post post);
}
