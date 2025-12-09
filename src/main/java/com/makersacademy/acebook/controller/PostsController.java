package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import jakarta.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
public class PostsController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @GetMapping("/posts")
//    public String index(@RequestParam(defaultValue="0") int page,Model model) {
//        Pageable pageable = PageRequest.of(page, 10); ;
//        Page<Post> posts = postRepository.getPostsNewestFirst(pageable);
//        model.addAttribute("posts", posts.getContent());
//        model.addAttribute("post", new Post());
//        return "posts/index";
//    }

    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Post> posts = postRepository.getPostsNewestFirst(pageable);
        model.addAttribute("posts", posts.getContent());
        model.addAttribute("post", new Post());
        model.addAttribute("page", posts);         // Provides page.first, page.last
        model.addAttribute("currentPage", page);// for highlighting active page
        return "posts/index";
    }

    @PostMapping("/posts")
    public RedirectView create(@ModelAttribute Post post,
                                @RequestParam(value = "image", required = false)
                                MultipartFile imageFile) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        String email = (String) principal.getAttributes().get("email");
        User user = userRepository.findUserByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        post.setUser(user);

        // Optional image upload handling
        if (imageFile != null && !imageFile.isEmpty()) {
            String uploadDir = "uploads"; // Folder relative to project root

            try {
                // Ensures upload directory exists
                Files.createDirectories(Paths.get(uploadDir));

                String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
                String extension = "";

                int dotIndex = originalFilename.lastIndexOf('.');
                if(dotIndex >= 0) {
                    extension = originalFilename.substring(dotIndex); // Includes the dot
                }

                // Generate a unique filename to avoid collisions
                String filename = UUID.randomUUID().toString() + extension;
                Path destination = Paths.get(uploadDir).resolve(filename);

                // Save the file to disk
                Files.copy(imageFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Store the web accessible path in the post
                post.setImagePath("/uploads/" + filename);

            } catch (IOException e) {
                throw new RuntimeException("Failed to store uploaded file.", e);
            }
        }

        postRepository.save(post);
        return new RedirectView("/posts");
    }

    @GetMapping("/posts/{id}")
    public String showPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        List<Comment> comments = commentRepository.getCommentsNewestFirst(post);
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        return "posts/showpost";
    }

    @GetMapping("/posts/{id}/edit")
    public String editPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        model.addAttribute("post", post);
        return "posts/edit";
    }

    @PostMapping("/posts/{id}/update")
    public RedirectView updatePost(
            @PathVariable Long id,
            @RequestParam("content") String updateContent
    ) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setContent(updateContent);
        post.setEditedAt(LocalDateTime.now());
        postRepository.save(post);
        return new RedirectView("/posts");
    }

    @PostMapping("/posts/{id}/delete")
    public RedirectView deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
        return new RedirectView("/posts");
    }

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
//}