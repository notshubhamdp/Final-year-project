package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Payment;
import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PaymentReceiptService {

    public byte[] generatePaymentReceipt(Payment payment, User tenant, Property property) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            Paragraph title = new Paragraph("NIVASA - Payment Receipt")
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // Receipt Number and Date
            Paragraph receiptInfo = new Paragraph("Receipt ID: " + payment.getId() + "\n" +
                    "Date: " + payment.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(20);
            document.add(receiptInfo);

            // Tenant Information
            document.add(new Paragraph("TENANT INFORMATION").setFontSize(14).setMarginBottom(10));
            Table tenantTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            tenantTable.setWidth(UnitValue.createPercentValue(100));
            tenantTable.addCell("Name:");
            tenantTable.addCell(tenant.getFirstName() + " " + tenant.getLastName());
            tenantTable.addCell("Email:");
            tenantTable.addCell(tenant.getEmail());
            document.add(tenantTable);
            document.add(new Paragraph("\n"));

            // Property Information
            document.add(new Paragraph("PROPERTY INFORMATION").setFontSize(14).setMarginBottom(10));
            Table propertyTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            propertyTable.setWidth(UnitValue.createPercentValue(100));
            propertyTable.addCell("Property Name:");
            propertyTable.addCell(property.getName());
            propertyTable.addCell("Address:");
            propertyTable.addCell(property.getAddress() + ", " + property.getCity() + ", " + property.getState() + " - " + property.getPincode());
            propertyTable.addCell("Owner:");
            propertyTable.addCell(property.getOwnerName());
            document.add(propertyTable);
            document.add(new Paragraph("\n"));

            // Payment Details
            document.add(new Paragraph("PAYMENT DETAILS").setFontSize(14).setMarginBottom(10));
            Table paymentTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            paymentTable.setWidth(UnitValue.createPercentValue(100));
            paymentTable.addCell("Payment ID:");
            paymentTable.addCell(payment.getStripePaymentId());
            paymentTable.addCell("Payment Type:");
            paymentTable.addCell(payment.getPaymentType());
            paymentTable.addCell("Amount:");
            paymentTable.addCell("₹" + String.format("%.2f", payment.getAmount()));
            paymentTable.addCell("Currency:");
            paymentTable.addCell(payment.getCurrency());
            paymentTable.addCell("Payment Method:");
            paymentTable.addCell(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A");
            paymentTable.addCell("Status:");
            paymentTable.addCell(payment.getStatus());
            if (payment.getDescription() != null && !payment.getDescription().isEmpty()) {
                paymentTable.addCell("Description:");
                paymentTable.addCell(payment.getDescription());
            }
            document.add(paymentTable);

            // Footer
            document.add(new Paragraph("\n\nThank you for using NIVASA!").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Find your place. Live your peace.").setTextAlignment(TextAlignment.CENTER).setFontSize(10));

            document.close();

        } catch (Exception e) {
            throw new RuntimeException("Error generating payment receipt PDF", e);
        }

        return outputStream.toByteArray();
    }
}