package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.service.ChatbotService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FAQController {

    private final ChatbotService chatbotService;

    public FAQController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping({"/faq", "/faq.html"})
    public String faq(Model model) {
        model.addAttribute("faqs", chatbotService.getFaqCatalog());
        model.addAttribute("supportEmail", chatbotService.getSupportEmail());
        return "faq";
    }
}
