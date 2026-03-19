package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.service.EmailService;
import com.SRHF.SRHF.util.DocumentUploadUtil;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Objects;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.SRHF.SRHF.util.FilePathHandler;

@Controller
@RequestMapping("/admin")
public class AdminPropertiesController {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(AdminPropertiesController.class);

    public AdminPropertiesController(PropertyRepository propertyRepository, UserRepository userRepository, EmailService emailService) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * View properties by status (PENDING, APPROVED, REJECTED)
     */
    @GetMapping("/pending-properties")
    public String viewPendingProperties(Model model) {
        List<Property> properties = propertyRepository.findByVerificationStatus("PENDING");
        model.addAttribute("properties", properties);
        model.addAttribute("status", "PENDING");
        return "admin-pending-properties";
    }

    @GetMapping("/approved-properties")
    public String viewApprovedProperties(Model model) {
        List<Property> properties = propertyRepository.findByVerificationStatus("APPROVED");
        model.addAttribute("properties", properties);
        model.addAttribute("status", "APPROVED");
        return "admin-pending-properties";
    }

    @GetMapping("/rejected-properties")
    public String viewRejectedProperties(Model model) {
        List<Property> properties = propertyRepository.findByVerificationStatus("REJECTED");
        model.addAttribute("properties", properties);
        model.addAttribute("status", "REJECTED");
        return "admin-pending-properties";
    }

    /**
     * View detailed property information for approval/rejection
     */
    @GetMapping("/property/{id}")
    public String viewPropertyDetail(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        String methodName = "viewPropertyDetail";
        logger.info("{} - START: Fetching property details for ID: {}", methodName, id);

        try {
            // Validate property ID
            if (id == null || id <= 0) {
                logger.warn("{} - Invalid property ID: {}", methodName, id);
                redirectAttributes.addFlashAttribute("error", "Invalid property ID");
                return "redirect:/admin/pending-properties";
            }

            // Fetch property
            logger.info("{} - Querying database for property ID: {}", methodName, id);
            Optional<Property> propertyOpt = propertyRepository.findById(id);

            if (propertyOpt.isEmpty()) {
                logger.warn("{} - Property not found with ID: {}", methodName, id);
                redirectAttributes.addFlashAttribute("error", "Property not found");
                return "redirect:/admin/pending-properties";
            }

            Property property = propertyOpt.get();
            logger.info("{} - Property found: {}", methodName, property.getName());

            // Fetch landlord
            logger.info("{} - Fetching landlord with ID: {}", methodName, property.getLandlordId());
            Optional<User> landlordOpt = userRepository.findById(property.getLandlordId());

            if (landlordOpt.isEmpty()) {
                logger.warn("{} - Landlord not found with ID: {}", methodName, property.getLandlordId());
                redirectAttributes.addFlashAttribute("error", "Landlord information not found");
                return "redirect:/admin/pending-properties";
            }

            User landlord = landlordOpt.get();
            logger.info("{} - Landlord found: {}", methodName, landlord.getEmail());

                // Parse images and documents using utility and extract filenames
                logger.info("{} - Parsing images and documents", methodName);
                List<String> rawImagePaths = DocumentUploadUtil.parseFilePaths(property.getImagesPath());
                List<String> rawDocumentPaths = DocumentUploadUtil.parseFilePaths(property.getDocumentsPath());

                // Convert stored relative paths (uploads/...) into plain filenames so they can be used in URL path variables
                    // filter and extract filenames
                    List<String> imagePaths = rawImagePaths.stream()
                        .map(p -> Paths.get(p).getFileName().toString())
                        .map(String::toLowerCase)
                        .filter(n -> n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".gif") || n.endsWith(".webp"))
                        .collect(Collectors.toList());

                    List<String> documentPaths = rawDocumentPaths.stream()
                        .map(p -> Paths.get(p).getFileName().toString())
                        .map(String::toLowerCase)
                        .filter(n -> n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".xls") || n.endsWith(".xlsx") || n.endsWith(".txt"))
                        .collect(Collectors.toList());

