package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Like;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.LikeRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;

@Controller
public class LikeController {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/posts/{postId}/likes")
    public RedirectView likePost(@PathVariable Long postId, Principal principal) {
        if (principal == null) {
            return new RedirectView("/login");
        }

        String username = principal.getName();
//        String username = (String) principal.getAttributes().get("email");

        User user = userRepository.findById(1L) // if not working hardcode user id for now
                .orElseThrow(() -> new RuntimeException("Id not found: " + username));

        Post post = postRepository.findById(postId)
              .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        boolean alreadyLiked = likeRepository.findByPostAndUser(post, user).isPresent();
        if (!alreadyLiked) {
            Like like = new Like();
            like.setPost(post);
            like.setUser(user);
            likeRepository.save(like);
        }

        return new RedirectView("/posts");
    }

    @PostMapping("/posts/{postId}/likes/unlike")
    public RedirectView unlikePost(@PathVariable Long postId, Principal principal) {
        if (principal == null) {
            return new RedirectView("/login");
        }

        String username = principal.getName();
//        String username = (String) principal.getAttributes().get("email");

        User user = userRepository.findById(1L) // if not working hardcode user id for now
                .orElseThrow(() -> new RuntimeException("Id not found: " + username));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

        likeRepository.findByPostAndUser(post, user)
                .ifPresent(likeRepository::delete);

        return new RedirectView("/posts");
    }
}
