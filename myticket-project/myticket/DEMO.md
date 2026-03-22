# MyTicket Demo Script

Follow this step-by-step walkthrough to present the MyTicket application end-to-end.

1. **Start backend in offline mode.**
   Run `mvn -pl myticket-backend spring-boot:run -Dspring-boot.run.profiles=offline`.
   Navigate your browser to `http://localhost:8080/api/health` and show that it returns `{"status":"ok","mode":"offline"}`.

2. **Open the Web Frontend (`index.html`).**
   Visit `http://localhost:8080/index.html`. Showcase the homepage featuring dynamically loaded events, the category filter pills, and the "Trending This Week" horizontal scrolling row.

3. **Register a Student Account.**
   Click "Login/Register" -> "Create Account". Complete the registration form. Navigate to the generated `emails/outbox/` folder in the project root and open the newest `.txt` file to show the mock verification email.

4. **Verify Email & Log in.**
   Extract the verification token from the text file and visit `http://localhost:8080/api/auth/verify-email?token={token}`. Proceed to `login.html` and log in with your new student credentials.

5. **Test Age Restriction (Age Gate).**
   Locate the "Tech Career Fair" event (it is seeded with an 18+ age restriction). Attempt to book it and enter a Date of Birth that evaluates to under 18. Demonstrate the system actively rejecting the booking.

6. **Free Event Booking & QR Code.**
   Browse to the "Maseno Cultural Festival 2026" event. Select the free "General Admission" tier. Proceed through the booking wizard. Once confirmed, show the QR Code directly rendered on the success page, demonstrate the WhatsApp share button, and show the physical QR image generated in the backend's `qr-codes/` directory.

7. **Mock STK Push Payment.**
   Browse to the "Comradery Night Bash" event and select the "Regular" paid tier. Upon attempting to book, show that the system detects `offline` mode and routes the user to the Mock Checkout terminal (`stk-waiting.html`). Simulate entering a PIN and confirming payment to receive the ticket.

8. **Organizer Login in JavaFX.**
   Open a new terminal. Run `mvn -pl myticket-desktop javafx:run`. On the login screen, enter the seeded Organizer credentials (`organizer@maseno.ac.ke` / `Organizer1234!`).

9. **Desktop Dashboard & Analytics.**
   Present the Organizer Dashboard. Show the high-level event statistics, tier breakdowns, and the master events table.

10. **WebSocket Live QR Scanning.**
    In the JavaFX app, navigate to the **Scanner** tab. Type in the alphanumeric Ticket Code generated during Step 6 and hit "Scan". Demonstrate the UI resolving the ticket status to USED and the live attendance counter incrementing dynamically via WebSockets.

11. **Comprehensive Analytics.**
    Navigate to the **Analytics** tab within the JavaFX app. Display the graphical charts illustrating booking velocity and overall attendance rates across events.

12. **Student Dashboard & Follow System.**
    Return to the Web browser at `account.html`. Display the "My Tickets" tab illustrating past and upcoming tickets. Showcase the activity feed, follow lists, and review gates (you can now leave a review for the ticket scanned in Step 10).

13. **Newsletter Campaign.**
    On the Web Homepage, subscribe to the newsletter. In the JavaFX application, create a newly scheduled Event. Immediately navigate back to the `emails/outbox/` folder to prove the automated newsletter blast was dispatched reliably to your subscription.

14. **Online Mode Transition.**
    Restart the backend without the offline profile (`mvn -pl myticket-backend spring-boot:run`). Refresh the Web login page to explicitly show the Google OAuth button activating. Navigate to a paid event to show the "Pay with M-Pesa STK Push" button asserting dominance over the Mock terminal.
