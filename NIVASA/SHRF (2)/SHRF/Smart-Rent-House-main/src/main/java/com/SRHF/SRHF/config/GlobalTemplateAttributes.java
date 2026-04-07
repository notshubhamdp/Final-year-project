package com.SRHF.SRHF.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalTemplateAttributes {

    private final String googleMapsApiKey;

    public GlobalTemplateAttributes(@Value("${app.google-maps.api-key:}") String googleMapsApiKey) {
        this.googleMapsApiKey = googleMapsApiKey;
    }

    @ModelAttribute("googleMapsApiKey")
    public String googleMapsApiKey() {
        return googleMapsApiKey;
    }
}
