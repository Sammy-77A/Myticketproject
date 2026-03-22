package com.myticket.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "offline", matchIfMissing = true)
public class MockEmailService implements EmailService {

    private final Path outboxDir;

    public MockEmailService() throws IOException {
        this.outboxDir = Paths.get("emails", "outbox");
        if (!Files.exists(outboxDir)) {
            Files.createDirectories(outboxDir);
        }
    }

    private void mockSend(String to, String type, String subject, String body) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
        String filename = String.format("%s_%s_%s.txt", timestamp, to, type);
        
        String content = String.format("To: %s\nSubject: %s\n\n%s", to, subject, body);
        
        try {
            Files.writeString(outboxDir.resolve(filename), content);
            System.out.println("MOCK EMAIL SENT: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String token) {
        String text = String.format("Hi %s,\n\nYour verification token is: %s\n\nThanks,\nMyTicket", fullName, token);
        mockSend(to, "verification", "MyTicket - Verify your email", text);
    }

    @Override
    public void sendBookingConfirmation(String to, String fullName, String eventTitle, String ticketCode, String qrPath) {
        String text = String.format("Hi %s,\n\nYour booking for '%s' is confirmed. Ticket Code: %s\nQR Path: %s\n\nThanks,\nMyTicket", fullName, eventTitle, ticketCode, qrPath);
        mockSend(to, "booking", "MyTicket - Booking Confirmation", text);
    }

    @Override
    public void sendCancellationNotice(String to, String fullName, String eventTitle) {
        mockSend(to, "cancellation", "MyTicket - Event Cancelled", "Hi " + fullName + ",\nThe event '" + eventTitle + "' has been cancelled.");
    }

    @Override
    public void sendTransferNotice(String to, String fullName, String eventTitle) {
        mockSend(to, "transfer", "MyTicket - Ticket Transferred", "Hi " + fullName + ",\nYou received a transferred ticket for '" + eventTitle + "'.");
    }

    @Override
    public void sendEventUpdate(String to, String fullName, String eventTitle, String updateDetail) {
        mockSend(to, "update", "MyTicket - Event Update", "Hi " + fullName + ",\nUpdate for '" + eventTitle + "':\n" + updateDetail);
    }

    @Override
    public void sendEventReminder(String to, String fullName, String eventTitle, String eventDate) {
        mockSend(to, "reminder", "MyTicket - Event Reminder", "Hi " + fullName + ",\nReminder: '" + eventTitle + "' is on " + eventDate);
    }

    @Override
    public void sendWaitlistSlotAvailable(String to, String fullName, String eventTitle, String claimLink) {
        mockSend(to, "waitlist", "MyTicket - Waitlist Slot", "Hi " + fullName + ",\nA slot for '" + eventTitle + "' is available! Claim here: " + claimLink);
    }

    @Override
    public void sendReferralReward(String to, String fullName, int creditsEarned) {
        mockSend(to, "referral", "MyTicket - Referral Reward", "Hi " + fullName + ",\nYou earned " + creditsEarned + " credits for a referral!");
    }

    @Override
    public void sendNewsletterAlert(String to, String eventTitle, String eventDate, String venue) {
        mockSend(to, "newsletter", "MyTicket - New Event", "A new event was posted: " + eventTitle + " on " + eventDate + " at " + venue);
    }
}
