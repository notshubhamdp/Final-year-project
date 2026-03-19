package com.SRHF.SRHF.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogoutController {

    @GetMapping("/logout")
    public String logoutRedirect(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // route to role-specific logout confirmation pages when available
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(auth -> auth != null)
                .map(String::toUpperCase)
                .filter(s -> s.contains("ADMIN") || s.contains("LANDLORD") || s.contains("TENANT"))
                .findFirst()
                .map(role -> {
                    if (role.contains("ADMIN")) return "redirect:/logout-admin";
                    if (role.contains("LANDLORD")) return "redirect:/logout-landlord";
                    return "redirect:/logout-tenant";
                })
                .orElse("redirect:/login");
    }

    @GetMapping("/logout-tenant")
    public String logoutPage(Model model) {
        // simply return the logout confirmation view
        return "logout-tenant";
    }
}
