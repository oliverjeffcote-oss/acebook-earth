package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.swing.*;

@Controller
public class CommentController {

        @Autowired
        CommentRepository commentRepository;

        @Autowired
        PostRepository postRepository;

        @Autowired
        UserRepository userRepository;

        @PostMapping("/posts/{id}/comment")
        public RedirectView createComment(
                @PathVariable("id") Long postId,
                @RequestParam("content") String content
        ) {
            DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            String email = (String) principal.getAttributes().get("email");

            User user = userRepository.findUserByUsername(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new RuntimeException("Post not found"));

            Comment comment = new Comment(content);
            comment.setUser(user);
            comment.setPost(post);

            commentRepository.save(comment);
            return new RedirectView("/posts/" + postId);

        }

    }
