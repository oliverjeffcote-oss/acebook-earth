package com.makersacademy.acebook.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.security.core.Principal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // CRITICAL: Override how the user is determined during handshake
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic","/user", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Inner class to align WebSocket Identity with Database Username
     */
    private static class CustomHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
            Principal principal = request.getPrincipal();

            if (principal instanceof OAuth2AuthenticationToken token) {
                // EXTRACT THE EXACT SAME ATTRIBUTE AS YOUR POSTS CONTROLLER
                String username = (String) token.getPrincipal().getAttributes().get("https://myapp.com/username");

                // Fallback if the custom attribute is missing
                if (username == null) {
                    username = token.getName();
                }

                final String finalUsername = username;

                // Return a simple Principal object wrapping the username
                return new Principal() {
                    @Override
                    public String getName() {
                        return finalUsername;
                    }
                };
            }
            return principal;
        }
    }
}