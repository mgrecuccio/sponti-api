package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityRuleEntity;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityOverrideRepository;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityRuleRepository;
import com.mgrtech.sponti_api.user.api.UserProfileView;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
public class EffectiveAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(EffectiveAvailabilityService.class);
    private static final ZoneId DEFAULT_ZONE_ID = ZoneOffset.UTC;

    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;
    private final UserQueryFacade userQueryFacade;

    public List<TimeWindow> compute(Long userId, Instant from, Instant to) {
        log.info("Computing effective availability: userId={}, from={}, to={}", userId, from, to);
        validateRange(from, to);

        var zoneId = resolveUserZoneId(userId);

        var rules = availabilityRuleRepository.findByUserIdAndEnabledTrue(userId);
        log.debug("UserId={} found {} availability rules from={} to={}", userId, rules.size(), from, to);

        var overrides = availabilityOverrideRepository
                .findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(userId, to, from);
        log.debug("UserId={} found {} overrides from={} to={}", userId, overrides.size(), from, to);

        var baseWindows = buildBaseWindowsFromRules(rules, from, to, zoneId);

        var unavailableWindows = overrides.stream()
                .filter(it -> it.getType() == AvailabilityOverrideType.UNAVAILABLE)
                .map(it -> clip(new TimeWindow(it.getStartDateTime(), it.getEndDateTime()), from, to))
                .filter(Objects::nonNull)
                .toList();
        log.debug("UserId={} found {} unavailability overrides from={} to={}", userId, unavailableWindows.size(), from, to);

        var availableWindows = overrides.stream()
                .filter(it -> it.getType() == AvailabilityOverrideType.AVAILABLE)
                .map(it -> clip(new TimeWindow(it.getStartDateTime(), it.getEndDateTime()), from, to))
                .filter(Objects::nonNull)
                .toList();
        log.debug("UserId={} found {} availability overrides from={} to={}", userId, availableWindows.size(), from, to);

        var combined = merge(normalize(concat(baseWindows, availableWindows)));
        var effective = subtractWindows(combined, normalize(unavailableWindows));
        effective = merge(normalize(effective));

        log.info("Computed effective availability: userId={} windowCount={} from={} to={}",
                userId, effective.size(), from, to);
        return effective;
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Effective availability range must satisfy from < to");
        }
    }

    private ZoneId resolveUserZoneId(Long userId) {
        return userQueryFacade.getProfileById(userId)
                .map(UserProfileView::timezone)
                .filter(value -> !value.isBlank())
                .map(this::safeZoneId)
                .orElse(DEFAULT_ZONE_ID);
    }

    private ZoneId safeZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            return DEFAULT_ZONE_ID;
        }
    }

    private List<TimeWindow> buildBaseWindowsFromRules(
            List<AvailabilityRuleEntity> rules,
            Instant from,
            Instant to,
            ZoneId zoneId
    ) {
        var result = new ArrayList<TimeWindow>();

        var startDate = from.atZone(zoneId).toLocalDate();
        var endDate = to.minusMillis(1).atZone(zoneId).toLocalDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (var rule : rules) {
                if (rule.getDayOfWeek() != date.getDayOfWeek()) {
                    continue;
                }

                var zonedStart = ZonedDateTime.of(date, rule.getStartTime(), zoneId);
                var zonedEnd = ZonedDateTime.of(date, rule.getEndTime(), zoneId);

                var window = clip(
                        new TimeWindow(zonedStart.toInstant(), zonedEnd.toInstant()),
                        from,
                        to
                );

                if (window != null) {
                    result.add(window);
                }
            }
        }
        return merge(result);
    }

    private List<TimeWindow> subtractWindows(List<TimeWindow> base, List<TimeWindow> removals) {
        var current = new ArrayList<>(base);

        for (var removal : removals) {
            var next = new ArrayList<TimeWindow>();

            for (var baseWindow : current) {
                next.addAll(subtractSingle(baseWindow, removal));
            }

            current = next;
        }

        return merge(current);
    }

    private List<TimeWindow> subtractSingle(TimeWindow source, TimeWindow removal) {
        if (!source.overlaps(removal)) {
            return List.of(source);
        }

        var result = new ArrayList<TimeWindow>();

        if (source.start().isBefore(removal.start())) {
            result.add(new TimeWindow(source.start(), min(source.end(), removal.start())));
        }

        if (source.end().isAfter(removal.end())) {
            result.add(new TimeWindow(max(source.start(), removal.end()), source.end()));
        }

        return result.stream()
                .filter(TimeWindow::isValid)
                .toList();
    }

    private List<TimeWindow> normalize(List<TimeWindow> windows) {
        return merge(windows.stream()
                .filter(TimeWindow::isValid)
                .sorted(Comparator.comparing(TimeWindow::start))
                .toList());
    }

    private List<TimeWindow> merge(List<TimeWindow> windows) {
        if (windows.isEmpty()) {
            return List.of();
        }

        var sorted = windows.stream()
                .filter(TimeWindow::isValid)
                .sorted(Comparator.comparing(TimeWindow::start))
                .toList();

        var merged = new ArrayList<TimeWindow>();
        var current = sorted.getFirst();

        for (int i = 1; i < sorted.size(); i++) {
            var next = sorted.get(i);

            if (!current.end().isBefore(next.start())) {
                current = new TimeWindow(current.start(), max(current.end(), next.end()));
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    private List<TimeWindow> concat(List<TimeWindow> first, List<TimeWindow> second) {
        var result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private TimeWindow clip(TimeWindow window, Instant from, Instant to) {
        var start = max(window.start(), from);
        var end = min(window.end(), to);

        if (!start.isBefore(end)) {
            return null;
        }

        return new TimeWindow(start, end);
    }

    private Instant max(Instant left, Instant right) {
        return left.isAfter(right) ? left : right;
    }

    private Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }
}
