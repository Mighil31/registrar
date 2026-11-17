package io.github.mighil31.registrar.event;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.github.mighil31.registrar.db.MongoTransaction;
import io.github.mighil31.registrar.registration.Registrations;
import io.github.mighil31.registrar.security.Caller;
import io.github.mighil31.registrar.user.Permissions;
import io.github.mighil31.registrar.web.PageResponse;
import io.github.mighil31.registrar.web.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.bson.types.ObjectId;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
class EventService {

    static final int MAX_PAGE_SIZE = 100;

    private final MongoTemplate mongo;
    private final Registrations registrations;
    private final Clock clock;

    EventService(MongoTemplate mongo, Registrations registrations, Clock clock) {
        this.mongo = mongo;
        this.registrations = registrations;
        this.clock = clock;
    }

    /** Which events a list shows. */
    enum Scope {
        /** Open, scheduled, not started; soonest first. What members browse. */
        BROWSE,
        /** Everything, latest first, with mode and status filters. Admins only. */
        ALL
    }

    PageResponse<EventResponse> list(
            Caller caller,
            Scope scope,
            String q,
            String tag,
            Event.Mode mode,
            Event.Status status,
            int page,
            int size) {
        if (scope == Scope.ALL && !caller.can(Permissions.EVENTS_MANAGE)) {
            throw ProblemException.forbidden();
        }
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_PAGE_SIZE);
        List<Criteria> filters = new ArrayList<>();
        if (scope == Scope.BROWSE) {
            // Equality, equality, range: the order of the mode_status_startsAt index.
            filters.add(where("mode").is(Event.Mode.OPEN));
            filters.add(where("status").is(Event.Status.SCHEDULED));
            filters.add(where("startsAt").gt(clock.instant()));
        } else {
            if (mode != null) {
                filters.add(where("mode").is(mode));
            }
            if (status != null) {
                filters.add(where("status").is(status));
            }
        }
        if (q != null && !q.isBlank()) {
            filters.add(where("title").regex(Pattern.quote(q.strip()), "i"));
        }
        if (tag != null && !tag.isBlank()) {
            filters.add(where("tags").is(tag));
        }
        Query query = filters.isEmpty() ? new Query() : new Query(new Criteria().andOperator(filters));
        long total = mongo.count(query, Event.class);
        query.with(
                        scope == Scope.BROWSE
                                ? Sort.by(Sort.Direction.ASC, "startsAt")
                                : Sort.by(Sort.Direction.DESC, "startsAt"))
                .skip((long) page * size)
                .limit(size);
        List<Event> events = mongo.find(query, Event.class);
        Set<ObjectId> going = registrations.eventsAttendedBy(
                caller.id(), events.stream().map(Event::id).toList());
        boolean admin = caller.can(Permissions.EVENTS_MANAGE);
        List<EventResponse> items = events.stream()
                .map(e -> EventResponse.of(e, going.contains(e.id()), admin || going.contains(e.id())))
                .toList();
        return new PageResponse<>(items, page, size, total);
    }

    /** A private event the caller isn't going to doesn't exist, as far as they can tell. */
    EventResponse get(Caller caller, ObjectId id) {
        Event event = find(id);
        boolean admin = caller.can(Permissions.EVENTS_MANAGE);
        boolean going = registrations.isRegistered(id, caller.id());
        if (event.mode() == Event.Mode.PRIVATE && !admin && !going) {
            throw ProblemException.notFound("Event");
        }
        return EventResponse.of(event, going, admin || going);
    }

    EventResponse create(Caller caller, EventRequest request) {
        checkTimes(request);
        Instant now = clock.instant();
        Event event = mongo.insert(new Event(
                null,
                request.title().strip(),
                blankToNull(request.description()),
                request.format(),
                blankToNull(request.location()),
                blankToNull(request.meetingLink()),
                request.startsAt(),
                request.endsAt(),
                request.mode(),
                request.capacity(),
                0,
                tags(request.tags()),
                Event.Status.SCHEDULED,
                caller.id(),
                now,
                now,
                0));
        return EventResponse.of(event, false, true);
    }

    /**
     * Saves the editor's fields only if nobody saved since {@code version}. Never touches
     * {@code registeredCount}, which registrations change at the same time. Title and start time are
     * copied into registrations, so those copies change in the same transaction.
     */
    @MongoTransaction
    EventResponse update(ObjectId id, EventRequest request) {
        if (request.version() == null) {
            throw ProblemException.invalid("version", "is required when editing");
        }
        checkTimes(request);
        Update update = new Update()
                .set("title", request.title().strip())
                .set("description", blankToNull(request.description()))
                .set("format", request.format())
                .set("location", blankToNull(request.location()))
                .set("meetingLink", blankToNull(request.meetingLink()))
                .set("startsAt", request.startsAt())
                .set("endsAt", request.endsAt())
                .set("mode", request.mode())
                .set("capacity", request.capacity())
                .set("tags", tags(request.tags()))
                .set("updatedAt", clock.instant())
                .inc("version", 1);
        Event saved = applyVersioned(id, request.version(), update);
        registrations.copyEvent(saved);
        return EventResponse.of(saved, false, true);
    }

    /** Registrations are kept: un-cancelling brings everyone back. */
    @MongoTransaction
    EventResponse setStatus(ObjectId id, Event.Status status) {
        Event event = mongo.findAndModify(
                Query.query(where("_id").is(id)),
                new Update()
                        .set("status", status)
                        .set("updatedAt", clock.instant())
                        .inc("version", 1),
                FindAndModifyOptions.options().returnNew(true),
                Event.class);
        if (event == null) {
            throw ProblemException.notFound("Event");
        }
        registrations.copyEvent(event);
        return EventResponse.of(event, false, true);
    }

    @MongoTransaction
    void delete(ObjectId id) {
        if (mongo.remove(Query.query(where("_id").is(id)), Event.class).getDeletedCount() == 0) {
            throw ProblemException.notFound("Event");
        }
        registrations.deleteForEvent(id);
    }

    private Event find(ObjectId id) {
        Event event = mongo.findById(id, Event.class);
        if (event == null) {
            throw ProblemException.notFound("Event");
        }
        return event;
    }

    private Event applyVersioned(ObjectId id, long version, Update update) {
        Event event = mongo.findAndModify(
                Query.query(where("_id").is(id).and("version").is(version)),
                update,
                FindAndModifyOptions.options().returnNew(true),
                Event.class);
        if (event == null) {
            find(id); // 404 if it's gone, otherwise the version was stale
            throw new OptimisticLockingFailureException("Event " + id + " changed since version " + version);
        }
        return event;
    }

    private static void checkTimes(EventRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw ProblemException.invalid("endsAt", "must be after the start");
        }
    }

    private static List<String> tags(List<String> tags) {
        return tags == null ? List.of() : List.copyOf(new LinkedHashSet<>(tags));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
