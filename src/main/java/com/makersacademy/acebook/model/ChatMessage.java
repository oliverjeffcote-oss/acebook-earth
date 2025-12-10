package com.makersacademy.acebook.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The actual text
    @Column(nullable = false)
    private String content;

    // Email/ID of the person SENDING (taken from Principal)
    @ManyToOne
   @JoinColumn(name = "sender_id")
    private User sender;

    // Email/ID of the person RECEIVING
    @ManyToOne
    @JoinColumn(name ="recipient_id", nullable = true)
    private User recipient;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}