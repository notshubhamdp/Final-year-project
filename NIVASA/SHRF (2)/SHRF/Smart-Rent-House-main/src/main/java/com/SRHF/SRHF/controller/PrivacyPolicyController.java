package com.SRHF.SRHF.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrivacyPolicyController {

    @GetMapping({"/privacy-policy", "/privacy-policy.html"})
    public String privacy() {
        return "privacy-policy"; // resolves to src/main/resources/templates/privacy-policy.html
    }
}
