package com.makersacademy.acebook.controller;

import com.makersacademy.acebook.model.ChatMessage;
import com.makersacademy.acebook.model.User;
import com.makersacademy.acebook.repository.ChatMessageRepository;
import com.makersacademy.acebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    // DTOs
    record ChatMessageDTO(String content, String recipientUsername) {} // For Private
    record PublicMessageDTO(String content) {} // NEW: For Public (no recipient needed)

    // Output DTO
    record OutgoingChatMessageDTO(String content, String senderName, String recipientName, String timestamp) {}

    // ===========================
    // 1. GLOBAL / PUBLIC CHAT
    // ===========================

    @GetMapping("/global")
    public String getGlobalChat(Model model, Principal principal) {
        String currentUsername = getRealUsername(principal);
        model.addAttribute("myUsername", currentUsername);

        // Fetch messages where recipient is null
        List<ChatMessage> globalHistory = chatMessageRepository.findByRecipientIsNullOrderByTimestampAsc();
        model.addAttribute("history", globalHistory);

        // Pass users if you want a "Who's Online" list (optional)
        model.addAttribute("users", userRepository.findAll());

        return "global-chat"; // Points to global-chat.html
    }

    @MessageMapping("/public-message")
    public void processPublicMessage(@Payload PublicMessageDTO messageDto, Principal principal) {
        String senderUsername = getRealUsername(principal);
        User sender = userRepository.findUserByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        // Save with NULL recipient
        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .recipient(null)
                .content(messageDto.content())
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        OutgoingChatMessageDTO outgoingDto = new OutgoingChatMessageDTO(
                saved.getContent(),
                saved.getSender().getUsername(),
                "Global",
                saved.getTimestamp().toString()
        );

        // Broadcast to everyone subscribed to /topic/public
        messagingTemplate.convertAndSend("/topic/public", outgoingDto);
    }

    // ===========================
    // 2. PRIVATE 1-ON-1 CHAT
    // ===========================

    @GetMapping("/chat")
    public String getPrivateChatPage(@RequestParam(required = false) String username, Model model, Principal principal) {
        String currentUsername = getRealUsername(principal);
        model.addAttribute("myUsername", currentUsername);
        model.addAttribute("users", userRepository.findAll());

        if (username != null) {
            User sender = userRepository.findUserByUsername(currentUsername).orElseThrow();
            User recipient = userRepository.findUserByUsername(username).orElseThrow();

            model.addAttribute("selectedRecipient", username);
            model.addAttribute("history", chatMessageRepository.findConversation(sender, recipient));
        }
        return "chat"; // Points to chat.html (your existing file)
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

        messagingTemplate.convertAndSendToUser(recipient.getUsername(), "user/queue/messages", outgoingDto);
        messagingTemplate.convertAndSendToUser(principal.getName(), "user/queue/messages", outgoingDto);
        System.out.println("Sending to User: " + recipient.getUsername());
        System.out.println("Sending to Principal: " + principal.getName());
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