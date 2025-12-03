package com.makersacademy.acebook.model;


import com.makersacademy.acebook.enums.Status;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name = "RELATIONSHIPS")
public class Relationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who started the friendship (Requester)
    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    // The user who received the friendship (Receiver)
    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Enumerated(EnumType.STRING)
    private Status status;


}
