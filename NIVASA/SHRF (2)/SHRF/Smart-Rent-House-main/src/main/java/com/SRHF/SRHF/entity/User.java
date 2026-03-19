package com.SRHF.SRHF.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User  implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(
            name = "first_name",
            nullable = false
    )
    private String firstName;

    @Column(
            name = "last_name",
            nullable = false
    )
    private String lastName;

    @Column(
            name = "email",
            nullable = false,
            unique = true
    )
    private String email;

    @Column(
            name = "password",
            nullable = false
    )
    private String password;

    @Column(
            name = "enabled"

    )
    private boolean enabled;

    @Column(name = "role")
    private String role; // "TENANT" or "LANDLORD"

    @Column(name = "tenant_type")
    private String tenantType; // "STUDENT" or "FAMILY" (only if role == "TENANT")

    @Column(name = "document_path")
    private String documentPath; // path to uploaded student ID document

    @Column(name = "student_verification_status")
    private String studentVerificationStatus; // "PENDING", "APPROVED", or "REJECTED" for students

    @Column(name = "banned")
    private Boolean banned;

    @Column(name = "ban_reason")
    private String banReason;
    private String landlordVerificationDocumentPath; // path to uploaded landlord name verification document

    @Column(name = "phone", nullable = false)
    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip")
    private String zip;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "stripe_connected_account_id")
    private String stripeConnectedAccountId;

    @Column(name = "stripe_onboarding_complete")
    private Boolean stripeOnboardingComplete;

    @Column(name = "stripe_payouts_enabled")
    private Boolean stripePayoutsEnabled;

    @ManyToMany
    @JoinTable(
            name = "user_favorites",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "property_id")
    )
    private Set<Property> favoriteProperties = new HashSet<>();

    public User() {
    }

    public User(
            String firstName,
            String lastName,
            String email,
            String password,
            boolean enabled) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleAuthority = "ROLE_USER"; // default
        if (this.role != null && !this.role.isEmpty()) {
            roleAuthority = "ROLE_" + this.role;
        }
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleAuthority);
        return List.of(authority);
    }

    @Override
    public @Nullable String getPassword() {
        // return the stored password value (encoded when registered)
        return this.password;
    }

    @Override
    public String getUsername() {
        // use email as the username
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Base enabled flag must be true
        if (!enabled) return false;

        // Check if user is banned (treat null as not banned)
        if (banned != null && banned) return false;

        // If the user is a tenant of type STUDENT, require admin approval
        if (this.role != null && this.role.equalsIgnoreCase("TENANT")
                && this.tenantType != null && this.tenantType.equalsIgnoreCase("STUDENT")) {
            return this.studentVerificationStatus != null && this.studentVerificationStatus.equalsIgnoreCase("APPROVED");
        }

        // For other users, rely on the enabled flag
        return true;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTenantType() {
        return tenantType;
    }

    public void setTenantType(String tenantType) {
        this.tenantType = tenantType;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getLandlordVerificationDocumentPath() {
        return landlordVerificationDocumentPath;
    }

    public void setLandlordVerificationDocumentPath(String landlordVerificationDocumentPath) {
        this.landlordVerificationDocumentPath = landlordVerificationDocumentPath;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getStripeConnectedAccountId() {
        return stripeConnectedAccountId;
    }

    public void setStripeConnectedAccountId(String stripeConnectedAccountId) {
        this.stripeConnectedAccountId = stripeConnectedAccountId;
    }

    public Boolean getStripeOnboardingComplete() {
        return stripeOnboardingComplete;
    }

    public void setStripeOnboardingComplete(Boolean stripeOnboardingComplete) {
        this.stripeOnboardingComplete = stripeOnboardingComplete;
    }

    public Boolean getStripePayoutsEnabled() {
        return stripePayoutsEnabled;
    }

    public void setStripePayoutsEnabled(Boolean stripePayoutsEnabled) {
        this.stripePayoutsEnabled = stripePayoutsEnabled;
    }

    public String getProfilePicturePath() {
        return "/images/default-avatar.svg";
    }

    public Set<Property> getFavoriteProperties() {
        return favoriteProperties;
    }

    public void setFavoriteProperties(Set<Property> favoriteProperties) {
        this.favoriteProperties = favoriteProperties;
    }

    public void addFavoriteProperty(Property property) {
        this.favoriteProperties.add(property);
    }

    public void removeFavoriteProperty(Property property) {
        this.favoriteProperties.remove(property);
    }

    public boolean isFavorite(Property property) {
        return this.favoriteProperties.contains(property);
    }

    public String getStudentVerificationStatus() {
        return studentVerificationStatus;
    }

    public void setStudentVerificationStatus(String studentVerificationStatus) {
        this.studentVerificationStatus = studentVerificationStatus;
    }

    public boolean isBanned() {
        return banned != null && banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }
}
