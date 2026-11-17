package io.github.mighil31.registrar.seed;

import io.github.mighil31.registrar.db.MongoTransaction;
import io.github.mighil31.registrar.event.Event;
import io.github.mighil31.registrar.registration.Registration;
import io.github.mighil31.registrar.user.Role;
import io.github.mighil31.registrar.user.User;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Demo data so the screens aren't empty: 40 people (one an admin), 12 events (upcoming, past, full,
 * nearly full, cancelled, private, online) and their registrations, with counts that match.
 *
 * <p>Only with {@code registrar.dev-seed=true}, and only once per database: it skips when its own
 * marker, the seeded admin ({@link #MARKER_EMAIL}), exists. Not "when there are no events": after
 * someone deletes every event, a re-run would collide with the seeded emails and stop the app
 * starting. A startup runner rather than a migration, like {@code AdminBootstrap}: it needs the password
 * encoder, and dates relative to today (a migration's "upcoming" events would be past in a week).
 * Runs after {@code AdminBootstrap}, so the first admin from the settings is created as usual. One
 * transaction: all of it or none.
 */
@Component
@ConditionalOnBooleanProperty("registrar.dev-seed")
@Order(1)
public class DevSeed implements ApplicationRunner {

    /** Every seeded person signs in with this. */
    public static final String PASSWORD = "registrar-demo";

    static final String DOMAIN = "@registrar.example";

    private static final Logger log = LoggerFactory.getLogger(DevSeed.class);

    private static final List<String> NAMES = List.of(
            "Dana Whitfield",
            "Aisha Bello",
            "Ben Carter",
            "Chloé Martin",
            "Diego Alvarez",
            "Emma Lindqvist",
            "Farah Haddad",
            "George Okoro",
            "Hana Sato",
            "Ivan Petrov",
            "Jade Nguyen",
            "Kofi Mensah",
            "Lena Fischer",
            "Liam O'Connor",
            "Maya Okafor",
            "Nikhil Rao",
            "Olivia Brown",
            "Pablo Ruiz",
            "Qing Li",
            "Rosa Costa",
            "Sam Taylor",
            "Tara Singh",
            "Uma Iyer",
            "Victor Dubois",
            "Wen Zhao",
            "Ximena Flores",
            "Yusuf Demir",
            "Zoe Adams",
            "Arjun Mehta",
            "Bianca Rossi",
            "Caleb Wright",
            "Dina Cohen",
            "Elias Berg",
            "Fatima Khan",
            "Gabe Moreau",
            "Holly Evans",
            "Isaac Levi",
            "Jun Park",
            "Kira Novak",
            "Leo Santos");

    /** The first seeded person, an admin; their account means the seed has run. */
    static final String MARKER_EMAIL = email(NAMES.getFirst());

    /** One seeded event: when it starts relative to now, and how many people go. */
    private record Plan(
            String title,
            String description,
            Event.Format format,
            Duration startsIn,
            Event.Mode mode,
            Integer capacity,
            int attendees,
            Event.Status status,
            List<String> tags) {}

    private static final List<Plan> EVENTS = List.of(
            new Plan(
                    "Schema design clinic",
                    "Bring your data model; leave with a better one.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(2),
                    Event.Mode.OPEN,
                    8,
                    8,
                    Event.Status.SCHEDULED,
                    List.of("workshop", "mongodb")),
            new Plan(
                    "Intro to aggregation pipelines",
                    "From $match to $facet, hands on.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(3),
                    Event.Mode.OPEN,
                    30,
                    18,
                    Event.Status.SCHEDULED,
                    List.of("workshop", "mongodb")),
            new Plan(
                    "New starter onboarding",
                    "Accounts, tools and who to ask.",
                    Event.Format.ONLINE,
                    Duration.ofDays(4),
                    Event.Mode.PRIVATE,
                    null,
                    5,
                    Event.Status.SCHEDULED,
                    List.of("onboarding")),
            new Plan(
                    "Spring Boot office hours",
                    "Drop in with your questions.",
                    Event.Format.ONLINE,
                    Duration.ofDays(5),
                    Event.Mode.OPEN,
                    null,
                    22,
                    Event.Status.SCHEDULED,
                    List.of("talk", "java")),
            new Plan(
                    "Lunch and learn: transactions",
                    "When MongoDB needs one, and when it doesn't.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(6),
                    Event.Mode.OPEN,
                    12,
                    11,
                    Event.Status.SCHEDULED,
                    List.of("talk", "mongodb")),
            new Plan(
                    "Accessibility review",
                    "Walking the app with a screen reader.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(7),
                    Event.Mode.OPEN,
                    25,
                    9,
                    Event.Status.CANCELLED,
                    List.of("design")),
            new Plan(
                    "Leadership offsite",
                    "Planning the next two quarters.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(9),
                    Event.Mode.PRIVATE,
                    15,
                    7,
                    Event.Status.SCHEDULED,
                    List.of("planning")),
            new Plan(
                    "Community meetup",
                    "Lightning talks, then pizza.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(10),
                    Event.Mode.OPEN,
                    60,
                    25,
                    Event.Status.SCHEDULED,
                    List.of("meetup", "social")),
            new Plan(
                    "Hack night",
                    "Build something small in one evening.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(14),
                    Event.Mode.OPEN,
                    20,
                    6,
                    Event.Status.SCHEDULED,
                    List.of("hackathon", "social")),
            new Plan(
                    "Kubernetes basics",
                    "Pods, deployments and services.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(-5),
                    Event.Mode.OPEN,
                    25,
                    21,
                    Event.Status.SCHEDULED,
                    List.of("workshop", "infra")),
            new Plan(
                    "Quarterly demo day",
                    "Every team shows one thing.",
                    Event.Format.ONLINE,
                    Duration.ofDays(-12),
                    Event.Mode.OPEN,
                    null,
                    30,
                    Event.Status.SCHEDULED,
                    List.of("talk")),
            new Plan(
                    "Security brown bag",
                    "Tokens, cookies and CSRF.",
                    Event.Format.IN_PERSON,
                    Duration.ofDays(-20),
                    Event.Mode.OPEN,
                    40,
                    14,
                    Event.Status.SCHEDULED,
                    List.of("security", "talk")));

    private final MongoTemplate mongo;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    DevSeed(MongoTemplate mongo, PasswordEncoder passwordEncoder, Clock clock) {
        this.mongo = mongo;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @MongoTransaction
    public void run(ApplicationArguments args) {
        if (mongo.exists(Query.query(Criteria.where("email").is(MARKER_EMAIL)), User.class)) {
            log.info("Dev seed skipped: already seeded ({} exists)", MARKER_EMAIL);
            return;
        }
        Random random = new Random(42); // the same people at the same events on every fresh start
        Instant now = clock.instant().truncatedTo(ChronoUnit.HOURS);
        List<User> people = List.copyOf(mongo.insertAll(people(now)));
        User admin = people.stream().filter(User::isAdmin).findFirst().orElseThrow();
        List<Registration> registrations = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        for (Plan plan : EVENTS) {
            Event event = mongo.insert(event(plan, admin.id(), now));
            events.add(event);
            List<User> going = new ArrayList<>(people);
            Collections.shuffle(going, random);
            for (User user : going.subList(0, plan.attendees())) {
                registrations.add(registration(event, user, admin, now, random));
            }
        }
        mongo.insertAll(registrations);
        log.info(
                "Dev seed: {} people, {} events, {} registrations; password '{}'",
                people.size(),
                events.size(),
                registrations.size(),
                PASSWORD);
    }

    private List<User> people(Instant now) {
        String hash = passwordEncoder.encode(PASSWORD); // bcrypt is slow on purpose: once, not 40 times
        List<User> people = new ArrayList<>();
        for (int i = 0; i < NAMES.size(); i++) {
            String name = NAMES.get(i);
            Instant joined = now.minus(Duration.ofDays(60 - i));
            User user = User.newMember(name, email(name), "2125550" + String.format("%03d", i), hash, joined);
            people.add(i == 0 ? user.withRoles(EnumSet.of(Role.MEMBER, Role.ADMIN), joined) : user);
        }
        return people;
    }

    /** "Liam O'Connor" → liam.oconnor@registrar.example; accents dropped. */
    static String email(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[^A-Za-z ]", "")
                .toLowerCase(Locale.ROOT);
        return ascii.replace(' ', '.') + DOMAIN;
    }

    private static Event event(Plan plan, ObjectId adminId, Instant now) {
        Instant startsAt =
                now.plus(plan.startsIn()).truncatedTo(ChronoUnit.DAYS).plus(Duration.ofHours(17));
        boolean online = plan.format() == Event.Format.ONLINE;
        Instant created = now.minus(Duration.ofDays(30));
        return new Event(
                null,
                plan.title(),
                plan.description(),
                plan.format(),
                online ? null : "Room 4.0" + (plan.attendees() % 9 + 1),
                online
                        ? "https://meet.registrar.example/"
                                + plan.title().toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", "-")
                        : null,
                startsAt,
                startsAt.plus(Duration.ofHours(2)),
                plan.mode(),
                plan.capacity(),
                plan.attendees(),
                plan.tags(),
                plan.status(),
                adminId,
                created,
                created,
                0);
    }

    /**
     * Upcoming events: registered some time in the last two weeks, so "this week" has numbers.
     * Past events: in the three weeks before they started. Private events: added by the admin.
     */
    private static Registration registration(Event event, User user, User admin, Instant now, Random random) {
        Instant latest = event.startsAt().isAfter(now) ? now : event.startsAt().minus(Duration.ofDays(1));
        Instant at = latest.minus(Duration.ofMinutes(30 + random.nextInt(14 * 24 * 60)));
        boolean added = event.mode() == Event.Mode.PRIVATE;
        return new Registration(
                null,
                event.id(),
                user.id(),
                at,
                added ? admin.id() : user.id(),
                false,
                new Registration.UserCopy(user.name(), user.email()),
                new Registration.EventCopy(event.title(), event.startsAt(), event.status()));
    }
}
