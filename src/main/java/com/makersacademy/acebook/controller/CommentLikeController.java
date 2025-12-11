package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.CommentLike;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentLikeRepository;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Optional;

@Controller
public class CommentLikeController {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    // like a comment
    @PostMapping("/comments/{commentId}/like")
    public RedirectView likeComment(@PathVariable Long commentId) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String username = (String) principal.getAttributes().get("https://myapp.com/username");

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));


        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));


        Optional<CommentLike> existing =
                commentLikeRepository.findByUserAndComment(user, comment);

        if (existing.isEmpty()) {
            CommentLike like = new CommentLike();
            like.setUser(user);
            like.setComment(comment);
            commentLikeRepository.save(like);
        }

        return new RedirectView("/posts/" + comment.getPost().getId());
    }

    // unlike a comment
    @PostMapping("/comments/{commentId}/unlike")
    public RedirectView unlikeComment(@PathVariable Long commentId) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String email = (String) principal.getAttributes().get("https://myapp.com/username");

        User user = userRepository.findUserByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        commentLikeRepository.findByUserAndComment(user, comment)
                .ifPresent(commentLikeRepository::delete);

        return new RedirectView("/posts/" + comment.getPost().getId());
    }
}
