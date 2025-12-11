package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.RelationshipRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.UUID;

@Controller
public class UsersController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

    // ---------------------------------------------------------------------
    // After login: ensure User exists, then go to posts
    // ---------------------------------------------------------------------
    @GetMapping("/users/after-login")
    public RedirectView afterLogin() {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");
        String userEmail = principal.getEmail();

        userRepository
                .findUserByUsername(auth0Username)
                .orElseGet(() -> userRepository.save(new User(auth0Username, userEmail)));

        return new RedirectView("/posts");
    }

    // ---------------------------------------------------------------------
    // Search bar functionality
    // ---------------------------------------------------------------------
    @GetMapping("/users/search")
    public String searchUsers(@RequestParam(name = "query", required = false) String query,
                              Model model) {

        // Handle empty / missing query gracefully
        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("query", "");
            model.addAttribute("results", java.util.Collections.emptyList());
            return "users/searchresults";
        }

        String trimmedQuery = query.trim();

        // Search by username (case-insensitive)
        var results = userRepository.findByUsernameContainingIgnoreCase(trimmedQuery);

        model.addAttribute("query", trimmedQuery);
        model.addAttribute("results", results);

        return "users/searchresults";
    }


    // ---------------------------------------------------------------------
    // View SOMEONE ELSE'S profile by id: /users/{id}
    // ---------------------------------------------------------------------
    @GetMapping("/users/{id}")
    public ModelAndView userProfile(@PathVariable("id") Long id) {
        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            ModelAndView errorView = new ModelAndView("errors/usernotfound");
            errorView.addObject("message", "The requested user does not exist.");
            return errorView;
        }

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User loggedInUser = userRepository.findUserByUsername(auth0Username)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + auth0Username));

        Relationship relationship = relationshipRepository.findByRequesterAndReceiver(loggedInUser, user)
                .orElseGet(() -> relationshipRepository
                        .findByRequesterAndReceiver(user, loggedInUser)
                        .orElse(null));

        ModelAndView modelAndView = new ModelAndView("users/userprofile");
        modelAndView.addObject("user", user);           // profile owner
        modelAndView.addObject("loggedInUser", loggedInUser);
        modelAndView.addObject("relationship", relationship);
        return modelAndView;
    }

    // ---------------------------------------------------------------------
    // View MY OWN profile: /users/profile
    // ---------------------------------------------------------------------
    @GetMapping("/users/profile")
    public String myProfile(Model model) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");
        String userEmail = principal.getEmail();

        User user = userRepository
                .findUserByUsername(auth0Username)
                .orElseGet(() -> userRepository.save(new User(auth0Username, userEmail)));

        model.addAttribute("user", user);
        model.addAttribute("loggedInUser", user);   // you are the logged-in user
        model.addAttribute("relationship", null);   // no friendship with yourself
        return "users/userprofile";
    }

    // ---------------------------------------------------------------------
    // Edit profile (form): /users/edit  [GET]
    // ---------------------------------------------------------------------
    @GetMapping("/users/edit")
    public String editUserProfileForm(Model model) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");
        String userEmail = principal.getEmail();

        User user = userRepository
                .findUserByUsername(auth0Username)
                .orElseGet(() -> userRepository.save(new User(auth0Username, userEmail)));

        model.addAttribute("user", user);
        return "users/edituser";
    }

    // ---------------------------------------------------------------------
    // Edit profile (submit): /users/edit  [POST]
    // ---------------------------------------------------------------------
    @PostMapping("/users/edit")
    public RedirectView updateUserProfile(
            @ModelAttribute("user") User formUser,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");
        String userEmail = principal.getEmail();

        // Always load the real user from the DB, don't trust IDs from the form
        User user = userRepository
                .findUserByUsername(auth0Username)
                .orElseGet(() -> userRepository.save(new User(auth0Username, userEmail)));

        // Copy editable fields from the form object into the real user
        user.setDescription(formUser.getDescription());
        user.setBirthday(formUser.getBirthday());
        user.setLocation(formUser.getLocation());
        user.setEducation(formUser.getEducation());
        user.setBirthplace(formUser.getBirthplace());
        user.setEmployment(formUser.getEmployment());

        // Handle optional new profile image upload
        if (imageFile != null && !imageFile.isEmpty()) {
            String uploadDir = "uploads";

            try {
                Files.createDirectories(Paths.get(uploadDir));

                String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
                String extension = "";
                int dotIndex = originalFilename.lastIndexOf('.');
                if (dotIndex >= 0) {
                    extension = originalFilename.substring(dotIndex); // includes the dot
                }

                String filename = UUID.randomUUID().toString() + extension;
                Path destination = Paths.get(uploadDir).resolve(filename);

                Files.copy(imageFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

                // Store web path so Thymeleaf can render it
                user.setImagePath("/uploads/" + filename);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store profile image", e);
            }
        }

        // Save updated user
        userRepository.save(user);

        // Back to profile page
        return new RedirectView("/users/profile");
    }
}
