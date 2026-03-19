package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private static final Logger logger = LoggerFactory.getLogger(PropertyService.class);

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    /**
     * Get property details by ID
     */
    public Optional<Property> getPropertyById(Long propertyId) {
        logger.info("Fetching property with ID: {}", propertyId);
        return propertyRepository.findById(propertyId);
    }

    /**
     * Get all properties for a landlord
     */
    public List<Property> getPropertiesByLandlord(Long landlordId) {
        logger.info("Fetching properties for landlord ID: {}", landlordId);
        return propertyRepository.findByLandlordId(landlordId);
    }

    /**
     * Get all approved properties (for tenant viewing)
     */
    public List<Property> getApprovedProperties() {
        logger.info("Fetching all approved properties");
        return propertyRepository.findByVerificationStatusOrderByCreatedAtDesc("APPROVED");
    }

    /**
     * Get all pending properties (for admin review)
     */
    public List<Property> getPendingProperties() {
        logger.info("Fetching all pending properties");
        return propertyRepository.findByVerificationStatus("PENDING");
    }

    /**
     * Get favorite properties for a user
     */
    public List<Property> getFavoriteProperties(Long userId) {
        logger.info("Fetching favorite properties for user ID: {}", userId);
        return propertyRepository.findFavoritesByUserId(userId);
    }

    /**
     * Save/Create a new property
     */
    public Property saveProperty(Property property) {
        logger.info("Saving property: {}", property.getName());
        return propertyRepository.save(property);
    }

    /**
     * Update an existing property
     */
    public Property updateProperty(Long propertyId, Property updatedProperty) {
        logger.info("Updating property with ID: {}", propertyId);
        Optional<Property> existingProperty = propertyRepository.findById(propertyId);
        
        if (existingProperty.isPresent()) {
            Property property = existingProperty.get();
            
            // Update only non-null fields
            if (updatedProperty.getName() != null) {
                property.setName(updatedProperty.getName());
            }
            if (updatedProperty.getDescription() != null) {
                property.setDescription(updatedProperty.getDescription());
            }
            if (updatedProperty.getPrice() != null) {
                property.setPrice(updatedProperty.getPrice());
            }
            if (updatedProperty.getPropertyType() != null) {
                property.setPropertyType(updatedProperty.getPropertyType());
            }
            if (updatedProperty.getBedrooms() != null) {
                property.setBedrooms(updatedProperty.getBedrooms());
            }
            if (updatedProperty.getBathrooms() != null) {
                property.setBathrooms(updatedProperty.getBathrooms());
            }
            if (updatedProperty.getIsFurnished() != null) {
                property.setIsFurnished(updatedProperty.getIsFurnished());
            }
            if (updatedProperty.getHasParking() != null) {
                property.setHasParking(updatedProperty.getHasParking());
            }
            if (updatedProperty.getHasWater() != null) {
                property.setHasWater(updatedProperty.getHasWater());
            }
            if (updatedProperty.getAvailabilityStatus() != null) {
                property.setAvailabilityStatus(updatedProperty.getAvailabilityStatus());
            }
            
            logger.info("Property updated successfully: {}", propertyId);
            return propertyRepository.save(property);
        }
        
        throw new IllegalArgumentException("Property not found with ID: " + propertyId);
    }

    /**
     * Delete a property
     */
    public void deleteProperty(Long propertyId) {
        logger.info("Deleting property with ID: {}", propertyId);
        propertyRepository.deleteById(propertyId);
    }

    /**
     * Get images as a list
     */
    public List<String> getPropertyImages(Long propertyId) {
        Optional<Property> property = propertyRepository.findById(propertyId);
        if (property.isPresent() && property.get().getImagesPath() != null) {
            String imagesPath = property.get().getImagesPath();
            return List.of(imagesPath.split(","));
        }
        return List.of();
    }

    /**
     * Get documents as a list
     */
    public List<String> getPropertyDocuments(Long propertyId) {
        Optional<Property> property = propertyRepository.findById(propertyId);
        if (property.isPresent() && property.get().getDocumentsPath() != null) {
            String docsPath = property.get().getDocumentsPath();
            return List.of(docsPath.split(","));
        }
        return List.of();
    }
}
