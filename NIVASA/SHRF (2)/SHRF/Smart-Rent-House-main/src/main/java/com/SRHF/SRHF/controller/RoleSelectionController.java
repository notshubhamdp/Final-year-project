package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.util.DocumentUploadUtil;
import com.SRHF.SRHF.util.FilePathHandler;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/role-selection")
public class RoleSelectionController {

    private final UserRepository userRepository;

    public RoleSelectionController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private String getUploadDir() {
        return FilePathHandler.getDocumentUploadsPath();
    }

    /**
     * Show role selection page (Tenant or Landlord)
     */
    @GetMapping
    public String showRoleSelection(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // If role already set, redirect to home or dashboard
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            return "redirect:/home";
        }

        model.addAttribute("user", user);
        return "role-selection";
    }

    /**
     * Set user role (TENANT or LANDLORD)
     */
    @PostMapping
    public String selectRole(
            @RequestParam("role") String role,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!role.equals("TENANT") && !role.equals("LANDLORD")) {
            redirectAttributes.addFlashAttribute("error", "Invalid role selection");
            return "redirect:/role-selection";
        }

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setRole(role);
        userRepository.save(user);

        // If LANDLORD, go to property listing form
        if ("LANDLORD".equals(role)) {
            redirectAttributes.addFlashAttribute("message", "Welcome, Landlord! Let's add your first property.");
            return "redirect:/landlord/add-property";
        }

        // If TENANT, go to tenant-type selection
        return "redirect:/role-selection/tenant-type";
    }

    /**
     * Show tenant sub-type selection (Student or Family)
     */
    @GetMapping("/tenant-type")
    public String showTenantType(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"TENANT".equals(user.getRole())) {
            return "redirect:/role-selection";
        }

        model.addAttribute("user", user);
        return "tenant-type";
    }

    /**
     * Set tenant sub-type (STUDENT or FAMILY)
     */
    @PostMapping("/tenant-type")
    public String selectTenantType(
            @RequestParam("tenantType") String tenantType,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (!tenantType.equals("STUDENT") && !tenantType.equals("FAMILY")) {
            redirectAttributes.addFlashAttribute("error", "Invalid tenant type selection");
            return "redirect:/role-selection/tenant-type";
        }

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setTenantType(tenantType);
        userRepository.save(user);

        // If STUDENT, go to document upload
        if ("STUDENT".equals(tenantType)) {
            return "redirect:/role-selection/student-upload";
        }

        // If FAMILY, go to home/dashboard
        redirectAttributes.addFlashAttribute("message", "Welcome, Family!");
        return "redirect:/home";
    }

    /**
     * Show student document upload page
     */
    @GetMapping("/student-upload")
    public String showStudentUpload(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"STUDENT".equals(user.getTenantType())) {
            return "redirect:/role-selection";
        }

        model.addAttribute("user", user);
        return "student-upload";
    }

    /**
     * Handle student ID document upload
     */
    @PostMapping("/student-upload")
    public String uploadStudentDocument(
            @RequestParam("document") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/role-selection/student-upload";
        }

        try {
            // Use utility class to handle student document upload
            String filePath = DocumentUploadUtil.uploadStudentDocument(file, user.getId(), getUploadDir());

            // Update user with document path and set status to PENDING
            user.setDocumentPath(filePath);
            user.setStudentVerificationStatus("PENDING"); // Set status to pending verification
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("message", 
                    "Document uploaded successfully! Your student ID will be verified by our team within 24 hours.");
            return "redirect:/home";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/role-selection/student-upload";
        } catch (java.io.IOException e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Failed to upload document: " + e.getMessage());
            return "redirect:/role-selection/student-upload";
        }
    }
}
