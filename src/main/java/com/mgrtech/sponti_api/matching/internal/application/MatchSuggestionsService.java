package com.mgrtech.sponti_api.matching.internal.application;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.mgrtech.sponti_api.availability.api.query.EffectiveAvailabilityQuery;
import com.mgrtech.sponti_api.availability.api.view.EffectiveAvailabilityView;
import com.mgrtech.sponti_api.contact.api.query.ContactQuery;
import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.matching.api.ContactLinkType;
import com.mgrtech.sponti_api.matching.api.ContactLinkView;
import com.mgrtech.sponti_api.matching.api.MatchInvitationView;
import com.mgrtech.sponti_api.matching.api.MatchView;
import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.matching.api.event.MatchProposalAcceptedEvent;
import com.mgrtech.sponti_api.matching.api.event.MatchProposalCreatedEvent;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingProperties;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalStatus;
import com.mgrtech.sponti_api.matching.internal.exception.*;
import com.mgrtech.sponti_api.matching.internal.repository.MatchProposalRepository;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import com.mgrtech.sponti_api.user.api.query.UserContactInfoQuery;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.mgrtech.sponti_api.matching.api.MatchView.toMatchView;

@Service
@AllArgsConstructor
public class MatchSuggestionsService implements MatchingFacade {

    private static final Logger log = LoggerFactory.getLogger(MatchSuggestionsService.class);

    private static final int MAX_SUGGESTIONS = 3;
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("UTC");
    private static final String MATCH_ALREADY_EXISTS_MESSAGE = "An active or accepted match already exists for this pair.";
    private static final PhoneNumberUtil PHONE_NUMBER_UTIL = PhoneNumberUtil.getInstance();

    private final Clock clock;
    private final MatchingProperties properties;
    private final EffectiveAvailabilityQuery effectiveAvailabilityQuery;
    private final ContactQuery contactQuery;
    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery;
    private final UserContactInfoQuery userContactInfoQuery;
    private final MatchProposalRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final OperationalMetrics metrics;

    @Override
    @Transactional
    public MatchView createMatch(Long userId, CreateMatchCommand command) {
        var now = Instant.now(clock);
        var to = now.plus(properties.searchWindow());
        var candidateUserId = command.candidateUserId();
        var requestedChannel = command.channelType();

        log.info("Creating match between initiatorId={} and candidateId={}", userId, candidateUserId);

        if(!userContactInfoQuery.hasPhoneNumber(userId)) {
            throw new PhoneNumberRequiredException("Phone number required for creating a match.");
        }

        var contact = contactQuery.findAcceptedContact(userId, candidateUserId)
                .orElseThrow(() -> new AcceptedContactNotFoundException("Candidate is not an accepted contact."));

        if(hasBlockingProposal(userId, candidateUserId, now)) {
            throw new MatchAlreadyExistsException(MATCH_ALREADY_EXISTS_MESSAGE);
        }

        var userPreferences = userMatchingPreferencesQuery.getMatchingPreferences(userId)
                .filter(UserMatchingPreferencesView::matchingEnabled)
                .orElseThrow(() -> new MatchingDisabledException("Matching is disabled for initiator."));

        var candidatePreferences = userMatchingPreferencesQuery.getMatchingPreferences(candidateUserId)
                .filter(UserMatchingPreferencesView::matchingEnabled)
                .orElseThrow(() -> new MatchingDisabledException("Matching is disabled for candidate."));

        if(!isChannelAllowed(requestedChannel, userPreferences)
                || !isChannelAllowed(requestedChannel, candidatePreferences)) {
            throw new ChannelNotAllowedException("Requested channel is not allowed by both users.");
        }

        var userAvailability = effectiveAvailabilityQuery.getChannelEffectiveAvailability(userId, now, to);
        var candidateAvailability = effectiveAvailabilityQuery.getChannelEffectiveAvailability(candidateUserId, now, to);

        var overlap = findBestOverlapForChannel(
                userAvailability,
                candidateAvailability,
                requestedChannel
        ).filter(availabilityOverlap -> availabilityOverlap.duration.compareTo(properties.minimumOverlap()) > 0)
                .orElseThrow(() -> new AvailabilityOverlapNotFoundException("No valid availability overlap for requested channel."));

        var scored = scoreCandidate(
                userId,
                contact,
                userPreferences,
                candidatePreferences,
                overlap,
                now
        );

        if(scored.score() < properties.scoring().minimumScore()) {
            throw new MatchScoreBelowThresholdException("Candidate score is below minimum threshold.");
        }

        try {
            var proposal = createProposal(
                    userId,
                    candidateUserId,
                    requestedChannel,
                    scored,
                    overlap,
                    now
            );

            eventPublisher.publishEvent(MatchProposalCreatedEvent.from(proposal));
            metrics.matchProposalCreated(proposal.getChannelType().name());
            log.info(
                    "Match proposal created: proposalId={} initiatorUserId={} candidateUserId={} channel={} score={}",
                    proposal.getId(),
                    proposal.getInitiatorUserId(),
                    proposal.getCandidateUserId(),
                    proposal.getChannelType(),
                    proposal.getScore()
            );
            return toMatchView(proposal);
        } catch (DataIntegrityViolationException e) {
            throw new MatchAlreadyExistsException(MATCH_ALREADY_EXISTS_MESSAGE);
        }
    }

