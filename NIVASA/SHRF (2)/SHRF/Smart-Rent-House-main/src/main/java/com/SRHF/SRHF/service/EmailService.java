package com.SRHF.SRHF.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // @Async
    public void send(String to, String token) {
        // Implementation for sending email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Confirm Your Email");
            String messageBody = """
                    Thank you for registering with Smart Rent House.
                    We’re glad to welcome you to our community.
                    
                    To complete your registration, please confirm your email address by clicking the link below:
                    
                    Confirm your account:
                    https://8hm41khf-8085.inc1.devtunnels.ms/register/tokenConfirmed?token=%s
                    
                    For security reasons, this link will remain active for 5 minutes.
                    If it expires, you can request a new confirmation link anytime from the login page.
                    
                    If you did not create this account, please ignore this email — no action is required and your information will remain safe.
                    
                    Wishing you a smooth and peaceful experience ahead.
                    
                    Warm regards,
                    NIVASA Team
                    Helping you find the right place peacefully
                    
                    """.formatted(token);
            message.setText(messageBody);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send confirmation email to {}", to, e);
        }
    }

    // @Async
    public void sendPasswordReset(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Password Reset OTP - Smart Rent House");
            String messageBody = "Use the following One-Time Password (OTP) to reset your password:\n\n" +
                    otp +
                    "\n\nThis OTP is valid for 5 minutes. If you did not request a password reset, please ignore this email.";
            message.setText(messageBody);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", to, e);
        }
    }

    // @Async
    public void sendVerificationSuccessEmail(String to, String firstName, String lastName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Account Successfully Verified - Smart Rent House");
            String fullName = firstName + " " + lastName;
            String messageBody = """
                    Dear %s,
                    
                    We're happy to let you know that your account has been successfully verified.
                    Thank you for confirming your email and joining the Smart Rent House community.
                    
                    You can now log in and start exploring rental homes that best suit your lifestyle, preferences, and comfort.
                    
                    We wish you a smooth, peaceful, and satisfying experience ahead.
                    If you ever need assistance, our support team is always here to help.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(fullName);
            message.setText(messageBody);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send verification success email to {}", to, e);
        }
    }

    // @Async
    public void sendPasswordChangeEmail(String to, String firstName, String lastName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Password Successfully Updated - Smart Rent House");
            String fullName = firstName + " " + lastName;
            String messageBody = """
                    Dear %s,
                    
                    This is a quick confirmation to let you know that your password has been successfully updated.
                    
                    If you made this change, no further action is required.
                    If you did not request this password update, please contact our support team immediately to secure your account.
                    
                    Your safety and peace of mind are important to us.
                    Thank you for being a valued part of the Smart Rent House community.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(fullName);
            message.setText(messageBody);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send password change email to {}", to, e);
        }
    }

    // @Async
    public void sendPropertyRegistrationEmail(String to, String firstName, String lastName, String propertyName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Property Registered Successfully - Smart Rent House");
            String fullName = firstName + " " + lastName;
            String messageBody = """
                    Dear %s,
                    
                    Congratulations! Your property "%s" has been successfully registered with Smart Rent House.
                    
                    Your property is now pending verification by our admin team. Our team will review your property details, images, and documents to ensure they meet our standards.
                    
                    Your property is good for verification and you will receive a confirmation email once it is approved or if any clarifications are needed.
                    
                    What happens next:
                    1. Our admin team will review your property within 2-3 business days
                    2. We will verify all documents and images
                    3. You will receive an email notification about the approval or any changes needed
                    
                    You can track your property status anytime by logging into your landlord dashboard.
                    
                    Thank you for listing your property with Smart Rent House. We look forward to helping you find the right tenants.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(fullName, propertyName);
            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Property registration email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send property registration email to {}", to, e);
        }
    }

    // @Async
    public void sendPropertyApprovedEmail(String to, String firstName, String lastName, String propertyName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Property Approved - Smart Rent House");
            String fullName = firstName + " " + lastName;
            String messageBody = """
                    Dear %s,
                    
                    Great news! Your property "%s" has been approved by our admin team and is now live on Smart Rent House.
                    
                    Your property is now visible to all tenants and students searching for rental accommodations. Prospective tenants will be able to see your property details, images, and can contact you for inquiries.
                    
                    What you can do now:
                    1. View and manage property inquiries from interested tenants
                    2. Respond to tenant messages and book requests
                    3. Update your property details anytime from your dashboard
                    4. Track bookings and manage your property
                    
                    Your success is our success. We're here to help you find the perfect tenant for your property.
                    
                    If you have any questions or need assistance, please don't hesitate to contact our support team.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(fullName, propertyName);
            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Property approval email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send property approval email to {}", to, e);
        }
    }

    // @Async
    public void sendPropertyRejectedEmail(String to, String firstName, String lastName, String propertyName, String rejectionReason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Property Verification - Additional Information Needed - Smart Rent House");
            String fullName = firstName + " " + lastName;
            String messageBody = """
                    Dear %s,
                    
                    Thank you for submitting your property "%s" to Smart Rent House.
                    
                    Our admin team has reviewed your property and has identified some concerns that need to be addressed:
                    
                    Reason: %s
                    
                    What you can do:
                    1. Review the feedback provided above
                    2. Update your property details, images, or documents as needed
                    3. Resubmit your property for verification
                    4. Contact our support team if you have any questions about the feedback
                    
                    We understand that verification requirements might seem strict, but they help us maintain the highest standards for our community and ensure a safe, trustworthy platform for all users.
                    
                    Please feel free to reach out to our support team if you need any clarification or assistance in addressing these concerns.
                    
                    We look forward to welcoming your property once all requirements are met.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(fullName, propertyName, rejectionReason);
            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Property rejection email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send property rejection email to {}", to, e);
        }
    }

    // @Async
    public void sendStudentVerificationApprovedEmail(String to, String firstName, String lastName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Student Verification Approved - Smart Rent House");
            String messageBody = """
                    Dear %s %s,
                    
                    Congratulations! Your student ID has been successfully verified.
                    
                    You can now proceed to:
                    1. Search for available properties
                    2. Book your desired accommodation
                    3. Contact landlords and property managers
                    4. Manage your bookings and payments
                    
                    Welcome to the Smart Rent House community!
                    
                    If you have any questions or need assistance, please don't hesitate to contact our support team.
                    
                    Best regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(firstName, lastName);
            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Student verification approved email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send student verification approved email to {}", to, e);
        }
    }

    // @Async
    public void sendStudentVerificationRejectedEmail(String to, String firstName, String lastName, String rejectionReason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Student Verification - Additional Information Needed - Smart Rent House");
            String messageBody = """
                    Dear %s %s,
                    
                    Thank you for submitting your student ID for verification.
                    
                    Our admin team has reviewed your document and was unable to verify your student status due to the following reason:
                    
                    %s
                    
                    What you can do:
                    1. Review the feedback provided above
                    2. Prepare a clearer or different student ID document
                    3. Upload the new document for verification
                    4. Contact our support team if you have any questions
                    
                    Please note that clear, valid student identification is required to proceed. Acceptable documents typically include:
                    - Student ID card with visible name and photo
                    - University enrollment certificate
                    - Admission letter with your name and enrollment details
                    
                    Please feel free to reach out to our support team if you need any clarification.
                    
                    Best regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(firstName, lastName, rejectionReason != null && !rejectionReason.isEmpty() 
                            ? rejectionReason 
                            : "Your student ID could not be clearly verified. Please upload a clearer document.");
            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Student verification rejection email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send student verification rejection email to {}", to, e);
        }
    }

    // @Async
    public void sendPaymentReceipt(String to, String userName, byte[] pdfBytes, String paymentType, Double amount) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom("smartrenthouseotp@gmail.com");
            helper.setSubject("Payment Receipt - NIVASA Smart Rent House");

            String messageBody = """
                    Dear %s,
                    
                    Thank you for your payment! We're pleased to confirm that your %s payment of ₹%.2f has been successfully processed.
                    
                    Your payment receipt is attached to this email. Please keep it for your records and future reference.
                    
                    Payment Details:
                    - Payment Type: %s
                    - Amount: ₹%.2f
                    - Status: COMPLETED
                    - Reference: NIVASA Payment Portal
                    
                    Next Steps:
                    You can now proceed with the property booking process. If you have any questions or need assistance, please don't hesitate to contact our support team.
                    
                    We appreciate your business and look forward to helping you find your perfect home!
                    
                    Best regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(userName, paymentType.toLowerCase(), amount, paymentType, amount);

            helper.setText(messageBody);
            helper.addAttachment("Payment_Receipt.pdf", () -> new java.io.ByteArrayInputStream(pdfBytes));

            mailSender.send(mimeMessage);
            logger.info("Payment receipt sent successfully to {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send payment receipt email to {}", to, e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending payment receipt to {}", to, e);
        }
    }

    // @Async
    public void sendPaymentSuccessEmail(String to, String userName, String paymentType, Double amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Payment Successful - NIVASA Smart Rent House");

            String messageBody = """
                    Dear %s,
                    
                    Your payment has been successfully processed!
                    
                    Payment Details:
                    - Payment Type: %s
                    - Amount: ₹%.2f
                    - Status: COMPLETED
                    
                    Your payment receipt is available for download from your account dashboard.
                    
                    Thank you for using NIVASA. We wish you a pleasant stay!
                    
                    Best regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(userName, paymentType, amount);

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Payment success email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send payment success email to {}", to, e);
        }
    }

    // @Async
    public void sendLandlordPropertyBookedEmail(String to,
                                                String landlordName,
                                                String propertyName,
                                                String propertyPublicId,
                                                String tenantName,
                                                Double amount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Property Booked - Smart Rent House");

            String messageBody = """
                    Dear %s,
                    
                    Great news! Your property has just been booked.
                    
                    Booking details:
                    - Property: %s
                    - Property ID: %s
                    - Tenant: %s
                    - Advance Paid: INR %.2f
                    
                    Please log in to your landlord dashboard to review and continue the booking process.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(
                    landlordName,
                    propertyName != null ? propertyName : "Your Property",
                    propertyPublicId != null ? propertyPublicId : "-",
                    tenantName != null ? tenantName : "Tenant",
                    amount != null ? amount : 0.0
            );

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Landlord booking notification email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send landlord booking notification email to {}", to, e);
        }
    }

    public void sendPropertyDeletedEmail(String to,
                                         String firstName,
                                         String lastName,
                                         String propertyName,
                                         String propertyPublicId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Property Deleted Successfully - Smart Rent House");

            String fullName = (firstName + " " + lastName).trim();
            String messageBody = """
                    Dear %s,
                    
                    This is a confirmation that your property has been deleted successfully.
                    
                    Property details:
                    - Property Name: %s
                    - Property ID: %s
                    
                    If this deletion was not done by you, please contact support immediately.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(
                    fullName,
                    propertyName != null ? propertyName : "Property",
                    propertyPublicId != null ? propertyPublicId : "-"
            );

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Property deletion confirmation email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send property deletion email to {}", to, e);
        }
    }

    public void sendMonthlyRentReminderEmail(String to,
                                             String tenantName,
                                             String propertyName,
                                             String propertyPublicId,
                                             LocalDate dueDate,
                                             Double rentAmount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Monthly Rent Reminder - Smart Rent House");

            String dueDateLabel = dueDate != null
                    ? dueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                    : "-";

            String messageBody = """
                    Dear %s,
                    
                    This is a friendly reminder that your monthly rent payment is due.
                    
                    Property details:
                    - Property: %s
                    - Property ID: %s
                    - Due Date: %s
                    - Monthly Rent: INR %.2f
                    
                    Please complete your rent payment on time to avoid any issues.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(
                    tenantName != null && !tenantName.isBlank() ? tenantName : "Tenant",
                    propertyName != null && !propertyName.isBlank() ? propertyName : "Property",
                    propertyPublicId != null && !propertyPublicId.isBlank() ? propertyPublicId : "-",
                    dueDateLabel,
                    rentAmount != null ? rentAmount : 0.0
            );

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Monthly rent reminder email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send monthly rent reminder email to {}", to, e);
        }
    }

    public void sendBookingApprovedEmail(String to,
                                         String tenantName,
                                         String propertyName,
                                         String propertyPublicId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Booking Approved - Smart Rent House");

            String messageBody = """
                    Dear %s,
                    
                    Your booking has been approved by the landlord.
                    
                    Property details:
                    - Property: %s
                    - Property ID: %s
                    
                    You can now continue your stay and pay monthly rent on time.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(
                    tenantName != null && !tenantName.isBlank() ? tenantName : "Tenant",
                    propertyName != null ? propertyName : "Property",
                    propertyPublicId != null ? propertyPublicId : "-"
            );

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Booking approved email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send booking approved email to {}", to, e);
        }
    }

    public void sendBookingRejectedEmail(String to,
                                         String tenantName,
                                         String propertyName,
                                         String propertyPublicId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Booking Rejected - Smart Rent House");

            String messageBody = """
                    Dear %s,
                    
                    Your booking has been rejected by the landlord.
                    
                    Property details:
                    - Property: %s
                    - Property ID: %s
                    
                    Please contact support if you need help with refund status or next steps.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(
                    tenantName != null && !tenantName.isBlank() ? tenantName : "Tenant",
                    propertyName != null ? propertyName : "Property",
                    propertyPublicId != null ? propertyPublicId : "-"
            );

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("Booking rejected email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send booking rejected email to {}", to, e);
        }
    }

    public void sendUserBanEmail(String to, String firstName, String lastName, String banReason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Account Banned - Smart Rent House");

            String messageBody = String.format("""
                    Dear %s %s,

                    We regret to inform you that your account on Smart Rent House has been banned.

                    Reason for ban: %s

                    As per our platform policy, brokers, agents, and intermediaries are not allowed on this platform. Only direct landlord-tenant transactions are permitted.

                    If you believe this ban was issued in error, please contact our support team for review.

                    Warm regards,
                    NIVASA Team
                    Helping you find the right place peacefully
                    """, firstName, lastName, banReason);

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("User ban email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send user ban email to {}", to, e);
        }
    }

    public void sendUserUnbanEmail(String to, String firstName, String lastName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Account Reactivated - Smart Rent House");

            String messageBody = String.format("""
                    Dear %s %s,

                    We are pleased to inform you that your account on Smart Rent House has been reactivated.

                    You can now access all platform features. Please ensure compliance with our platform policies:
                    - Only direct landlord-tenant transactions are permitted
                    - Brokers, agents, and intermediaries are not allowed

                    If you have any questions, please contact our support team.

                    Warm regards,
                    NIVASA Team
                    Helping you find the right place peacefully
                    """, firstName, lastName);

            message.setText(messageBody);
            mailSender.send(message);
            logger.info("User unban email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send user unban email to {}", to, e);
        }
    }

    public void sendSimpleNotification(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send simple notification to {}", to, e);
        }
    }

    public void sendBookingRefundCompletedEmail(String to,
                                                String tenantName,
                                                Double amount,
                                                String refundReference) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setFrom("smartrenthouseotp@gmail.com");
            message.setSubject("Booking Refund Initiated and Refunded - Smart Rent House");

            String body = """
                    Dear %s,
                    
                    Your booking refund has been initiated and refunded to your account.
                    
                    Refund details:
                    - Amount: INR %.2f
                    - Reference: %s
                    - Status: REFUNDED
                    
                    If you do not see the amount immediately in your bank statement, please allow standard bank processing time.
                    
                    Warm regards,
                    NIVASA Team
                    Find your place. Live your peace.
                    """.formatted(
                    tenantName != null && !tenantName.isBlank() ? tenantName : "Tenant",
                    amount != null ? amount : 0.0,
                    refundReference != null ? refundReference : "-"
            );

            message.setText(body);
            mailSender.send(message);
            logger.info("Booking refund completion email sent to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send booking refund email to {}", to, e);
        }
    }
}
