package com.SRHF.SRHF.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FAQController {

    @GetMapping({"/faq", "/faq.html"})
    public String faq() {
        return "faq"; // resolves to src/main/resources/templates/faq.html
    }
}