    @Override
    @Transactional
    public MatchView acceptMatch(Long candidateUserId, Long proposalId) {
        log.info("Candidate user id = {} is accepting the proposal id ={}", candidateUserId, proposalId);
        var now = Instant.now(clock);
        var proposal = repository.findByIdAndCandidateUserId(proposalId, candidateUserId)
                .orElseThrow(
                        () -> new MatchNotFoundException("Match proposal not found")
                );

        proposal.ensureNotExpired(now);

        if(!userContactInfoQuery.hasPhoneNumber(candidateUserId)) {
            throw new PhoneNumberRequiredException("Phone number required for accepting a match.");
        }

        proposal.acceptBy(candidateUserId);
        metrics.matchProposalResponded("accepted");
        eventPublisher.publishEvent(MatchProposalAcceptedEvent.from(proposal));

        log.info("Proposal id = {} accepted by candidate user id = {}", proposal.getId(), candidateUserId);
        return toMatchView(proposal);
    }

    @Override
    @Transactional
    public MatchView declineMatch(Long candidateUserId, Long proposalId) {
        log.info("Candidate user id = {} is declining the proposal id ={}", candidateUserId, proposalId);
        var now = Instant.now(clock);
        var proposal = repository.findByIdAndCandidateUserId(proposalId, candidateUserId)
                .orElseThrow(
                        () -> new MatchNotFoundException("Match proposal not found")
                );
        proposal.ensureNotExpired(now);
        proposal.declineBy(candidateUserId);
        metrics.matchProposalResponded("declined");
        log.info("Proposal id = {} declined by candidate user id = {}", proposal.getId(), candidateUserId);
        return toMatchView(proposal);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactLinkView createContactLink(Long matchId, Long userId) {
        log.info("UserId = {} requested contact link creation for matchId = {}", userId, matchId);

        var match = repository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException("Match not found."));

        match.ensureParticipant(userId);
        match.ensureContactable();
        ensureAcceptedContactRelationship(match);
        ensureBothParticipantsHavePhoneNumbers(match);

        var contactUserId = match.otherParticipantId(userId);
        var phoneNumber = userContactInfoQuery.getPhoneNumber(contactUserId)
                .orElseThrow(() -> new PhoneNumberRequiredException("Phone number required for creating a contact link."));

        metrics.contactLinkCreated(ContactLinkType.WHATSAPP.name());
        log.info("Contact link created by userId = {} for matchId = {}", userId, matchId);

        return new ContactLinkView(
                ContactLinkType.WHATSAPP,
                whatsappUrl(phoneNumber),
                null
        );
    }

