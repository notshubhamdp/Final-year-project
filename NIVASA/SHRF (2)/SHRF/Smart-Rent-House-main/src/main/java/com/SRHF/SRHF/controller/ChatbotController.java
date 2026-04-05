package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
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
        body.put("matched", reply.isMatched());
        body.put("supportEmail", reply.getSupportEmail());
        body.put("matchedQuestion", reply.getMatchedQuestion());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/faqs")
    public ResponseEntity<Map<String, Object>> faqs() {
        List<Map<String, Object>> faqItems = chatbotService.getFaqCatalog().stream()
                .map(faq -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("question", faq.getQuestion());
                    item.put("answer", faq.getAnswer());
                    return item;
                })
                .toList();

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("supportEmail", chatbotService.getSupportEmail());
        body.put("faqs", faqItems);
        return ResponseEntity.ok(body);
    }
}
