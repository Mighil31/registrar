package io.github.mighil31.registrar.registration;

import io.github.mighil31.registrar.event.Event;
import java.time.Instant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One person at one event, in {@code registrations} ({@code _0003__CreateRegistrations}).
 *
 * <p>{@code user} and {@code event} are copies of the fields My events and the attendee list
 * show, so each is one indexed query with no join. {@link Registrations} keeps them in step.
 *
 * @param addedBy the user themself, or the admin who added them
 * @param overCapacity an admin added them after the event was full
 */
@Document("registrations")
public record Registration(
        @Id ObjectId id,
        ObjectId eventId,
        ObjectId userId,
        Instant registeredAt,
        ObjectId addedBy,
        boolean overCapacity,
        UserCopy user,
        EventCopy event) {

    public record UserCopy(String name, String email) {}

    public record EventCopy(String title, Instant startsAt, Event.Status status) {

        static EventCopy of(Event event) {
            return new EventCopy(event.title(), event.startsAt(), event.status());
        }
    }
}
