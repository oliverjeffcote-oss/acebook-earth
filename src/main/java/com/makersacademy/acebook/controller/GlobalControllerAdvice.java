package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserRepository userRepository;

    public GlobalControllerAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute
    public void addLoggedInUser(Model model) {
        try {
            Object principalObj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principalObj instanceof DefaultOidcUser principal) {
                String username = (String) principal.getAttributes().get("https://myapp.com/username");
                User loggedInUser = userRepository.findUserByUsername(username)
                        .orElse(null);
                model.addAttribute("loggedInUser", loggedInUser);
            }
        } catch (Exception ignored) {
            // No logged-in user, do nothing
        }
    }
}
