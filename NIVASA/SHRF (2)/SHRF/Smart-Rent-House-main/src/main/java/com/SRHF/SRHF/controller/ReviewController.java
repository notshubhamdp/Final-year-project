package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.AppReview;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.AppReviewRepository;
import com.SRHF.SRHF.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class ReviewController {

    private final UserRepository userRepository;
    private final AppReviewRepository appReviewRepository;

    public ReviewController(UserRepository userRepository, AppReviewRepository appReviewRepository) {
        this.userRepository = userRepository;
        this.appReviewRepository = appReviewRepository;
    }

    @PostMapping("/reviews/submit")
    public String submitReview(@RequestParam("rating") Integer rating,
                               @RequestParam("reviewText") String reviewText,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
        String redirectPath = "LANDLORD".equals(role) ? "/landlord-dashboard" : "/tenant-dashboard";

        if (!"TENANT".equals(role) && !"LANDLORD".equals(role)) {
            redirectAttributes.addFlashAttribute("error", "Only tenants and landlords can submit reviews.");
            return "redirect:" + redirectPath;
        }

        if (appReviewRepository.existsByUserId(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You have already submitted your one-time review.");
            return "redirect:" + redirectPath;
        }

        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "Please provide a valid rating between 1 and 5.");
            return "redirect:" + redirectPath;
        }

        String cleanedReview = reviewText == null ? "" : reviewText.trim();
        if (cleanedReview.length() < 5 || cleanedReview.length() > 500) {
            redirectAttributes.addFlashAttribute("error", "Review must be between 5 and 500 characters.");
            return "redirect:" + redirectPath;
        }

        AppReview appReview = new AppReview();
        appReview.setUser(user);
        appReview.setRating(rating);
        appReview.setReviewText(cleanedReview);
        appReview.setReviewerName((user.getFirstName() + " " + user.getLastName()).trim());
        appReview.setReviewerRole(role);
        appReview.setReviewerCity(user.getCity());
        appReview.setCreatedAt(LocalDateTime.now());
        appReviewRepository.save(appReview);

        redirectAttributes.addFlashAttribute("message", "Thanks for reviewing NIVASA.");
        return "redirect:" + redirectPath;
    }
}
