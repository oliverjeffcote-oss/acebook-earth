package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.RelationshipRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class UsersController {
    @Autowired
    UserRepository userRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

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

    @GetMapping("/users/{id}")
    public ModelAndView userProfile(@PathVariable("id") Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Invalid User Id:" + id));

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User loggedInUser = userRepository.findUserByUsername(auth0Username).orElseThrow();

        Relationship relationship = relationshipRepository.findByRequesterAndReceiver(loggedInUser, user)
                .orElseGet(() -> relationshipRepository.findByRequesterAndReceiver(user, loggedInUser).orElse(null));

        ModelAndView modelAndView = new ModelAndView("/users/userprofile");
        modelAndView.addObject("user", user); //the user object for the profile you are looking at (profile owner)
        modelAndView.addObject("loggedInUser", loggedInUser);
        modelAndView.addObject("relationship", relationship);
        return modelAndView;
    }


    @GetMapping("/users/profile")
    public RedirectView currentUserProfile() {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User user = userRepository.findUserByUsername(auth0Username).orElseThrow();

        return new RedirectView("/users/" + user.getId());
    }
}