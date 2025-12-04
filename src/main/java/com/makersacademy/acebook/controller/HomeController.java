package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class HomeController {

    @Autowired
    private UserRepository userRepository;

    @RequestMapping(value = "/")
    public RedirectView index(@AuthenticationPrincipal OidcUser principal) {
        // 1. If the user is logged in (principal is not null)
        if (principal != null) {
            String auth0Id = principal.getSubject();
            String email = principal.getEmail();
            // Fallback to nickname if given name is null (common in some Auth0 setups)
            String username = principal.getGivenName() != null ? principal.getGivenName() : principal.getNickName();

            // 2. Check if user exists, if not, create them
            User user = userRepository.findByAuth0Id(auth0Id).orElse(null);

            if (user == null) {
                user = new User();
                user.setAuth0Id(auth0Id);
                user.setUsername(email);
                userRepository.save(user);
            }
        }

        // 3. Continue to posts
        return new RedirectView("/");
    }
}