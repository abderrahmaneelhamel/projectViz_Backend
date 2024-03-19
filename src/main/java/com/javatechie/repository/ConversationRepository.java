package com.javatechie.repository;

import com.javatechie.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    @Query("SELECT c FROM Conversation c WHERE c.client.id = :ClientId")
    List<Conversation> findByClientId(Integer ClientId);

    @Query("SELECT new Conversation(c.id,c.Prompt) FROM Conversation c WHERE c.client.id = :ClientId")
    List<Conversation> findByClientIdLite(Integer ClientId);
}