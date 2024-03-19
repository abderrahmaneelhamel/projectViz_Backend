package com.javatechie.controller;

import com.javatechie.Exceptions.UpgradeplanException;
import com.javatechie.Service.ChatService;
import com.javatechie.dto.ChatGPTPrompt;
import com.javatechie.entity.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/bot")
public class ChatController {

    @Autowired
    ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity chat(@RequestBody ChatGPTPrompt chatGPTPrompt) {
        try {
            UserResponse response = chatService.processProjectDescription(chatGPTPrompt);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (UpgradeplanException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("An internal server error occurred: " + ex.getMessage());
        }

    }
}
