package com.javatechie.entity;

import com.javatechie.auth.user.User;
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

    public Conversation(User user, String prompt, UserResponse response) {
        this.user = user;
        Prompt = prompt;
        Response = response;
    }
    public Conversation(Long id,String prompt, UserResponse response) {
        this.id = id;
        Prompt = prompt;
        Response = response;
    }

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    String Prompt;

    @ManyToOne
    @JoinColumn(name = "response_id")
    UserResponse Response;
}
