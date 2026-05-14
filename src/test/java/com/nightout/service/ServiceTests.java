package com.nightout.service;

import com.nightout.domain.*;
import com.nightout.dto.*;
import com.nightout.exception.*;
import com.nightout.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock private VenueRepository venueRepository;
    @Mock private UserRepository userRepository;
    @Mock private VenueRatingRepository ratingRepository;
    @Mock private RsvpRepository rsvpRepository;
    @InjectMocks private VenueService venueService;

    private User owner;
    private Venue venue;
    private UUID ownerId, venueId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        venueId = UUID.randomUUID();
        owner = User.builder().username("owner").email("o@t.com").passwordHash("h").build();
        owner.setId(ownerId);
        Address address = Address.builder().street("s").city("Bucharest").country("RO").build();
        venue = Venue.builder().name("Test Club").type(Venue.VenueType.CLUB)
                .address(address).owner(owner).averageRating(0.0).totalRatings(0).build();
        venue.setId(venueId);
    }

    @Test @DisplayName("createVenue: returns response when owner exists")
    void createVenue_whenOwnerExists_returnsResponse() {
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(venueRepository.save(any())).willReturn(venue);
        CreateVenueRequest req = CreateVenueRequest.builder()
                .name("Test Club").type(Venue.VenueType.CLUB)
                .address(AddressRequest.builder().street("s").city("c").build()).build();

        VenueResponse res = venueService.createVenue(req, ownerId);

        assertThat(res.getName()).isEqualTo("Test Club");
        then(venueRepository).should(times(1)).save(any());
    }

    @Test @DisplayName("createVenue: throws when owner not found")
    void createVenue_whenOwnerNotFound_throws() {
        given(userRepository.findById(ownerId)).willReturn(Optional.empty());
        CreateVenueRequest req = CreateVenueRequest.builder().name("x").type(Venue.VenueType.CLUB)
                .address(AddressRequest.builder().street("s").city("c").build()).build();

        assertThatThrownBy(() -> venueService.createVenue(req, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);
        then(venueRepository).should(never()).save(any());
    }

    @Test @DisplayName("getVenueById: returns response when exists")
    void getVenueById_whenExists_returnsResponse() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        assertThat(venueService.getVenueById(venueId).getId()).isEqualTo(venueId);
    }

    @Test @DisplayName("getVenueById: throws when not found")
    void getVenueById_whenNotFound_throws() {
        given(venueRepository.findById(venueId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> venueService.getVenueById(venueId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("deleteVenue: deletes when requester is owner")
    void deleteVenue_whenOwner_deletes() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        venueService.deleteVenue(venueId, ownerId, false);
        then(venueRepository).should().delete(venue);
    }

    @Test @DisplayName("deleteVenue: deletes when requester is admin")
    void deleteVenue_whenAdmin_deletes() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        venueService.deleteVenue(venueId, UUID.randomUUID(), true);
        then(venueRepository).should().delete(venue);
    }

    @Test @DisplayName("deleteVenue: throws when non-owner non-admin tries")
    void deleteVenue_whenIntruder_throws() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        assertThatThrownBy(() -> venueService.deleteVenue(venueId, UUID.randomUUID(), false))
                .isInstanceOf(UnauthorizedException.class);
        then(venueRepository).should(never()).delete(any());
    }

    @Test @DisplayName("addRating: creates rating and updates venue average")
    void addRating_createsAndUpdatesAverage() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
        given(ratingRepository.findByUserIdAndVenueId(ownerId, venueId)).willReturn(Optional.empty());
        VenueRating saved = VenueRating.builder().user(owner).venue(venue).score(5).build();
        given(ratingRepository.save(any())).willReturn(saved);
        given(venueRepository.save(any())).willReturn(venue);

        RatingResponse res = venueService.addRating(venueId, ownerId,
                CreateRatingRequest.builder().score(5).comment("Great!").build());

        assertThat(res.getScore()).isEqualTo(5);
        then(venueRepository).should().save(venue);
    }

    @Test @DisplayName("getRankedVenues: returns paginated summaries")
    void getRankedVenues_returnsPaginatedSummaries() {
        User u = User.builder().username("u").email("u@t.com").passwordHash("h").build();
        u.setId(ownerId);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(u));
        given(venueRepository.findRankedVenues(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(venue)));

        PageResponse<VenueSummary> res =
                venueService.getRankedVenues(ownerId, LocalDate.now(), PageRequest.of(0, 10));

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getName()).isEqualTo("Test Club");
    }
}

