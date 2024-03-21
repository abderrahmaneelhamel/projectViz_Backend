package com.javatechie.repository;

import com.javatechie.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    @Query("SELECT new Conversation(c.id,c.Prompt,c.Response) FROM Conversation c WHERE c.user.id = :UserId")
    List<Conversation> findByUserId(Integer UserId);

    @Query("SELECT new Conversation(c.id,c.Prompt) FROM Conversation c WHERE c.user.id = :UserId")
    List<Conversation> findByUserIdLite(Integer UserId);

}