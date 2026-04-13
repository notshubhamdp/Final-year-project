package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.ChatbotFaq;
import com.SRHF.SRHF.repository.ChatbotFaqRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final String SUPPORT_EMAIL = "nivasacontact@gmail.com";

    private final ChatbotFaqRepository chatbotFaqRepository;

    public ChatbotService(ChatbotFaqRepository chatbotFaqRepository) {
        this.chatbotFaqRepository = chatbotFaqRepository;
    }

    public ChatbotReply reply(String rawQuestion) {
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        List<SupportFaq> faqs = getFaqCatalog();

        if (question.isEmpty()) {
            return new ChatbotReply(
                    "Welcome to NIVASA customer care. Ask a question or choose one of the common support topics below. This support uses fixed FAQ answers only.",
                    quickReplyQuestions(faqs),
                    true,
                    SUPPORT_EMAIL,
                    null
            );
        }

        SupportFaq matchedFaq = findBestMatch(question, faqs);
        if (matchedFaq != null) {
            return new ChatbotReply(
                    matchedFaq.getAnswer(),
                    relatedQuestions(matchedFaq, faqs),
                    true,
                    SUPPORT_EMAIL,
                    matchedFaq.getQuestion()
            );
        }

        return new ChatbotReply(
                "We could not find an exact answer for that question. Please contact customer care at " + SUPPORT_EMAIL + ".",
                quickReplyQuestions(faqs),
                false,
                SUPPORT_EMAIL,
                null
        );
    }

    public List<SupportFaq> getFaqCatalog() {
        Map<String, SupportFaq> merged = new LinkedHashMap<>();

        for (SupportFaq faq : defaultFaqCatalog()) {
            merged.put(normalizeForSearch(faq.getQuestion()), faq);
        }

        for (ChatbotFaq faq : chatbotFaqRepository.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            SupportFaq supportFaq = new SupportFaq(
                    faq.getQuestion(),
                    faq.getAnswer(),
                    faq.getTags(),
                    faq.getSortOrder() != null ? faq.getSortOrder() : 999
            );
            merged.put(normalizeForSearch(supportFaq.getQuestion()), supportFaq);
        }

        return merged.values().stream()
                .sorted((left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()))
                .toList();
    }

    public String getSupportEmail() {
        return SUPPORT_EMAIL;
    }

    private SupportFaq findBestMatch(String question, List<SupportFaq> faqs) {
        String normalizedQuestion = normalizeForSearch(question);
        if (normalizedQuestion.isBlank()) {
            return null;
        }

        SupportFaq bestFaq = null;
        int bestScore = 0;

        for (SupportFaq faq : faqs) {
            int score = scoreMatch(normalizedQuestion, faq);
            if (score > bestScore) {
                bestScore = score;
                bestFaq = faq;
            }
        }

        return bestScore >= 6 ? bestFaq : null;
    }

    private int scoreMatch(String normalizedQuestion, SupportFaq faq) {
        String normalizedFaqQuestion = normalizeForSearch(faq.getQuestion());
        String normalizedTags = normalizeForSearch(faq.getTags());
        String combined = (normalizedFaqQuestion + " " + normalizedTags).trim();

        if (normalizedFaqQuestion.equals(normalizedQuestion)) {
            return 100;
        }
        if (!normalizedQuestion.isBlank() && combined.contains(normalizedQuestion)) {
            return 40 + Math.min(normalizedQuestion.length(), 20);
        }

        Set<String> questionTokens = tokenize(normalizedQuestion);
        Set<String> faqTokens = tokenize(combined);
        int overlap = 0;
        for (String token : questionTokens) {
            if (faqTokens.contains(token)) {
                overlap++;
            }
        }

        if (overlap == 0) {
            return 0;
        }

        int score = overlap * 5;
        if (questionTokens.size() > 0 && overlap == questionTokens.size()) {
            score += 8;
        }
        if (normalizedTags.contains(normalizedQuestion) && !normalizedQuestion.isBlank()) {
            score += 10;
        }

        return score;
    }

    private List<String> relatedQuestions(SupportFaq matchedFaq, List<SupportFaq> faqs) {
        Set<String> matchedTags = tokenize(matchedFaq.getTags());
        List<String> related = faqs.stream()
                .filter(faq -> !faq.getQuestion().equalsIgnoreCase(matchedFaq.getQuestion()))
                .sorted((left, right) -> Integer.compare(sharedTagCount(right, matchedTags), sharedTagCount(left, matchedTags)))
                .map(SupportFaq::getQuestion)
                .filter(question -> !question.equalsIgnoreCase(matchedFaq.getQuestion()))
                .limit(4)
                .toList();

        if (!related.isEmpty()) {
            return related;
        }

        return quickReplyQuestions(faqs);
    }

    private int sharedTagCount(SupportFaq faq, Set<String> matchedTags) {
        if (matchedTags.isEmpty()) {
            return 0;
        }
        Set<String> faqTags = tokenize(faq.getTags());
        int count = 0;
        for (String token : matchedTags) {
            if (faqTags.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private List<String> quickReplyQuestions(List<SupportFaq> faqs) {
        return faqs.stream()
                .map(SupportFaq::getQuestion)
                .limit(6)
                .toList();
    }

    private String normalizeForSearch(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokenize(String text) {
        String normalized = normalizeForSearch(text);
        if (normalized.isBlank()) {
            return Set.of();
        }

        Set<String> ignored = Set.of(
                "a", "an", "the", "is", "are", "i", "me", "my", "to", "for", "of",
                "on", "in", "at", "do", "does", "how", "what", "where", "can", "and",
                "or", "be", "you", "your", "with"
        );

        return java.util.Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() > 2)
                .filter(token -> !ignored.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SupportFaq> defaultFaqCatalog() {
        return List.of(
                faq("How do I create a tenant account?",
                        "Open Login/Register, choose the tenant role, enter your details, then complete the email or OTP verification steps.",
                        "tenant register signup create account login", 1),
                faq("How do I create a landlord account?",
                        "Open Login/Register, choose landlord, complete your profile, and upload the required verification document if prompted.",
                        "landlord register signup create account verification", 2),
                faq("I did not receive the OTP or verification email.",
                        "Wait a moment, then check spam or junk folders. If it still does not arrive, retry the verification flow from the login or register page.",
                        "otp email verification mail not received", 3),
                faq("How do I reset my password?",
                        "Use the Forgot Password option on the login page, enter your email, and follow the reset link or OTP instructions shown by the system.",
                        "forgot password reset login access", 4),
                faq("How do I update my profile details?",
                        "After logging in, open your profile page, edit the required details such as phone or address, and save the changes.",
                        "profile update edit account phone address", 5),
                faq("How do I search for properties?",
                        "Open the tenant dashboard, use the search bar and filters for city, price, and property type, then open the matching property cards.",
                        "search find property tenant dashboard filter city price", 6),
                faq("How do I save a property to Favorites?",
                        "Open a property card or property details page and choose Add to Favorites. You can review saved listings from the Favorites page.",
                        "favorites save wishlist shortlist property", 7),
                faq("How do I contact a landlord?",
                        "Open the property details page and use Contact Landlord or Messages to start a conversation with the landlord for that property.",
                        "contact landlord message chat property", 8),
                faq("How do messages work on NIVASA?",
                        "The Messages section stores conversations between tenants and landlords. Open a conversation, send your update, and continue the discussion there.",
                        "messages conversation chat inbox support", 9),
                faq("How do I schedule a property visit?",
                        "Use the visit scheduling option from the property or contact flow, choose your preferred date and time, and then track the visit on the Visits page.",
                        "visit schedule inspection appointment property", 10),
                faq("How do I book a property?",
                        "Open the property details page, choose Book Now, review the advance amount, and continue to the payment checkout page.",
                        "book booking property advance checkout", 11),
                faq("What happens after I pay the advance amount?",
                        "Your booking request is created and sent to the landlord for approval. Until the landlord approves it, the booking status remains pending approval.",
                        "advance payment after booking pending approval landlord", 12),
                faq("Where can I see my booked property after booking?",
                        "Open the tenant dashboard. Your booked properties appear in the My Booked Properties section together with approval status and payment details.",
                        "booked property booking dashboard tenant", 13),
                faq("Where can I see my payment history?",
                        "Open Payment History from the tenant dashboard or payment pages to review advance, rent, and deposit payments with their related property details.",
                        "payment history receipt past payments", 14),
                faq("How do I download my payment receipt?",
                        "Open Payment History, select the payment details page, and use Download Receipt for that payment record.",
                        "download receipt invoice payment history", 15),
                faq("What payment methods are available?",
                        "NIVASA supports the checkout methods shown on the payment page, such as card and other supported online payment options available in the flow.",
                        "payment method card online checkout", 16),
                faq("What should I do if my payment fails?",
                        "Please retry the payment once, verify your payment details, and then check Payment History. If the amount was deducted but status is not updated, contact customer care.",
                        "payment failed error checkout deducted", 17),
                faq("How do I list a property as a landlord?",
                        "Open the landlord dashboard, choose Add Property, fill in the property details, upload images and documents, and submit it for verification.",
                        "landlord list property add upload images documents", 18),
                faq("What documents are needed for property verification?",
                        "Landlords should upload the ownership or supporting property documents requested in the upload steps so the admin team can review the listing.",
                        "documents verification property proof ownership upload", 19),
                faq("How do I check property verification status?",
                        "Landlords can review each listing from My Properties or the landlord dashboard where the property shows pending, approved, or rejected verification status.",
                        "verification status approved rejected pending property", 20),
                faq("Can I manage multiple properties from one account?",
                        "Yes. A landlord account can add and manage multiple property listings from the same dashboard.",
                        "multiple properties landlord dashboard manage", 21),
                faq("How do landlord wallet and payouts work?",
                        "Completed tenant payments appear in the landlord wallet and payout sections, where landlords can review settlement and payout status.",
                        "wallet payouts landlord settlement money", 22),
                faq("How do deposit refunds work?",
                        "Deposit refund requests and approvals are tracked through the payment records. The landlord must approve eligible refunds before completion.",
                        "deposit refund refund request approve payment", 23),
                faq("My account is banned or I cannot log in.",
                        "First confirm that your email and password are correct and try Forgot Password if needed. If the issue continues or you see a ban-related message, contact customer care.",
                        "banned account cannot login locked support", 24),
                faq("Is NIVASA free for tenants and does it charge brokerage?",
                        "Browsing properties and using the tenant side is free. NIVASA does not charge brokerage unless a clearly stated premium service is introduced in the future.",
                        "free tenant brokerage charges fees", 25),
                faq("Where can I get customer care support?",
                        "If you cannot find your answer in the FAQ or support widget, please email customer care at " + SUPPORT_EMAIL + ".",
                        "customer care support help contact email", 26),
                faq("What should I include when contacting customer care?",
                        "Please include your registered email, the page where the problem happened, a short description of the issue, and a screenshot if possible.",
                        "customer care support screenshot issue email details", 27),
                faq("Where can I read the privacy policy or terms?",
                        "Use the Privacy Policy and Terms links available in the footer or site navigation to review NIVASA policies.",
                        "privacy policy terms legal footer", 28),
                faq("How do I change my phone number or address after registration?",
                        "Open your profile after logging in, update the phone number, address, city, or other editable fields, and save the changes.",
                        "change phone number address city profile update account", 29),
                faq("Can tenants contact multiple landlords at the same time?",
                        "Yes. Tenants can open different property listings and start separate message conversations with each landlord.",
                        "tenant multiple landlords messages conversations contact", 30),
                faq("How do I know if a landlord replied to my message?",
                        "Open the Messages section to check the latest conversation updates and unread message count.",
                        "landlord replied reply unread message count inbox", 31),
                faq("Can landlords reply to tenant inquiries from the dashboard?",
                        "Yes. Landlords can open Messages from the landlord dashboard and continue conversations with interested tenants.",
                        "landlord dashboard reply tenant inquiry messages", 32),
                faq("How do I view my scheduled visits?",
                        "Open the Visits page to review requested, approved, rescheduled, or completed visit entries.",
                        "scheduled visits visit status approved rescheduled completed", 33),
                faq("What does pending approval mean after booking?",
                        "Pending approval means the tenant has submitted the booking request and payment step, and the landlord still needs to review and approve it.",
                        "pending approval booking request landlord review", 34),
                faq("Can I pay rent from NIVASA?",
                        "Yes. Rent-related payments are available through the payment flow and can be reviewed later in Payment History.",
                        "pay rent online payment history tenant", 35),
                faq("Where can landlords see tenant payments?",
                        "Landlords can review payment status, wallet records, and payout-related details from the landlord payment sections.",
                        "landlord see tenant payments wallet payouts history", 36),
                faq("How do I know if a property is verified?",
                        "Verified or approved listing status appears in the landlord property management flow and approved properties become available to tenants for normal browsing.",
                        "property verified approved listing status", 37),
                faq("Can I edit my property details after listing it?",
                        "If the property management flow allows editing, landlords can reopen the listing from My Properties and update eligible details there.",
                        "edit property listing landlord my properties update", 38),
                faq("What should I do if property images are not uploading?",
                        "Check your internet connection, confirm the file is valid, retry the upload, and contact support if the problem continues.",
                        "property image upload failed error landlord", 39),
                faq("What should I do if the page is not loading correctly?",
                        "Refresh the page, sign in again if needed, and clear the browser cache if the issue persists. If it still fails, contact customer care with a screenshot.",
                        "page not loading refresh cache error support", 40),
                faq("How do I log out of my account?",
                        "Use the Logout option from your dashboard or navigation menu to safely sign out of NIVASA.",
                        "logout sign out dashboard account", 41),
                faq("Can students and families both use NIVASA as tenants?",
                        "Yes. During tenant setup, the platform supports tenant types such as student and family.",
                        "student family tenant type role selection", 42),
                faq("What happens if my student verification is pending?",
                        "If student verification is pending, wait for admin review. Access to some tenant features may depend on approval status.",
                        "student verification pending admin approval tenant", 43),
                faq("How do I contact customer care for technical issues?",
                        "Email " + SUPPORT_EMAIL + " with your registered email, the page name, a short issue summary, and a screenshot if available.",
                        "technical issue customer care support email screenshot", 44)
        );
    }

    private SupportFaq faq(String question, String answer, String tags, int sortOrder) {
        return new SupportFaq(question, answer, tags, sortOrder);
    }

    public static class ChatbotReply {
        private final String answer;
        private final List<String> suggestions;
        private final boolean matched;
        private final String supportEmail;
        private final String matchedQuestion;

        public ChatbotReply(String answer,
                            List<String> suggestions,
                            boolean matched,
                            String supportEmail,
                            String matchedQuestion) {
            this.answer = answer;
            this.suggestions = suggestions;
            this.matched = matched;
            this.supportEmail = supportEmail;
            this.matchedQuestion = matchedQuestion;
        }

        public String getAnswer() {
            return answer;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public boolean isMatched() {
            return matched;
        }

        public String getSupportEmail() {
            return supportEmail;
        }

        public String getMatchedQuestion() {
            return matchedQuestion;
        }
    }

    public static class SupportFaq {
        private final String question;
        private final String answer;
        private final String tags;
        private final int sortOrder;

        public SupportFaq(String question, String answer, String tags, int sortOrder) {
            this.question = question;
            this.answer = answer;
            this.tags = tags;
            this.sortOrder = sortOrder;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public String getTags() {
            return tags;
        }

        public int getSortOrder() {
            return sortOrder;
        }
    }
}
