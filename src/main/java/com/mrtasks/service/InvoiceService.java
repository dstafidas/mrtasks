package com.mrtasks.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import com.mrtasks.model.Task;
import com.mrtasks.model.User;
import com.mrtasks.model.UserProfile;
import com.mrtasks.repository.TaskRepository;
import com.mrtasks.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final TaskRepository taskRepository;
    private final UserProfileRepository userProfileRepository;

    public byte[] generateInvoice(User user, List<Long> taskIds) throws Exception {
        List<Task> selectedTasks = taskRepository.findByUserAndIdIn(user, taskIds)
                .stream()
                .filter(Task::isBillable)
                .toList();

        if (selectedTasks.isEmpty()) {
            throw new IllegalStateException("No billable tasks selected.");
        }

        String invoiceTo = selectedTasks.stream()
                .map(Task::getClientName)
                .filter(name -> name != null && !name.isEmpty())
                .findFirst()
                .orElse("Client");

        Optional<UserProfile> profileOpt = userProfileRepository.findByUser(user);
        UserProfile profile = profileOpt.orElse(new UserProfile());
        String sender = StringUtils.hasText(profile.getCompanyName()) ? profile.getCompanyName() : "";

        // Load the user's preferred language
        String language = profile.getLanguage() != null ? profile.getLanguage() : "en";
        Locale locale = new Locale(language);
        ResourceBundle messages = ResourceBundle.getBundle("messages", locale);

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        BaseFont baseFont;
        try (InputStream fontStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {
            byte[] fontBytes = fontStream.readAllBytes();
            baseFont = BaseFont.createFont("DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
        }
        BaseFont boldBaseFont;
        try (InputStream boldFontStream = new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream()) {
            byte[] boldFontBytes = boldFontStream.readAllBytes();
            boldBaseFont = BaseFont.createFont("DejaVuSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, boldFontBytes, null);
        }

        Font headerFont = new Font(boldBaseFont, 16, Font.NORMAL, new Color(64, 64, 64)); // Dark gray
        Font normalFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
        Font boldFont = new Font(boldBaseFont, 10, Font.BOLD, Color.BLACK);
        Font footerFont = new Font(baseFont, 9, Font.ITALIC, Color.GRAY);

        // Header Section
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        if (profile.getLogoUrl() != null && !profile.getLogoUrl().isEmpty()) {
            try {
                Image logo = Image.getInstance(profile.getLogoUrl());
                logo.scaleToFit(100, 100);
                logoCell.addElement(logo);
            } catch (Exception e) {
                logoCell.addElement(new Paragraph(messages.getString("invoice.logo.unavailable"), normalFont));
            }
        }
        headerTable.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_TOP);
        String invoiceNumber = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        titleCell.addElement(new Paragraph(messages.getString("invoice.title") + invoiceNumber, normalFont));
        titleCell.addElement(new Paragraph(messages.getString("invoice.date.label") + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", locale)), normalFont));
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(Chunk.NEWLINE);

        // From and To Section
        PdfPTable addressTable = new PdfPTable(2);
        addressTable.setWidthPercentage(100);
        addressTable.setWidths(new float[]{1, 1});

        PdfPCell fromCell = new PdfPCell();
        fromCell.setBorder(Rectangle.NO_BORDER);
        fromCell.addElement(new Paragraph(messages.getString("invoice.from.label"), boldFont));
        fromCell.addElement(new Paragraph(sender, normalFont));
        if (profile.getEmail() != null) fromCell.addElement(new Paragraph(profile.getEmail(), normalFont));
        if (profile.getPhone() != null) fromCell.addElement(new Paragraph(profile.getPhone(), normalFont));
        addressTable.addCell(fromCell);

        PdfPCell toCell = new PdfPCell();
        toCell.setBorder(Rectangle.NO_BORDER);
        toCell.addElement(new Paragraph(messages.getString("invoice.to.label"), boldFont));
        toCell.addElement(new Paragraph(invoiceTo, normalFont));
        addressTable.addCell(toCell);

        document.add(addressTable);
        document.add(Chunk.NEWLINE);

        // Task Table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 2f, 1f, 1f, 1f, 1.6f, 1.4f});
        table.getDefaultCell().setPadding(5);
        table.getDefaultCell().setBackgroundColor(new Color(230, 230, 230)); // Light gray for header
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

        // Table Headers
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.task.header"), boldFont)));
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.description.header"), boldFont)));
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.hours.header"), boldFont)));
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.rate.header"), boldFont)));
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.total.header"), boldFont)));
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.advance.paid.header"), boldFont)));
        table.addCell(new PdfPCell(new Phrase(messages.getString("invoice.amount.due.header"), boldFont)));
        table.getDefaultCell().setBackgroundColor(null);

        // Table Rows
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
        double grandTotal = 0;
        double grandAdvance = 0;
        double grandRemainingDue = 0;
        for (Task task : selectedTasks) {
            table.addCell(new PdfPCell(new Phrase(task.getTitle(), normalFont)));
            table.addCell(new PdfPCell(new Phrase(task.getDescription() != null ? task.getDescription() : "N/A", normalFont)));
            table.addCell(new PdfPCell(new Phrase(String.valueOf(task.getHoursWorked()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", task.getHourlyRate()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", task.getTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", task.getAdvancePayment()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("$%.2f", task.getRemainingDue()), normalFont)));
            grandTotal += task.getTotal();
            grandAdvance += task.getAdvancePayment();
            grandRemainingDue += task.getRemainingDue();
        }
        document.add(table);

        // Totals Section
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        totalsTable.addCell(new PdfPCell(new Phrase(messages.getString("invoice.grand.total"), boldFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(String.format("$%.2f", grandTotal), normalFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(messages.getString("invoice.advance.paid"), boldFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(String.format("$%.2f", grandAdvance), normalFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(messages.getString("invoice.amount.due"), boldFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(String.format("$%.2f", grandRemainingDue), boldFont)));
        document.add(Chunk.NEWLINE);
        document.add(totalsTable);

        // Footer
        document.add(Chunk.NEWLINE);
        if (StringUtils.hasText(profile.getEmail())) {
            Paragraph footer = new Paragraph(messages.getString("invoice.contact.us") + " " + profile.getEmail(), footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
        }
        document.close();
        return out.toByteArray();
    }
}