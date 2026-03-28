package com.mgrtech.sponti_api.availability.internal.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;

class TimeWindowTest {

    @Test
    void should_detect_overlap_when_windows_intersect() {
        // A: [09:00, 12:00)
        var from_9_to_12 = new TimeWindow(
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T12:00:00Z")
        );

        // B: [11:00, 13:00)
        var from_11_to_13 = new TimeWindow(
                Instant.parse("2026-03-30T11:00:00Z"),
                Instant.parse("2026-03-30T13:00:00Z")
        );

        assertThat(from_9_to_12.overlaps(from_11_to_13)).isTrue();
        assertThat(from_11_to_13.overlaps(from_9_to_12)).isTrue();
    }

    @Test
    void should_not_detect_overlap_when_windows_dont_intersect_half_open_semantic() {
        // A: [09:00, 12:00)
        var from_9_to_12 = new TimeWindow(
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T12:00:00Z")
        );

        // B: [8:00, 9:00)
        var from_8_to_9 = new TimeWindow(
                Instant.parse("2026-03-30T08:00:00Z"),
                Instant.parse("2026-03-30T09:00:00Z")
        );

        assertThat(from_9_to_12.overlaps(from_8_to_9)).isFalse();
        assertThat(from_8_to_9.overlaps(from_9_to_12)).isFalse();
    }

    @Test
    void should_not_detect_overlap_when_windows_dont_intersect() {
        // A: [09:00, 12:00)
        var from_9_to_12 = new TimeWindow(
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T12:00:00Z")
        );

        // B: [14:00, 18:00)
        var from_14_to_18 = new TimeWindow(
                Instant.parse("2026-03-30T14:00:00Z"),
                Instant.parse("2026-03-30T18:00:00Z")
        );

        assertThat(from_9_to_12.overlaps(from_14_to_18)).isFalse();
        assertThat(from_14_to_18.overlaps(from_9_to_12)).isFalse();
    }

    @Test
    void should_return_true_if_time_window_is_valid() {
        var timeWindow = new TimeWindow(
                Instant.parse("2026-03-30T14:00:00Z"),
                Instant.parse("2026-03-30T18:00:00Z")
        );
        assertThat(timeWindow.isValid()).isTrue();
    }

    @Test
    void should_return_false_if_start_time_window_is_missing() {
        var timeWindow = new TimeWindow(
                null,
                Instant.parse("2026-03-30T18:00:00Z")
        );
        assertThat(timeWindow.isValid()).isFalse();
    }

    @Test
    void should_return_false_if_end_time_window_is_missing() {
        var timeWindow = new TimeWindow(
                Instant.parse("2026-03-30T18:00:00Z"),
                null
        );
        assertThat(timeWindow.isValid()).isFalse();
    }

    @Test
    void should_return_false_if_end_time_before_start_date() {
        var timeWindow = new TimeWindow(
                Instant.parse("2026-03-30T18:00:00Z"),
                Instant.parse("2026-03-30T09:00:00Z")
        );
        assertThat(timeWindow.isValid()).isFalse();
    }
}