package com.myticket.backend.service;

public interface EmailService {
    void sendVerificationEmail(String to, String fullName, String token);
    void sendBookingConfirmation(String to, String fullName, String eventTitle, String ticketCode, String qrPath);
    void sendCancellationNotice(String to, String fullName, String eventTitle);
    void sendTransferNotice(String to, String fullName, String eventTitle);
    void sendEventUpdate(String to, String fullName, String eventTitle, String updateDetail);
    void sendEventReminder(String to, String fullName, String eventTitle, String eventDate);
    void sendWaitlistSlotAvailable(String to, String fullName, String eventTitle, String claimLink);
    void sendReferralReward(String to, String fullName, int creditsEarned);
    void sendNewsletterAlert(String to, String eventTitle, String eventDate, String venue);
}
