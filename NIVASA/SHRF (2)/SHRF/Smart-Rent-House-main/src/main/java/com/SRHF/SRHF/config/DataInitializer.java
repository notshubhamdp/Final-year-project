package com.SRHF.SRHF.config;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.util.PropertyIdGenerator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeAdminUser(UserRepository userRepository,
                                                 PropertyRepository propertyRepository,
                                                 PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if admin user already exists
            if (userRepository.findByemail("admin@gmail.com").isEmpty()) {
                // Create default admin user
                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setEmail("admin@gmail.com");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole("ADMIN");
                admin.setEnabled(true);
                admin.setPhone("1234567890");
                
                userRepository.save(admin);
                System.out.println("Default admin user created successfully!");
                System.out.println("Email: admin@gmail.com");
                System.out.println("Password: admin");
            } else {
                System.out.println("Admin user already exists");
            }

            // Backfill missing/invalid public property IDs to 10-digit random IDs.
            int updatedCount = 0;
            for (Property property : propertyRepository.findAll()) {
                String publicId = property.getPropertyId();
                boolean valid = publicId != null && publicId.matches("\\d{10}");
                if (!valid) {
                    property.setPropertyId(generateUniquePropertyId(propertyRepository));
                    propertyRepository.save(property);
                    updatedCount++;
                }
            }
            if (updatedCount > 0) {
                System.out.println("Backfilled 10-digit property IDs for " + updatedCount + " properties.");
            }
        };
    }

    private String generateUniquePropertyId(PropertyRepository propertyRepository) {
        String candidate;
        do {
            candidate = PropertyIdGenerator.generatePropertyId();
        } while (propertyRepository.existsByPropertyId(candidate));
        return candidate;
    }
}
