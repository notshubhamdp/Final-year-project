package com.SRHF.SRHF.entity;

import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;
import com.SRHF.SRHF.util.PropertyIdGenerator;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "property_id", nullable = false, unique = true, length = 10)
    private String propertyId; // 10-digit unique property identifier

    @Column(name = "name", nullable = false)
    private String name; // House name/title

    @Column(name = "owner_name", nullable = false)
    private String ownerName; // Landlord's name

    @Column(name = "description")
    private String description; // House description

    @Column(name = "address", nullable = false)
    private String address; // Full address

    @Column(name = "city")
    private String city; // City

    @Column(name = "state")
    private String state; // State/Province

    @Column(name = "pincode")
    private String pincode; // Postal/Zip code

    @Column(name = "latitude")
    private Double latitude; // House location latitude

    @Column(name = "longitude")
    private Double longitude; // House location longitude

    @Column(name = "price", nullable = false, columnDefinition = "DECIMAL(12,2)")
    private Double price; // Monthly rent price

    @Column(name = "landlord_id", nullable = false)
    private Long landlordId; // Reference to User who owns this property

    @Column(name = "images_path", columnDefinition = "LONGTEXT")
    private String imagesPath; // Comma-separated paths to house images

    @Column(name = "documents_path", columnDefinition = "LONGTEXT")
    private String documentsPath; // Comma-separated paths to ownership documents

    @Column(name = "verification_status")
    private String verificationStatus; // "PENDING", "APPROVED", "REJECTED"

    @Column(name = "admin_notes")
    private String adminNotes; // Admin's notes on verification

    @Column(name = "created_at")
    private Long createdAt; // Timestamp when property was created

    @Column(name = "property_type")
    private String propertyType; // HOUSE, FLAT, ROOM

    @Column(name = "bedrooms")
    private Integer bedrooms; // Number of bedrooms

    @Column(name = "bathrooms")
    private Integer bathrooms; // Number of bathrooms

    @Column(name = "is_furnished")
    private Boolean isFurnished; // true = furnished, false = unfurnished

    @Column(name = "has_parking")
    private Boolean hasParking; // true if parking available

    @Column(name = "has_water")
    private Boolean hasWater; // true if water facility available

    @Column(name = "availability_status")
    private String availabilityStatus; // AVAILABLE, NOT_AVAILABLE

    @ManyToMany(mappedBy = "favoriteProperties")
    private Set<User> favoredBy = new HashSet<>();

    public Property() {
    }

    public Property(String name, String ownerName, String address, Double price, Long landlordId) {
        this.name = name;
        this.ownerName = ownerName;
        this.address = address;
        this.price = price;
        this.landlordId = landlordId;
        this.verificationStatus = "PENDING";
        this.availabilityStatus = "AVAILABLE";
        this.createdAt = System.currentTimeMillis();
    }

    @PrePersist
    protected void generatePropertyId() {
        if (this.propertyId == null) {
            this.propertyId = PropertyIdGenerator.generatePropertyId();
        }
        if (this.availabilityStatus == null || this.availabilityStatus.isBlank()) {
            this.availabilityStatus = "AVAILABLE";
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(Long landlordId) {
        this.landlordId = landlordId;
    }

    public String getImagesPath() {
        return imagesPath;
    }

    public void setImagesPath(String imagesPath) {
        this.imagesPath = imagesPath;
    }

    public String getDocumentsPath() {
        return documentsPath;
    }

    public void setDocumentsPath(String documentsPath) {
        this.documentsPath = documentsPath;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Set<User> getFavoredBy() {
        return favoredBy;
    }

    public void setFavoredBy(Set<User> favoredBy) {
        this.favoredBy = favoredBy;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public Boolean getIsFurnished() {
        return isFurnished;
    }

    public void setIsFurnished(Boolean isFurnished) {
        this.isFurnished = isFurnished;
    }

    // JavaBean-style boolean getter for Thymeleaf expressions like ${property.isFurnished}
    public boolean isFurnished() {
        return Boolean.TRUE.equals(this.isFurnished);
    }

    public Boolean getHasParking() {
        return hasParking;
    }

    public void setHasParking(Boolean hasParking) {
        this.hasParking = hasParking;
    }

    public Boolean getHasWater() {
        return hasWater;
    }

    public void setHasWater(Boolean hasWater) {
        this.hasWater = hasWater;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
