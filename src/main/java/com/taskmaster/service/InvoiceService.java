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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

        // Use the clientName from the first task (assumes all tasks share the same client for simplicity)
        String invoiceTo = selectedTasks.stream()
                .map(Task::getClientName)
                .filter(name -> name != null && !name.isEmpty())
                .findFirst()
                .orElse("Client");

        Optional<UserProfile> profileOpt = userProfileRepository.findByUser(user);
        UserProfile profile = profileOpt.orElse(new UserProfile());

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        if (profile.getLogoUrl() != null && !profile.getLogoUrl().isEmpty()) {
            Image logo = Image.getInstance(profile.getLogoUrl());
            logo.scaleToFit(100, 100);
            document.add(logo);
        }

        document.add(new Paragraph("Invoice", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
        document.add(new Paragraph("From: " + (profile.getCompanyName() != null ? profile.getCompanyName() : user.getUsername())));
        document.add(new Paragraph("To: " + invoiceTo));
        document.add(new Paragraph("Date: " + LocalDateTime.now().toString()));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(6);
        table.setWidths(new float[]{3, 1, 1, 1, 1, 1});
        table.addCell(new PdfPCell(new Phrase("Task", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Hours", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Rate", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Total", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Advance", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));
        table.addCell(new PdfPCell(new Phrase("Remaining", FontFactory.getFont(FontFactory.HELVETICA_BOLD))));

        double grandTotal = 0;
        double grandAdvance = 0;
        double grandRemainingDue = 0;
        for (Task task : selectedTasks) {
            table.addCell(task.getTitle());
            table.addCell(String.valueOf(task.getHoursWorked()));
            table.addCell(String.format("$%.2f", task.getHourlyRate()));
            table.addCell(String.format("$%.2f", task.getTotal()));
            table.addCell(String.format("$%.2f", task.getAdvancePayment()));
            table.addCell(String.format("$%.2f", task.getRemainingDue()));
            grandTotal += task.getTotal();
            grandAdvance += task.getAdvancePayment();
            grandRemainingDue += task.getRemainingDue();
        }
        document.add(table);

        document.add(Chunk.NEWLINE);
        document.add(new Paragraph("Grand Total: $" + String.format("%.2f", grandTotal)));
        document.add(new Paragraph("Advance Paid: $" + String.format("%.2f", grandAdvance)));
        document.add(new Paragraph("Remaining Due: $" + String.format("%.2f", grandRemainingDue),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD)));

        document.close();
        return out.toByteArray();
    }
}