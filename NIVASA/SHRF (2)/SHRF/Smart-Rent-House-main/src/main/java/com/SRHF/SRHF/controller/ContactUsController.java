package com.SRHF.SRHF.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContactUsController {

    @GetMapping({"/contact-us", "/contact-us.html"})
    public String contactUs() {
        return "contact-us"; // resolves to src/main/resources/templates/contact-us.html
    }
}
