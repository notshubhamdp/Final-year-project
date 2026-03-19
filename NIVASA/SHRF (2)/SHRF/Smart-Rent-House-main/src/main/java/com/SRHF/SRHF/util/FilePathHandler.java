package com.SRHF.SRHF.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Utility class for handling file paths and directory operations
 */
public class FilePathHandler {

    private static final String FORWARD_SLASH = "/";
    // Use the uploads folder (not in src/main/resources)
    private static final String UPLOADS_BASE_DIR = "uploads";
    private static final String PROPERTIES_SUBDIR = "properties";
    private static final String DOCUMENTS_SUBDIR = "documents";

    /**
     * Create directory if it doesn't exist
     * @param uploadPath the path to create
     * @return true if directory was created or already exists, false if creation fails
     */
    public static boolean ensureDirectoryExists(Path uploadPath) {
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Convert path to web-friendly format (using forward slashes)
     * @param path the path to convert
     * @return web-friendly path with forward slashes
     */
    public static String toWebPath(String path) {
        if (path == null) {
            return null;
        }
        return path.replace("\\", "/");
    }

    /**
     * Generate a unique filename with timestamp
     * @param originalFilename the original filename
     * @return unique filename
     */
    public static String generateUniqueFileName(String originalFilename) {
        return "file_" + System.currentTimeMillis() + "_" + originalFilename;
    }

    /**
     * Generate a unique filename with prefix and timestamp
     * @param prefix the prefix for the filename (e.g., "image", "doc")
     * @param originalFilename the original filename
     * @return unique filename with prefix
     */
    public static String generateUniqueFileName(String prefix, String originalFilename) {
        return prefix + "_" + System.currentTimeMillis() + "_" + originalFilename;
    }

    /**
     * Generate a unique filename with user ID, prefix, and timestamp
     * @param userId the user ID
     * @param prefix the prefix for the filename (e.g., "image", "doc")
     * @param originalFilename the original filename
     * @return unique filename with user ID and prefix
     */
    public static String generateUniqueFileName(Long userId, String prefix, String originalFilename) {
        return userId + "_" + prefix + "_" + System.currentTimeMillis() + "_" + originalFilename;
    }

    /**
     * Build relative web path for uploaded file
     * @param baseUploadDir the base upload directory (e.g., "uploads/properties")
     * @param subPath the sub path (e.g., "property-1", "property-1/documents")
     * @param fileName the filename
     * @return relative web path with forward slashes
     */
    public static String buildRelativeWebPath(String baseUploadDir, String subPath, String fileName) {
        String basePath = toWebPath(baseUploadDir);
        if (subPath != null && !subPath.isEmpty()) {
            return basePath + FORWARD_SLASH + toWebPath(subPath) + FORWARD_SLASH + fileName;
        }
        return basePath + FORWARD_SLASH + fileName;
    }

    /**
     * Build file path for saving file to disk
     * @param baseDir the base directory
     * @param subPath the sub path
     * @param fileName the filename
     * @return Path object for the file
     */
    public static Path buildFilePath(String baseDir, String subPath, String fileName) {
        if (subPath != null && !subPath.isEmpty()) {
            return Paths.get(baseDir, subPath, fileName);
        }
        return Paths.get(baseDir, fileName);
    }

    /**
     * Get the sub path for property uploads
     * @param propertyId the property ID
     * @return sub path for the property
     */
    public static String getPropertySubPath(Long propertyId) {
        return "property-" + propertyId;
    }

    /**
     * Get the sub path for property documents
     * @param propertyId the property ID
     * @return sub path for the property documents
     */
    public static String getPropertyDocumentsSubPath(Long propertyId) {
        return "property-" + propertyId + "/documents";
    }

    /**
     * Get the full path for resources uploads folder
     * @return full path to resources/uploads directory
     */
    public static String getResourcesUploadsPath() {
        return UPLOADS_BASE_DIR;
    }

    /**
     * Get the full path for property uploads folder
     * @return full path to resources/uploads/properties directory
     */
    public static String getPropertyUploadsPath() {
        return UPLOADS_BASE_DIR + FORWARD_SLASH + PROPERTIES_SUBDIR;
    }

    /**
     * Get the full path for document uploads folder
     * @return full path to resources/uploads/documents directory
     */
    public static String getDocumentUploadsPath() {
        return UPLOADS_BASE_DIR + FORWARD_SLASH + DOCUMENTS_SUBDIR;
    }

    /**
     * Get the uploads base directory path
     * @return full path to uploads directory
     */
    public static String getUploadDirectory() {
        return UPLOADS_BASE_DIR;
    }

    /**
     * Get the absolute path for document uploads
     * Resolves relative to the parent of the project directory
     * @return absolute path to documents directory
     */
    public static String getAbsoluteDocumentUploadsPath() {
        try {
            // Get the working directory and navigate up to find the uploads folder
            Path currentPath = Paths.get(System.getProperty("user.dir"));
            
            // Check if uploads/documents exists relative to current directory
            Path uploadsPath = currentPath.resolve(UPLOADS_BASE_DIR).resolve(DOCUMENTS_SUBDIR);
            if (Files.exists(uploadsPath)) {
                return uploadsPath.toAbsolutePath().toString();
            }
            
            // If not found, try parent directory (for when running from Smart-Rent-House-main folder)
            uploadsPath = currentPath.getParent().resolve(UPLOADS_BASE_DIR).resolve(DOCUMENTS_SUBDIR);
            if (Files.exists(uploadsPath)) {
                return uploadsPath.toAbsolutePath().toString();
            }
            
            // Fallback: return the relative path
            return getDocumentUploadsPath();
        } catch (Exception e) {
            return getDocumentUploadsPath();
        }
    }
}
