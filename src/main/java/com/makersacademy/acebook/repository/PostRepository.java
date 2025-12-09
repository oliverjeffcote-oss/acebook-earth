package com.makersacademy.acebook.repository;

import com.makersacademy.acebook.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface PostRepository extends CrudRepository<Post, Long> {
    @Query("FROM Post ORDER BY COALESCE(editedAt, createdAt) DESC")
    Page<Post> getPostsNewestFirst(Pageable pageable);
//    coalesce ensures new posts appear on top
//    also if posts are edited, they take precedence to appear on top
//    never edited posts appear sorted
}
