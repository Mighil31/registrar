package io.github.mighil31.registrar.dashboard;

import io.github.mighil31.registrar.event.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * The admin dashboard: three numbers, the next upcoming events with their seat counts, and the
 * latest registrations.
 *
 * @param members people who aren't admins
 * @param upcomingEvents scheduled events that haven't started (open and private)
 * @param registrationsThisWeek registrations in the last 7 days
 */
public record Dashboard(
        long members,
        long upcomingEvents,
        long registrationsThisWeek,
        List<EventRow> events,
        List<RecentRegistration> recentRegistrations) {

    /**
     * @param capacity null: unlimited
     * @param percentFull null when unlimited; can pass 100 when admins added people past capacity
     */
    public record EventRow(
            String id,
            String title,
            Instant startsAt,
            Event.Mode mode,
            Event.Format format,
            @Schema(nullable = true) String location,
            List<String> tags,
            @Schema(nullable = true) Integer capacity,
            int registeredCount,
            @Schema(nullable = true) Integer percentFull) {}

    /** @param addedByAdmin an admin added them, rather than they registered themselves */
    public record RecentRegistration(
            String eventId,
            String eventTitle,
            String userId,
            String userName,
            Instant registeredAt,
            boolean addedByAdmin) {}
}
