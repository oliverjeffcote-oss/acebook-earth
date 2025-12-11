package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.RelationshipRepository;
import com.makersacademy.acebook.repository.UserRepository;
import com.makersacademy.acebook.services.S3Service;
import com.makersacademy.acebook.repository.PostRepository;
import com.makersacademy.acebook.repository.LikeRepository;
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
import java.util.Locale;
import java.util.UUID;

import com.makersacademy.acebook.enums.Status;
import com.makersacademy.acebook.model.Relationship;
import org.springframework.security.core.Authentication;


@Controller
public class UsersController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeRepository likeRepository;

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
    public String searchUsers(
            @RequestParam(name = "query", required = false) String query,
            Model model
    ) {

        // Handle empty / missing query gracefully
        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("query", "");
            model.addAttribute("results", java.util.Collections.emptyList());
            model.addAttribute("suggestedUsers", java.util.Collections.emptyList());
            return "users/searchresults";
        }

        String trimmedQuery = query.trim();

        // Search by username (case-insensitive)
        var results = userRepository.findByUsernameContainingIgnoreCase(trimmedQuery);

        model.addAttribute("query", trimmedQuery);
        model.addAttribute("results", results);

        // ----- Suggested close matches (for typos / variations) -----
        java.util.List<User> suggestedUsers = new java.util.ArrayList<>();

        // Only bother with suggestions if we got no direct matches
        if (results.isEmpty()) {
            String lowerQuery = trimmedQuery.toLowerCase();

            for (User candidate : userRepository.findAll()) {
                if (candidate.getUsername() == null) continue;

                String candidateName = candidate.getUsername().toLowerCase();

                // Skip exact matches
                if (candidateName.equals(lowerQuery)) continue;

                int distance = levenshteinDistance(lowerQuery, candidateName);

                // Very small distance => good suggestion
                if (distance > 0 && distance <= 2) {
                    suggestedUsers.add(candidate);
                }
            }
        }

        model.addAttribute("suggestedUsers", suggestedUsers);

        // ----- Friendship info (only if authenticated) -----
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof DefaultOidcUser principal) {

            String auth0Username = principal.getAttribute("https://myapp.com/username");

            User loggedInUser = userRepository.findUserByUsername(auth0Username)
                    .orElseThrow(() -> new RuntimeException("Logged-in user not found: " + auth0Username));

            // Map of userId -> status string
            java.util.Map<Long, String> friendshipStatuses = new java.util.HashMap<>();

            for (User u : results) {
                // Mark yourself
                if (u.getId().equals(loggedInUser.getId())) {
                    friendshipStatuses.put(u.getId(), "SELF");
                    continue;
                }

                // Check relationship in both directions
                Relationship relationship = relationshipRepository
                        .findByRequesterAndReceiver(loggedInUser, u)
                        .orElseGet(() -> relationshipRepository
                                .findByRequesterAndReceiver(u, loggedInUser)
                                .orElse(null));

                if (relationship == null) {
                    friendshipStatuses.put(u.getId(), "NONE");
                } else if (relationship.getStatus() == Status.ACCEPTED) {
                    friendshipStatuses.put(u.getId(), "FRIENDS");
                } else if (relationship.getStatus() == Status.PENDING) {
                    // Distinguish who sent the request
                    if (relationship.getRequester().getId().equals(loggedInUser.getId())) {
                        friendshipStatuses.put(u.getId(), "OUTGOING_PENDING");
                    } else {
                        friendshipStatuses.put(u.getId(), "INCOMING_PENDING");
                    }
                } else {
                    friendshipStatuses.put(u.getId(), "NONE");
                }
            }

            model.addAttribute("friendshipStatuses", friendshipStatuses);
            model.addAttribute("loggedInUser", loggedInUser);
        }

        return "users/searchresults";
    }

    /**
     * Simple Levenshtein distance implementation for fuzzy username matching.
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,      // deletion
                                dp[i][j - 1] + 1       // insertion
                        ),
                        dp[i - 1][j - 1] + cost      // substitution
                );
            }
        }

        return dp[a.length()][b.length()];
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
// This will give a count for posts
        long postCount = postRepository.countByUser(user);
//      This will give a count for likes received on all posts owned by user
        long likesCount = likeRepository.countLikesReceivedByUser(user);


        model.addAttribute("user", user);
        model.addAttribute("loggedInUser", user);   // you are the logged-in user
        model.addAttribute("relationship", null);   // no friendship with yourself
        model.addAttribute("postCount", postCount);
        model.addAttribute("likesCount", likesCount);
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
            try {
                String originalProfileImage = user.getImagePath();
                String filename = s3Service.uploadImage(imageFile);
                user.setImagePath(filename);
                s3Service.deleteImage(originalProfileImage);
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
