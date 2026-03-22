package com.myticket.backend.config;

import com.myticket.backend.model.*;
import com.myticket.backend.repository.*;
import com.myticket.common.enums.Role;
import com.myticket.common.enums.EventStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      EventRepository eventRepository,
                      TicketTierRepository ticketTierRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.eventRepository = eventRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() > 0) {
            return;
        }

        System.out.println("Starting Data Seeder...");

        // 1. Categories
        Category sports = new Category(); sports.setName("Sports"); sports.setColorHex("#0d6efd"); categoryRepository.save(sports);
        Category academic = new Category(); academic.setName("Academic"); academic.setColorHex("#6610f2"); categoryRepository.save(academic);
        Category cultural = new Category(); cultural.setName("Cultural"); cultural.setColorHex("#d63384"); categoryRepository.save(cultural);
        Category social = new Category(); social.setName("Social"); social.setColorHex("#fd7e14"); categoryRepository.save(social);
        Category career = new Category(); career.setName("Career"); career.setColorHex("#20c997"); categoryRepository.save(career);

        // 2. Users
        User admin = new User();
        admin.setEmail("admin@maseno.ac.ke");
        admin.setFullName("Maseno Admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin1234!"));
        admin.setRole(Role.ADMIN);
        admin.setVerified(true);
        admin.setDateOfBirth(LocalDate.of(1990, 1, 1));
        userRepository.save(admin);

        User organizer = new User();
        organizer.setEmail("organizer@maseno.ac.ke");
        organizer.setFullName("Maseno Drama Club");
        organizer.setPasswordHash(passwordEncoder.encode("Organizer1234!"));
        organizer.setRole(Role.ORGANIZER);
        organizer.setVerified(true);
        organizer.setBio("Official Drama Club of Maseno University. We host the best plays in campus!");
        organizer.setDateOfBirth(LocalDate.of(1995, 1, 1));
        userRepository.save(organizer);

        User student1 = new User();
        student1.setEmail("student1@student.maseno.ac.ke");
        student1.setFullName("John Doe");
        student1.setPasswordHash(passwordEncoder.encode("Student1234!"));
        student1.setRole(Role.STUDENT);
        student1.setVerified(true);
        student1.setDateOfBirth(LocalDate.of(2000, 1, 1));
        userRepository.save(student1);

        User student2 = new User();
        student2.setEmail("student2@student.maseno.ac.ke");
        student2.setFullName("Jane Smith");
        student2.setPasswordHash(passwordEncoder.encode("Student1234!"));
        student2.setRole(Role.STUDENT);
        student2.setVerified(true);
        student2.setDateOfBirth(LocalDate.of(2001, 1, 1));
        userRepository.save(student2);

        User student3 = new User();
        student3.setEmail("student3@student.maseno.ac.ke");
        student3.setFullName("Peter Parker");
        student3.setPasswordHash(passwordEncoder.encode("Student1234!"));
        student3.setRole(Role.STUDENT);
        student3.setVerified(true);
        student3.setDateOfBirth(LocalDate.of(2002, 1, 1));
        userRepository.save(student3);

        // 3. Events
        Event event1 = new Event();
        event1.setTitle("Maseno Cultural Festival 2026");
        event1.setDescription("Annual cultural extravaganza featuring dances, poetry, and fashion shows.");
        event1.setVenue("Main Graduation Square");
        event1.setEventDate(LocalDateTime.now().plusDays(10));
        event1.setCategory(cultural);
        event1.setOrganizer(organizer);
        event1.setMinAge(0);
        event1.setStatus(EventStatus.UPCOMING);
        event1.setTotalCapacity(550);
        eventRepository.save(event1);

        TicketTier e1t1 = new TicketTier();
        e1t1.setEvent(event1);
        e1t1.setName("General Admission");
        e1t1.setPrice(0);
        e1t1.setCapacity(500);
        e1t1.setPerks("Entry");
        ticketTierRepository.save(e1t1);

        TicketTier e1t2 = new TicketTier();
        e1t2.setEvent(event1);
        e1t2.setName("VIP");
        e1t2.setPrice(500);
        e1t2.setCapacity(50);
        e1t2.setPerks("Front Row Seat, Free Drink");
        ticketTierRepository.save(e1t2);

        Event event2 = new Event();
        event2.setTitle("Tech Career Fair");
        event2.setDescription("Meet top tech employers and secure your internship.");
        event2.setVenue("Science Complex Hall");
        event2.setEventDate(LocalDateTime.now().plusDays(5));
        event2.setCategory(career);
        event2.setOrganizer(admin);
        event2.setMinAge(18); // age gate
        event2.setStatus(EventStatus.UPCOMING);
        event2.setTotalCapacity(300);
        eventRepository.save(event2);

        TicketTier e2t1 = new TicketTier();
        e2t1.setEvent(event2);
        e2t1.setName("Student Pass");
        e2t1.setPrice(200);
        e2t1.setCapacity(200);
        e2t1.setPerks("Booth Access");
        e2t1.setEarlyBird(true);
        e2t1.setClosesAt(LocalDateTime.now().plusDays(1)); // Closing soon
        ticketTierRepository.save(e2t1);

        TicketTier e2t2 = new TicketTier();
        e2t2.setEvent(event2);
        e2t2.setName("Standard Pass");
        e2t2.setPrice(300);
        e2t2.setCapacity(100);
        e2t2.setPerks("Booth Access");
        ticketTierRepository.save(e2t2);

        Event event3 = new Event();
        event3.setTitle("Comradery Night Bash");
        event3.setDescription("The biggest social gathering of the semester.");
        event3.setVenue("Student Center");
        event3.setEventDate(LocalDateTime.now().plusDays(20));
        event3.setCategory(social);
        event3.setOrganizer(organizer);
        event3.setMinAge(18);
        event3.setStatus(EventStatus.UPCOMING);
        event3.setTotalCapacity(350);
        eventRepository.save(event3);

        TicketTier e3t1 = new TicketTier();
        e3t1.setEvent(event3);
        e3t1.setName("Early Bird");
        e3t1.setPrice(150);
        e3t1.setCapacity(50);
        e3t1.setEarlyBird(true);
        e3t1.setClosesAt(LocalDateTime.now().minusDays(1)); // Expired tier
        e3t1.setExpired(true);
        ticketTierRepository.save(e3t1);

        TicketTier e3t2 = new TicketTier();
        e3t2.setEvent(event3);
        e3t2.setName("Regular");
        e3t2.setPrice(250);
        e3t2.setCapacity(300);
        ticketTierRepository.save(e3t2);

        System.out.println("Demo data seeded successfully.");
    }
}
