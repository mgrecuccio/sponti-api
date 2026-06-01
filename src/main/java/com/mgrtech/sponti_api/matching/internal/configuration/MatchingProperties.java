package com.mgrtech.sponti_api.matching.internal.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "sponti.matching")
public record MatchingProperties(

        @NotNull
        Duration searchWindow,

        @NotNull
        Duration minimumOverlap,

        @NotNull
        Duration acceptedCooldown,

        @NotNull
        Duration declineCooldown,

        @NotNull
        Duration suggestionCooldown,

        @NotNull
        Duration proposalTtl,

        @Valid
        @NotNull
        Scoring scoring
) {
    public record Scoring(

            int favoriteBoost,

            int freeNowBoost,

            int recentAcceptedPenalty,

            int recentDeclinePenalty,

            int recentSuggestionPenalty,

            int quietHoursPenalty,

            int minimumScore,

            int notificationMinimumScore
    ) {
    }
}