    @Override
    @Transactional
    public List<SuggestedMatchView> getSuggestions(Long userId) {
        log.info("Matching suggestions requested for userId={}", userId);
        var now = Instant.now(clock);
        var to = now.plus(properties.searchWindow());

        var userPreferences = userMatchingPreferencesQuery.getMatchingPreferences(userId)
                .filter(UserMatchingPreferencesView::matchingEnabled);

        if (userPreferences.isEmpty()) {
            log.debug("Suggestions not found for userId={}: missing user preferences", userId);
            return List.of();
        }

        var userAvailability = effectiveAvailabilityQuery.getChannelEffectiveAvailability(userId, now, to);

        return contactQuery.getAcceptedContacts(userId)
                .stream()
                .flatMap(contact -> scoreCandidate(userId, contact, userPreferences.get(), userAvailability, now, to).stream())
                .sorted(Comparator.comparingInt(SuggestedMatchView::score).reversed())
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchInvitationView> getAcceptedMatches(Long userId) {
        log.info("Accepted match invitations requested for userId={}", userId);
        return getMatches(userId, MatchProposalStatus.ACCEPTED, true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchInvitationView> getIncomingMatches(Long userId) {
        log.info("Incoming match invitations requested for userId={}", userId);
        return getMatches(userId, MatchProposalStatus.PROPOSED, false, true);
    }

    private List<MatchInvitationView> getMatches(
            Long userId,
            MatchProposalStatus status,
            boolean includeInitiated,
            boolean requireUnexpired
    ) {
        return repository.findVisibleByUserIdAndStatus(
                        userId,
                        status,
                        Instant.now(clock),
                        includeInitiated,
                        requireUnexpired
                )
                .stream()
                .map(MatchInvitationView::toMatchInvitationView)
                .toList();
    }

    private MatchProposalEntity createProposal(
            Long userId,
            Long candidateUserId,
            ChannelType requestedChannel,
            ScoredCandidate scored,
            AvailabilityOverlap overlap,
            Instant now
    ) {
        return repository.save(new MatchProposalEntity(
                userId,
                candidateUserId,
                requestedChannel,
                scored.score(),
                overlap.start(),
                overlap.end(),
                expiresAt(now, overlap.end())
        ));
    }

    private Optional<AvailabilityOverlap> findBestOverlapForChannel(
            List<EffectiveAvailabilityView> userSlots,
            List<EffectiveAvailabilityView> candidateSlots,
            ChannelType requestedChannel
    ) {
        return userSlots.stream()
                .filter(slot -> slotSupportsChannel(slot, requestedChannel))
                .flatMap(userSlot -> candidateSlots.stream()
                        .filter(candidateSlot -> slotSupportsChannel(candidateSlot, requestedChannel))
                        .map(candidateSlot -> overlap(userSlot, candidateSlot, requestedChannel))
                        .flatMap(Optional::stream)
                )
                .max(Comparator.comparing(AvailabilityOverlap::duration));
    }

    private boolean slotSupportsChannel(EffectiveAvailabilityView slot, ChannelType requestedChannel) {
        return slot.channelType() == null || slot.channelType() == requestedChannel;
    }

    private Optional<SuggestedMatchView> scoreCandidate(
            Long userId,
            ContactView contact,
            UserMatchingPreferencesView userPreferences,
            List<EffectiveAvailabilityView> userAvailability,
            Instant now,
            Instant to
    ) {
        var candidateUserId = contact.contactUserId();

        if (hasBlockingProposal(userId, candidateUserId, now)) {
            return Optional.empty();
        }

        var candidatePreferences = userMatchingPreferencesQuery.getMatchingPreferences(candidateUserId);

        if (candidatePreferences.isEmpty() || !candidatePreferences.get().matchingEnabled()) {
            log.debug("Skipped candidateUserId={}: empty candidate preferences or no matching enabled", candidateUserId);
            return Optional.empty();
        }

        var candidateAvailability = effectiveAvailabilityQuery.getChannelEffectiveAvailability(candidateUserId, now, to);

        return findBestOverlap(userAvailability, candidateAvailability, userPreferences, candidatePreferences.get())
                .filter(overlap -> overlap.duration().compareTo(properties.minimumOverlap()) >= 0)
                .map(overlap -> scoreCandidate(userId, contact, userPreferences, candidatePreferences.get(), overlap, now))
                .filter(scoredCandidate -> scoredCandidate.score() >= properties.scoring().minimumScore())
                .map(scoredCandidate -> new SuggestedMatchView(
                        candidateUserId,
                        contact.nickName(),
                        contact.favorite(),
                        scoredCandidate.overlap().channelType(),
                        scoredCandidate.score(),
                        scoredCandidate.overlap().start(),
                        scoredCandidate.overlap().end()
                ));
    }

    private Optional<AvailabilityOverlap> findBestOverlap(
            List<EffectiveAvailabilityView> userSlots,
            List<EffectiveAvailabilityView> candidateSlots,
            UserMatchingPreferencesView userPreferences,
            UserMatchingPreferencesView candidatePreferences
    ) {
        return userSlots.stream()
                .flatMap(userSlot -> candidateSlots.stream()
                        .flatMap(candidateSlot -> matchingChannels(userSlot.channelType(), candidateSlot.channelType()).stream()
                                .filter(channelType -> isChannelAllowed(channelType, userPreferences))
                                .filter(channelType -> isChannelAllowed(channelType, candidatePreferences))
                                .map(channelType -> overlap(userSlot, candidateSlot, channelType))
                                .flatMap(Optional::stream)
                        )
                )
                .max(Comparator.comparing(AvailabilityOverlap::duration));
    }

    private ScoredCandidate scoreCandidate(
            Long userId,
            ContactView contact,
            UserMatchingPreferencesView userPreferences,
            UserMatchingPreferencesView candidatePreferences,
            AvailabilityOverlap overlap,
            Instant now
    ) {
        var scoring = properties.scoring();
        int score = Math.toIntExact(overlap.duration().toMinutes());

        if (contact.favorite()) {
            score += scoring.favoriteBoost();
        }

        if (!overlap.start().isAfter(now)) {
            score += scoring.freeNowBoost();
        }

        if (isInsideQuietHours(userPreferences, now) || isInsideQuietHours(candidatePreferences, now)) {
            score -= scoring.quietHoursPenalty();
        }

        var candidateUserId = contact.contactUserId();

        if (latestRespondedMatch(userId, candidateUserId, MatchProposalStatus.ACCEPTED)
                .map(MatchProposalEntity::getRespondedAt)
                .filter(respondedAt -> isAfter(respondedAt, now.minus(properties.acceptedCooldown())))
                .isPresent()) {
            score -= scoring.recentAcceptedPenalty();
        }

        if (latestRespondedMatch(userId, candidateUserId, MatchProposalStatus.DECLINED)
                .map(MatchProposalEntity::getRespondedAt)
                .filter(respondedAt -> isAfter(respondedAt, now.minus(properties.declineCooldown())))
                .isPresent()) {
            score -= scoring.recentDeclinePenalty();
        }

        if (latestProposal(userId, candidateUserId)
                .map(MatchProposalEntity::getCreatedAt)
                .filter(createdAt -> isAfter(createdAt, now.minus(properties.suggestionCooldown())))
                .isPresent()) {
            score -= scoring.recentSuggestionPenalty();
        }

        log.info("Score of {} has been calculated between initiator user id={} and candidate user id={}", score, userId, candidateUserId);
        return new ScoredCandidate(overlap, score);
    }

    private Optional<AvailabilityOverlap> overlap(
            EffectiveAvailabilityView initiatorAvailability,
            EffectiveAvailabilityView candidateAvailability,
            ChannelType channelType
    ) {
        var start = max(initiatorAvailability.startDateTime(), candidateAvailability.startDateTime());
        var end = min(initiatorAvailability.endDateTime(), candidateAvailability.endDateTime());

        if (!start.isBefore(end)) {
            return Optional.empty();
        }

        return Optional.of(new AvailabilityOverlap(
                start,
                end,
                Duration.between(start, end),
                channelType
        ));
    }

    private List<ChannelType> matchingChannels(ChannelType initiatorChannelType, ChannelType candidateChannelType) {
        if (initiatorChannelType != null && candidateChannelType != null) {
            return initiatorChannelType == candidateChannelType
                    ? List.of(initiatorChannelType)
                    : List.of();
        }

        if (initiatorChannelType != null) {
            return List.of(initiatorChannelType);
        }

        if (candidateChannelType != null) {
            return List.of(candidateChannelType);
        }

        return List.of(ChannelType.CHAT, ChannelType.CALL);
    }

    private Instant max(Instant left, Instant right) {
        return left.isAfter(right) ? left : right;
    }

    private Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private boolean hasBlockingProposal(Long userId, Long candidateUserId, Instant now) {
        return repository.existsBlockingProposalBetweenUsers(
                userId,
                candidateUserId,
                MatchProposalStatus.PROPOSED,
                MatchProposalStatus.ACCEPTED,
                now
        );
    }

    private boolean isChannelAllowed(ChannelType channelType, UserMatchingPreferencesView preferences) {
        return switch (channelType) {
            case CHAT -> preferences.allowChat();
            case CALL -> preferences.allowCall();
        };
    }

    private void ensureBothParticipantsHavePhoneNumbers(MatchProposalEntity match) {
        if (!userContactInfoQuery.hasPhoneNumber(match.getInitiatorUserId())
                || !userContactInfoQuery.hasPhoneNumber(match.getCandidateUserId())) {
            throw new PhoneNumberRequiredException("Both users need valid phone numbers for creating a contact link.");
        }
    }

    private void ensureAcceptedContactRelationship(MatchProposalEntity match) {
        var initiatorUserId = match.getInitiatorUserId();
        var candidateUserId = match.getCandidateUserId();

        if (contactQuery.findAcceptedContact(initiatorUserId, candidateUserId).isEmpty()
                || contactQuery.findAcceptedContact(candidateUserId, initiatorUserId).isEmpty()) {
            throw new AcceptedContactNotFoundException("Users are no longer accepted contacts.");
        }
    }

    private String whatsappUrl(String phoneNumber) {
        return "https://wa.me/" + normalizedE164PhoneNumber(phoneNumber).substring(1);
    }

    private String normalizedE164PhoneNumber(String phoneNumber) {
        try {
            var parsedPhoneNumber = PHONE_NUMBER_UTIL.parse(phoneNumber, null);
            var normalizedPhoneNumber = PHONE_NUMBER_UTIL.format(parsedPhoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);

            if (!PHONE_NUMBER_UTIL.isValidNumber(parsedPhoneNumber) || !phoneNumber.equals(normalizedPhoneNumber)) {
                throw new PhoneNumberRequiredException("Valid phone number required for creating a contact link.");
            }

            return normalizedPhoneNumber;
        } catch (NumberParseException e) {
            throw new PhoneNumberRequiredException("Valid phone number required for creating a contact link.");
        }
    }

    private Instant expiresAt(Instant now, Instant overlapEnd) {
        var ttlExpiration = now.plus(properties.proposalTtl());
        return ttlExpiration.isBefore(overlapEnd) ? ttlExpiration : overlapEnd;
    }

    private Optional<MatchProposalEntity> latestRespondedMatch(
            Long userId,
            Long candidateUserId,
            MatchProposalStatus status
    ) {
        var initiatedByUser = repository.findFirstByInitiatorUserIdAndCandidateUserIdAndStatusOrderByRespondedAtDesc(
                userId,
                candidateUserId,
                status
        );

        var initiatedByCandidate = repository.findFirstByInitiatorUserIdAndCandidateUserIdAndStatusOrderByRespondedAtDesc(
                candidateUserId,
                userId,
                status
        );

        return latestByRespondedAt(initiatedByUser.orElse(null), initiatedByCandidate.orElse(null));
    }

    private Optional<MatchProposalEntity> latestProposal(Long userId, Long candidateUserId) {
        var initiatedByUser = repository.findFirstByInitiatorUserIdAndCandidateUserIdOrderByCreatedAtDesc(
                userId,
                candidateUserId
        );

        var initiatedByCandidate = repository.findFirstByInitiatorUserIdAndCandidateUserIdOrderByCreatedAtDesc(
                candidateUserId,
                userId
        );

        return latestByCreatedAt(initiatedByUser.orElse(null), initiatedByCandidate.orElse(null));
    }

    private Optional<MatchProposalEntity> latestByRespondedAt(
            MatchProposalEntity left,
            MatchProposalEntity right
    ) {
        if (left == null) {
            return Optional.ofNullable(right);
        }

        if (right == null) {
            return Optional.of(left);
        }

        return Optional.of(isAfter(left.getRespondedAt(), right.getRespondedAt()) ? left : right);
    }

    private Optional<MatchProposalEntity> latestByCreatedAt(
            MatchProposalEntity left,
            MatchProposalEntity right
    ) {
        if (left == null) {
            return Optional.ofNullable(right);
        }

        if (right == null) {
            return Optional.of(left);
        }

        return Optional.of(isAfter(left.getCreatedAt(), right.getCreatedAt()) ? left : right);
    }

    private boolean isAfter(Instant value, Instant threshold) {
        return value != null && value.isAfter(threshold);
    }

    private boolean isInsideQuietHours(UserMatchingPreferencesView preferences, Instant now) {
        if (preferences.quietHoursStart() == null || preferences.quietHoursEnd() == null) {
            return false;
        }

        var localTime = LocalTime.ofInstant(now, zoneId(preferences.timezone()));
        var start = preferences.quietHoursStart();
        var end = preferences.quietHoursEnd();

        if (start.equals(end)) {
            return false;
        }

        if (start.isBefore(end)) {
            return !localTime.isBefore(start) && localTime.isBefore(end);
        }

        return !localTime.isBefore(start) || localTime.isBefore(end);
    }

    private ZoneId zoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return FALLBACK_ZONE;
        }

        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            return FALLBACK_ZONE;
        }
    }

    private record AvailabilityOverlap(
            Instant start,
            Instant end,
            Duration duration,
            ChannelType channelType
    ) {
    }

    private record ScoredCandidate(
            AvailabilityOverlap overlap,
            int score
    ) {
    }
}
