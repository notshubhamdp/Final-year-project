package com.SRHF.SRHF.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Uploaded file is too large. Maximum allowed size is 100MB. Please try with a smaller file.");
        return "redirect:/profile";
    }

    /**
     * Handle general exceptions - displays a user-friendly error page
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericException(Exception ex, Model model, HttpServletRequest request) {
        logger.error("Unexpected error occurred at path: {}", request.getRequestURI(), ex);
        
        model.addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("message", "An unexpected error occurred. Please try again or contact support if the problem persists.");
        model.addAttribute("path", request.getRequestURI());
        
        // Log specific error details for debugging
        logger.error("Error Type: {}", ex.getClass().getName());
        logger.error("Error Message: {}", ex.getMessage());
        
        return "error";
    }

    /**
     * Handle IllegalArgumentException - bad input
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model, HttpServletRequest request) {
        logger.warn("Illegal argument at path: {}", request.getRequestURI());
        logger.warn("Error: {}", ex.getMessage());
        
        model.addAttribute("status", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("message", ex.getMessage() != null ? ex.getMessage() : "Invalid request parameter.");
        model.addAttribute("path", request.getRequestURI());
        
        return "error";
    }

}