                logger.info("{} - Found {} images and {} documents", methodName, imagePaths.size(), documentPaths.size());

            // Add attributes to model
            model.addAttribute("property", property);
            model.addAttribute("landlord", landlord);
            model.addAttribute("imagePaths", imagePaths);
            model.addAttribute("documentPaths", documentPaths);

            logger.info("{} - SUCCESS: Property details loaded for ID: {}", methodName, id);
            return "admin-property-detail";

        } catch (Exception e) {
            logger.error("{} - EXCEPTION: {}", methodName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An error occurred while loading property details: " + e.getMessage());
            return "redirect:/admin/pending-properties";
        }
    }

    /**
     * Approve a property
     */
    @PostMapping("/property/{id}/approve")
    public String approveProperty(
            @PathVariable Long id,
            @RequestParam(value = "notes", required = false) String notes,
            RedirectAttributes redirectAttributes) {

        String methodName = "approveProperty";
        logger.info("{} - START: Approving property ID: {}", methodName, id);

        try {
            // Validate property ID
            if (id == null || id <= 0) {
                logger.warn("{} - Invalid property ID: {}", methodName, id);
                redirectAttributes.addFlashAttribute("error", "Invalid property ID");
                return "redirect:/admin/pending-properties";
            }

            // Fetch property
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (propertyOpt.isEmpty()) {
                logger.warn("{} - Property not found with ID: {}", methodName, id);
                redirectAttributes.addFlashAttribute("error", "Property not found");
                return "redirect:/admin/pending-properties";
            }

            Property property = propertyOpt.get();
            logger.info("{} - Property found: {}", methodName, property.getName());

            // Fetch landlord
            Optional<User> landlordOpt = userRepository.findById(property.getLandlordId());
            if (landlordOpt.isEmpty()) {
                logger.warn("{} - Landlord not found with ID: {}", methodName, property.getLandlordId());
                redirectAttributes.addFlashAttribute("error", "Landlord not found");
                return "redirect:/admin/property/" + id;
            }

            User landlord = landlordOpt.get();

            // Update property status
            property.setVerificationStatus("APPROVED");
            property.setAdminNotes(notes != null && !notes.isEmpty() ? notes : "Approved by admin");
            propertyRepository.save(property);
            logger.info("{} - Property status updated to APPROVED", methodName);

            // Send email to landlord
            try {
                emailService.sendPropertyApprovedEmail(landlord.getEmail(), landlord.getFirstName(), landlord.getLastName(), property.getName());
                logger.info("{} - Approval email sent to: {}", methodName, landlord.getEmail());
            } catch (Exception emailException) {
                logger.warn("{} - Failed to send approval email: {}", methodName, emailException.getMessage());
            }

            redirectAttributes.addFlashAttribute("message", "Property approved successfully! Email notification has been sent to the landlord.");
            logger.info("{} - SUCCESS: Property {} approved", methodName, id);
            return "redirect:/admin/pending-properties";

        } catch (Exception e) {
            logger.error("{} - EXCEPTION: {}", methodName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error approving property: " + e.getMessage());
            return "redirect:/admin/property/" + id;
        }
    }

    /**
     * Reject a property
     */
    @PostMapping("/property/{id}/reject")
    public String rejectProperty(
            @PathVariable Long id,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
            RedirectAttributes redirectAttributes) {

        String methodName = "rejectProperty";
        logger.info("{} - START: Rejecting property ID: {}", methodName, id);

        try {
            // Validate rejection reason
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                logger.warn("{} - Rejection reason is missing", methodName);
                redirectAttributes.addFlashAttribute("error", "Rejection reason is required");
                return "redirect:/admin/property/" + id;
            }

            // Validate property ID
            if (id == null || id <= 0) {
                logger.warn("{} - Invalid property ID: {}", methodName, id);
                redirectAttributes.addFlashAttribute("error", "Invalid property ID");
                return "redirect:/admin/pending-properties";
            }

            // Fetch property
            Optional<Property> propertyOpt = propertyRepository.findById(id);
            if (propertyOpt.isEmpty()) {
                logger.warn("{} - Property not found with ID: {}", methodName, id);
                redirectAttributes.addFlashAttribute("error", "Property not found");
                return "redirect:/admin/pending-properties";
            }

            Property property = propertyOpt.get();
            logger.info("{} - Property found: {}", methodName, property.getName());

            // Fetch landlord
            Optional<User> landlordOpt = userRepository.findById(property.getLandlordId());
            if (landlordOpt.isEmpty()) {
                logger.warn("{} - Landlord not found with ID: {}", methodName, property.getLandlordId());
                redirectAttributes.addFlashAttribute("error", "Landlord not found");
                return "redirect:/admin/property/" + id;
            }

            User landlord = landlordOpt.get();

            // Update property status
            property.setVerificationStatus("REJECTED");
            property.setAdminNotes("Rejection reason: " + rejectionReason);
            propertyRepository.save(property);
            logger.info("{} - Property status updated to REJECTED", methodName);

            // Send email to landlord
            try {
                emailService.sendPropertyRejectedEmail(landlord.getEmail(), landlord.getFirstName(), landlord.getLastName(), property.getName(), rejectionReason);
                logger.info("{} - Rejection email sent to: {}", methodName, landlord.getEmail());
            } catch (Exception emailException) {
                logger.warn("{} - Failed to send rejection email: {}", methodName, emailException.getMessage());
            }

            redirectAttributes.addFlashAttribute("message", "Property rejected successfully! Email notification has been sent to the landlord.");
            logger.info("{} - SUCCESS: Property {} rejected", methodName, id);
            return "redirect:/admin/pending-properties";

        } catch (Exception e) {
            logger.error("{} - EXCEPTION: {}", methodName, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error rejecting property: " + e.getMessage());
            return "redirect:/admin/property/" + id;
        }
    }

    /**
     * Serve property image file for admin verification
     */
    @GetMapping("/file/image/{propertyId}/{filename:.+}")
    public ResponseEntity<Resource> servePropertyImage(
            @PathVariable Long propertyId,
            @PathVariable String filename) {
        
        String methodName = "servePropertyImage";
        logger.info("{} - START: Fetching image for property ID: {}, filename: {}", methodName, propertyId, filename);
        
        try {
            // Build file path using FilePathHandler (property images are stored under property uploads)
            String subPath = FilePathHandler.getPropertySubPath(propertyId);
            Path filePath = FilePathHandler.buildFilePath(FilePathHandler.getPropertyUploadsPath(), subPath, filename);
            
            logger.info("{} - File path: {}", methodName, filePath.toString());
            
            // Check if file exists
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                logger.warn("{} - File not found: {}", methodName, filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Determine media type
            String contentType = "image/jpeg";
            String fileNameLower = filename.toLowerCase();
            if (fileNameLower.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileNameLower.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (fileNameLower.endsWith(".webp")) {
                contentType = "image/webp";
            }
            
            Resource resource = new FileSystemResource(filePath.toFile());
            logger.info("{} - SUCCESS: Image served", methodName);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("{} - EXCEPTION: {}", methodName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve property document file for admin verification
     */
    @GetMapping("/file/document/{propertyId}/{filename:.+}")
    public ResponseEntity<Resource> servePropertyDocument(
            @PathVariable Long propertyId,
            @PathVariable String filename) {
        
        String methodName = "servePropertyDocument";
        logger.info("{} - START: Fetching document for property ID: {}, filename: {}", methodName, propertyId, filename);
        
        try {
            // Build file path using FilePathHandler (documents are stored under property uploads/documents)
            String subPath = FilePathHandler.getPropertyDocumentsSubPath(propertyId);
            Path filePath = FilePathHandler.buildFilePath(FilePathHandler.getPropertyUploadsPath(), subPath, filename);
            
            logger.info("{} - File path: {}", methodName, filePath.toString());
            
            // Check if file exists
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                logger.warn("{} - File not found: {}", methodName, filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Determine media type
            String contentType = "application/octet-stream";
            String fileNameLower = filename.toLowerCase();
            if (fileNameLower.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (fileNameLower.endsWith(".doc") || fileNameLower.endsWith(".docx")) {
                contentType = "application/msword";
            } else if (fileNameLower.endsWith(".xls") || fileNameLower.endsWith(".xlsx")) {
                contentType = "application/vnd.ms-excel";
            }
            
            Resource resource = new FileSystemResource(filePath.toFile());
            logger.info("{} - SUCCESS: Document served", methodName);
            
                // Use inline display for PDFs (view in browser), download for other document types
                String disposition = fileNameLower.endsWith(".pdf") ? "inline" : "attachment";
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("{} - EXCEPTION: {}", methodName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * View pending student verifications
     */
    @GetMapping("/pending-student-verifications")
    public String viewPendingStudentVerifications(Model model) {
        // Query for students with PENDING verification status
        List<User> pendingStudents = userRepository.findByTenantTypeAndStudentVerificationStatus("STUDENT", "PENDING");
        
        model.addAttribute("students", pendingStudents);
        model.addAttribute("status", "PENDING");
        return "admin-pending-student-verifications";
    }

    /**
     * View student verification details
     */
    @GetMapping("/student-verification/{id}")
    public String viewStudentVerificationDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Student not found");
            return "redirect:/admin/pending-student-verifications";
        }
        
        User student = userOpt.get();
        if (!"STUDENT".equals(student.getTenantType())) {
            redirectAttributes.addFlashAttribute("error", "This user is not a student");
            return "redirect:/admin/pending-student-verifications";
        }
        
        // Extract filename from document path
        String documentFilename = null;
        if (student.getDocumentPath() != null && !student.getDocumentPath().isEmpty()) {
            documentFilename = student.getDocumentPath().substring(student.getDocumentPath().lastIndexOf('/') + 1);
        }
        
        model.addAttribute("student", student);
        model.addAttribute("documentFilename", documentFilename);
        return "admin-student-verification-detail";
    }

    /**
     * Approve student verification
     */
    @PostMapping("/student/{id}/approve")
    public String approveStudentVerification(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Student not found");
            return "redirect:/admin/pending-student-verifications";
        }
        
        User student = userOpt.get();
        student.setStudentVerificationStatus("APPROVED");
        userRepository.save(student);
        
        // Send approval email
        try {
            emailService.sendStudentVerificationApprovedEmail(student.getEmail(), student.getFirstName(), student.getLastName());
        } catch (Exception e) {
            logger.warn("Failed to send approval email to {}: {}", student.getEmail(), e.getMessage());
        }
        
        redirectAttributes.addFlashAttribute("message", "Student verification approved!");
        return "redirect:/admin/pending-student-verifications";
    }

    /**
     * Reject student verification
     */
    @PostMapping("/student/{id}/reject")
    public String rejectStudentVerification(
            @PathVariable Long id,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
            RedirectAttributes redirectAttributes) {
        
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Student not found");
            return "redirect:/admin/pending-student-verifications";
        }
        
        User student = userOpt.get();
        student.setStudentVerificationStatus("REJECTED");
        userRepository.save(student);
        
        // Send rejection email
        try {
            emailService.sendStudentVerificationRejectedEmail(student.getEmail(), student.getFirstName(), student.getLastName(), rejectionReason);
        } catch (Exception e) {
            logger.warn("Failed to send rejection email to {}: {}", student.getEmail(), e.getMessage());
        }
        
        redirectAttributes.addFlashAttribute("message", "Student verification rejected!");
        return "redirect:/admin/pending-student-verifications";
    }

    /**
     * Serve student document for admin review (images and PDFs)
     */
    @GetMapping("/file/student/{userId}/{filename}")
    public ResponseEntity<?> serveStudentDocument(
            @PathVariable Long userId,
            @PathVariable String filename,
            Authentication authentication) {
        
        // Verify admin is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get student to retrieve document path
            Optional<User> studentOpt = userRepository.findById(userId);
            if (studentOpt.isEmpty() || studentOpt.get().getDocumentPath() == null) {
                return ResponseEntity.notFound().build();
            }

            String documentPath = studentOpt.get().getDocumentPath();
            String uploadDir = FilePathHandler.getAbsoluteDocumentUploadsPath();
            
            // Extract just the filename from the stored path (handles both relative and absolute paths)
            String justFilename = Paths.get(documentPath).getFileName().toString();
            Path filePath = Paths.get(uploadDir, justFilename).normalize();

            // Security check: ensure file is within upload directory
            if (!filePath.toRealPath().startsWith(Paths.get(uploadDir).toRealPath())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(file);
            MediaType mediaType;
            String disposition;

            // Determine content type and disposition based on file extension
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
                disposition = "inline"; // Display PDF in browser
            } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg") || 
                       lowerFilename.endsWith(".png") || lowerFilename.endsWith(".gif") ||
                       lowerFilename.endsWith(".webp")) {
                mediaType = MediaType.IMAGE_JPEG;
                disposition = "inline"; // Display image in browser
                
                // Determine actual image type
                if (lowerFilename.endsWith(".png")) {
                    mediaType = MediaType.IMAGE_PNG;
                } else if (lowerFilename.endsWith(".gif")) {
                    mediaType = MediaType.IMAGE_GIF;
                } else if (lowerFilename.endsWith(".webp")) {
                    mediaType = MediaType.valueOf("image/webp");
                }
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                disposition = "attachment"; // Download other document types
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            disposition + "; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving student document: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * View banned users
     */
    @GetMapping("/banned-users")
    public String viewBannedUsers(Model model) {
        List<User> bannedUsers = userRepository.findByBanned(true);
        model.addAttribute("users", bannedUsers);
        return "admin-banned-users";
    }

    /**
     * Ban a user
     */
    @PostMapping("/user/{id}/ban")
    public String banUser(
            @PathVariable Long id,
            @RequestParam(value = "banReason", required = false) String banReason,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/banned-users";
        }

        User user = userOpt.get();
        user.setBanned(true);
        user.setBanReason(banReason != null && !banReason.trim().isEmpty() ? banReason : "Violation of platform policy: Brokers, agents, and intermediaries are not allowed.");
        userRepository.save(user);

        // Send ban notification email
        try {
            emailService.sendUserBanEmail(user.getEmail(), user.getFirstName(), user.getLastName(), user.getBanReason());
        } catch (Exception e) {
            logger.warn("Failed to send ban email to {}: {}", user.getEmail(), e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", "User has been banned!");
        return "redirect:/admin/banned-users";
    }

    /**
     * Unban a user
     */
    @PostMapping("/user/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/admin/banned-users";
        }

        User user = userOpt.get();
        user.setBanned(false);
        user.setBanReason(null);
        userRepository.save(user);

        // Send unban notification email
        try {
            emailService.sendUserUnbanEmail(user.getEmail(), user.getFirstName(), user.getLastName());
        } catch (Exception e) {
            logger.warn("Failed to send unban email to {}: {}", user.getEmail(), e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", "User has been unbanned!");
        return "redirect:/admin/banned-users";
    }

    /**
     * Ban user by email
     */
    @PostMapping("/ban-user-by-email")
    public String banUserByEmail(
            @RequestParam String email,
            @RequestParam(required = false) String banReason,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOpt = userRepository.findByemail(email);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User with email '" + email + "' not found");
            return "redirect:/admin/banned-users";
        }

        User user = userOpt.get();

        if (user.isBanned()) {
            redirectAttributes.addFlashAttribute("error", "User is already banned");
            return "redirect:/admin/banned-users";
        }

        user.setBanned(true);
        user.setBanReason(banReason != null && !banReason.trim().isEmpty() ?
            banReason : "Violation of platform policy: Brokers, agents, and intermediaries are not allowed.");
        userRepository.save(user);

        // Send ban notification email
        try {
            emailService.sendUserBanEmail(user.getEmail(), user.getFirstName(), user.getLastName(), user.getBanReason());
        } catch (Exception e) {
            logger.warn("Failed to send ban email to {}: {}", user.getEmail(), e.getMessage());
        }

        redirectAttributes.addFlashAttribute("message", "User '" + email + "' has been banned!");
        return "redirect:/admin/banned-users";
    }
}
