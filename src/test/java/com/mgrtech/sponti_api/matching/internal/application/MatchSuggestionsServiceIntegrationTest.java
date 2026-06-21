package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.FixedClockTestConfiguration;
import com.mgrtech.sponti_api.FullIntegrationTest;
import com.mgrtech.sponti_api.availability.internal.application.AvailabilityFacade;
import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.internal.application.view.AvailabilityRuleView;
import com.mgrtech.sponti_api.contact.internal.application.ContactFacade;
import com.mgrtech.sponti_api.contact.internal.application.command.EditContactCommand;
import com.mgrtech.sponti_api.contact.internal.application.command.SendContactInvitationCommand;
import com.mgrtech.sponti_api.matching.api.MatchInvitationView;
import com.mgrtech.sponti_api.matching.api.MatchingQuery;
import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingProperties;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalStatus;
import com.mgrtech.sponti_api.matching.internal.exception.*;
import com.mgrtech.sponti_api.matching.internal.repository.MatchProposalRepository;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.view.CreatedUserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FullIntegrationTest
@Import(FixedClockTestConfiguration.class)
public class MatchSuggestionsServiceIntegrationTest {

    @Autowired
    MatchingQuery matchingQuery;

    @Autowired
    MatchingFacade matchingFacade;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    AvailabilityFacade availabilityFacade;

    @Autowired
    ContactFacade contactFacade;

    @Autowired
    MatchingProperties properties;

