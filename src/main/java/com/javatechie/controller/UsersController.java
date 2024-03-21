package com.javatechie.controller;

import com.javatechie.Service.UsersService;
import com.javatechie.auth.user.User;
import com.javatechie.auth.user.UserRepository;
import com.javatechie.entity.Conversation;
import com.javatechie.repository.ConversationRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UsersController {

    private final UsersService usersService;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @Autowired
    public  UsersController(UsersService usersService, UserRepository userRepository, ConversationRepository conversationRepository) {
        this.usersService = usersService;
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<Conversation>> getConversations(@PathVariable Integer userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        return new ResponseEntity<>(conversations, HttpStatus.OK);
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity deleteConversation(@PathVariable Integer conversationId) {
        conversationRepository.deleteById(conversationId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/update-plan")
    public ResponseEntity updatePlan(@RequestBody Map<String, String> credentials) {
        try {
            Integer UserId = Integer.valueOf(credentials.get("userId"));
            Long planId = Long.valueOf(credentials.get("planId"));
            String cardToken = credentials.get("cardToken");

            Boolean success = usersService.updatePlan(UserId, planId, cardToken);

            if (success) {
                return ResponseEntity.ok(planId);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User or plan not found");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input format");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating plan");
        }
    }
}
