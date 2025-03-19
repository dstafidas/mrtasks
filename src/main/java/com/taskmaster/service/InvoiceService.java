package com.taskmaster.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.taskmaster.model.Task;
import com.taskmaster.model.User;
import com.taskmaster.model.UserProfile;
import com.taskmaster.repository.TaskRepository;
import com.taskmaster.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
        String sender = profile.getCompanyName() != null && !profile.getCompanyName().isEmpty() ?
                profile.getCompanyName() : user.getUsername();

        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Fonts
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY);

        // Header Section (Simplified and Flexible)
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
                logoCell.addElement(new Paragraph("Logo unavailable", normalFont));
            }
        }
        headerTable.addCell(logoCell);

        // Date with locale support
        // Optional: Override with user preference from UserProfile (uncomment if implemented)
        // if (profile.getPreferredLanguage() != null && profile.getPreferredLanguage().equals("en")) {
        //     dateLocale = Locale.ENGLISH;
        // }

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_TOP);
        String invoiceNumber = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        titleCell.addElement(new Paragraph("Invoice #" + invoiceNumber, normalFont));
        titleCell.addElement(new Paragraph("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)), normalFont));
        headerTable.addCell(titleCell);

        document.add(headerTable);
        document.add(Chunk.NEWLINE);

        // From and To Section
        PdfPTable addressTable = new PdfPTable(2);
        addressTable.setWidthPercentage(100);
        addressTable.setWidths(new float[]{1, 1});

        PdfPCell fromCell = new PdfPCell();
        fromCell.setBorder(Rectangle.NO_BORDER);
        fromCell.addElement(new Paragraph("From:", boldFont));
        fromCell.addElement(new Paragraph(sender, normalFont));
        if (profile.getEmail() != null) fromCell.addElement(new Paragraph(profile.getEmail(), normalFont));
        if (profile.getPhone() != null) fromCell.addElement(new Paragraph(profile.getPhone(), normalFont));
        addressTable.addCell(fromCell);

        PdfPCell toCell = new PdfPCell();
        toCell.setBorder(Rectangle.NO_BORDER);
        toCell.addElement(new Paragraph("To:", boldFont));
        toCell.addElement(new Paragraph(invoiceTo, normalFont));
        addressTable.addCell(toCell);

        document.add(addressTable);
        document.add(Chunk.NEWLINE);

        // Task Table (Adjusted for better fit)
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.5f, 2.5f, 1f, 1f, 1f, 1f, 1f}); // Adjusted widths for better spacing
        table.getDefaultCell().setPadding(5);
        table.getDefaultCell().setBackgroundColor(BaseColor.LIGHT_GRAY);

        // Table Headers with multi-line support
        PdfPCell taskHeader = new PdfPCell(new Phrase("Task", boldFont));
        PdfPCell descHeader = new PdfPCell(new Phrase("Description", boldFont));
        PdfPCell hoursHeader = new PdfPCell(new Phrase("Hours", boldFont));
        PdfPCell rateHeader = new PdfPCell(new Phrase("Rate", boldFont));
        PdfPCell totalHeader = new PdfPCell(new Phrase("Total", boldFont));
        PdfPCell advanceHeader = new PdfPCell(new Phrase("Advance\nPaid", boldFont)); // Split into two lines
        PdfPCell remainingHeader = new PdfPCell(new Phrase("Amount\nDue", boldFont)); // Split into two lines
        table.addCell(taskHeader);
        table.addCell(descHeader);
        table.addCell(hoursHeader);
        table.addCell(rateHeader);
        table.addCell(totalHeader);
        table.addCell(advanceHeader);
        table.addCell(remainingHeader);
        table.getDefaultCell().setBackgroundColor(null);

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
        totalsTable.addCell(new PdfPCell(new Phrase("Grand Total:", boldFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(String.format("$%.2f", grandTotal), normalFont)));
        totalsTable.addCell(new PdfPCell(new Phrase("Advance Paid:", boldFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(String.format("$%.2f", grandAdvance), normalFont)));
        totalsTable.addCell(new PdfPCell(new Phrase("Amount Due:", boldFont)));
        totalsTable.addCell(new PdfPCell(new Phrase(String.format("$%.2f", grandRemainingDue), boldFont)));
        document.add(Chunk.NEWLINE);
        document.add(totalsTable);

        // Footer (Simplified)
        document.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Contact us at " +
                (profile.getEmail() != null ? profile.getEmail() : user.getUsername() + "@example.com"), footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
        return out.toByteArray();
    }
}