package com.nightout.config;

import com.nightout.domain.*;
import com.nightout.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DataInitializer — seeds the database with realistic test data on startup.
 *
 * WHY CommandLineRunner?
 * CommandLineRunner is a Spring interface with one method: run().
 * Spring calls run() automatically after the application context is fully
 * started — after all beans are wired and the database schema is ready.
 * This is the right place to insert seed data.
 *
 * @Profile("dev") — this bean ONLY exists in the dev profile.
 * It will NOT run in tests (which use the 'test' profile) or in production.
 * This prevents seed data from polluting your test assertions or prod DB.
 *
 * The seeder is IDEMPOTENT — it checks before inserting.
 * Running the app multiple times won't duplicate rows.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final NightRepository nightRepository;
    private final TagRepository tagRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("=== Seeding development data ===");
        seedRoles();
        seedUsers();
        seedVenuesAndNights();
        log.info("=== Seeding complete ===");
    }

    // ── STEP 1: Roles ─────────────────────────────────────────────────────────

    private void seedRoles() {
        for (Role.RoleName name : Role.RoleName.values()) {
            if (!roleRepository.existsByName(name)) {
                roleRepository.save(Role.builder().name(name).build());
                log.debug("Created role: {}", name);
            }
        }
    }

    // ── STEP 2: Users ─────────────────────────────────────────────────────────

    private void seedUsers() {
        if (userRepository.existsByEmail("admin@nightout.com")) return;

        Role adminRole  = roleRepository.findByName(Role.RoleName.ROLE_ADMIN).orElseThrow();
        Role userRole   = roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow();
        Role ownerRole  = roleRepository.findByName(Role.RoleName.ROLE_VENUE_OWNER).orElseThrow();

        // Admin account
        User admin = User.builder()
                .username("admin")
                .email("admin@nightout.com")
                .passwordHash(passwordEncoder.encode("Admin1234!"))
                .bio("NightOut platform administrator")
                .build();
        admin.addRole(adminRole);
        admin.addRole(userRole);
        userRepository.save(admin);

        // Venue owner 1 — owns Club Nova
        User owner1 = User.builder()
                .username("clubnova_owner")
                .email("owner@clubnova.com")
                .passwordHash(passwordEncoder.encode("Owner1234!"))
                .bio("Owner of Club Nova — the city's premier electronic music venue")
                .build();
        owner1.addRole(ownerRole);
        owner1.addRole(userRole);
        userRepository.save(owner1);

        // Venue owner 2 — owns Skybar
        User owner2 = User.builder()
                .username("skybar_owner")
                .email("owner@skybar.com")
                .passwordHash(passwordEncoder.encode("Owner1234!"))
                .bio("Managing director at Skybar Rooftop")
                .build();
        owner2.addRole(ownerRole);
        owner2.addRole(userRole);
        userRepository.save(owner2);

        // Regular users
        User alice = createRegularUser("alice", "alice@example.com", "Nightlife explorer 🌙");
        User bob   = createRegularUser("bob",   "bob@example.com",   "House music lover 🎵");
        User carol = createRegularUser("carol", "carol@example.com", "Always up for a good time");

        // Build a small social graph: alice follows bob and carol
        alice.follow(bob);
        alice.follow(carol);
        bob.follow(alice);
        userRepository.save(alice);
        userRepository.save(bob);

        log.info("Seeded {} users", userRepository.count());
    }

    private User createRegularUser(String username, String email, String bio) {
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow();
        User user = User.builder()
                .username(username).email(email)
                .passwordHash(passwordEncoder.encode("User1234!"))
                .bio(bio).build();
        user.addRole(userRole);
        return userRepository.save(user);
    }

    // ── STEP 3: Venues and nights ─────────────────────────────────────────────

    private void seedVenuesAndNights() {
        if (venueRepository.count() > 0) return;

        User owner1 = userRepository.findByEmail("owner@clubnova.com").orElseThrow();
        User owner2 = userRepository.findByEmail("owner@skybar.com").orElseThrow();

        // Tags
        Tag houseTag   = getOrCreateTag("house music");
        Tag technoTag  = getOrCreateTag("techno");
        Tag rooftopTag = getOrCreateTag("rooftop");
        Tag liveTag    = getOrCreateTag("live act");
        Tag dresscode  = getOrCreateTag("dress code");

        // ── Club Nova ──────────────────────────────────────────────────────────
        Address novaAddress = Address.builder()
                .street("Strada Victoriei 42").city("Bucharest")
                .postalCode("010072").country("Romania")
                .latitude(44.4396).longitude(26.0963).build();

        Venue clubNova = Venue.builder()
                .name("Club Nova").type(Venue.VenueType.CLUB)
                .description("Bucharest's premier underground electronic music club. " +
                        "State-of-the-art sound system, internationally acclaimed DJs.")
                .address(novaAddress).owner(owner1)
                .averageRating(4.7).totalRatings(238).verified(true).build();
        venueRepository.save(clubNova);

        // Tonight's night at Club Nova
        Night novaTonight = Night.builder()
                .venue(clubNova).title("Deep House Friday")
                .description("Join us for an all-night deep house session with residents DJ Mara and Ion B.")
                .date(LocalDate.now()).startTime(LocalTime.of(23, 0))
                .specialGuest(false).theme("Deep House")
                .tableCapacity(40).active(true).build();
        novaTonight.getTags().add(houseTag);
        novaTonight.getTags().add(dresscode);
        nightRepository.save(novaTonight);

        // Special event next weekend
        Night novaSpecial = Night.builder()
                .venue(clubNova).title("Solomun Guest Night")
                .description("An unmissable night. Solomun plays an exclusive 6-hour set.")
                .date(LocalDate.now().plusDays(7)).startTime(LocalTime.of(22, 0))
                .specialGuest(true).guestName("Solomun").theme("Techno / Melodic")
                .tableCapacity(20).active(true).build();
        novaSpecial.getTags().add(technoTag);
        novaSpecial.getTags().add(liveTag);
        novaSpecial.getTags().add(dresscode);
        nightRepository.save(novaSpecial);

        // ── Skybar Rooftop ────────────────────────────────────────────────────
        Address skybarAddress = Address.builder()
                .street("Calea Dorobanților 5").city("Bucharest")
                .postalCode("010551").country("Romania")
                .latitude(44.4529).longitude(26.0934).build();

        Venue skybar = Venue.builder()
                .name("Skybar Rooftop").type(Venue.VenueType.ROOFTOP)
                .description("12th floor rooftop bar with panoramic city views. " +
                        "Cocktails, sunset sessions, and weekend DJ sets.")
                .address(skybarAddress).owner(owner2)
                .averageRating(4.4).totalRatings(156).verified(true).build();
        venueRepository.save(skybar);

        // Tonight at Skybar
        Night skybarTonight = Night.builder()
                .venue(skybar).title("Sunset Cocktails & Beats")
                .description("Rooftop sunset with live DJ and signature cocktail menu.")
                .date(LocalDate.now()).startTime(LocalTime.of(18, 0)).endTime(LocalTime.of(2, 0))
                .specialGuest(false).theme("Rooftop Vibes")
                .tableCapacity(60).active(true).build();
        skybarTonight.getTags().add(rooftopTag);
        nightRepository.save(skybarTonight);

        log.info("Seeded {} venues and {} nights",
                venueRepository.count(), nightRepository.count());
    }

    private Tag getOrCreateTag(String name) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
    }
}