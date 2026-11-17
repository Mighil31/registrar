package io.github.mighil31.registrar.event;

import java.time.Instant;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An event, in {@code events} (validator and indexes: {@code _0002__CreateEvents}).
 *
 * <p>{@code registeredCount} only changes by {@code $inc}, in the same transaction as the
 * registration it counts, and never bumps {@code version}: a registration mustn't make an admin's
 * open edit form stale. That's why {@code version} is a plain field checked by {@code EventService}:
 * Spring's {@code @Version} would bump it on every update, the count included.
 */
@Document("events")
public record Event(
        @Id ObjectId id,
        String title,
        String description,
        Format format,
        String location,
        String meetingLink,
        Instant startsAt,
        Instant endsAt,
        Mode mode,
        Integer capacity,
        int registeredCount,
        List<String> tags,
        Status status,
        ObjectId createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public enum Format {
        IN_PERSON,
        ONLINE
    }

    /** OPEN: listed, members join themselves. PRIVATE: unlisted, only admins add people. */
    public enum Mode {
        OPEN,
        PRIVATE
    }

    public enum Status {
        SCHEDULED,
        CANCELLED
    }

    public boolean isFull() {
        return capacity != null && registeredCount >= capacity;
    }

    /** Started or cancelled: members can no longer join or leave. */
    public boolean isClosed(Instant now) {
        return status == Status.CANCELLED || !startsAt.isAfter(now);
    }
}
