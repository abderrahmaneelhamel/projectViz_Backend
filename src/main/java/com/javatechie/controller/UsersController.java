package com.javatechie.controller;

import com.javatechie.Service.UsersService;
import com.javatechie.entity.Client;
import com.javatechie.entity.Conversation;
import com.javatechie.repository.ClientRepository;
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
    private final ClientRepository clientRepository;
    private final ConversationRepository conversationRepository;

    @Autowired
    public  UsersController(UsersService usersService, ClientRepository clientRepository, ConversationRepository conversationRepository) {
        this.usersService = usersService;
        this.clientRepository = clientRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    @GetMapping("/conversations/{clientId}")
    public ResponseEntity<List<Conversation>> getConversations(@PathVariable Integer clientId) {
        List<Conversation> conversations = conversationRepository.findByClientId(clientId);
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
            Long clientId = Long.valueOf(credentials.get("clientId"));
            Long planId = Long.valueOf(credentials.get("planId"));
            String cardToken = credentials.get("cardToken");

            Client updatedClient = usersService.updatePlan(clientId, planId, cardToken);

            if (updatedClient != null) {
                return ResponseEntity.ok(updatedClient);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("client or plan not found");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid input format");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating plan");
        }
    }

    @GetMapping("client/{clientId}")
    public ResponseEntity getClientById(@PathVariable Long clientId) {
        try {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client != null) {
                System.out.println("succsess : "+client);
                return ResponseEntity.ok(client);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving client");
        }
    }
}
