package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.Payment;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.PaymentRepository;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.service.EmailService;
import com.SRHF.SRHF.util.DocumentUploadUtil;
import com.SRHF.SRHF.util.FilePathHandler;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Controller
@RequestMapping("/landlord")
public class LandlordPropertyController {

    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(LandlordPropertyController.class);

    public LandlordPropertyController(PropertyRepository propertyRepository,
                                      PaymentRepository paymentRepository,
                                      UserRepository userRepository,
                                      EmailService emailService) {
        this.propertyRepository = propertyRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    private String getUploadDir() {
        return FilePathHandler.getPropertyUploadsPath();
    }


    @PostMapping("/property/{id}/update")
    public String updatePropertyByLandlord(@PathVariable Long id,
                                           @RequestParam String name,
                                           @RequestParam String ownerName,
                                           @RequestParam Double price,
                                           @RequestParam(required = false) String description,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Property> opt = propertyRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Property not found");
            return "redirect:/landlord/my-properties";
        }

        Property property = opt.get();
        if (!Objects.equals(property.getLandlordId(), user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to update this property");
            return "redirect:/landlord/my-properties";
        }

        property.setName(name);
        property.setOwnerName(ownerName);
        property.setPrice(price);
        property.setDescription(description);
        propertyRepository.save(property);

        redirectAttributes.addFlashAttribute("message", "Property updated successfully");
        return "redirect:/landlord/property-details/" + id;
    }

    @PostMapping("/booking/{paymentId}/approve")
    public String approveBooking(@PathVariable Long paymentId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User landlord = userRepository.findByemail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking payment not found"));

            if (!Objects.equals(payment.getLandlordId(), landlord.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to approve this booking");
                return "redirect:/landlord-dashboard";
            }

            if (!"ADVANCE".equalsIgnoreCase(payment.getPaymentType()) ||
                    !"COMPLETED".equalsIgnoreCase(payment.getStatus()) ||
                    !"PENDING_APPROVAL".equalsIgnoreCase(payment.getBookingApprovalStatus())) {
                redirectAttributes.addFlashAttribute("error", "This booking is not pending approval");
                return "redirect:/landlord-dashboard";
            }

            Property property = propertyRepository.findById(payment.getPropertyId())
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));

            payment.setBookingApprovalStatus("APPROVED");
            paymentRepository.save(payment);

            property.setAvailabilityStatus("NOT_AVAILABLE");
            propertyRepository.save(property);

            userRepository.findById(payment.getTenantId()).ifPresent(tenant -> {
                String tenantName = (tenant.getFirstName() + " " + tenant.getLastName()).trim();
                String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                        ? property.getPropertyId()
                        : String.valueOf(property.getId());
                emailService.sendBookingApprovedEmail(
                        tenant.getEmail(),
                        tenantName,
                        property.getName(),
                        propertyPublicId
                );
            });

            redirectAttributes.addFlashAttribute("message", "Booking approved successfully.");
            return "redirect:/landlord-dashboard";
        } catch (Exception e) {
            logger.error("Error approving booking {}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to approve booking: " + e.getMessage());
            return "redirect:/landlord-dashboard";
        }
    }

    @PostMapping("/booking/{paymentId}/reject")
    public String rejectBooking(@PathVariable Long paymentId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User landlord = userRepository.findByemail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking payment not found"));

            if (!Objects.equals(payment.getLandlordId(), landlord.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to reject this booking");
                return "redirect:/landlord-dashboard";
            }

            if (!"ADVANCE".equalsIgnoreCase(payment.getPaymentType()) ||
                    !"COMPLETED".equalsIgnoreCase(payment.getStatus()) ||
                    !"PENDING_APPROVAL".equalsIgnoreCase(payment.getBookingApprovalStatus())) {
                redirectAttributes.addFlashAttribute("error", "This booking is not pending approval");
                return "redirect:/landlord-dashboard";
            }

            Property property = propertyRepository.findById(payment.getPropertyId())
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));

            payment.setBookingApprovalStatus("REJECTED");
            payment.setBookingRefundStatus("PENDING");
            payment.setBookingRejectedAt(java.time.LocalDateTime.now());
            paymentRepository.save(payment);

            property.setAvailabilityStatus("AVAILABLE");
            propertyRepository.save(property);

            userRepository.findById(payment.getTenantId()).ifPresent(tenant -> {
                String tenantName = (tenant.getFirstName() + " " + tenant.getLastName()).trim();
                String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                        ? property.getPropertyId()
                        : String.valueOf(property.getId());
                emailService.sendBookingRejectedEmail(
                        tenant.getEmail(),
                        tenantName,
                        property.getName(),
                        propertyPublicId
                );
            });

            redirectAttributes.addFlashAttribute("message", "Booking rejected. Property is now available.");
            return "redirect:/landlord-dashboard";
        } catch (Exception e) {
            logger.error("Error rejecting booking {}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to reject booking: " + e.getMessage());
            return "redirect:/landlord-dashboard";
        }
    }

    @PostMapping("/property/{id}/delete")
    public String deletePropertyByLandlord(@PathVariable Long id,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByemail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Optional<Property> opt = propertyRepository.findById(id);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Property not found");
                return "redirect:/landlord/my-properties";
            }

            Property property = opt.get();
            if (!Objects.equals(property.getLandlordId(), user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to delete this property");
                return "redirect:/landlord/my-properties";
            }

            // Remove property from all tenant favorites before delete to avoid join-table constraint issues.
            for (User existingUser : userRepository.findAll()) {
                boolean removed = existingUser.getFavoriteProperties()
                        .removeIf(fav -> Objects.equals(fav.getId(), property.getId()));
                if (removed) {
                    userRepository.save(existingUser);
                }
            }

            String propertyName = property.getName();
            String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                    ? property.getPropertyId()
                    : String.valueOf(property.getId());

            propertyRepository.delete(property);

            emailService.sendPropertyDeletedEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    propertyName,
                    propertyPublicId
            );

            redirectAttributes.addFlashAttribute("message",
                    "Property deleted successfully. A confirmation email has been sent.");
            return "redirect:/landlord/my-properties";
        } catch (Exception e) {
            logger.error("Error deleting property {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete property: " + e.getMessage());
            return "redirect:/landlord/my-properties";
        }
    }

    /**
     * Serve property image file for landlord viewing
     */
    @GetMapping("/file/image/{propertyId}/{filename:.+}")
    public ResponseEntity<Resource> serveLandlordPropertyImage(
            @PathVariable Long propertyId,
            @PathVariable String filename,
            Authentication authentication) {
        
        try {
            // Verify landlord owns the property
            String email = authentication.getName();
            User user = userRepository.findByemail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<Property> opt = propertyRepository.findById(propertyId);
            if (opt.isEmpty() || !Objects.equals(opt.get().getLandlordId(), user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Build file path
            String subPath = FilePathHandler.getPropertySubPath(propertyId);
            Path filePath = FilePathHandler.buildFilePath(FilePathHandler.getPropertyUploadsPath(), subPath, filename);
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
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
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Serve property document file for landlord viewing
     */
    @GetMapping("/file/document/{propertyId}/{filename:.+}")
    public ResponseEntity<Resource> serveLandlordPropertyDocument(
            @PathVariable Long propertyId,
            @PathVariable String filename,
            Authentication authentication) {
        
        try {
            // Verify landlord owns the property
            String email = authentication.getName();
            User user = userRepository.findByemail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Optional<Property> opt = propertyRepository.findById(propertyId);
            if (opt.isEmpty() || !Objects.equals(opt.get().getLandlordId(), user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            // Build file path
            String subPath = FilePathHandler.getPropertyDocumentsSubPath(propertyId);
            Path filePath = FilePathHandler.buildFilePath(FilePathHandler.getPropertyUploadsPath(), subPath, filename);
            
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
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
            
            String disposition = fileNameLower.endsWith(".pdf") ? "inline" : "attachment";
            Resource resource = new FileSystemResource(filePath.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Show property listing form for landlord to add a new property
     */
    @GetMapping("/add-property")
    public String showAddPropertyForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"LANDLORD".equals(user.getRole())) {
            return "redirect:/role-selection";
        }

        model.addAttribute("user", user);
        model.addAttribute("property", new Property());
        return "landlord-property-form";
    }

    /**
     * Save property details (name, owner, address, city, state, pincode, price, description, location)
     */
    @PostMapping("/add-property")
    public String saveProperty(
            @RequestParam("name") String name,
            @RequestParam("ownerName") String ownerName,
            @RequestParam("address") String address,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("pincode") String pincode,
            @RequestParam("price") Double price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("latitude") Double latitude,
            @RequestParam("longitude") Double longitude,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Property property = new Property();
        property.setName(name);
        property.setOwnerName(ownerName);
        property.setAddress(address);
        property.setCity(city);
        property.setState(state);
        property.setPincode(pincode);
        property.setPrice(price);
        property.setDescription(description);
        property.setLatitude(latitude);
        property.setLongitude(longitude);
        property.setLandlordId(user.getId());
        property.setVerificationStatus("PENDING");
        property.setCreatedAt(System.currentTimeMillis());

        Property savedProperty = propertyRepository.save(property);
        redirectAttributes.addFlashAttribute("message", "Property details saved! Now upload images and documents.");

        return "redirect:/landlord/upload-images/" + savedProperty.getId();
    }

    /**
     * Show landlord name verification document upload page
     */
    @GetMapping("/upload-verification-document")
    public String showVerificationDocumentForm(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"LANDLORD".equals(user.getRole())) {
            return "redirect:/role-selection";
        }

        model.addAttribute("user", user);
        return "landlord-upload-verification";
    }

    /**
     * Handle landlord name verification document upload
     */
    @PostMapping("/upload-verification-document")
    public String uploadVerificationDocument(
            @RequestParam("document") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/landlord/upload-verification-document";
        }

        try {
            // Use utility class to handle landlord verification document upload
            String documentPath = DocumentUploadUtil.uploadLandlordVerificationDocument(file, user.getId(), FilePathHandler.getDocumentUploadsPath());
            
            user.setLandlordVerificationDocumentPath(documentPath);
            userRepository.save(user);

            redirectAttributes.addFlashAttribute("message", 
                    "Verification document uploaded successfully! Our team will verify your details.");
            return "redirect:/landlord/dashboard";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/landlord/upload-verification-document";
        } catch (java.io.IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload document: " + e.getMessage());
            return "redirect:/landlord/upload-verification-document";
        }
    }

    /**
     * Show image upload page
     */
    @GetMapping("/upload-images/{propertyId}")
    public String showImageUploadForm(@PathVariable Long propertyId, Model model) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        model.addAttribute("property", property);
        return "landlord-upload-images";
    }

    /**
     * Handle multiple image uploads
     */
    @PostMapping("/upload-images/{propertyId}")
    public String uploadImages(
            @PathVariable Long propertyId,
            @RequestParam("images") MultipartFile[] files,
            RedirectAttributes redirectAttributes) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (files.length == 0) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one image");
            return "redirect:/landlord/upload-images/" + propertyId;
        }

        try {
            logger.info("Starting image upload for property ID: {}", propertyId);
            
            // Use utility class to handle image uploads
            List<String> newImagePaths = DocumentUploadUtil.uploadPropertyImages(files, propertyId, getUploadDir());
            
            // Append to existing images instead of replacing
            List<String> allImagePaths = new ArrayList<>(DocumentUploadUtil.parseFilePaths(property.getImagesPath()));
            allImagePaths.addAll(newImagePaths);
            
            property.setImagesPath(DocumentUploadUtil.combineFilePaths(allImagePaths));
            propertyRepository.save(property);
            
            logger.info("Images uploaded successfully for property ID: {}", propertyId);

            redirectAttributes.addFlashAttribute("message", "Images uploaded successfully! Now upload documents.");
            return "redirect:/landlord/upload-documents/" + propertyId;

        } catch (IllegalArgumentException e) {
            logger.error("Validation error during image upload for property ID {}: {}", propertyId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/landlord/upload-images/" + propertyId;
        } catch (java.io.IOException e) {
            logger.error("IO error during image upload for property ID {}: {}", propertyId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to upload images: " + e.getMessage());
            return "redirect:/landlord/upload-images/" + propertyId;
        } catch (Exception e) {
            logger.error("Unexpected error during image upload for property ID {}: {}", propertyId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/landlord/upload-images/" + propertyId;
        }
    }

    /**
     * Show document upload page
     */
    @GetMapping("/upload-documents/{propertyId}")
    public String showDocumentUploadForm(@PathVariable Long propertyId, Model model) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        model.addAttribute("property", property);
        return "landlord-upload-documents";
    }

    /**
     * Handle document uploads (ownership proof, etc.)
     */
    @PostMapping("/upload-documents/{propertyId}")
    public String uploadDocuments(
            @PathVariable Long propertyId,
            @RequestParam("documents") MultipartFile[] files,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));

        if (files.length == 0) {
            redirectAttributes.addFlashAttribute("error", "Please upload at least one ownership document");
            return "redirect:/landlord/upload-documents/" + propertyId;
        }

        try {
            logger.info("Starting document upload for property ID: {}", propertyId);
            
            // Use utility class to handle document uploads
            List<String> newDocPaths = DocumentUploadUtil.uploadPropertyDocuments(files, propertyId, getUploadDir());
            
            // Append to existing documents instead of replacing
            List<String> allDocPaths = new ArrayList<>(DocumentUploadUtil.parseFilePaths(property.getDocumentsPath()));
            allDocPaths.addAll(newDocPaths);
            
            property.setDocumentsPath(DocumentUploadUtil.combineFilePaths(allDocPaths));
            propertyRepository.save(property);
            
            logger.info("Documents uploaded successfully for property ID: {}", propertyId);

            // Send email to landlord about successful property registration
            String email = authentication.getName();
            User user = userRepository.findByemail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            
            emailService.sendPropertyRegistrationEmail(user.getEmail(), user.getFirstName(), user.getLastName(), property.getName());

            redirectAttributes.addFlashAttribute("message", 
                    "Documents uploaded successfully! Your property is now pending admin verification. You will receive an email confirmation shortly.");
            return "redirect:/landlord/my-properties";

        } catch (IllegalArgumentException e) {
            logger.error("Validation error during document upload for property ID {}: {}", propertyId, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/landlord/upload-documents/" + propertyId;
        } catch (java.io.IOException e) {
            logger.error("IO error during document upload for property ID {}: {}", propertyId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to upload documents: " + e.getMessage());
            return "redirect:/landlord/upload-documents/" + propertyId;
        } catch (Exception e) {
            logger.error("Unexpected error during document upload for property ID {}: {}", propertyId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred: " + e.getMessage());
            return "redirect:/landlord/upload-documents/" + propertyId;
        }
    }

    /**
     * Show all properties listed by the landlord
     */
    @GetMapping("/my-properties")
    public String myProperties(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Property> properties = propertyRepository.findByLandlordId(user.getId());
        model.addAttribute("properties", properties);
        model.addAttribute("user", user);

        return "landlord-my-properties";
    }

    /**
     * Show complete property details
     */
    @GetMapping("/property-details/{propertyId}")
    public String viewPropertyDetails(
            @PathVariable Long propertyId,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        String methodName = "viewPropertyDetails";
        
        try {
            logger.info("{} - START: Fetching property details for ID: {}", methodName, propertyId);
            
            if (propertyId == null || propertyId <= 0) {
                logger.warn("{} - Invalid property ID: {}", methodName, propertyId);
                redirectAttributes.addFlashAttribute("error", "Invalid property ID");
                return "redirect:/landlord/my-properties";
            }
            
            // Step 1: Fetch property from repository
            logger.info("{} - Step 1: Querying database for property ID: {}", methodName, propertyId);
            Optional<Property> property = propertyRepository.findById(propertyId);
            
            if (property.isEmpty()) {
                logger.warn("{} - Step 1 FAILED: Property not found with ID: {}", methodName, propertyId);
                redirectAttributes.addFlashAttribute("error", "Property with ID " + propertyId + " not found");
                return "redirect:/landlord/my-properties";
            }
            
            Property prop = property.get();
            logger.info("{} - Step 1 SUCCESS: Found property: {}", methodName, prop.getName());
            
            // Step 2: Get current user
            logger.info("{} - Step 2: Getting current user from authentication", methodName);
            if (authentication == null) {
                logger.warn("{} - Step 2 FAILED: No authentication found", methodName);
                return "redirect:/login";
            }
            
            String email = authentication.getName();
            logger.info("{} - Step 2: Current user email: {}", methodName, email);
            
            Optional<User> userOpt = userRepository.findByemail(email);
            if (userOpt.isEmpty()) {
                logger.warn("{} - Step 2 FAILED: User not found: {}", methodName, email);
                return "redirect:/login";
            }
            
            User user = userOpt.get();
            logger.info("{} - Step 2 SUCCESS: Found user with ID: {}", methodName, user.getId());
            
            // Step 3: Verify ownership
            logger.info("{} - Step 3: Verifying ownership. Property landlord ID: {}, User ID: {}", 
                    methodName, prop.getLandlordId(), user.getId());
            
            if (prop.getLandlordId() == null || !prop.getLandlordId().equals(user.getId())) {
                logger.warn("{} - Step 3 FAILED: Unauthorized access. Property belongs to: {}, User is: {}", 
                        methodName, prop.getLandlordId(), user.getId());
                redirectAttributes.addFlashAttribute("error", "You can only view your own properties");
                return "redirect:/landlord/my-properties";
            }
            
            logger.info("{} - Step 3 SUCCESS: Ownership verified", methodName);
            
            // Step 4: Parse images
            logger.info("{} - Step 4: Parsing images", methodName);
            List<String> images = DocumentUploadUtil.parseFilePaths(prop.getImagesPath());
            logger.info("{} - Step 4 SUCCESS: Found {} images", methodName, images.size());
            
            // Step 5: Parse documents
            logger.info("{} - Step 5: Parsing documents", methodName);
            List<String> documents = DocumentUploadUtil.parseFilePaths(prop.getDocumentsPath());
            logger.info("{} - Step 5 SUCCESS: Found {} documents", methodName, documents.size());
            
            // Convert to filename lists for template (imagePaths / documentPaths)
            List<String> imagePaths = images.stream()
                    .map(p -> Paths.get(p).getFileName().toString())
                    .filter(n -> {
                        String lower = n.toLowerCase();
                        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || 
                               lower.endsWith(".gif") || lower.endsWith(".webp");
                    })
                    .collect(Collectors.toList());

            List<String> documentPaths = documents.stream()
                    .map(p -> Paths.get(p).getFileName().toString())
                    .filter(n -> {
                        String lower = n.toLowerCase();
                        return lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || 
                               lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".txt");
                    })
                    .collect(Collectors.toList());

            // Step 6: Add attributes to model
            logger.info("{} - Step 6: Adding attributes to model", methodName);
            model.addAttribute("property", prop);
            model.addAttribute("imagePaths", imagePaths);
            model.addAttribute("documentPaths", documentPaths);
            model.addAttribute("user", user);
            
            logger.info("{} - SUCCESS: Property details loaded for ID: {}. Rendering template: landlord-property-detail", 
                    methodName, propertyId);
            return "landlord-property-detail";
            
        } catch (NullPointerException npe) {
            logger.error("{} - NullPointerException: {}", methodName, npe.getMessage(), npe);
            redirectAttributes.addFlashAttribute("error", "A null value was encountered: " + npe.getMessage());
            return "redirect:/landlord/my-properties";
            
        } catch (Exception e) {
            logger.error("{} - EXCEPTION: {}", methodName, e.getMessage(), e);
            logger.error("{} - Exception type: {}", methodName, e.getClass().getName());
            logger.error("{} - Stack trace:", methodName);
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error loading property details: " + e.getMessage());
            return "redirect:/landlord/my-properties";
        }
    }

    /**
     * Display property map page for landlord to locate/update property location
     */
    @GetMapping("/property-map/{id}")
    public String showPropertyMap(@PathVariable Long id,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

            Optional<Property> opt = propertyRepository.findById(id);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Property not found");
                return "redirect:/landlord/my-properties";
            }

            Property property = opt.get();
            if (!Objects.equals(property.getLandlordId(), user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to access this property");
                return "redirect:/landlord/my-properties";
            }

            model.addAttribute("property", property);
            return "landlord-property-map";

        } catch (Exception e) {
            logger.error("Error displaying property map: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error loading property map: " + e.getMessage());
            return "redirect:/landlord/my-properties";
        }
    }

    /**
     * Update property location (latitude and longitude)
     */
    @PostMapping("/property/{id}/update-location")
    public String updatePropertyLocation(@PathVariable Long id,
                                         @RequestParam Double latitude,
                                         @RequestParam Double longitude,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

            Optional<Property> opt = propertyRepository.findById(id);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Property not found");
                return "redirect:/landlord/my-properties";
            }

            Property property = opt.get();
            if (!Objects.equals(property.getLandlordId(), user.getId())) {
                redirectAttributes.addFlashAttribute("error", "You do not have permission to update this property");
                return "redirect:/landlord/my-properties";
            }

            // Validate coordinates
            if (latitude == null || longitude == null) {
                redirectAttributes.addFlashAttribute("error", "Please provide valid latitude and longitude");
                return "redirect:/landlord/property-map/" + id;
            }

            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                redirectAttributes.addFlashAttribute("error", "Invalid coordinates. Latitude must be between -90 and 90, longitude between -180 and 180");
                return "redirect:/landlord/property-map/" + id;
            }

            property.setLatitude(latitude);
            property.setLongitude(longitude);
            propertyRepository.save(property);

            redirectAttributes.addFlashAttribute("message", "Property location updated successfully!");
            return "redirect:/landlord/property-details/" + id;

        } catch (Exception e) {
            logger.error("Error updating property location: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error updating location: " + e.getMessage());
            return "redirect:/landlord/property-map/" + id;
        }
    }
}
