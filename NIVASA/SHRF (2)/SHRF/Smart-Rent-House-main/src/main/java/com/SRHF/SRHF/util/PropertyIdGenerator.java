package com.SRHF.SRHF.util;

import java.util.Random;

/**
 * Utility class for generating unique 10-digit property IDs
 */
public class PropertyIdGenerator {

    private static final Random random = new Random();
    private static final int PROPERTY_ID_LENGTH = 10;

    /**
     * Generates a random 10-digit property ID
     * Format: 10 random digits (0-9)
     *
     * @return A 10-digit random property ID as a String
     */
    public static String generatePropertyId() {
        StringBuilder propertyId = new StringBuilder();
        
        for (int i = 0; i < PROPERTY_ID_LENGTH; i++) {
            int digit = random.nextInt(10); // Generates 0-9
            propertyId.append(digit);
        }
        
        return propertyId.toString();
    }
}