@ExtendWith(MockitoExtension.class)
class NightServiceTest {

    @Mock private NightRepository nightRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private TagRepository tagRepository;
    @InjectMocks private NightService nightService;

    private User owner;
    private Venue venue;
    private Night night;
    private UUID ownerId, venueId, nightId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID(); venueId = UUID.randomUUID(); nightId = UUID.randomUUID();
        owner = User.builder().username("owner").email("o@t.com").passwordHash("h").build();
        owner.setId(ownerId);
        Venue v = Venue.builder().name("Club").type(Venue.VenueType.CLUB)
                .address(Address.builder().street("s").city("c").build())
                .owner(owner).averageRating(0.0).totalRatings(0).build();
        v.setId(venueId);
        venue = v;
        night = Night.builder().venue(venue).title("Test Night")
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(22,0)).active(true).build();
        night.setId(nightId);
    }

    @Test @DisplayName("createNight: creates with tags when owner calls")
    void createNight_whenOwner_creates() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        given(nightRepository.save(any())).willReturn(night);
        given(tagRepository.findByNameIgnoreCase("house")).willReturn(
                Optional.of(com.nightout.domain.Tag.builder().name("house").build()));

        NightResponse res = nightService.createNight(venueId, CreateNightRequest.builder()
                .title("Test Night").date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(22,0)).tags(List.of("house")).build(), ownerId);

        assertThat(res.getTitle()).isEqualTo("Test Night");
        then(nightRepository).should().save(any());
    }

    @Test @DisplayName("createNight: throws when not owner")
    void createNight_whenNotOwner_throws() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        assertThatThrownBy(() -> nightService.createNight(venueId, CreateNightRequest.builder()
                        .title("x").date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(22,0)).build(),
                UUID.randomUUID())).isInstanceOf(UnauthorizedException.class);
        then(nightRepository).should(never()).save(any());
    }

    @Test @DisplayName("createNight: creates new tag when it does not exist")
    void createNight_whenTagMissing_createsNewTag() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        given(nightRepository.save(any())).willReturn(night);
        given(tagRepository.findByNameIgnoreCase("afro")).willReturn(Optional.empty());
        given(tagRepository.save(any())).willReturn(com.nightout.domain.Tag.builder().name("afro").build());

        nightService.createNight(venueId, CreateNightRequest.builder()
                .title("T").date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(22,0)).tags(List.of("afro")).build(), ownerId);

        then(tagRepository).should().save(any(com.nightout.domain.Tag.class));
    }

    @Test @DisplayName("getNightById: returns response when exists")
    void getNightById_whenExists_returnsResponse() {
        given(nightRepository.findById(nightId)).willReturn(Optional.of(night));
        assertThat(nightService.getNightById(nightId, null).getTitle()).isEqualTo("Test Night");
    }

    @Test @DisplayName("deleteNight: throws when not owner")
    void deleteNight_whenNotOwner_throws() {
        given(nightRepository.findById(nightId)).willReturn(Optional.of(night));
        assertThatThrownBy(() -> nightService.deleteNight(nightId, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class);
        then(nightRepository).should(never()).delete(any());
    }

    @Test
    void getNightsForVenue_returnsPage() {
        Night n1 = Night.builder()
                .title("A")
                .venue(venue)
                .tags(Set.of())
                .build();

        given(nightRepository.findByVenueIdOrderByDateDesc(eq(venueId), any()))
                .willReturn(new PageImpl<>(List.of(n1)));

        PageResponse<NightSummary> res =
                nightService.getNightsForVenue(venueId, PageRequest.of(0, 10));

        assertThat(res.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("createNight: works when tags are null")
    void createNight_whenTagsNull_stillCreatesNight() {
        given(venueRepository.findById(venueId)).willReturn(Optional.of(venue));
        given(nightRepository.save(any())).willReturn(night);

        CreateNightRequest req = CreateNightRequest.builder()
                .title("T")
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(22,0))
                .tags(null)
                .build();

        nightService.createNight(venueId, req, ownerId);

        then(tagRepository).should(never()).findByNameIgnoreCase(any());
    }
}

@ExtendWith(MockitoExtension.class)
class RsvpServiceTest {

    @Mock private RsvpRepository rsvpRepository;
    @Mock private NightRepository nightRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @InjectMocks private RsvpService rsvpService;

    private User user;
    private Night night;
    private UUID userId, nightId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID(); nightId = UUID.randomUUID();
        user = User.builder().username("alice").email("alice@t.com").passwordHash("h").build();
        user.setId(userId);
        User owner = User.builder().username("o").email("ow@t.com").passwordHash("h").build();
        Venue venue = Venue.builder().name("Club").type(Venue.VenueType.CLUB)
                .address(Address.builder().street("s").city("c").build())
                .owner(owner).averageRating(0.0).totalRatings(0).build();
        night = Night.builder().venue(venue).title("Night")
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(22,0))
                .tableCapacity(50).active(true).build();
        night.setId(nightId);
    }

    @Test @DisplayName("createRsvp GOING: saves and notifies")
    void createRsvp_going_savesAndNotifies() {
        given(nightRepository.findById(nightId)).willReturn(Optional.of(night));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(rsvpRepository.findByUserIdAndNightId(userId, nightId)).willReturn(Optional.empty());
        Rsvp saved = Rsvp.builder().user(user).night(night).status(Rsvp.RsvpStatus.GOING).build();
        given(rsvpRepository.save(any())).willReturn(saved);

        RsvpResponse res = rsvpService.createOrUpdateRsvp(nightId, userId,
                CreateRsvpRequest.builder().status(Rsvp.RsvpStatus.GOING).tableSize(2).build());

        assertThat(res.getStatus()).isEqualTo("GOING");
        then(notificationService).should().sendRsvpConfirmation(user, night);
    }

    @Test @DisplayName("createRsvp: throws for past night")
    void createRsvp_pastNight_throws() {
        night.setDate(LocalDate.now().minusDays(1));
        given(nightRepository.findById(nightId)).willReturn(Optional.of(night));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> rsvpService.createOrUpdateRsvp(nightId, userId,
                CreateRsvpRequest.builder().status(Rsvp.RsvpStatus.GOING).build()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("past");
        then(rsvpRepository).should(never()).save(any());
    }

    @Test @DisplayName("createRsvp INTERESTED: saves without notification")
    void createRsvp_interested_noNotification() {
        given(nightRepository.findById(nightId)).willReturn(Optional.of(night));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(rsvpRepository.findByUserIdAndNightId(userId, nightId)).willReturn(Optional.empty());
        Rsvp saved = Rsvp.builder().user(user).night(night).status(Rsvp.RsvpStatus.INTERESTED).build();
        given(rsvpRepository.save(any())).willReturn(saved);

        rsvpService.createOrUpdateRsvp(nightId, userId,
                CreateRsvpRequest.builder().status(Rsvp.RsvpStatus.INTERESTED).build());

        then(notificationService).should(never()).sendRsvpConfirmation(any(), any());
    }

    @Test @DisplayName("createRsvp: updates existing RSVP instead of inserting duplicate")
    void createRsvp_existingRsvp_updates() {
        Rsvp existing = Rsvp.builder().user(user).night(night)
                .status(Rsvp.RsvpStatus.INTERESTED).build();
        given(nightRepository.findById(nightId)).willReturn(Optional.of(night));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(rsvpRepository.findByUserIdAndNightId(userId, nightId))
                .willReturn(Optional.of(existing));
        given(rsvpRepository.save(any())).willReturn(existing);

        rsvpService.createOrUpdateRsvp(nightId, userId,
                CreateRsvpRequest.builder().status(Rsvp.RsvpStatus.GOING).build());

        then(rsvpRepository).should(times(1)).save(any());
        assertThat(existing.getStatus()).isEqualTo(Rsvp.RsvpStatus.GOING);
    }
}