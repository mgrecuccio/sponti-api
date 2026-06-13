package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.matching.api.event.MatchSuggestionsAvailableEvent;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingOpportunitySchedulerProperties;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingProperties;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
class MatchingOpportunityApplicationService {

    private static final Logger log = LoggerFactory.getLogger(MatchingOpportunityApplicationService.class);

    private final Clock clock;
    private final MatchingProperties properties;
    private final MatchingOpportunitySchedulerProperties schedulerProperties;
    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery;
    private final MatchSuggestionsService matchSuggestionsService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void checkCurrentOpportunities() {
        var userIds = userMatchingPreferencesQuery.getMatchingEnabledUserIds();
        log.info("Checking matching opportunities: userCount={}", userIds.size());
        userIds.forEach(this::checkCurrentOpportunitiesForUser);
    }

    @Transactional(readOnly = true)
    public void checkCurrentOpportunitiesForUser(Long userId) {
        var now = Instant.now(clock);
        var suggestions = matchSuggestionsService.getSuggestions(userId)
                .stream()
                .filter(suggestion -> isCurrent(suggestion, now))
                .filter(suggestion -> suggestion.score() >= properties.scoring().notificationMinimumScore())
                .sorted(Comparator.comparingInt(SuggestedMatchView::score).reversed())
                .toList();

        if (suggestions.isEmpty()) {
            log.debug("Matching opportunity notification skipped: userId={} reason=no-current-strong-suggestions", userId);
            return;
        }

        var bestSuggestion = suggestions.getFirst();
        eventPublisher.publishEvent(new MatchSuggestionsAvailableEvent(
                userId,
                suggestions.stream().map(SuggestedMatchView::candidateUserId).toList(),
                bestSuggestion.score(),
                bestSuggestion.overlapStart(),
                bestSuggestion.overlapEnd()
        ));
    }

    private boolean isCurrent(SuggestedMatchView suggestion, Instant now) {
        var nextSchedulerRunAt = now.plus(schedulerProperties.fixedDelay());
        var startsBeforeNextSchedulerRun = !suggestion.overlapStart().isAfter(nextSchedulerRunAt);
        var endsAfterNow = suggestion.overlapEnd().isAfter(now);

        if (!startsBeforeNextSchedulerRun) {
            log.debug(
                    "Matching opportunity suggestion skipped: candidateUserId={} reason=starts-after-next-scheduler-run overlapStart={} nextSchedulerRunAt={}",
                    suggestion.candidateUserId(),
                    suggestion.overlapStart(),
                    nextSchedulerRunAt
            );
        }
        if (!endsAfterNow) {
            log.debug(
                    "Matching opportunity suggestion skipped: candidateUserId={} reason=overlap-ended overlapEnd={} now={}",
                    suggestion.candidateUserId(),
                    suggestion.overlapEnd(),
                    now
            );
        }

        return startsBeforeNextSchedulerRun && endsAfterNow;
    }
}
