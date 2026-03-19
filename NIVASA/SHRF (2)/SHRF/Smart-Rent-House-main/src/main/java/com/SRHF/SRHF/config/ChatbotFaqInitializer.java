package com.SRHF.SRHF.config;

import com.SRHF.SRHF.entity.ChatbotFaq;
import com.SRHF.SRHF.repository.ChatbotFaqRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatbotFaqInitializer {

    @Bean
    public CommandLineRunner initializeChatbotFaqs(ChatbotFaqRepository faqRepository) {
        return args -> {
            if (!faqRepository.findByEnabledTrueOrderBySortOrderAscIdAsc().isEmpty()) {
                return;
            }

            List<ChatbotFaq> seedFaqs = List.of(
                    faq("How do I register on NIVASA?",
                            "Open Login/Register, choose tenant or landlord, complete details, and verify email/OTP.",
                            "register,signup,account", 1),
                    faq("How do tenants find and shortlist properties?",
                            "Use the tenant dashboard filters for city, rent, and type. Save good matches in Favorites.",
                            "tenant,search,favorites,property", 2),
                    faq("How does landlord property listing work?",
                            "Landlords can add property details, upload images/documents, and submit for admin verification.",
                            "landlord,list,upload,verification", 3),
                    faq("How can I contact a landlord?",
                            "Open property details and use Contact/Messages to start a conversation directly.",
                            "message,contact,chat,landlord", 4),
                    faq("How do visits work?",
                            "Schedule visits from the property/contact flow and track request status on the Visits page.",
                            "visit,schedule,inspection", 5),
                    faq("How are payments handled?",
                            "Use the Payment section for checkout, history, and receipt downloads. Landlords can view wallet/payout records.",
                            "payment,receipt,wallet,payout,rent", 6),
                    faq("What should I do if I face login or platform issues?",
                            "Use Forgot Password for access problems, and Contact Us with your email, page URL, and screenshot for support.",
                            "support,forgot,password,issue,help", 7)
            );

            faqRepository.saveAll(seedFaqs);
            System.out.println("Seeded chatbot FAQs: " + seedFaqs.size());
        };
    }

    private ChatbotFaq faq(String question, String answer, String tags, int sortOrder) {
        ChatbotFaq faq = new ChatbotFaq();
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setTags(tags);
        faq.setSortOrder(sortOrder);
        faq.setEnabled(true);
        return faq;
    }
}

