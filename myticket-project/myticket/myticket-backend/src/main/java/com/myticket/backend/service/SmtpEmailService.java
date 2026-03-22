package com.myticket.backend.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mode", havingValue = "online")
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@myticket.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    @Override
    public void sendVerificationEmail(String to, String fullName, String token) {
        String text = String.format("Hi %s,\n\nYour verification token is: %s\n\nThanks,\nMyTicket", fullName, token);
        send(to, "MyTicket - Verify your email", text);
    }

    @Override
    public void sendBookingConfirmation(String to, String fullName, String eventTitle, String ticketCode, String qrPath) {
        String text = String.format("Hi %s,\n\nYour booking for '%s' is confirmed. Ticket Code: %s\n\nThanks,\nMyTicket", fullName, eventTitle, ticketCode);
        send(to, "MyTicket - Booking Confirmation", text);
    }

    @Override
    public void sendCancellationNotice(String to, String fullName, String eventTitle) {
        send(to, "MyTicket - Event Cancelled", "Hi " + fullName + ",\nThe event '" + eventTitle + "' has been cancelled.");
    }

    @Override
    public void sendTransferNotice(String to, String fullName, String eventTitle) {
        send(to, "MyTicket - Ticket Transferred", "Hi " + fullName + ",\nYou received a transferred ticket for '" + eventTitle + "'.");
    }

    @Override
    public void sendEventUpdate(String to, String fullName, String eventTitle, String updateDetail) {
        send(to, "MyTicket - Event Update", "Hi " + fullName + ",\nUpdate for '" + eventTitle + "':\n" + updateDetail);
    }

    @Override
    public void sendEventReminder(String to, String fullName, String eventTitle, String eventDate) {
        send(to, "MyTicket - Event Reminder", "Hi " + fullName + ",\nReminder: '" + eventTitle + "' is on " + eventDate);
    }

    @Override
    public void sendWaitlistSlotAvailable(String to, String fullName, String eventTitle, String claimLink) {
        send(to, "MyTicket - Waitlist Slot", "Hi " + fullName + ",\nA slot for '" + eventTitle + "' is available! Claim here: " + claimLink);
    }

    @Override
    public void sendReferralReward(String to, String fullName, int creditsEarned) {
        send(to, "MyTicket - Referral Reward", "Hi " + fullName + ",\nYou earned " + creditsEarned + " credits for a referral!");
    }

    @Override
    public void sendNewsletterAlert(String to, String eventTitle, String eventDate, String venue) {
        send(to, "MyTicket - New Event", "A new event was posted: " + eventTitle + " on " + eventDate + " at " + venue);
    }
}
