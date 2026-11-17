package io.github.mighil31.registrar.registration;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.github.mighil31.registrar.auth.InvalidRefreshTokenException;
import io.github.mighil31.registrar.db.Transactions;
import io.github.mighil31.registrar.event.Event;
import io.github.mighil31.registrar.user.User;
import io.github.mighil31.registrar.web.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
class RegistrationService {

    static final int MAX_BULK_ADD = 100;

    private final MongoTemplate mongo;
    private final Registrations registrations;
    private final Transactions transactions;
    private final Clock clock;

    RegistrationService(MongoTemplate mongo, Registrations registrations, Transactions transactions, Clock clock) {
        this.mongo = mongo;
        this.registrations = registrations;
        this.transactions = transactions;
        this.clock = clock;
    }

    /**
     * A member takes a seat. Step 1 adds one to the count only if the event is open, upcoming and
     * not full: one atomic update on one document, so two people can't both get the last seat.
     * Step 2 inserts the registration; if it's a duplicate the transaction aborts and the count goes
     * back down.
     */
    Registration register(ObjectId eventId, ObjectId userId) {
        User user = mongo.findById(userId, User.class);
        if (user == null) {
            throw new InvalidRefreshTokenException();
        }
        return transactions.run(() -> {
            Instant now = clock.instant();
            Event event = mongo.findAndModify(
                    Query.query(new Criteria()
                            .andOperator(
                                    where("_id").is(eventId),
                                    where("mode").is(Event.Mode.OPEN),
                                    where("status").is(Event.Status.SCHEDULED),
                                    where("startsAt").gt(now),
                                    new Criteria()
                                            .orOperator(
                                                    where("capacity").isNull(),
                                                    Criteria.expr(ComparisonOperators.valueOf("registeredCount")
                                                            .lessThan("capacity"))))),
                    new Update().inc("registeredCount", 1),
                    FindAndModifyOptions.options().returnNew(true),
                    Event.class);
            if (event == null) {
                throw whyNot(eventId, userId, now);
            }
            try {
                return mongo.insert(new Registration(
                        null,
                        eventId,
                        userId,
                        now,
                        userId,
                        false,
                        new Registration.UserCopy(user.name(), user.email()),
                        Registration.EventCopy.of(event)));
            } catch (DuplicateKeyException e) {
                throw alreadyRegistered();
            }
        });
    }

    /** A member leaves, only before the event starts. */
    void leave(ObjectId eventId, ObjectId userId) {
        transactions.run(() -> {
            Event event = mongo.findById(eventId, Event.class);
            if (event == null) {
                throw ProblemException.notFound("Event");
            }
            if (!registrations.isRegistered(eventId, userId)) {
                throw ProblemException.notFound("Registration");
            }
            if (event.isClosed(clock.instant())) {
                throw closed(event);
            }
            removeAndFreeSeat(eventId, userId);
        });
    }

