package io.github.mighil31.registrar.event;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * An event as the caller may see it. {@code meetingLink} is only filled in for attendees and
 * admins; {@code registered} says whether the caller is going.
 */
public record EventResponse(
        String id,
        String title,
        @Schema(nullable = true) String description,
        Event.Format format,
        @Schema(nullable = true) String location,
        @Schema(nullable = true) String meetingLink,
        Instant startsAt,
        Instant endsAt,
        Event.Mode mode,
        @Schema(nullable = true) Integer capacity,
        int registeredCount,
        List<String> tags,
        Event.Status status,
        boolean registered,
        long version) {

    static EventResponse of(Event e, boolean registered, boolean showLink) {
        return new EventResponse(
                e.id().toHexString(),
                e.title(),
                e.description(),
                e.format(),
                e.location(),
                showLink ? e.meetingLink() : null,
                e.startsAt(),
                e.endsAt(),
                e.mode(),
                e.capacity(),
                e.registeredCount(),
                e.tags(),
                e.status(),
                registered,
                e.version());
    }
}
