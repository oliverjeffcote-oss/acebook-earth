package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Comment;
import com.makersacademy.acebook.model.Post;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.LikeRepository;
import com.makersacademy.acebook.repository.CommentRepository;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.UserRepository;
import com.makersacademy.acebook.services.S3Service;
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

    @Autowired
    private S3Service s3Service;

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

    private String saveImageToUploads(MultipartFile imageFile) {
        try {
            String uploadDir = "uploads";
            Files.createDirectories(Paths.get(uploadDir));

            String originalFilename = imageFile.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID() + extension;

            Path destination = Paths.get(uploadDir).resolve(filename);
            Files.copy(imageFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save uploaded image", e);
        }
    }

    private void deleteFileFromUploads(String imagePath) {
        try {
            // Convert "/uploads/abc.jpg" → "uploads/abc.jpg"
            String relativePath = imagePath.replaceFirst("/", "");
            Path filePath = Paths.get(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image file", e);
        }
    }



    @PostMapping("/posts")
    public RedirectView create(@ModelAttribute Post post,
                                @RequestParam(value = "image", required = false)
                                MultipartFile imageFile) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        String username = (String) principal.getAttributes().get("https://myapp.com/username");
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        post.setUser(user);

        // Optional image upload handling
        if (imageFile != null && !imageFile.isEmpty()) {
            String uploadDir = "uploads"; // Folder relative to project root

            try {
                String newPath = s3Service.uploadImage(imageFile);
                post.setImagePath(newPath);
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
            @RequestParam("content") String updateContent,
            @RequestParam(value = "removeImage", required = false) String removeImage,
            @RequestParam(value = "image", required = false) MultipartFile newImage
    ) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setContent(updateContent);
        post.setEditedAt(LocalDateTime.now());
        // If user checks "Remove image"
        if (removeImage != null && post.getImagePath() != null) {
            deleteFileFromUploads(post.getImagePath());
            post.setImagePath(null);
        }
        // If user uploads a new image
        if (newImage != null && !newImage.isEmpty()) {
            // Delete old image if it exists
            if (post.getImagePath() != null) {
                deleteFileFromUploads(post.getImagePath());
            }
            // Save new image
            try {
                String newPath = s3Service.uploadImage(newImage);
                post.setImagePath(newPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update image file.", e);
            }
        }
        postRepository.save(post);
        return new RedirectView("/posts");
    }

    @PostMapping("/posts/{id}/delete")
    public RedirectView deletePost(@PathVariable Long id) {
        Post post = postRepository.findById(id).orElseThrow();
        if (post.getImagePath() != null) {
            deleteFileFromUploads(post.getImagePath());
        }

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