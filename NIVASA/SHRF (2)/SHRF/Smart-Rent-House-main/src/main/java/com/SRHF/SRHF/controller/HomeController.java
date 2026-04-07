package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.Payment;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.File;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.PaymentRepository;
import com.SRHF.SRHF.repository.AppReviewRepository;
import com.SRHF.SRHF.service.PaymentService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final PaymentRepository paymentRepository;
    private final AppReviewRepository appReviewRepository;
    private final PaymentService paymentService;
   
    public HomeController(UserRepository userRepository,
                          PropertyRepository propertyRepository,
                          PaymentRepository paymentRepository,
                          AppReviewRepository appReviewRepository,
                          PaymentService paymentService) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.paymentRepository = paymentRepository;
        this.appReviewRepository = appReviewRepository;
        this.paymentService = paymentService;
    }

    private Optional<Property> resolveProperty(String propertyRef) {
        Optional<Property> byPublicId = propertyRepository.findByPropertyId(propertyRef);
        if (byPublicId.isPresent()) {
            return byPublicId;
        }
        try {
            return propertyRepository.findById(Long.valueOf(propertyRef));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private List<HomeReviewView> getHomeReviews() {
        return appReviewRepository.findTop3ByOrderByCreatedAtDesc()
                .stream()
                .map(review -> new HomeReviewView(
                        review.getReviewerName(),
                        review.getReviewerRole(),
                        review.getReviewerCity(),
                        review.getReviewText(),
                        review.getRating() != null ? review.getRating() : 0
                ))
                .toList();
    }

    private void addHomeStats(Model model) {
        long totalPropertiesListed = propertyRepository.count();
        long totalUsersRegistered = userRepository.count();
        long totalReviewRating = Optional.ofNullable(appReviewRepository.sumAllRatings()).orElse(0L);

        model.addAttribute("totalPropertiesListed", totalPropertiesListed);
        model.addAttribute("totalUsersRegistered", totalUsersRegistered);
        model.addAttribute("totalReviewRating", totalReviewRating);
    }

    private String resolvePropertyPublicId(Property property, Long fallbackId) {
        if (property != null && property.getPropertyId() != null && !property.getPropertyId().isBlank()) {
            return property.getPropertyId();
        }
        return fallbackId != null ? String.valueOf(fallbackId) : "";
    }

    private List<Payment> sortPaymentsNewestFirst(List<Payment> payments) {
        return payments.stream()
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.getCreatedAt() != null ? left.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime rightTime = right.getCreatedAt() != null ? right.getCreatedAt() : LocalDateTime.MIN;
                    return rightTime.compareTo(leftTime);
                })
                .toList();
    }

    private Map<Long, Property> loadPropertiesById(List<Payment> payments) {
        List<Long> propertyIds = payments.stream()
                .map(Payment::getPropertyId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, Property> propertyById = new HashMap<>();
        propertyRepository.findAllById(propertyIds)
                .forEach(property -> propertyById.put(property.getId(), property));
        return propertyById;
    }

    private String formatPaymentTypeLabel(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return "Payment";
        }
        return switch (paymentType.toUpperCase()) {
            case "ADVANCE" -> "Advance Payment";
            case "RENT" -> "Monthly Rent";
            case "DEPOSIT" -> "Security Deposit";
            default -> paymentType.substring(0, 1).toUpperCase() + paymentType.substring(1).toLowerCase();
        };
    }

    private String formatBookingStatusLabel(String bookingApprovalStatus) {
        if (bookingApprovalStatus == null || bookingApprovalStatus.isBlank()) {
            return "Processing";
        }
        return switch (bookingApprovalStatus.toUpperCase()) {
            case "APPROVED" -> "Booking Approved";
            case "PENDING_APPROVAL" -> "Awaiting Landlord Approval";
            case "REJECTED" -> "Booking Rejected";
            default -> bookingApprovalStatus.replace('_', ' ');
        };
    }

    private List<LandlordActiveRentalView> buildLandlordActiveRentals(Long landlordId,
                                                                      Map<Long, Property> propertyById,
                                                                      Map<Long, String> tenantNameCache) {
        Map<Long, Payment> latestApprovedBookingByProperty = new LinkedHashMap<>();

        paymentRepository.findByLandlordIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(landlordId, "COMPLETED", "ADVANCE")
                .stream()
                .filter(payment -> "APPROVED".equalsIgnoreCase(payment.getBookingApprovalStatus()))
                .forEach(payment -> latestApprovedBookingByProperty.putIfAbsent(payment.getPropertyId(), payment));

        return latestApprovedBookingByProperty.values().stream()
                .map(booking -> {
                    Property property = propertyById.get(booking.getPropertyId());
                    if (property == null) {
                        return null;
                    }

                    String tenantName = tenantNameCache.computeIfAbsent(
                            booking.getTenantId(),
                            tenantId -> userRepository.findById(tenantId)
                                    .map(tenant -> (tenant.getFirstName() + " " + tenant.getLastName()).trim())
                                    .orElse("Tenant")
                    );

                    PaymentService.RentCycleStatus rentCycleStatus = paymentService.getCurrentRentCycleStatus(booking);
                    Payment currentRentPayment = rentCycleStatus.currentCyclePayment();

                    return new LandlordActiveRentalView(
                            property.getName(),
                            resolvePropertyPublicId(property, booking.getPropertyId()),
                            tenantName,
                            property.getPrice() != null ? property.getPrice() : 0.0,
                            rentCycleStatus.dueDate(),
                            rentCycleStatus.paidForCurrentCycle(),
                            currentRentPayment != null ? currentRentPayment.getId() : null,
                            currentRentPayment != null ? currentRentPayment.getCreatedAt() : null
                    );
                })
                .filter(java.util.Objects::nonNull)
                .limit(10)
                .toList();
    }

    private List<TenantBookedPropertyView> buildTenantBookedProperties(List<Payment> tenantPayments,
                                                                       Map<Long, Property> propertyById) {
        Map<Long, TenantBookedPropertyView> bookingsByProperty = new LinkedHashMap<>();

        for (Payment payment : sortPaymentsNewestFirst(tenantPayments)) {
            if (!"ADVANCE".equalsIgnoreCase(payment.getPaymentType())
                    || !"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                continue;
            }

            String bookingStatus = payment.getBookingApprovalStatus();
            if (!"PENDING_APPROVAL".equalsIgnoreCase(bookingStatus)
                    && !"APPROVED".equalsIgnoreCase(bookingStatus)) {
                continue;
            }

            Property property = propertyById.get(payment.getPropertyId());
            if (property == null || bookingsByProperty.containsKey(property.getId())) {
                continue;
            }

            PaymentService.RentCycleStatus rentCycleStatus = "APPROVED".equalsIgnoreCase(bookingStatus)
                    ? paymentService.getCurrentRentCycleStatus(payment)
                    : new PaymentService.RentCycleStatus(null, false, null);
            Payment currentRentPayment = rentCycleStatus.currentCyclePayment();

            String addressLine = property.getAddress();
            if (property.getCity() != null && !property.getCity().isBlank()) {
                addressLine = addressLine + ", " + property.getCity();
            }

            bookingsByProperty.put(property.getId(), new TenantBookedPropertyView(
                    property.getId(),
                    resolvePropertyPublicId(property, payment.getPropertyId()),
                    property.getName(),
                    addressLine,
                    property.getOwnerName(),
                    property.getPrice() != null ? property.getPrice() : 0.0,
                    payment.getAmount() != null ? payment.getAmount() : 0.0,
                    bookingStatus,
                    formatBookingStatusLabel(bookingStatus),
                    payment.getId(),
                    payment.getCreatedAt(),
                    property.getImagesPath() != null && !property.getImagesPath().isBlank(),
                    rentCycleStatus.dueDate(),
                    rentCycleStatus.paidForCurrentCycle(),
                    currentRentPayment != null ? currentRentPayment.getId() : null
            ));
        }

        return new ArrayList<>(bookingsByProperty.values());
    }

    private List<TenantPaymentSummaryView> buildTenantRecentPayments(List<Payment> tenantPayments,
                                                                     Map<Long, Property> propertyById,
                                                                     int limit) {
        return sortPaymentsNewestFirst(tenantPayments).stream()
                .limit(limit)
                .map(payment -> {
                    Property property = propertyById.get(payment.getPropertyId());
                    String propertyName = property != null && property.getName() != null && !property.getName().isBlank()
                            ? property.getName()
                            : "Property #" + payment.getPropertyId();
                    String propertyPublicId = resolvePropertyPublicId(property, payment.getPropertyId());
                    String bookingStatusLabel = "ADVANCE".equalsIgnoreCase(payment.getPaymentType())
                            ? formatBookingStatusLabel(payment.getBookingApprovalStatus())
                            : null;

                    return new TenantPaymentSummaryView(
                            payment.getId(),
                            propertyName,
                            propertyPublicId,
                            payment.getAmount() != null ? payment.getAmount() : 0.0,
                            payment.getStatus(),
                            formatPaymentTypeLabel(payment.getPaymentType()),
                            bookingStatusLabel,
                            payment.getCreatedAt()
                    );
                })
                .toList();
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("homeReviews", getHomeReviews());
        addHomeStats(model);
        return "home";
    }

    @GetMapping("/home")
    public String homePage(Model model) {
        model.addAttribute("homeReviews", getHomeReviews());
        addHomeStats(model);
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User user = userRepository.findByemail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("ADMIN".equals(user.getRole())) {
            return "redirect:/admin-dashboard";
        }

        if ("LANDLORD".equals(user.getRole())) {
            return "redirect:/landlord-dashboard";
        }

        if ("TENANT".equals(user.getRole())) {
            return "redirect:/tenant-dashboard";
        }

        return "redirect:/home";
    }

    @GetMapping("/landlord-dashboard")
    public String landlordDashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!"LANDLORD".equals(user.getRole())) {
            return "redirect:/home";
        }
        
        // Load landlord properties and compute stats
        List<Property> properties = propertyRepository.findByLandlordId(user.getId());

        long totalProperties = properties.size();
        long pendingCount = properties.stream().filter(p -> "PENDING".equals(p.getVerificationStatus())).count();
        long approvedCount = properties.stream().filter(p -> "APPROVED".equals(p.getVerificationStatus())).count();
        long rejectedCount = properties.stream().filter(p -> "REJECTED".equals(p.getVerificationStatus())).count();
        long rentedCount = properties.stream().filter(p -> "NOT_AVAILABLE".equalsIgnoreCase(p.getAvailabilityStatus())).count();

        // recent properties (most recent 6)
        List<Property> recentProperties = properties.stream()
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .limit(6)
                .toList();

        // Recent booking notifications (completed ADVANCE payments for this landlord)
        Map<Long, Property> propertyById = properties.stream()
                .collect(Collectors.toMap(Property::getId, p -> p));
        Map<Long, String> tenantNameCache = new HashMap<>();

        List<BookingNotificationView> recentBookings = paymentRepository
                .findByLandlordIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(user.getId(), "COMPLETED", "ADVANCE")
                .stream()
                .map(payment -> {
                    Property property = propertyById.get(payment.getPropertyId());
                    if (property == null) {
                        return null;
                    }

                    String tenantName = tenantNameCache.computeIfAbsent(
                            payment.getTenantId(),
                            tenantId -> userRepository.findById(tenantId)
                                    .map(tenant -> (tenant.getFirstName() + " " + tenant.getLastName()).trim())
                                    .orElse("Tenant")
                    );

                    String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                            ? property.getPropertyId()
                            : String.valueOf(property.getId());

                    return new BookingNotificationView(
                            property.getName(),
                            propertyPublicId,
                            tenantName,
                            payment.getAmount(),
                            payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : ""
                    );
                })
                .filter(java.util.Objects::nonNull)
                .limit(6)
                .toList();

        List<PendingBookingApprovalView> pendingBookingApprovals = paymentRepository
                .findByLandlordIdAndStatusAndPaymentTypeAndBookingApprovalStatusOrderByCreatedAtDesc(
                        user.getId(), "COMPLETED", "ADVANCE", "PENDING_APPROVAL")
                .stream()
                .map(payment -> {
                    Property property = propertyById.get(payment.getPropertyId());
                    if (property == null) {
                        return null;
                    }

                    String tenantName = tenantNameCache.computeIfAbsent(
                            payment.getTenantId(),
                            tenantId -> userRepository.findById(tenantId)
                                    .map(tenant -> (tenant.getFirstName() + " " + tenant.getLastName()).trim())
                                    .orElse("Tenant")
                    );

                    String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                            ? property.getPropertyId()
                            : String.valueOf(property.getId());

                    return new PendingBookingApprovalView(
                            payment.getId(),
                            property.getName(),
                            propertyPublicId,
                            tenantName,
                            payment.getAmount() != null ? payment.getAmount() : 0.0,
                            payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : ""
                    );
                })
                .filter(java.util.Objects::nonNull)
                .limit(10)
                .toList();

        List<LandlordActiveRentalView> activeRentals = buildLandlordActiveRentals(user.getId(), propertyById, tenantNameCache);

        model.addAttribute("user", user);
        model.addAttribute("totalProperties", totalProperties);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("rentedCount", rentedCount);
        model.addAttribute("recentProperties", recentProperties);
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("pendingBookingApprovals", pendingBookingApprovals);
        model.addAttribute("activeRentals", activeRentals);
        model.addAttribute("canSubmitReview", !appReviewRepository.existsByUserId(user.getId()));
        appReviewRepository.findByUserId(user.getId())
                .ifPresent(review -> model.addAttribute("existingReview", review));

        return "landlord-dashboard";
    }

    @GetMapping("/landlord-wallet")
    public String landlordWallet(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"LANDLORD".equals(user.getRole())) {
            return "redirect:/home";
        }

        List<Property> properties = propertyRepository.findByLandlordId(user.getId());
        Map<Long, Property> propertyById = properties.stream()
                .collect(Collectors.toMap(Property::getId, p -> p));
        Map<Long, String> tenantNameCache = new HashMap<>();

        List<Payment> completedPayments = paymentRepository.findByLandlordIdAndStatus(user.getId(), "COMPLETED")
                .stream()
                .sorted((a, b) -> {
                    LocalDateTime at = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime bt = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return bt.compareTo(at);
                })
                .toList();

        double walletTotalAmount = completedPayments.stream()
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        LocalDate now = LocalDate.now();
        double thisMonthAmount = completedPayments.stream()
                .filter(p -> p.getCreatedAt() != null
                        && p.getCreatedAt().getMonthValue() == now.getMonthValue()
                        && p.getCreatedAt().getYear() == now.getYear())
                .mapToDouble(p -> p.getAmount() != null ? p.getAmount() : 0.0)
                .sum();

        long thisMonthPaymentCount = completedPayments.stream()
                .filter(p -> p.getCreatedAt() != null
                        && p.getCreatedAt().getMonthValue() == now.getMonthValue()
                        && p.getCreatedAt().getYear() == now.getYear())
                .count();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        List<WalletPaymentView> walletPayments = completedPayments.stream()
                .map(payment -> {
                    Property property = propertyById.get(payment.getPropertyId());
                    String propertyName = property != null ? property.getName() : "Property";
                    String propertyPublicId = property != null && property.getPropertyId() != null && !property.getPropertyId().isBlank()
                            ? property.getPropertyId()
                            : String.valueOf(payment.getPropertyId());

                    String tenantName = tenantNameCache.computeIfAbsent(
                            payment.getTenantId(),
                            tenantId -> userRepository.findById(tenantId)
                                    .map(tenant -> (tenant.getFirstName() + " " + tenant.getLastName()).trim())
                                    .orElse("Tenant")
                    );

                    LocalDateTime paidAt = payment.getCreatedAt() != null ? payment.getCreatedAt() : LocalDateTime.now();
                    return new WalletPaymentView(
                            propertyName,
                            propertyPublicId,
                            tenantName,
                            payment.getPaymentType() != null ? payment.getPaymentType() : "-",
                            payment.getAmount() != null ? payment.getAmount() : 0.0,
                            monthFormatter.format(paidAt),
                            dateFormatter.format(paidAt),
                            timeFormatter.format(paidAt)
                    );
                })
                .limit(50)
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("walletTotalAmount", walletTotalAmount);
        model.addAttribute("walletPaymentCount", completedPayments.size());
        model.addAttribute("thisMonthAmount", thisMonthAmount);
        model.addAttribute("thisMonthPaymentCount", thisMonthPaymentCount);
        model.addAttribute("walletPayments", walletPayments);

        return "landlord-wallet";
    }

    public static class BookingNotificationView {
        private final String propertyName;
        private final String propertyId;
        private final String tenantName;
        private final Double amount;
        private final String bookedAt;

        public BookingNotificationView(String propertyName, String propertyId, String tenantName, Double amount, String bookedAt) {
            this.propertyName = propertyName;
            this.propertyId = propertyId;
            this.tenantName = tenantName;
            this.amount = amount;
            this.bookedAt = bookedAt;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public Double getAmount() {
            return amount;
        }

        public String getBookedAt() {
            return bookedAt;
        }
    }

    public static class WalletPaymentView {
        private final String propertyName;
        private final String propertyId;
        private final String tenantName;
        private final String paymentType;
        private final Double amount;
        private final String monthLabel;
        private final String paidDate;
        private final String paidTime;

        public WalletPaymentView(String propertyName,
                                 String propertyId,
                                 String tenantName,
                                 String paymentType,
                                 Double amount,
                                 String monthLabel,
                                 String paidDate,
                                 String paidTime) {
            this.propertyName = propertyName;
            this.propertyId = propertyId;
            this.tenantName = tenantName;
            this.paymentType = paymentType;
            this.amount = amount;
            this.monthLabel = monthLabel;
            this.paidDate = paidDate;
            this.paidTime = paidTime;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public Double getAmount() {
            return amount;
        }

        public String getMonthLabel() {
            return monthLabel;
        }

        public String getPaidDate() {
            return paidDate;
        }

        public String getPaidTime() {
            return paidTime;
        }
    }

    public static class PendingBookingApprovalView {
        private final Long paymentId;
        private final String propertyName;
        private final String propertyId;
        private final String tenantName;
        private final Double amount;
        private final String requestedAt;

        public PendingBookingApprovalView(Long paymentId,
                                          String propertyName,
                                          String propertyId,
                                          String tenantName,
                                          Double amount,
                                          String requestedAt) {
            this.paymentId = paymentId;
            this.propertyName = propertyName;
            this.propertyId = propertyId;
            this.tenantName = tenantName;
            this.amount = amount;
            this.requestedAt = requestedAt;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public Double getAmount() {
            return amount;
        }

        public String getRequestedAt() {
            return requestedAt;
        }
    }

    public static class LandlordActiveRentalView {
        private final String propertyName;
        private final String propertyId;
        private final String tenantName;
        private final Double monthlyRent;
        private final LocalDate currentRentDueDate;
        private final boolean currentRentPaid;
        private final Long currentRentPaymentId;
        private final LocalDateTime currentRentPaidAt;

        public LandlordActiveRentalView(String propertyName,
                                        String propertyId,
                                        String tenantName,
                                        Double monthlyRent,
                                        LocalDate currentRentDueDate,
                                        boolean currentRentPaid,
                                        Long currentRentPaymentId,
                                        LocalDateTime currentRentPaidAt) {
            this.propertyName = propertyName;
            this.propertyId = propertyId;
            this.tenantName = tenantName;
            this.monthlyRent = monthlyRent;
            this.currentRentDueDate = currentRentDueDate;
            this.currentRentPaid = currentRentPaid;
            this.currentRentPaymentId = currentRentPaymentId;
            this.currentRentPaidAt = currentRentPaidAt;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyId() {
            return propertyId;
        }

        public String getTenantName() {
            return tenantName;
        }

        public Double getMonthlyRent() {
            return monthlyRent;
        }

        public LocalDate getCurrentRentDueDate() {
            return currentRentDueDate;
        }

        public boolean isCurrentRentPaid() {
            return currentRentPaid;
        }

        public Long getCurrentRentPaymentId() {
            return currentRentPaymentId;
        }

        public LocalDateTime getCurrentRentPaidAt() {
            return currentRentPaidAt;
        }

        public String getCurrentRentStatusLabel() {
            return currentRentPaid ? "Monthly rent paid" : "Monthly rent pending";
        }
    }

    public static class TenantBookedPropertyView {
        private final Long internalPropertyId;
        private final String propertyPublicId;
        private final String propertyName;
        private final String addressLine;
        private final String ownerName;
        private final Double monthlyRent;
        private final Double bookingAmount;
        private final String bookingApprovalStatus;
        private final String bookingStatusLabel;
        private final Long paymentId;
        private final LocalDateTime bookedAt;
        private final boolean hasImage;
        private final LocalDate currentRentDueDate;
        private final boolean currentRentPaid;
        private final Long currentRentPaymentId;

        public TenantBookedPropertyView(Long internalPropertyId,
                                        String propertyPublicId,
                                        String propertyName,
                                        String addressLine,
                                        String ownerName,
                                        Double monthlyRent,
                                        Double bookingAmount,
                                        String bookingApprovalStatus,
                                        String bookingStatusLabel,
                                        Long paymentId,
                                        LocalDateTime bookedAt,
                                        boolean hasImage,
                                        LocalDate currentRentDueDate,
                                        boolean currentRentPaid,
                                        Long currentRentPaymentId) {
            this.internalPropertyId = internalPropertyId;
            this.propertyPublicId = propertyPublicId;
            this.propertyName = propertyName;
            this.addressLine = addressLine;
            this.ownerName = ownerName;
            this.monthlyRent = monthlyRent;
            this.bookingAmount = bookingAmount;
            this.bookingApprovalStatus = bookingApprovalStatus;
            this.bookingStatusLabel = bookingStatusLabel;
            this.paymentId = paymentId;
            this.bookedAt = bookedAt;
            this.hasImage = hasImage;
            this.currentRentDueDate = currentRentDueDate;
            this.currentRentPaid = currentRentPaid;
            this.currentRentPaymentId = currentRentPaymentId;
        }

        public Long getInternalPropertyId() {
            return internalPropertyId;
        }

        public String getPropertyPublicId() {
            return propertyPublicId;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getAddressLine() {
            return addressLine;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public Double getMonthlyRent() {
            return monthlyRent;
        }

        public Double getBookingAmount() {
            return bookingAmount;
        }

        public String getBookingApprovalStatus() {
            return bookingApprovalStatus;
        }

        public String getBookingStatusLabel() {
            return bookingStatusLabel;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public LocalDateTime getBookedAt() {
            return bookedAt;
        }

        public boolean isHasImage() {
            return hasImage;
        }

        public LocalDate getCurrentRentDueDate() {
            return currentRentDueDate;
        }

        public boolean isCurrentRentPaid() {
            return currentRentPaid;
        }

        public Long getCurrentRentPaymentId() {
            return currentRentPaymentId;
        }

        public boolean isApproved() {
            return "APPROVED".equalsIgnoreCase(bookingApprovalStatus);
        }

        public boolean isPendingApproval() {
            return "PENDING_APPROVAL".equalsIgnoreCase(bookingApprovalStatus);
        }

        public boolean isCanPayMonthlyRent() {
            return isApproved() && !currentRentPaid;
        }

        public String getCurrentRentStatusLabel() {
            if (isPendingApproval()) {
                return "Monthly rent opens after landlord approval";
            }
            return currentRentPaid ? "Monthly rent paid for current cycle" : "Monthly rent pending for current cycle";
        }
    }

    public static class TenantPaymentSummaryView {
        private final Long paymentId;
        private final String propertyName;
        private final String propertyPublicId;
        private final Double amount;
        private final String paymentStatus;
        private final String paymentTypeLabel;
        private final String bookingStatusLabel;
        private final LocalDateTime createdAt;

        public TenantPaymentSummaryView(Long paymentId,
                                        String propertyName,
                                        String propertyPublicId,
                                        Double amount,
                                        String paymentStatus,
                                        String paymentTypeLabel,
                                        String bookingStatusLabel,
                                        LocalDateTime createdAt) {
            this.paymentId = paymentId;
            this.propertyName = propertyName;
            this.propertyPublicId = propertyPublicId;
            this.amount = amount;
            this.paymentStatus = paymentStatus;
            this.paymentTypeLabel = paymentTypeLabel;
            this.bookingStatusLabel = bookingStatusLabel;
            this.createdAt = createdAt;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyPublicId() {
            return propertyPublicId;
        }

        public Double getAmount() {
            return amount;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public String getPaymentTypeLabel() {
            return paymentTypeLabel;
        }

        public String getBookingStatusLabel() {
            return bookingStatusLabel;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    public static class HomeReviewView {
        private final String reviewerName;
        private final String reviewerRole;
        private final String reviewerCity;
        private final String reviewText;
        private final Integer rating;

        public HomeReviewView(String reviewerName, String reviewerRole, String reviewerCity, String reviewText, Integer rating) {
            this.reviewerName = reviewerName;
            this.reviewerRole = reviewerRole;
            this.reviewerCity = reviewerCity;
            this.reviewText = reviewText;
            this.rating = rating;
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public String getReviewText() {
            return reviewText;
        }

        public Integer getRating() {
            return rating;
        }

        public String getRoleLabel() {
            if ("LANDLORD".equalsIgnoreCase(reviewerRole)) {
                return "Landlord";
            }
            if ("TENANT".equalsIgnoreCase(reviewerRole)) {
                return "Tenant";
            }
            return "User";
        }

        public String getRoleAndCity() {
            String roleLabel = getRoleLabel();
            if (reviewerCity == null || reviewerCity.isBlank()) {
                return roleLabel;
            }
            return roleLabel + ", " + reviewerCity;
        }

        public String getStarText() {
            int safeRating = Math.max(1, Math.min(5, rating != null ? rating : 1));
            return "\u2605".repeat(safeRating) + "\u2606".repeat(5 - safeRating);
        }
    }

    @GetMapping("/tenant-dashboard")
    public String tenantDashboard(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!"TENANT".equals(user.getRole())) {
            return "redirect:/home";
        }

        List<Property> availableProperties =
                propertyRepository.findByVerificationStatusAndAvailabilityStatusOrderByCreatedAtDesc("APPROVED", "AVAILABLE");
        List<Payment> tenantPayments = sortPaymentsNewestFirst(paymentRepository.findByTenantId(user.getId()));
        Map<Long, Property> propertyById = loadPropertiesById(tenantPayments);
        List<TenantBookedPropertyView> bookedProperties = buildTenantBookedProperties(tenantPayments, propertyById);
        List<TenantPaymentSummaryView> recentPayments = buildTenantRecentPayments(tenantPayments, propertyById, 5);

        model.addAttribute("user", user);
        model.addAttribute("availableProperties", availableProperties);
        model.addAttribute("favoriteProperties", user.getFavoriteProperties());
        model.addAttribute("bookedProperties", bookedProperties);
        model.addAttribute("recentPayments", recentPayments);
        model.addAttribute("bookedPropertyCount", bookedProperties.size());
        model.addAttribute("paymentCount", tenantPayments.size());
        model.addAttribute("canSubmitReview", !appReviewRepository.existsByUserId(user.getId()));
        appReviewRepository.findByUserId(user.getId())
                .ifPresent(review -> model.addAttribute("existingReview", review));
        return "tenant-dashboard";
    }

    @GetMapping("/favorites")
    public String myFavorites(Authentication authentication, Model model) {
        String email = authentication.getName();
        User user = userRepository.findByemail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!"TENANT".equals(user.getRole())) {
            return "redirect:/home";
        }

        // Hide rented properties from favorites so tenants only see available options.
        List<Property> availableFavorites = user.getFavoriteProperties().stream()
                .filter(p -> !"NOT_AVAILABLE".equalsIgnoreCase(p.getAvailabilityStatus()))
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("favoriteProperties", availableFavorites);
        return "Favorite-Properties-tenant";
    }

    @GetMapping("/property/{id}")
    public String propertyDetail(@org.springframework.web.bind.annotation.PathVariable String id, Model model, Authentication authentication) {
        // Add current user if available
        User currentUser = null;
        if (authentication != null) {
            String email = authentication.getName();
            currentUser = userRepository.findByemail(email).orElse(null);
            if (currentUser != null) {
                model.addAttribute("user", currentUser);
            }
        }

        Property property = resolveProperty(id).orElseThrow(() -> new IllegalArgumentException("Property not found"));
        model.addAttribute("property", property);
        String propertyRef = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                ? property.getPropertyId()
                : String.valueOf(property.getId());
        model.addAttribute("propertyRef", propertyRef);

        // Pass raw images path for the controller to handle
        model.addAttribute("imagesPath", property.getImagesPath());

        // Extract and parse document paths
        List<String> documentPaths = new ArrayList<>();
        if (property.getDocumentsPath() != null && !property.getDocumentsPath().isEmpty()) {
            documentPaths = new ArrayList<>(List.of(property.getDocumentsPath().split(",")))
                    .stream()
                    .map(String::trim)
                    .map(p -> Paths.get(p).getFileName().toString())
                    .map(String::toLowerCase)
                    .filter(n -> n.endsWith(".pdf") || n.endsWith(".doc") || n.endsWith(".docx") || 
                                 n.endsWith(".xlsx") || n.endsWith(".txt"))
                    .collect(Collectors.toList());
        }
        model.addAttribute("documentPaths", documentPaths);

        // Fetch landlord details so template can show contact phone and name reliably
        if (property.getLandlordId() != null) {
            userRepository.findById(property.getLandlordId()).ifPresent(landlord -> model.addAttribute("landlord", landlord));
        }

        // Suggest advance payment - defaulting to one month's rent (can be changed later)
        Double advance = property.getPrice() != null ? property.getPrice() : 0.0;
        model.addAttribute("advanceAmount", advance);

        Optional<Payment> activeBookingOpt = Optional.empty();
        boolean canBookProperty = "AVAILABLE".equalsIgnoreCase(property.getAvailabilityStatus());
        boolean currentTenantBookingPendingApproval = false;
        boolean currentTenantBookingApproved = false;
        boolean canPayMonthlyRent = false;
        boolean currentRentPaid = false;
        Long currentRentPaymentId = null;
        LocalDate currentRentDueDate = null;
        String propertyActionMessage = null;

        if (currentUser != null && "TENANT".equalsIgnoreCase(currentUser.getRole())) {
            activeBookingOpt = paymentService.getLatestActiveAdvanceBooking(currentUser.getId(), property.getId());
            if (activeBookingOpt.isPresent()) {
                Payment activeBooking = activeBookingOpt.get();
                canBookProperty = false;
                currentTenantBookingPendingApproval = "PENDING_APPROVAL".equalsIgnoreCase(activeBooking.getBookingApprovalStatus());
                currentTenantBookingApproved = "APPROVED".equalsIgnoreCase(activeBooking.getBookingApprovalStatus());

                if (currentTenantBookingPendingApproval) {
                    propertyActionMessage = "Advance payment already completed. Waiting for landlord approval.";
                } else if (currentTenantBookingApproved) {
                    PaymentService.RentCycleStatus rentCycleStatus = paymentService.getCurrentRentCycleStatus(activeBooking);
                    currentRentDueDate = rentCycleStatus.dueDate();
                    currentRentPaid = rentCycleStatus.paidForCurrentCycle();
                    currentRentPaymentId = rentCycleStatus.currentCyclePayment() != null
                            ? rentCycleStatus.currentCyclePayment().getId()
                            : null;
                    canPayMonthlyRent = !currentRentPaid;
                    propertyActionMessage = currentRentPaid
                            ? "Your booking is approved and the current monthly rent is already paid."
                            : "Your booking is approved. You can now pay the current monthly rent.";
                }
            } else if ("BOOKED_PENDING_APPROVAL".equalsIgnoreCase(property.getAvailabilityStatus())) {
                canBookProperty = false;
                propertyActionMessage = "This property already has an advance booking waiting for landlord approval.";
            } else if ("NOT_AVAILABLE".equalsIgnoreCase(property.getAvailabilityStatus())) {
                canBookProperty = false;
                propertyActionMessage = "Property already rented.";
            }
        } else if ("BOOKED_PENDING_APPROVAL".equalsIgnoreCase(property.getAvailabilityStatus())) {
            canBookProperty = false;
            propertyActionMessage = "This property is currently under booking review.";
        } else if ("NOT_AVAILABLE".equalsIgnoreCase(property.getAvailabilityStatus())) {
            canBookProperty = false;
            propertyActionMessage = "Property already rented.";
        }

        model.addAttribute("canBookProperty", canBookProperty);
        model.addAttribute("currentTenantBookingPendingApproval", currentTenantBookingPendingApproval);
        model.addAttribute("currentTenantBookingApproved", currentTenantBookingApproved);
        model.addAttribute("canPayMonthlyRent", canPayMonthlyRent);
        model.addAttribute("currentRentPaid", currentRentPaid);
        model.addAttribute("currentRentPaymentId", currentRentPaymentId);
        model.addAttribute("currentRentDueDate", currentRentDueDate);
        model.addAttribute("propertyActionMessage", propertyActionMessage);

        return "property-detail";
    }

    // Contact landlord page (form)
    @GetMapping("/contact-landlord")
    public String contactLandlord(@org.springframework.web.bind.annotation.RequestParam String propertyId, Model model, Authentication authentication) {
        Property property = resolveProperty(propertyId).orElseThrow(() -> new IllegalArgumentException("Property not found"));
        model.addAttribute("property", property);
        String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                ? property.getPropertyId()
                : String.valueOf(property.getId());
        model.addAttribute("propertyPublicId", propertyPublicId);
        if (property.getLandlordId() != null) {
            userRepository.findById(property.getLandlordId()).ifPresent(landlord -> model.addAttribute("landlord", landlord));
        }
        if (authentication != null) {
            String email = authentication.getName();
            userRepository.findByemail(email).ifPresent(user -> model.addAttribute("user", user));
        }
        return "contact-landlord";
    }

    // Handle contact form submission (simple confirmation)
    @org.springframework.web.bind.annotation.PostMapping("/contact-landlord")
    public String sendContactMessage(@org.springframework.web.bind.annotation.RequestParam String propertyId,
                                     @org.springframework.web.bind.annotation.RequestParam String message,
                                     Authentication authentication,
                                     org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        // Try to send an in-app message to the landlord using MessageService
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("message", "You must be logged in to contact the landlord.");
            return "redirect:/property/" + propertyId;
        }

        try {
            String email = authentication.getName();
            var senderOpt = userRepository.findByemail(email);
            if (senderOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Unable to find your account.");
                return "redirect:/property/" + propertyId;
            }

            var propertyOpt = resolveProperty(propertyId);
            if (propertyOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Property not found.");
                return "redirect:/home";
            }

            var property = propertyOpt.get();
            Long internalPropertyId = property.getId();
            String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                    ? property.getPropertyId()
                    : String.valueOf(internalPropertyId);
            Long landlordId = property.getLandlordId();
            if (landlordId == null) {
                redirectAttributes.addFlashAttribute("message", "Landlord not available for this property.");
                return "redirect:/property/" + propertyPublicId;
            }

            var landlordOpt = userRepository.findById(landlordId);
            if (landlordOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Landlord account not found.");
                return "redirect:/property/" + propertyPublicId;
            }

           
            redirectAttributes.addFlashAttribute("message", "Your message was sent to the landlord.");
            redirectAttributes.addFlashAttribute("success", true);
            
            // Redirect to the conversation view
            String propertyRef = propertyPublicId;
            return "redirect:/messages/conversation/" + landlordId + "/" + propertyRef;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("message", "Could not send message: " + ex.getMessage());
            return "redirect:/property/" + propertyId;
        } catch (Exception ex) {
            // Log the exception for debugging
            System.err.println("Error sending contact message: " + ex.getMessage());
            ex.printStackTrace();
            redirectAttributes.addFlashAttribute("message", "An error occurred while sending your message. Please try again.");
            return "redirect:/property/" + propertyId;
        }
    }

    // Booking page - show advance and booking form

    @GetMapping("/book-property")
    public String bookProperty(@org.springframework.web.bind.annotation.RequestParam String propertyId, Model model, Authentication authentication) {
        Property property = resolveProperty(propertyId).orElseThrow(() -> new IllegalArgumentException("Property not found"));
        model.addAttribute("property", property);
        Double advance = property.getPrice() != null ? property.getPrice() : 0.0;
        model.addAttribute("advanceAmount", advance);
        if (authentication != null) {
            String email = authentication.getName();
            userRepository.findByemail(email).ifPresent(user -> model.addAttribute("user", user));
        }
        return "book-property";
    }

    @org.springframework.web.bind.annotation.PostMapping("/book-property")
    public String confirmBooking(@org.springframework.web.bind.annotation.RequestParam String propertyId,
                                 @org.springframework.web.bind.annotation.RequestParam Double advance,
                                 Authentication authentication,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to book a property.");
            return "redirect:/login";
        }

        Property property = resolveProperty(propertyId).orElseThrow(() -> new IllegalArgumentException("Property not found"));

        String propertyRef = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                ? property.getPropertyId()
                : String.valueOf(property.getId());

        // Redirect to payment page with property details
        redirectAttributes.addAttribute("propertyId", propertyRef);
        redirectAttributes.addAttribute("amount", advance);
        redirectAttributes.addAttribute("paymentType", "ADVANCE");
        return "redirect:/payment/checkout";
    }

    /**
     * Serve property images for tenants by index
     * Converts image index to the actual image filename from imagesPath
     */
    @GetMapping("/tenant/file/image/{propertyId}/{imageIndex}")
    public ResponseEntity<byte[]> servePropertyImage(
            @PathVariable Long propertyId,
            @PathVariable int imageIndex) {
        try {
            Property property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));
            
            if (property.getImagesPath() == null || property.getImagesPath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Parse image paths from comma-separated string
            String[] imagePaths = property.getImagesPath().split(",");
            
            // Validate index
            if (imageIndex < 0 || imageIndex >= imagePaths.length) {
                return ResponseEntity.badRequest().build();
            }
            
            String imagePath = imagePaths[imageIndex].trim();
            File imageFile = new File(imagePath);
            
            if (!imageFile.exists() || !imageFile.isFile()) {
                return ResponseEntity.notFound().build();
            }
            
            // Determine media type
            String fileName = imageFile.getName().toLowerCase();
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (fileName.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (fileName.endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            }
            
            // Read and return file
            byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(imageBytes);
                    
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

}