    /**
     * An admin adds people. Past capacity is allowed (the UI warns first); those registrations are
     * marked {@code overCapacity}. People already going are skipped and reported.
     */
    AddResult add(ObjectId eventId, List<String> userIdValues, ObjectId adminId) {
        Set<ObjectId> userIds = new LinkedHashSet<>();
        for (String value : userIdValues) {
            if (!ObjectId.isValid(value)) {
                throw ProblemException.invalid("userIds", "contains an unknown user");
            }
            userIds.add(new ObjectId(value));
        }
        return transactions.run(() -> {
            Event event = mongo.findById(eventId, Event.class);
            if (event == null) {
                throw ProblemException.notFound("Event");
            }
            if (event.status() == Event.Status.CANCELLED) {
                throw closed(event);
            }
            Map<ObjectId, User> users = mongo.find(Query.query(where("_id").in(userIds)), User.class).stream()
                    .collect(Collectors.toMap(User::id, Function.identity()));
            if (users.size() != userIds.size()) {
                throw ProblemException.invalid("userIds", "contains an unknown user");
            }
            Set<ObjectId> going = mongo
                    .find(Query.query(where("eventId").is(eventId).and("userId").in(userIds)), Registration.class)
                    .stream()
                    .map(Registration::userId)
                    .collect(Collectors.toSet());
            List<ObjectId> toAdd =
                    userIds.stream().filter(id -> !going.contains(id)).toList();
            if (toAdd.isEmpty()) {
                return new AddResult(List.of(), hex(going), event.registeredCount(), event.capacity());
            }
            Event counted = mongo.findAndModify(
                    Query.query(where("_id").is(eventId)),
                    new Update().inc("registeredCount", toAdd.size()),
                    FindAndModifyOptions.options().returnNew(true),
                    Event.class);
            int seat = counted.registeredCount() - toAdd.size();
            Instant now = clock.instant();
            List<Registration> added = new ArrayList<>();
            for (ObjectId id : toAdd) {
                seat++;
                User user = users.get(id);
                added.add(new Registration(
                        null,
                        eventId,
                        id,
                        now,
                        adminId,
                        counted.capacity() != null && seat > counted.capacity(),
                        new Registration.UserCopy(user.name(), user.email()),
                        Registration.EventCopy.of(counted)));
            }
            try {
                mongo.insertAll(added);
            } catch (DuplicateKeyException e) {
                throw alreadyRegistered();
            }
            return new AddResult(hex(toAdd), hex(going), counted.registeredCount(), counted.capacity());
        });
    }

    /** An admin removes someone, at any time. */
    void remove(ObjectId eventId, ObjectId userId) {
        transactions.run(() -> {
            if (!registrations.isRegistered(eventId, userId)) {
                throw ProblemException.notFound("Registration");
            }
            removeAndFreeSeat(eventId, userId);
        });
    }

    /** My events: upcoming soonest first, or past latest first. Uses the userId_eventStartsAt index. */
    List<Registration> mine(ObjectId userId, boolean upcoming) {
        Instant now = clock.instant();
        Criteria when = upcoming
                ? where("event.startsAt").gt(now)
                : where("event.startsAt").lte(now);
        return mongo.find(
                Query.query(where("userId").is(userId).andOperator(when))
                        .with(Sort.by(upcoming ? Sort.Direction.ASC : Sort.Direction.DESC, "event.startsAt")),
                Registration.class);
    }

    /** An event's attendees, in the order they registered. */
    List<Registration> attendees(ObjectId eventId) {
        if (!mongo.exists(Query.query(where("_id").is(eventId)), Event.class)) {
            throw ProblemException.notFound("Event");
        }
        return mongo.find(Query.query(where("eventId").is(eventId)).with(Sort.by("registeredAt")), Registration.class);
    }

    record AddResult(List<String> added, List<String> alreadyRegistered, int registeredCount, Integer capacity) {}

    private void removeAndFreeSeat(ObjectId eventId, ObjectId userId) {
        mongo.remove(Registrations.byEventAndUser(eventId, userId), Registration.class);
        mongo.updateFirst(Query.query(where("_id").is(eventId)), new Update().inc("registeredCount", -1), Event.class);
    }

    /** The conditional update matched nothing: say which rule refused it. */
    private ProblemException whyNot(ObjectId eventId, ObjectId userId, Instant now) {
        Event event = mongo.findById(eventId, Event.class);
        boolean going = event != null && registrations.isRegistered(eventId, userId);
        if (event == null || (event.mode() == Event.Mode.PRIVATE && !going)) {
            return ProblemException.notFound("Event");
        }
        if (going) {
            return alreadyRegistered();
        }
        if (event.isClosed(now)) {
            return closed(event);
        }
        return ProblemException.conflict("event-full", "This event is full");
    }

    private static ProblemException alreadyRegistered() {
        return ProblemException.conflict("already-registered", "You're already registered for this event");
    }

    private static ProblemException closed(Event event) {
        return ProblemException.conflict(
                "event-closed",
                event.status() == Event.Status.CANCELLED
                        ? "This event was cancelled"
                        : "This event has already started");
    }

    private static List<String> hex(Collection<ObjectId> ids) {
        return ids.stream().map(ObjectId::toHexString).toList();
    }
}