    @Autowired
    MatchProposalRepository matchProposalRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private int phoneNumberSequence;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
        phoneNumberSequence = 1;
    }

    @Test
    void get_empty_match_suggestions_when_user_is_not_found() {
        var suggestions = matchingQuery.getSuggestions(1L);
        assertThat(suggestions).isEmpty();
    }

    @Test
    void get_match_suggestions_when_users_do_not_have_penalties() {
        var initiator = createUser("initiator", "Initiator");
        var initiatorAvailability = createChatAvailability(initiator.id(), LocalTime.of(10, 30));
        var candidate = createUser("candidate", "Candidate");
        createChatAvailability(candidate.id(), LocalTime.of(11, 0));
        createAcceptedContact(initiator.id(), candidate, true);

        var suggestions = matchingQuery.getSuggestions(initiator.id());

        var expectedScore = Duration.between(initiatorAvailability.startTime(), initiatorAvailability.endTime()).toMinutes()
                + properties.scoring().favoriteBoost()
                + properties.scoring().freeNowBoost();

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions).satisfiesExactly(
                match -> {
                    assertThat(match.candidateUserId()).isEqualTo(candidate.id());
                    assertThat(match.score()).isEqualTo(expectedScore);
                });

    }

    @Test
    void get_match_suggestions_when_users_are_contacts_favorite_and_non_blocking_penalties_apply() {
        var initiator = createUser("initiator", "Initiator");
        createChatAvailability(initiator.id(), LocalTime.of(13, 0));
        var candidate = createUser("candidate", "Candidate");
        createChatAvailability(candidate.id(), LocalTime.of(13, 0));
        createAcceptedContact(initiator.id(), candidate, true);

        putUserInsideQuietHours(initiator.id());
        recordRespondedSuggestion(
                candidate.id(),
                initiator.id(),
                MatchProposalStatus.DECLINED,
                Instant.parse("2026-03-30T08:30:00Z")
        );

        var suggestions = matchingQuery.getSuggestions(initiator.id());

        var expectedScore = Duration.ofHours(4).toMinutes()
                + properties.scoring().favoriteBoost()
                + properties.scoring().freeNowBoost()
                - properties.scoring().quietHoursPenalty()
                - properties.scoring().recentDeclinePenalty()
                - properties.scoring().recentSuggestionPenalty();

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions).satisfiesExactly(
                match -> {
                    assertThat(match.candidateUserId()).isEqualTo(candidate.id());
                    assertThat(match.favorite()).isTrue();
                    assertThat(match.channelType()).isEqualTo(ChannelType.CHAT);
                    assertThat(match.overlapStart()).isEqualTo(Instant.parse("2026-03-30T09:00:00Z"));
                    assertThat(match.overlapEnd()).isEqualTo(Instant.parse("2026-03-30T13:00:00Z"));
                    assertThat(match.score()).isEqualTo(expectedScore);
                });
    }

    @Test
    void get_match_suggestions_returns_top_three_contacts_ordered_by_score() {
        var initiator = createUser("top-three-initiator", "Top Three Initiator");
        createChatAvailability(initiator.id(), LocalTime.of(13, 0));

        var favorite = createUser("top-three-favorite", "Top Three Favorite");
        var second = createUser("top-three-second", "Top Three Second");
        var third = createUser("top-three-third", "Top Three Third");
        var fourth = createUser("top-three-fourth", "Top Three Fourth");

        createAcceptedContact(initiator.id(), favorite, true);
        createAcceptedContact(initiator.id(), second, false);
        createAcceptedContact(initiator.id(), third, false);
        createAcceptedContact(initiator.id(), fourth, false);

        createChatAvailability(favorite.id(), LocalTime.of(10, 45));
        createChatAvailability(second.id(), LocalTime.of(10, 30));
        createChatAvailability(third.id(), LocalTime.of(10, 0));
        createChatAvailability(fourth.id(), LocalTime.of(9, 30));

        var suggestions = matchingQuery.getSuggestions(initiator.id());

        var freeNowBoost = properties.scoring().freeNowBoost();
        var favoriteBoost = properties.scoring().favoriteBoost();

        assertThat(suggestions).hasSize(3);
        assertThat(suggestions)
                .extracting(SuggestedMatchView::candidateUserId)
                .containsExactly(favorite.id(), second.id(), third.id());
        assertThat(suggestions)
                .extracting(SuggestedMatchView::score)
                .containsExactly(
                        105 + favoriteBoost + freeNowBoost,
                        90 + freeNowBoost,
                        60 + freeNowBoost
                );
        assertThat(suggestions)
                .extracting(SuggestedMatchView::favorite)
                .containsExactly(true, false, false);
        assertThat(suggestions)
                .noneMatch(match -> match.candidateUserId().equals(fourth.id()));
    }

    @Test
    void create_match_persists_proposed_match_with_recomputed_score_and_overlap() {
        var initiator = createUser("create-match-initiator", "Create Match Initiator");
        createChatAvailability(initiator.id(), LocalTime.of(10, 0));
        var candidate = createUser("create-match-candidate", "Create Match Candidate");
        createChatAvailability(candidate.id(), LocalTime.of(10, 0));
        createAcceptedContact(initiator.id(), candidate, true);

        var match = matchingFacade.createMatch(
                initiator.id(),
                new CreateMatchCommand(candidate.id(), ChannelType.CHAT)
        );

        var expectedScore = Duration.ofHours(1).toMinutes()
                + properties.scoring().favoriteBoost()
                + properties.scoring().freeNowBoost();

        assertThat(match.id()).isNotNull();
        assertThat(match.candidateUserId()).isEqualTo(candidate.id());
        assertThat(match.channelType()).isEqualTo(ChannelType.CHAT);
        assertThat(match.status()).isEqualTo(MatchProposalStatus.PROPOSED.name());
        assertThat(match.score()).isEqualTo(expectedScore);
        assertThat(match.overlapStart()).isEqualTo(Instant.parse("2026-03-30T09:00:00Z"));
        assertThat(match.overlapEnd()).isEqualTo(Instant.parse("2026-03-30T10:00:00Z"));
        assertThat(match.createdAt()).isNotNull();

        assertThat(matchProposalRepository.findById(match.id()))
                .hasValueSatisfying(entity -> {
                    assertThat(entity.getInitiatorUserId()).isEqualTo(initiator.id());
                    assertThat(entity.getCandidateUserId()).isEqualTo(candidate.id());
                    assertThat(entity.getChannelType()).isEqualTo(ChannelType.CHAT);
                    assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED);
                    assertThat(entity.getScore()).isEqualTo(expectedScore);
                    assertThat(entity.getOverlapStart()).isEqualTo(Instant.parse("2026-03-30T09:00:00Z"));
                    assertThat(entity.getOverlapEnd()).isEqualTo(Instant.parse("2026-03-30T10:00:00Z"));
                });
    }

    @Test
    void create_match_fails_when_candidate_is_not_accepted_contact() {
        var initiator = createUser("not-contact-initiator", "Not Contact Initiator");
        var candidate = createUser("not-contact-candidate", "Not Contact Candidate");

        assertThatThrownBy(() -> matchingFacade.createMatch(
                initiator.id(),
                new CreateMatchCommand(candidate.id(), ChannelType.CHAT)
        )).isInstanceOf(AcceptedContactNotFoundException.class);

        assertThat(matchProposalRepository.findAll()).isEmpty();
    }

    @Test
    void create_match_fails_when_initiator_has_no_phone_number() {
        var initiator = createUserWithoutPhoneNumber("no-phone-initiator", "No Phone Initiator");
        var candidate = createUser("no-phone-candidate", "No Phone Candidate");
        createAcceptedContact(initiator.id(), candidate, false);

        assertThatThrownBy(() -> matchingFacade.createMatch(
                initiator.id(),
                new CreateMatchCommand(candidate.id(), ChannelType.CHAT)
        )).isInstanceOf(PhoneNumberRequiredException.class)
          .hasMessage("Phone number required for creating a match.");

        assertThat(matchProposalRepository.findAll()).isEmpty();
    }

    @Test
    void create_match_fails_when_active_proposal_already_exists() {
        var initiator = createUser("duplicate-match-initiator", "Duplicate Match Initiator");
        var candidate = createUser("duplicate-match-candidate", "Duplicate Match Candidate");
        createAcceptedContact(initiator.id(), candidate, false);
        matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        assertThatThrownBy(() -> matchingFacade.createMatch(
                initiator.id(),
                new CreateMatchCommand(candidate.id(), ChannelType.CHAT)
        )).isInstanceOf(MatchAlreadyExistsException.class);

        assertThat(matchProposalRepository.findAll()).hasSize(1);
    }

    @Test
    void create_match_fails_when_requested_channel_is_not_allowed() {
        var initiator = createUser("channel-not-allowed-initiator", "Channel Not Allowed Initiator");
        var candidate = createUser("channel-not-allowed-candidate", "Channel Not Allowed Candidate");
        createAcceptedContact(initiator.id(), candidate, false);
        setChannelPreferences(initiator.id(), false, true);

        assertThatThrownBy(() -> matchingFacade.createMatch(
                initiator.id(),
                new CreateMatchCommand(candidate.id(), ChannelType.CHAT)
        )).isInstanceOf(ChannelNotAllowedException.class);

        assertThat(matchProposalRepository.findAll()).isEmpty();
    }

    @Test
    void create_match_fails_when_no_overlap_exists_for_requested_channel() {
        var initiator = createUser("no-overlap-initiator", "No Overlap Initiator");
        createChatAvailability(initiator.id(), LocalTime.of(10, 0));
        var candidate = createUser("no-overlap-candidate", "No Overlap Candidate");
        createChatAvailability(candidate.id(), LocalTime.of(13, 0), LocalTime.of(14, 0));
        createAcceptedContact(initiator.id(), candidate, false);

        assertThatThrownBy(() -> matchingFacade.createMatch(
                initiator.id(),
                new CreateMatchCommand(candidate.id(), ChannelType.CHAT)
        )).isInstanceOf(AvailabilityOverlapNotFoundException.class);

        assertThat(matchProposalRepository.findAll()).isEmpty();
    }

    @Test
    void accept_match_marks_proposed_match_as_accepted() {
        var initiator = createUser("accept-match-initiator", "Accept Match Initiator");
        var candidate = createUser("accept-match-candidate", "Accept Match Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        var match = matchingFacade.acceptMatch(candidate.id(), suggestion.getId());

        assertThat(match.id()).isEqualTo(suggestion.getId());
        assertThat(match.candidateUserId()).isEqualTo(candidate.id());
        assertThat(match.channelType()).isEqualTo(ChannelType.CHAT);
        assertThat(match.status()).isEqualTo(MatchProposalStatus.ACCEPTED.name());
        assertThat(match.score()).isEqualTo(100);
        assertThat(match.overlapStart()).isEqualTo(Instant.parse("2026-03-30T09:00:00Z"));
        assertThat(match.overlapEnd()).isEqualTo(Instant.parse("2026-03-30T10:00:00Z"));

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.ACCEPTED));
    }

    @Test
    void accept_match_fails_when_candidate_has_no_phone_number() {
        var initiator = createUser("accept-no-phone-initiator", "Accept No Phone Initiator");
        var candidate = createUserWithoutPhoneNumber("accept-no-phone-candidate", "Accept No Phone Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        assertThatThrownBy(() -> matchingFacade.acceptMatch(candidate.id(), suggestion.getId()))
                .isInstanceOf(PhoneNumberRequiredException.class)
                .hasMessage("Phone number required for accepting a match.");

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void accept_match_fails_when_match_does_not_belong_to_candidate() {
        var owner = createUser("accept-not-found-owner", "Accept Not Found Owner");
        var other = createUser("accept-not-found-other", "Accept Not Found Other");
        var candidate = createUser("accept-not-found-candidate", "Accept Not Found Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                owner.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        assertThatThrownBy(() -> matchingFacade.acceptMatch(other.id(), suggestion.getId()))
                .isInstanceOf(MatchNotFoundException.class);

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void accept_match_fails_when_match_is_expired() {
        var initiator = createUser("accept-expired-initiator", "Accept Expired Initiator");
        var candidate = createUser("accept-expired-candidate", "Accept Expired Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z"),
                Instant.parse("2026-03-30T08:59:59Z")
        ));

        assertThatThrownBy(() -> matchingFacade.acceptMatch(candidate.id(), suggestion.getId()))
                .isInstanceOf(MatchProposalExpiredException.class)
                .hasMessage("Match proposal has expired");

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void decline_match_marks_proposed_match_as_declined() {
        var initiator = createUser("decline-match-initiator", "Decline Match Initiator");
        var candidate = createUser("decline-match-candidate", "Decline Match Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CALL,
                80,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T09:45:00Z")
        ));

        var match = matchingFacade.declineMatch(candidate.id(), suggestion.getId());

        assertThat(match.id()).isEqualTo(suggestion.getId());
        assertThat(match.candidateUserId()).isEqualTo(candidate.id());
        assertThat(match.channelType()).isEqualTo(ChannelType.CALL);
        assertThat(match.status()).isEqualTo(MatchProposalStatus.DECLINED.name());
        assertThat(match.score()).isEqualTo(80);
        assertThat(match.overlapStart()).isEqualTo(Instant.parse("2026-03-30T09:00:00Z"));
        assertThat(match.overlapEnd()).isEqualTo(Instant.parse("2026-03-30T09:45:00Z"));

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.DECLINED));
    }

    @Test
    void decline_match_fails_when_match_does_not_belong_to_candidate() {
        var owner = createUser("decline-not-found-owner", "Decline Not Found Owner");
        var other = createUser("decline-not-found-other", "Decline Not Found Other");
        var candidate = createUser("decline-not-found-candidate", "Decline Not Found Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                owner.id(),
                candidate.id(),
                ChannelType.CALL,
                80,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T09:45:00Z")
        ));

        assertThatThrownBy(() -> matchingFacade.declineMatch(other.id(), suggestion.getId()))
                .isInstanceOf(MatchNotFoundException.class);

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void decline_match_fails_when_match_is_expired() {
        var initiator = createUser("decline-expired-initiator", "Decline Expired Initiator");
        var candidate = createUser("decline-expired-candidate", "Decline Expired Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CALL,
                80,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T09:45:00Z"),
                Instant.parse("2026-03-30T08:59:59Z")
        ));

        assertThatThrownBy(() -> matchingFacade.declineMatch(candidate.id(), suggestion.getId()))
                .isInstanceOf(MatchProposalExpiredException.class)
                .hasMessage("Match proposal has expired");

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void accept_match_fails_when_initiator_tries_to_accept_own_outgoing_match() {
        var initiator = createUser("accept-outgoing-initiator", "Accept Outgoing Initiator");
        var candidate = createUser("accept-outgoing-candidate", "Accept Outgoing Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        assertThatThrownBy(() -> matchingFacade.acceptMatch(initiator.id(), suggestion.getId()))
                .isInstanceOf(MatchNotFoundException.class);

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void decline_match_fails_when_initiator_tries_to_decline_own_outgoing_match() {
        var initiator = createUser("decline-outgoing-initiator", "Decline Outgoing Initiator");
        var candidate = createUser("decline-outgoing-candidate", "Decline Outgoing Candidate");
        var suggestion = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        assertThatThrownBy(() -> matchingFacade.declineMatch(initiator.id(), suggestion.getId()))
                .isInstanceOf(MatchNotFoundException.class);

        assertThat(matchProposalRepository.findById(suggestion.getId()))
                .hasValueSatisfying(entity -> assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED));
    }

    @Test
    void match_proposal_creation_fails_if_same_pair_already_exists() {
        var candidate = createUser("incoming-candidate", "Incoming Candidate");
        var initiator = createUser("incoming-initiator", "Incoming Initiator");
        createChatAvailability(candidate.id(), LocalTime.of(11, 0));
        createChatAvailability(initiator.id(), LocalTime.of(11, 0));
        createAcceptedContact(initiator.id(), candidate, true);

        var proposal1 = matchingFacade.createMatch(initiator.id(), new CreateMatchCommand(candidate.id(), ChannelType.CHAT));

        assertThat(matchProposalRepository.findById(proposal1.id()).isPresent()).isTrue();
        assertThat(matchProposalRepository.findById(proposal1.id()).get().getStatus()).isEqualTo(MatchProposalStatus.PROPOSED);

        assertThatThrownBy(() -> matchingFacade.createMatch(
                candidate.id(), new CreateMatchCommand(initiator.id(), ChannelType.CHAT)
        )).isInstanceOf(MatchAlreadyExistsException.class)
          .hasMessage("An active or accepted match already exists for this pair.");
    }

    @Test
    void get_incoming_matches_returns_only_active_proposed_matches_for_candidate() {
        var candidate = createUser("incoming-candidate", "Incoming Candidate");
        var candidate2 = createUser("incoming-candidate2", "Incoming Candidate2");
        var initiator = createUser("incoming-initiator", "Incoming Initiator");
        var otherInitiator = createUser("incoming-other-initiator", "Incoming Other Initiator");
        var outgoingCandidate = createUser("incoming-outgoing-candidate", "Incoming Outgoing Candidate");

       var incoming = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate.id(),
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));
        var secondIncoming = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                otherInitiator.id(),
                candidate.id(),
                ChannelType.CALL,
                80,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T09:45:00Z")
        ));
        matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                candidate.id(),
                outgoingCandidate.id(),
                ChannelType.CHAT,
                70,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));
        var accepted = matchProposalRepository.saveAndFlush(new MatchProposalEntity(
                initiator.id(),
                candidate2.id(),
                ChannelType.CHAT,
                60,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));
        accepted.acceptBy(candidate2.id());
        matchProposalRepository.saveAndFlush(accepted);

        var invitations = matchingFacade.getIncomingMatches(candidate.id());

        assertThat(invitations)
                .extracting(MatchInvitationView::id)
                .containsExactlyInAnyOrder(secondIncoming.getId(), incoming.getId());
        assertThat(invitations)
                .allSatisfy(match -> assertThat(match.status()).isEqualTo(MatchProposalStatus.PROPOSED.name()));
    }

    private CreatedUserView createUser(String emailPrefix, String displayName) {
        return createUser(emailPrefix, displayName, nextPhoneNumber());
    }

    private CreatedUserView createUserWithoutPhoneNumber(String emailPrefix, String displayName) {
        return createUser(emailPrefix, displayName, null);
    }

    private CreatedUserView createUser(String emailPrefix, String displayName, String phoneNumber) {
        return userRegistrationFacade.createUser(
                new CreateUserCommand(
                        emailPrefix + "@example.com",
                        "hash",
                        displayName,
                        phoneNumber,
                        "UTC"
                )
        );
    }

    private String nextPhoneNumber() {
        return "+32470" + String.format("%06d", phoneNumberSequence++);
    }

    private void createAcceptedContact(Long initiatorId, CreatedUserView contact, boolean favorite) {
        var invitation = contactFacade.sendInvitation(
                initiatorId,
                new SendContactInvitationCommand(contact.email(), contact.displayName())
        );
        contactFacade.acceptInvitation(contact.id(), invitation.id());

        if (favorite) {
            contactFacade.editContact(
                    initiatorId,
                    contact.id(),
                    new EditContactCommand(contact.displayName(), true)
            );
        }
    }

    private AvailabilityRuleView createChatAvailability(Long userId, LocalTime endTime) {
        return createChatAvailability(userId, LocalTime.of(9, 0), endTime);
    }

    private AvailabilityRuleView createChatAvailability(Long userId, LocalTime startTime, LocalTime endTime) {
        return availabilityFacade.createRule(
                userId,
                new CreateAvailabilityRuleCommand(
                        DayOfWeek.MONDAY,
                        startTime,
                        endTime,
                        ChannelType.CHAT
                )
        );
    }

    private void setChannelPreferences(Long userId, boolean allowChat, boolean allowCall) {
        jdbcTemplate.update(
                """
                UPDATE user_preferences
                SET allow_chat = ?, allow_call = ?
                WHERE user_id = ?
                """,
                allowChat,
                allowCall,
                userId
        );
    }

    private void putUserInsideQuietHours(Long userId) {
        jdbcTemplate.update(
                """
                UPDATE user_preferences
                SET quiet_hours_start = ?, quiet_hours_end = ?
                WHERE user_id = ?
                """,
                LocalTime.of(8, 0),
                LocalTime.of(10, 0),
                userId
        );
    }

    private void recordRespondedSuggestion(
            Long initiatorUserId,
            Long candidateUserId,
            MatchProposalStatus status,
            Instant timestamp
    ) {
        var suggestion = new MatchProposalEntity(
                initiatorUserId,
                candidateUserId,
                ChannelType.CHAT,
                100,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T13:00:00Z")
        );

        if (status == MatchProposalStatus.ACCEPTED) {
            suggestion.acceptBy(candidateUserId);
        } else if (status == MatchProposalStatus.DECLINED) {
            suggestion.declineBy(candidateUserId);
        } else {
            throw new IllegalArgumentException("Only responded statuses are supported");
        }

        var persisted = matchProposalRepository.saveAndFlush(suggestion);
        jdbcTemplate.update(
                """
                UPDATE match_proposals
                SET created_at = ?, responded_at = ?
                WHERE id = ?
                """,
                Timestamp.from(timestamp),
                Timestamp.from(timestamp),
                persisted.getId()
        );
    }
}
