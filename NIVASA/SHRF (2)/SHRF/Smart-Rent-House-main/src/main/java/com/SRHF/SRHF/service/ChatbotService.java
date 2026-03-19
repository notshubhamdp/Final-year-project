package com.SRHF.SRHF.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ChatbotService {

    public ChatbotReply reply(String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isEmpty()) {
            return new ChatbotReply(
                    "Ask me anything about NIVASA: rent, listing property, messages, visits, payments, or account help.",
                    quickReplies()
            );
        }

        String q = question.toLowerCase(Locale.ENGLISH);

        if (containsAny(q, "hi", "hello", "hey", "good morning", "good evening")) {
            return new ChatbotReply(
                    "Hello. I can help with renting, listing property, payments, visits, and account issues.",
                    quickReplies()
            );
        }

        if (containsAny(q, "register", "signup", "sign up", "create account")) {
            return new ChatbotReply(
                    "To create an account, open Login/Register, choose your role, fill details, and verify your email/OTP.",
                    Arrays.asList("I forgot my password", "How to list property", "How to contact landlord")
            );
        }

        if (containsAny(q, "password", "forgot password", "reset")) {
            return new ChatbotReply(
                    "Use the 'Forgot Password' option on login. Enter your email and follow the reset link/OTP flow.",
                    Arrays.asList("Login issue", "Contact support", "Email not received")
            );
        }

        if (containsAny(q, "tenant", "find house", "search", "rent", "book")) {
            return new ChatbotReply(
                    "As a tenant: open dashboard, filter properties by city/price, view details, message landlord, and schedule visits.",
                    Arrays.asList("How to save favorites", "How to message landlord", "How to schedule visit")
            );
        }

        if (containsAny(q, "favorite", "save property", "wishlist")) {
            return new ChatbotReply(
                    "Open a property and click Add to Favorites. You can manage them from the Favorites page.",
                    Arrays.asList("How to contact landlord", "How to book property", "Tenant dashboard help")
            );
        }

        if (containsAny(q, "landlord", "list", "add property", "upload")) {
            return new ChatbotReply(
                    "As a landlord: go to your dashboard, add property details, upload images/documents, then submit for verification.",
                    Arrays.asList("What documents are needed", "Property verification status", "Manage multiple properties")
            );
        }

        if (containsAny(q, "verification", "verify", "approved", "pending")) {
            return new ChatbotReply(
                    "Property verification is reviewed by admin. Pending means under review. Approved listings appear for tenants.",
                    Arrays.asList("How long verification takes", "Edit property details", "Contact support")
            );
        }

        if (containsAny(q, "message", "chat", "conversation")) {
            return new ChatbotReply(
                    "Use the Messages section to chat with landlords/tenants. Open a conversation and send updates in real time.",
                    Arrays.asList("Messages not opening", "How to start conversation", "Visit scheduling")
            );
        }

        if (containsAny(q, "visit", "schedule", "inspection")) {
            return new ChatbotReply(
                    "Use Visit Scheduling from property/contact flow. You can check status and updates from the Visits page.",
                    Arrays.asList("How to contact landlord", "Where to see visit status", "Payment steps")
            );
        }

        if (containsAny(q, "payment", "pay rent", "receipt", "checkout", "wallet", "payout")) {
            return new ChatbotReply(
                    "For payments, use the Payment section to checkout and track history. Landlords can review wallet/payout records.",
                    Arrays.asList("Download receipt", "Payment failed", "View payment history")
            );
        }

        if (containsAny(q, "support", "contact", "help", "issue", "problem", "bug")) {
            return new ChatbotReply(
                    "For direct support, open Contact Us and share your account email, page URL, and screenshot for faster help.",
                    Arrays.asList("Open Contact Us", "FAQ", "Report login issue")
            );
        }

        return new ChatbotReply(
                "I can help with account setup, property listing, messaging, visits, and payments. Try a more specific question.",
                quickReplies()
        );
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private List<String> quickReplies() {
        Set<String> unique = new LinkedHashSet<>();
        unique.add("How to register");
        unique.add("How to list property");
        unique.add("How to message landlord");
        unique.add("Payment help");
        unique.add("Contact support");
        return new ArrayList<>(unique);
    }

    public static class ChatbotReply {
        private final String answer;
        private final List<String> suggestions;

        public ChatbotReply(String answer, List<String> suggestions) {
            this.answer = answer;
            this.suggestions = suggestions;
        }

        public String getAnswer() {
            return answer;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }
    }
}

