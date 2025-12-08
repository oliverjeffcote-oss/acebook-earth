package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Like;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.LikeRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class PostsController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/posts")
    public String index(@RequestParam(defaultValue="0") int page,Model model) {
        Pageable pageable = PageRequest.of(page, 10); ;
        Page<Post> posts = postRepository.getPostsNewestFirst(pageable);
        model.addAttribute("posts", posts.getContent());
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
