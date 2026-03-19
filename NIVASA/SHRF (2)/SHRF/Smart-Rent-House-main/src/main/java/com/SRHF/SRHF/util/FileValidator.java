package com.SRHF.SRHF.util;

import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for validating uploaded files
 */
public class FileValidator {

    // File type constants
    public static final String IMAGE_MIME_TYPE = "image/";
    public static final String PDF_MIME_TYPE = "application/pdf";
    
    // File size constants (in bytes)
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    // Valid extensions
    private static final Set<String> VALID_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    ));
    
    private static final Set<String> VALID_DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx", ".xls", ".xlsx", ".txt"
    ));

    /**
     * Validate if file is a valid image
     * @param file the multipart file to validate
     * @return true if file is a valid image, false otherwise
     */
    public static boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }
        
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        // Check MIME type first
        if (contentType != null && contentType.startsWith(IMAGE_MIME_TYPE)) {
            return true;
        }
        
        // Fall back to extension validation
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            return VALID_IMAGE_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        }
        
        return false;
    }

    /**
     * Validate if file is a valid document (PDF or Image)
     * @param file the multipart file to validate
     * @return true if file is a valid document, false otherwise
     */
    public static boolean isValidDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return false;
        }
        
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        // Check MIME type first
        if (contentType != null) {
            if (contentType.startsWith(IMAGE_MIME_TYPE) || contentType.equals(PDF_MIME_TYPE)) {
                return true;
            }
        }
        
        // Fall back to extension validation
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            return VALID_DOCUMENT_EXTENSIONS.stream().anyMatch(lowerName::endsWith);
        }
        
        return false;
    }

    /**
     * Validate if file is a valid student document (PDF or Image)
     * @param file the multipart file to validate
     * @return true if file is a valid student document, false otherwise
     */
    public static boolean isValidStudentDocument(MultipartFile file) {
        return isValidDocument(file);
    }

    /**
     * Get error message for invalid file type
     * @param isImageOnly whether only images are allowed
     * @return error message
     */
    public static String getInvalidFileTypeMessage(boolean isImageOnly) {
        if (isImageOnly) {
            return "Only image files (JPG, PNG, GIF, WebP) are allowed";
        }
        return "Only PDF and image files (JPG, PNG) are allowed";
    }
    
    /**
     * Get error message for file too large
     * @return error message
     */
    public static String getFileSizeErrorMessage() {
        return "File size exceeds maximum limit of 5MB. Please use a smaller file.";
    }
}
