package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.List;

@Controller
public class PostsController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/posts")
    public String index(Model model) {
        Iterable<Post> posts = postRepository.findAll();
        model.addAttribute("posts", posts);
        model.addAttribute("post", new Post());
        return "posts/index";
    }

    @PostMapping("/posts")
    public RedirectView create(@ModelAttribute Post post) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        String email = (String) principal.getAttributes().get("email");
        User user = userRepository.findUserByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        post.setUser(user);
        postRepository.save(post);
        return new RedirectView("/posts");
    }

    @GetMapping("/posts/{id}")
    public String showPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        model.addAttribute("post", post);
        return "posts/showpost";
    }

//    @PostMapping("/posts")
//    public RedirectView create(@ModelAttribute Post post, Principal principal) {
//        String username = principal.getName();
//        User user = userRepository.findUserByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found - " + username));
//        post.setUser(user);
//        postRepository.save(post);
//        return new RedirectView("/posts");
//    }
}
