package com.makersacademy.acebook.controller;


import com.makersacademy.acebook.enums.Status;
import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.RelationshipRepository;
import com.makersacademy.acebook.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.List;

@Controller
public class RelationshipController {

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/requests/pending") // Note the simplified URL
    public ModelAndView userRequests(@AuthenticationPrincipal DefaultOidcUser principal) {

        // 1. Get the username from the principal's attributes
        String auth0Username = principal.getAttribute("https://myapp.com/username");

        // 2. Load the User entity from the database using the username
        // The current user is guaranteed to be logged in, so orElseThrow() is safe.
        User user = userRepository.findUserByUsername(auth0Username).orElseThrow();

        // The authorization check (if (!user.getId().equals(userId))) is now unnecessary,
        // as we are only acting on the user we just retrieved from the security context.

        // 3. Retrieve the pending requests
        List<Relationship> pendingRequests = relationshipRepository.findPendingRequests(user, Status.PENDING);
        List<User> requesters = pendingRequests.stream().map(Relationship::getRequester).toList();

        ModelAndView modelAndView = new ModelAndView("/users/requests");

        // Add data to the model
        modelAndView.addObject("requesters", requesters);
        modelAndView.addObject("pendingRequests", pendingRequests);
        modelAndView.addObject("user", user);

        return modelAndView;
    }

    @GetMapping("/users/{id}/friends")
    public ModelAndView seeFriendsOnUserProfile(@PathVariable("id") Long userId) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User user = userRepository.findUserByUsername(auth0Username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User profileOwner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        List<Relationship> friendships = relationshipRepository.findAllFriends(profileOwner.getId(), Status.ACCEPTED);
        List<User> friends = friendships.stream()
                .map(relationship -> {
                    if (relationship.getRequester().getId().equals(profileOwner.getId())) {
                        return relationship.getReceiver();
                    } else {
                        return relationship.getRequester();
                    }
                }).toList();

        ModelAndView modelAndView = new ModelAndView("/users/friends");

        modelAndView.addObject("friends", friends); // add the friend user accounts to page
        modelAndView.addObject("user", user); // add currently logged-in user
        modelAndView.addObject("profileOwner", profileOwner); // whose friends we're viewing

        return modelAndView;
    }

    @PostMapping("/users/{id}/requests/add")
    public RedirectView userPage(@PathVariable("id") Long receiverId) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User requester = userRepository.findUserByUsername(auth0Username)
                .orElseThrow(() -> new RuntimeException("Requester user not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver user not found"));

        //check if relationship record exists with both variants (requester and receiver ids)
        boolean relationshipExists = relationshipRepository.findByRequesterAndReceiver(requester, receiver).isPresent() ||
                relationshipRepository.findByRequesterAndReceiver(receiver, requester).isPresent();

        if (!relationshipExists) {
            Relationship friendRequest = new Relationship();
            friendRequest.setRequester(requester);
            friendRequest.setReceiver(receiver); //status is set to PENDING by default in the model
            relationshipRepository.save(friendRequest);
        }

        return new RedirectView("/users/" + receiverId);
    }

    @PostMapping("/users/{id}/requests/accept")
    public RedirectView requestAccepted(@PathVariable("id") Long requesterId, HttpServletRequest request) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User receiver = userRepository.findUserByUsername(auth0Username)
                .orElseThrow(() -> new RuntimeException("Receiver user not found"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester user not found"));

        Relationship relationship = relationshipRepository.findByRequesterAndReceiver(requester, receiver)
                .orElseThrow(() -> new RuntimeException("Pending friend request not found"));

        if (relationship.getStatus() == Status.PENDING) {
            relationship.setStatus(Status.ACCEPTED);
            relationshipRepository.save(relationship);
        }

        String referer = request.getHeader("Referer");
        return new RedirectView(referer != null ? referer : "/");
    }

    @PostMapping("/users/{id}/requests/reject")
    public RedirectView requestRejected(@PathVariable("id") Long requesterId, HttpServletRequest request) {

        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User receiver = userRepository.findUserByUsername(auth0Username)
                .orElseThrow(() -> new RuntimeException("Receiver user not found"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Receiver user not found"));

        Relationship relationship = relationshipRepository.findByRequesterAndReceiver(requester, receiver)
                .orElseThrow(() -> new RuntimeException("Pending friend request not found"));

        if (relationship.getStatus() == Status.PENDING) {
            relationshipRepository.delete(relationship);
        }

        String referer = request.getHeader("Referer");
        return new RedirectView(referer != null ? referer : "/");
    }

    @PostMapping("/users/{id}/remove")
    public RedirectView removeFriend(@PathVariable("id") Long profileUserId, @AuthenticationPrincipal DefaultOidcUser principal) {

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User user = userRepository.findUserByUsername(auth0Username)
                .orElseThrow();
        User profileUser = userRepository.findById(profileUserId)
                .orElseThrow(() -> new RuntimeException("Profile user not found"));

        Relationship relationship = relationshipRepository.findByRequesterAndReceiver(user, profileUser)
                .orElseGet(() -> relationshipRepository.findByRequesterAndReceiver(profileUser, user)
                        .orElseThrow(() -> new RuntimeException("Friendship not found")));

        if (relationship.getStatus() == Status.ACCEPTED) {
            relationshipRepository.delete(relationship);
        }

        return new RedirectView("/users/" + profileUserId);
    }

}

