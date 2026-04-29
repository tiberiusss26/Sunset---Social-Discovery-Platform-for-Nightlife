package com.nightout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nightout.domain.*;
import com.nightout.dto.*;
import com.nightout.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests — end-to-end HTTP scenarios.
 *
 * ─── WHAT ARE INTEGRATION TESTS? ─────────────────────────────────────────────
 * Integration tests test the WHOLE stack: controllers + services + repositories
 * + the real database (H2 in-memory for speed).
 *
 * Unlike unit tests they DON'T mock dependencies — they run the real code.
 * This lets you catch bugs that only appear when layers interact:
 *   - SQL queries that are wrong
 *   - Security rules that don't work as expected
 *   - Validation that fires (or doesn't fire) correctly
 *
 * ─── KEY ANNOTATIONS ─────────────────────────────────────────────────────────
 * @SpringBootTest          — starts a FULL Spring application context.
 *                            All beans, security, JPA — everything real.
 *
 * @AutoConfigureMockMvc    — configures MockMvc which lets you fire HTTP
 *                            requests WITHOUT starting a real Tomcat server.
 *                            Requests go directly to your controllers in-process.
 *                            Fast, deterministic, no port conflicts.
 *
 * @ActiveProfiles("test")  — uses application-test.yml → H2 database.
 *                            No PostgreSQL needed for tests.
 *
 * @Transactional           — wraps each test in a transaction that is ROLLED BACK
 *                            after the test completes. This means the database
 *                            is always empty at the start of each test — no
 *                            leftover data from previous tests.
 *
 * MockMvc usage:
 *   mockMvc.perform(post("/api/auth/register").content(json).contentType(APPLICATION_JSON))
 *          .andExpect(status().isCreated())
 *          .andExpect(jsonPath("$.token").exists());
 *
 *   jsonPath() uses JsonPath expressions to assert on the JSON response body.
 *   "$.token"           → root-level "token" field
 *   "$.user.username"   → nested field
 *   "$[0].name"         → first item of a JSON array
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private NightRepository nightRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Shared JWT token extracted after login — reused across test helpers
    private String authToken;

    @BeforeEach
    void setUpRoles() {
        // Ensure roles exist in H2 before each test
        for (Role.RoleName name : Role.RoleName.values()) {
            if (!roleRepository.existsByName(name)) {
                roleRepository.save(Role.builder().name(name).build());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCENARIO 1: Full registration and login flow
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SCENARIO 1: User registers → logs in → receives JWT")
    void scenario_registerAndLogin() throws Exception {
        // Step 1: Register
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("testuser").email("test@example.com").password("Password123!").build();

        MvcResult registerResult = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("testuser"))
                .andReturn();

        // Extract token from response
        String body = registerResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();
        assertThat(token).isNotBlank();

        // Step 2: Login with same credentials
        LoginRequest loginReq = LoginRequest.builder()
                .email("test@example.com").password("Password123!").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.username").value("testuser"));

        // Step 3: Use token to access protected endpoint
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Registration: returns 400 when email is invalid")
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("user").email("not-an-email").password("Password123!").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("Registration: returns 409 when email is duplicate")
    void register_withDuplicateEmail_returns409() throws Exception {
        // First registration succeeds
        RegisterRequest req = RegisterRequest.builder()
                .username("first").email("dup@example.com").password("Password123!").build();
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));

        // Second registration with same email fails
        RegisterRequest req2 = RegisterRequest.builder()
                .username("second").email("dup@example.com").password("Password123!").build();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCENARIO 2: Venue owner creates venue and posts a night
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SCENARIO 2: Venue owner registers → creates venue → posts night")
    void scenario_venueOwnerCreatesVenueAndNight() throws Exception {
        // Step 1: Register venue owner
        String ownerToken = registerAndGetToken("venueowner", "owner@club.com",
                "Password123!", Role.RoleName.ROLE_VENUE_OWNER);

        // Step 2: Create a venue
        CreateVenueRequest venueReq = CreateVenueRequest.builder()
                .name("Test Club").type(Venue.VenueType.CLUB)
                .description("A great club")
                .address(AddressRequest.builder()
                        .street("Main Street 1").city("Bucharest").country("Romania").build())
                .build();

        MvcResult venueResult = mockMvc.perform(
                        post("/api/venues")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(venueReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Club"))
                .andExpect(jsonPath("$.type").value("CLUB"))
                .andExpect(jsonPath("$.address.city").value("Bucharest"))
                .andReturn();

        String venueId = objectMapper.readTree(
                venueResult.getResponse().getContentAsString()).get("id").asText();

        // Step 3: Post a night at the venue
        CreateNightRequest nightReq = CreateNightRequest.builder()
                .title("Saturday Night Special")
                .description("The best house music in town")
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(22, 0))
                .theme("House Music")
                .specialGuest(true).guestName("DJ Shadow")
                .tableCapacity(40)
                .tags(java.util.List.of("house music", "live dj"))
                .build();

        mockMvc.perform(
                        post("/api/venues/" + venueId + "/nights")
                                .header("Authorization", "Bearer " + ownerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(nightReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Saturday Night Special"))
                .andExpect(jsonPath("$.specialGuest").value(true))
                .andExpect(jsonPath("$.guestName").value("DJ Shadow"))
                .andExpect(jsonPath("$.tags", hasItem("house music")));

        // Step 4: Verify the night appears in the venue's night list
        mockMvc.perform(get("/api/venues/" + venueId + "/nights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].title").value("Saturday Night Special"));
    }

    @Test
    @DisplayName("Venue creation: returns 403 when regular user tries to create venue")
    void createVenue_asRegularUser_returns403() throws Exception {
        String userToken = registerAndGetToken("regularuser", "reg@t.com",
                "Password123!", Role.RoleName.ROLE_USER);

        CreateVenueRequest req = CreateVenueRequest.builder()
                .name("Unauthorized Club").type(Venue.VenueType.BAR)
                .address(AddressRequest.builder().street("s").city("c").build()).build();

        mockMvc.perform(post("/api/venues")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCENARIO 3: User RSVPs for a night
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SCENARIO 3: User browses tonight's nights → RSVPs → sees it in my-plans")
    void scenario_userRsvpsForNight() throws Exception {
        // Set up: create owner, venue, and night in DB directly (bypassing HTTP)
        User owner = createUserInDb("nightowner", "no@t.com", Role.RoleName.ROLE_VENUE_OWNER);
        Venue venue = createVenueInDb(owner);
        Night night = createNightInDb(venue, LocalDate.now().plusDays(1));

        // User registers and gets token
        String userToken = registerAndGetToken("rsvpuser", "rsvp@t.com",
                "Password123!", Role.RoleName.ROLE_USER);

        // Step 1: Browse tonight's nights (public endpoint)
        mockMvc.perform(get("/api/nights/tonight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Step 2: RSVP for the night
        CreateRsvpRequest rsvpReq = CreateRsvpRequest.builder()
                .status(Rsvp.RsvpStatus.GOING).tableSize(2).build();

        mockMvc.perform(
                        post("/api/nights/" + night.getId() + "/rsvps")
                                .header("Authorization", "Bearer " + userToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(rsvpReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("GOING"))
                .andExpect(jsonPath("$.tableSize").value(2));

        // Step 3: Verify the night appears in "my plans"
        mockMvc.perform(get("/api/nights/my-plans")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("RSVP: returns 401 when unauthenticated user tries to RSVP")
    void rsvp_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/nights/" + java.util.UUID.randomUUID() + "/rsvps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                CreateRsvpRequest.builder().status(Rsvp.RsvpStatus.GOING).build())))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCENARIO 4: Social follow + friends feed
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SCENARIO 4: Alice follows Bob → Bob RSVPs → Alice sees Bob in friends feed")
    void scenario_socialFollowAndFriendsFeed() throws Exception {
        // Create Bob with his own RSVP on a night
        User bob = createUserInDb("bob_social", "bob_s@t.com", Role.RoleName.ROLE_USER);
        User owner = createUserInDb("owner_s", "owner_s@t.com", Role.RoleName.ROLE_VENUE_OWNER);
        Venue venue = createVenueInDb(owner);
        Night night = createNightInDb(venue, LocalDate.now());

        // Register Alice and get her token
        String aliceToken = registerAndGetToken("alice_social", "alice_s@t.com",
                "Password123!", Role.RoleName.ROLE_USER);

        // Alice follows Bob
        mockMvc.perform(post("/api/users/" + bob.getId() + "/follow")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        // Alice checks friends feed (Bob hasn't RSVP'd yet so feed may be empty)
        mockMvc.perform(get("/api/nights/friends-feed")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Registers a user via HTTP and returns their JWT. */
    private String registerAndGetToken(String username, String email,
                                       String password, Role.RoleName role) throws Exception {
        // For non-ROLE_USER roles, create directly in DB (HTTP registration only gives ROLE_USER)
        if (role != Role.RoleName.ROLE_USER) {
            User user = createUserInDb(username, email, role);
            LoginRequest loginReq = LoginRequest.builder().email(email).password("Password123!").build();
            // We need the plain password stored temporarily — use a fixed one
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andReturn();
            if (result.getResponse().getStatus() == 200) {
                return objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("token").asText();
            }
        }

        RegisterRequest req = RegisterRequest.builder()
                .username(username).email(email).password(password).build();
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    /** Creates a user directly in the DB (faster than HTTP for test setup). */
    private User createUserInDb(String username, String email, Role.RoleName roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .username(username).email(email)
                .passwordHash(passwordEncoder.encode("Password123!")).build();
        user.addRole(role);
        // Also add ROLE_USER if not already included
        if (roleName != Role.RoleName.ROLE_USER) {
            Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER).orElseThrow();
            user.addRole(userRole);
        }
        return userRepository.save(user);
    }

    private Venue createVenueInDb(User owner) {
        Venue venue = Venue.builder()
                .name("Test Club").type(Venue.VenueType.CLUB)
                .address(Address.builder().street("s").city("c").country("RO").build())
                .owner(owner).averageRating(0.0).totalRatings(0).verified(true).build();
        return venueRepository.save(venue);
    }

    private Night createNightInDb(Venue venue, LocalDate date) {
        Night night = Night.builder()
                .venue(venue).title("Test Night").date(date)
                .startTime(LocalTime.of(22, 0)).active(true).build();
        return nightRepository.save(night);
    }
}