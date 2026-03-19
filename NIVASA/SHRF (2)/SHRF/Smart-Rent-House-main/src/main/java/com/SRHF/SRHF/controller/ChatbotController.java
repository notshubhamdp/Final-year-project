package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestParam(name = "q", required = false) String query) {
        ChatbotService.ChatbotReply reply = chatbotService.reply(query);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("answer", reply.getAnswer());
        body.put("suggestions", reply.getSuggestions());
        return ResponseEntity.ok(body);
    }
}

