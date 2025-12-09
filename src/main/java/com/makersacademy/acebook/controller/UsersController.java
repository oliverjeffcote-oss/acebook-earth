package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
public class UsersController {
    @Autowired
    UserRepository userRepository;

    @GetMapping("/users/after-login")
    public RedirectView afterLogin() {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String username = (String) principal.getAttributes().get("email");
        userRepository
                .findUserByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username)));

        return new RedirectView("/posts");
    }

    @GetMapping("/user/profile")
    public String userProfile(Model model) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
               .getContext()
               .getAuthentication()
               .getPrincipal();

        String email = (String) principal.getAttributes().get("email");

        User user = userRepository.findUserByUsername(email)
               .orElseThrow(() -> new RuntimeException("User not found: " + email));

        model.addAttribute("user", user);
        return "users/userprofile"; // your template
}

    @GetMapping("/user/edit")
    public String editUserProfileForm(Model model) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String email = (String) principal.getAttributes().get("email");

        User user = userRepository.findUserByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        model.addAttribute("user", user);
        return "users/edituser"; // your edit template
    }

    @PostMapping("/user/edit")
    public RedirectView updateUserProfile(
            @ModelAttribute("user") User formUser,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String email = (String) principal.getAttributes().get("email");

        // Always load the real user from the DB, don't trust IDs from the form
        User user = userRepository.findUserByUsername(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

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
        return new RedirectView("/user/profile");
    }

}
