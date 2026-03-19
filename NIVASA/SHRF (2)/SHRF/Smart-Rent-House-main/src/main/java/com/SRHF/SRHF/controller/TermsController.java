package com.SRHF.SRHF.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TermsController {

    @GetMapping({"/terms", "/terms.html"})
    public String terms() {
        return "terms"; // resolves to src/main/resources/templates/terms.html
    }
}
