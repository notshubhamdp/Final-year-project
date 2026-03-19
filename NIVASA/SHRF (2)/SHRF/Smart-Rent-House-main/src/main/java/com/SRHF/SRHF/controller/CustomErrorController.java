package com.SRHF.SRHF.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.RequestDispatcher;

@Controller
public class CustomErrorController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    /**
     * Handle 413 errors that occur during request parsing (before reaching controller)
     * These bypass the @ExceptionHandler above, so we need this explicit mapping
     */
    @GetMapping("/error")
    public String handleError(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            logger.warn("HTTP Error {} at path {}", statusCode, path);
            
            if (statusCode == 413) {
                logger.warn("413 Payload Too Large error");
                redirectAttributes.addFlashAttribute("error", 
                    "File size exceeds limit (max 50MB per file, 100MB total). Please upload smaller files.");
                
                // Redirect back to the appropriate upload page based on the request path
                String requestPath = path != null ? path.toString() : "";
                if (requestPath.contains("upload-images")) {
                    return "redirect:/landlord/upload-images";
                } else if (requestPath.contains("upload-documents")) {
                    return "redirect:/landlord/upload-documents";
                }
                return "redirect:/landlord/my-properties";
            }
        }
        
        redirectAttributes.addFlashAttribute("error", "An error occurred. Please try again.");
        return "redirect:/home";
    }

}
