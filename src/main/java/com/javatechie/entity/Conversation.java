package com.javatechie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    public Conversation(Long id, String prompt) {
        this.id = id;
        Prompt = prompt;
    }

    public Conversation(Client client, String prompt, UserResponse response) {
        this.client = client;
        Prompt = prompt;
        Response = response;
    }

    @ManyToOne
    @JoinColumn(name = "client_id")
    Client client;

    String Prompt;

    @ManyToOne
    @JoinColumn(name = "response_id")
    UserResponse Response;
}
