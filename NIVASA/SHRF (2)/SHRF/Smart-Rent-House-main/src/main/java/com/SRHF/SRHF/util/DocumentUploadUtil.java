package com.SRHF.SRHF.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling document uploads
 * Manages property images, property documents, and student ID documents
 */
public class DocumentUploadUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadUtil.class);

    /**
     * Upload multiple image files for a property
     * @param files the image files to upload
     * @param propertyId the property ID
     * @param baseUploadDir the base upload directory (e.g., "uploads/properties")
     * @return list of absolute file paths for uploaded images
     * @throws IOException if file operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public static List<String> uploadPropertyImages(MultipartFile[] files, Long propertyId, String baseUploadDir) 
            throws IOException, IllegalArgumentException {
        
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided for upload");
        }

        List<String> imagePaths = new ArrayList<>();
        String subPath = FilePathHandler.getPropertySubPath(propertyId);

        // Build absolute path based on working directory
        String workingDir = System.getProperty("user.dir");
        Path absoluteUploadDir = Paths.get(workingDir, baseUploadDir).toAbsolutePath();
        
        logger.info("Uploading images to: {}", absoluteUploadDir);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                logger.warn("Skipping empty file: {}", file.getOriginalFilename());
                continue;
            }

            String filename = file.getOriginalFilename();
            logger.info("Processing image file: {}, ContentType: {}, Size: {} bytes", 
                       filename, file.getContentType(), file.getSize());

            // Validate file size
            if (file.getSize() > FileValidator.MAX_FILE_SIZE) {
                logger.error("Image file too large: {} ({} bytes, max {})", 
                           filename, file.getSize(), FileValidator.MAX_FILE_SIZE);
                throw new IllegalArgumentException("File '" + filename + "' is too large. " + 
                                                  FileValidator.getFileSizeErrorMessage());
            }

            // Validate file type
            if (!FileValidator.isValidImage(file)) {
                logger.error("Invalid image file: {} - Content-Type: {}", filename, file.getContentType());
                throw new IllegalArgumentException("File '" + filename + "' is not a valid image. " + 
                                                  FileValidator.getInvalidFileTypeMessage(true));
            }

            try {
                // Ensure directory exists
                Path uploadPath = absoluteUploadDir.resolve(subPath).toAbsolutePath();
                if (!FilePathHandler.ensureDirectoryExists(uploadPath)) {
                    throw new IOException("Failed to create upload directory: " + uploadPath);
                }

                // Generate unique filename and save
                String fileName = FilePathHandler.generateUniqueFileName("image", filename);
                Path filePath = uploadPath.resolve(fileName);
                Files.write(filePath, file.getBytes());
                
                logger.info("Image uploaded successfully: {}", filePath);

                // Add absolute path (for serving later)
                imagePaths.add(filePath.toString());
            } catch (IOException e) {
                logger.error("Failed to save image file: {}", filename, e);
                throw e;
            }
        }

        if (imagePaths.isEmpty()) {
            throw new IllegalArgumentException("No valid images were uploaded");
        }

        return imagePaths;
    }

    /**
     * Upload multiple document files for a property
     * @param files the document files to upload
     * @param propertyId the property ID
     * @param baseUploadDir the base upload directory (e.g., "uploads/properties")
     * @return list of absolute file paths for uploaded documents
     * @throws IOException if file operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public static List<String> uploadPropertyDocuments(MultipartFile[] files, Long propertyId, String baseUploadDir) 
            throws IOException, IllegalArgumentException {
        
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("No files provided for upload");
        }

        List<String> docPaths = new ArrayList<>();
        String subPath = FilePathHandler.getPropertyDocumentsSubPath(propertyId);

        // Build absolute path based on working directory
        String workingDir = System.getProperty("user.dir");
        Path absoluteUploadDir = Paths.get(workingDir, baseUploadDir).toAbsolutePath();
        
        logger.info("Uploading documents to: {}", absoluteUploadDir);

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                logger.warn("Skipping empty file: {}", file.getOriginalFilename());
                continue;
            }

            String filename = file.getOriginalFilename();
            logger.info("Processing document file: {}, ContentType: {}, Size: {} bytes", 
                       filename, file.getContentType(), file.getSize());

            // Validate file size
            if (file.getSize() > FileValidator.MAX_FILE_SIZE) {
                logger.error("Document file too large: {} ({} bytes, max {})", 
                           filename, file.getSize(), FileValidator.MAX_FILE_SIZE);
                throw new IllegalArgumentException("File '" + filename + "' is too large. " + 
                                                  FileValidator.getFileSizeErrorMessage());
            }

            // Validate file type
            if (!FileValidator.isValidDocument(file)) {
                logger.error("Invalid document file: {} - Content-Type: {}", filename, file.getContentType());
                throw new IllegalArgumentException("File '" + filename + "' is not a valid document. " + 
                                                  FileValidator.getInvalidFileTypeMessage(false));
            }

            try {
                // Ensure directory exists
                Path uploadPath = absoluteUploadDir.resolve(subPath).toAbsolutePath();
                if (!FilePathHandler.ensureDirectoryExists(uploadPath)) {
                    throw new IOException("Failed to create upload directory: " + uploadPath);
                }

                // Generate unique filename and save
                String fileName = FilePathHandler.generateUniqueFileName("doc", filename);
                Path filePath = uploadPath.resolve(fileName);
                Files.write(filePath, file.getBytes());
                
                logger.info("Document uploaded successfully: {}", filePath);

                // Add absolute path (for serving later)
                docPaths.add(filePath.toString());
            } catch (IOException e) {
                logger.error("Failed to save document file: {}", filename, e);
                throw e;
            }
        }

        if (docPaths.isEmpty()) {
            throw new IllegalArgumentException("No valid documents were uploaded");
        }

        return docPaths;
    }

    /**
     * Upload a single student ID document
     * @param file the student ID document file
     * @param userId the user ID
     * @param baseUploadDir the base upload directory (e.g., "uploads/documents")
     * @return absolute file path where the document is saved
     * @throws IOException if file operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public static String uploadStudentDocument(MultipartFile file, Long userId, String baseUploadDir) 
            throws IOException, IllegalArgumentException {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided for upload");
        }

        // Validate file type
        if (!FileValidator.isValidStudentDocument(file)) {
            throw new IllegalArgumentException(FileValidator.getInvalidFileTypeMessage(false));
        }

        // Ensure directory exists
        Path uploadPath = Paths.get(baseUploadDir);
        if (!FilePathHandler.ensureDirectoryExists(uploadPath)) {
            throw new IOException("Failed to create upload directory: " + uploadPath);
        }

        // Generate unique filename and save
        String fileName = FilePathHandler.generateUniqueFileName(userId, "document", file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Return only the filename (relative path) for database storage
        return fileName;
    }

    /**
     * Upload a single landlord name verification document
     * @param file the verification document file
     * @param userId the user ID
     * @param baseUploadDir the base upload directory (e.g., "uploads/documents")
     * @return absolute file path where the document is saved
     * @throws IOException if file operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public static String uploadLandlordVerificationDocument(MultipartFile file, Long userId, String baseUploadDir) 
            throws IOException, IllegalArgumentException {
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided for upload");
        }

        // Validate file type
        if (!FileValidator.isValidDocument(file)) {
            throw new IllegalArgumentException(FileValidator.getInvalidFileTypeMessage(false));
        }

        // Ensure directory exists
        Path uploadPath = Paths.get(baseUploadDir);
        if (!FilePathHandler.ensureDirectoryExists(uploadPath)) {
            throw new IOException("Failed to create upload directory: " + uploadPath);
        }

        // Generate unique filename and save
        String fileName = FilePathHandler.generateUniqueFileName(userId, "landlord_verification", file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // Return only the filename (relative path) for database storage
        return fileName;
    }

    /**
     * Parse comma-separated file paths into a list
     * @param pathString comma-separated paths
     * @return list of individual paths
     */
    public static List<String> parseFilePaths(String pathString) {
        List<String> paths = new ArrayList<>();
        if (pathString != null && !pathString.trim().isEmpty()) {
            String[] pathArray = pathString.split(",");
            for (String path : pathArray) {
                String trimmedPath = path.trim();
                if (!trimmedPath.isEmpty()) {
                    paths.add(trimmedPath);
                }
            }
        }
        return paths;
    }

    /**
     * Combine list of file paths into comma-separated string
     * @param paths list of file paths
     * @return comma-separated paths
     */
    public static String combineFilePaths(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return null;
        }
        return String.join(",", paths);
    }
}
