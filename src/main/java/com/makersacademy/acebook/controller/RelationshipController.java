package com.makersacademy.acebook.controller;


import com.makersacademy.acebook.enums.Status;
import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.RelationshipRepository;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
public class RelationshipController {

    @Autowired
    private RelationshipRepository relationshipRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users/{id}/requests/pending")
    public ModelAndView userRequests(@PathVariable("id") Long userId) {
        DefaultOidcUser principal = (DefaultOidcUser) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String auth0Username = principal.getAttribute("https://myapp.com/username");

        User user = userRepository.findUserByUsername(auth0Username).orElseThrow();

        if (!user.getId().equals(userId)) {
            return new ModelAndView("/posts");
        }

        List<Relationship> pendingRequests = relationshipRepository.findPendingRequests(user, Status.PENDING);
        List<User> requesters = pendingRequests.stream().map(Relationship::getRequester).toList();

        ModelAndView modelAndView = new ModelAndView("/users/friend-requests");

        modelAndView.addObject("requesters", requesters); // to see person requesting
        modelAndView.addObject("pendingRequests", pendingRequests); // so we can have accept/reject buttons
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

//        modelAndView.addObject("friendships", friendships); // add the relationships - not needed?
        modelAndView.addObject("friends", friends); // add the friend user accounts to page
        modelAndView.addObject("user", user); // add currently logged in user

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
    public RedirectView requestAccepted(@PathVariable("id") Long requesterId) {

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

        return new RedirectView("/users/" + requesterId);
    }

    @PostMapping("/users/{id}/requests/reject")
    public RedirectView requestRejected(@PathVariable("id") Long requesterId) {

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

        return new RedirectView("/users/" + requesterId);
    }
}

