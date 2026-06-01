package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityRuleEntity;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityOverrideRepository;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityRuleRepository;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
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
    private final UserProfileQuery userProfileQuery;

    public List<ChannelTimeWindow> compute(Long userId, Instant from, Instant to) {
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
                .flatMap(window -> toAllChannelWindows(window).stream())
                .toList();
        log.debug("UserId={} found {} availability overrides from={} to={}", userId, availableWindows.size(), from, to);

        var combined = merge(normalize(concat(baseWindows, availableWindows)));
        var effective = subtractWindows(combined, normalizeRemovals(unavailableWindows));
        effective = merge(normalize(effective));

        log.info("Computed effective availability: userId={} windowCount={} from={} to={}",
                userId, effective.size(), from, to);
        return effective;
    }

    public List<ChannelTimeWindow> computeChannelAgnostic(Long userId, Instant from, Instant to) {
        return mergeTimeWindows(compute(userId, from, to)).stream()
                .map(window -> new ChannelTimeWindow(window.start(), window.end(), null))
                .toList();
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException("Effective availability range must satisfy from < to");
        }
    }

    private ZoneId resolveUserZoneId(Long userId) {
        return userProfileQuery.getProfileById(userId)
                .map(UserProfileView::timezone)
                .filter(value -> !value.isBlank())
                .map(this::safeZoneId)
                .orElseGet(() -> {
                    log.warn("No timezone found for userId={}, falling back to UTC", userId);
                    return DEFAULT_ZONE_ID;
                });
    }

    private ZoneId safeZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            log.warn("Invalid timezone={}, falling back to UTC", timezone);
            return DEFAULT_ZONE_ID;
        }
    }

    private List<ChannelTimeWindow> buildBaseWindowsFromRules(
            List<AvailabilityRuleEntity> rules,
            Instant from,
            Instant to,
            ZoneId zoneId
    ) {
        var result = new ArrayList<ChannelTimeWindow>();

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
                    result.add(new ChannelTimeWindow(window.start(), window.end(), rule.getChannelType()));
                }
            }
        }
        return merge(result);
    }

    private List<ChannelTimeWindow> subtractWindows(List<ChannelTimeWindow> base, List<TimeWindow> removals) {
        var current = new ArrayList<>(base);

        for (var removal : removals) {
            var next = new ArrayList<ChannelTimeWindow>();

            for (var baseWindow : current) {
                next.addAll(subtractSingle(baseWindow, removal));
            }

            current = next;
        }

        return merge(current);
    }

    private List<ChannelTimeWindow> subtractSingle(ChannelTimeWindow source, TimeWindow removal) {
        if (!source.overlaps(removal)) {
            return List.of(source);
        }

        var result = new ArrayList<ChannelTimeWindow>();

        if (source.start().isBefore(removal.start())) {
            result.add(new ChannelTimeWindow(source.start(), min(source.end(), removal.start()), source.channelType()));
        }

        if (source.end().isAfter(removal.end())) {
            result.add(new ChannelTimeWindow(max(source.start(), removal.end()), source.end(), source.channelType()));
        }

        return result.stream()
                .filter(ChannelTimeWindow::isValid)
                .toList();
    }

    private List<ChannelTimeWindow> normalize(List<ChannelTimeWindow> windows) {
        return merge(windows.stream()
                .filter(ChannelTimeWindow::isValid)
                .sorted(channelWindowComparator())
                .toList());
    }

    private List<TimeWindow> normalizeRemovals(List<TimeWindow> windows) {
        return mergeRemovals(windows.stream()
                .filter(TimeWindow::isValid)
                .sorted(Comparator.comparing(TimeWindow::start))
                .toList());
    }

    private List<ChannelTimeWindow> merge(List<ChannelTimeWindow> windows) {
        if (windows.isEmpty()) {
            return List.of();
        }

        var sorted = windows.stream()
                .filter(ChannelTimeWindow::isValid)
                .sorted(channelWindowComparator())
                .toList();

        var merged = new ArrayList<ChannelTimeWindow>();
        var current = sorted.getFirst();

        for (int i = 1; i < sorted.size(); i++) {
            var next = sorted.get(i);

            if (current.channelType() == next.channelType() && !current.end().isBefore(next.start())) {
                current = new ChannelTimeWindow(current.start(), max(current.end(), next.end()), current.channelType());
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    private List<TimeWindow> mergeRemovals(List<TimeWindow> windows) {
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

    private List<TimeWindow> mergeTimeWindows(List<ChannelTimeWindow> windows) {
        return mergeRemovals(windows.stream()
                .filter(ChannelTimeWindow::isValid)
                .map(window -> new TimeWindow(window.start(), window.end()))
                .sorted(Comparator.comparing(TimeWindow::start))
                .toList());
    }

    private List<ChannelTimeWindow> concat(List<ChannelTimeWindow> first, List<ChannelTimeWindow> second) {
        var result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }

    private List<ChannelTimeWindow> toAllChannelWindows(TimeWindow window) {
        return List.of(
                new ChannelTimeWindow(window.start(), window.end(), ChannelType.CHAT),
                new ChannelTimeWindow(window.start(), window.end(), ChannelType.CALL)
        );
    }

    private Comparator<ChannelTimeWindow> channelWindowComparator() {
        return Comparator
                .comparing(ChannelTimeWindow::channelType)
                .thenComparing(ChannelTimeWindow::start);
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
