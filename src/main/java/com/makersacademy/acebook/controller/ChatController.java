package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.ChatMessage;
import com.makersacademy.acebook.model.Relationship;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.ChatMessageRepository;
import com.makersacademy.acebook.repository.RelationshipRepository;
import com.makersacademy.acebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final  SimpMessagingTemplate messagingTemplate;
    @Autowired
    ChatMessageRepository chatMessageRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

    // DTOs
    record ChatMessageDTO(String content, String recipientUsername) {} // For Private
    record OutgoingChatMessageDTO(String content, String senderName, String recipientName, String timestamp) {}

    // ===========================
    //    PRIVATE 1-ON-1 CHAT
    // ===========================

    @GetMapping("/chat")
    public String getPrivateChatPage(@RequestParam(required = false) String username, Model model, Principal principal) {
        String currentUsername = getRealUsername(principal);

        model.addAttribute("myUsername", currentUsername);

        User currentUserObj = userRepository.findUserByUsername(currentUsername).orElseThrow();


        List<Relationship> friendships = relationshipRepository.findAllFriends(currentUserObj.getId(), com.makersacademy.acebook.enums.Status.ACCEPTED);
        // Filter: If I am the Requester, return the Receiver. If I am the Receiver, return the Requester.

        List<User> availableContacts = friendships.stream()
                .map(rel -> rel.getRequester().getId().equals(currentUserObj.getId()) ? rel.getReceiver() : rel.getRequester())
                .toList();

        model.addAttribute("users", availableContacts);

        if (username != null) {
            User sender = userRepository.findUserByUsername(currentUsername).orElseThrow();
            User recipient = userRepository.findUserByUsername(username).orElseThrow();

            model.addAttribute("selectedRecipient", username);
            model.addAttribute("history", chatMessageRepository.findConversation(sender, recipient));
        }
        return "chat/chat"; // Points to chat.html (your existing file)
    }

    @MessageMapping("/private-message")
    public void processPrivateMessage(@Payload ChatMessageDTO messageDto, Principal principal) {
        String senderUsername = getRealUsername(principal);
        User sender = userRepository.findUserByUsername(senderUsername).orElseThrow();
        User recipient = userRepository.findUserByUsername(messageDto.recipientUsername()).orElseThrow();

        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(messageDto.content())
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        OutgoingChatMessageDTO outgoingDto = new OutgoingChatMessageDTO(
                saved.getContent(),
                saved.getSender().getUsername(),
                saved.getRecipient().getUsername(),
                saved.getTimestamp().toString()

        );

        messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/messages", outgoingDto);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/messages", outgoingDto);

    }

    // Helper
    private String getRealUsername(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof DefaultOidcUser oidcUser) {
            String customName = (String) oidcUser.getAttributes().get("https://myapp.com/username");
            if (customName != null && !customName.isEmpty()) return customName;
        }
        return principal.getName();
    }
}