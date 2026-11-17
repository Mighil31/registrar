package io.github.mighil31.registrar.user;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import io.github.mighil31.registrar.auth.AuthService;
import io.github.mighil31.registrar.auth.InvalidRefreshTokenException;
import io.github.mighil31.registrar.db.MongoTransaction;
import io.github.mighil31.registrar.registration.Registrations;
import io.github.mighil31.registrar.web.PageResponse;
import io.github.mighil31.registrar.web.ProblemException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * The People screen: list and search, make or revoke admin, delete.
 *
 * <p>Rules for role changes and deletes: there is always at least one admin, and admins can't
 * change or delete themselves. Both are checked inside one transaction that also
 * <em>writes the caller's own document</em> (see {@link #lockCaller}). Without that write, two admins
 * removing each other at the same moment would each read "the other is still admin", write different
 * documents, and both commit: no admins left (write skew). With it, both transactions write both
 * documents, MongoDB refuses one with a write conflict, and its retry sees the caller is no longer an
 * admin.
 */
@Service
class UserService {

    static final int MAX_PAGE_SIZE = 100;

    private final MongoTemplate mongo;
    private final UserRepository users;
    private final Registrations registrations;
    private final AuthService auth;
    private final Clock clock;

    UserService(MongoTemplate mongo, UserRepository users, Registrations registrations, AuthService auth, Clock clock) {
        this.mongo = mongo;
        this.users = users;
        this.registrations = registrations;
        this.auth = auth;
        this.clock = clock;
    }

    /**
     * By name. {@code q} is the start of any word of the name ("jo" finds Jo Park and Ann Jones), or
     * of the email; any case. {@code role=ADMIN} is the admins, {@code role=MEMBER} everyone else
     * (every admin is also a member).
     */
    PageResponse<PersonResponse> list(String q, Role role, int page, int size) {
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_PAGE_SIZE);
        List<Criteria> filters = new ArrayList<>();
        if (role == Role.ADMIN) {
            filters.add(where("roles").is(Role.ADMIN));
        } else if (role == Role.MEMBER) {
            filters.add(where("roles").ne(Role.ADMIN));
        }
        if (q != null && !q.isBlank()) {
            String prefix = q.strip();
            // Emails are stored lower-case, so an anchored, case-sensitive regex can use the email index.
            // The name regex scans the name index: fine at this size; Atlas Search is the answer at scale.
            filters.add(new Criteria()
                    .orOperator(
                            where("name").regex("(?:^|\\s)" + Pattern.quote(prefix), "i"),
                            where("email").regex("^" + Pattern.quote(User.normalizeEmail(prefix)))));
        }
        Query query = filters.isEmpty() ? new Query() : new Query(new Criteria().andOperator(filters));
        long total = mongo.count(query, User.class);
        query.with(Sort.by("name", "_id")).skip((long) page * size).limit(size);
        List<User> found = mongo.find(query, User.class);
        Map<ObjectId, Integer> counts =
                registrations.countByUser(found.stream().map(User::id).toList());
        List<PersonResponse> items = found.stream()
                .map(u -> PersonResponse.of(u, counts.getOrDefault(u.id(), 0)))
                .toList();
        return new PageResponse<>(items, page, size, total);
    }

    PersonResponse get(ObjectId id) {
        return response(find(id));
    }

    /**
     * Sets a user's roles. Every user keeps MEMBER. Losing ADMIN also ends their sessions; gaining it
     * takes effect at their next token refresh (the new token carries the new permissions).
     */
    @MongoTransaction
    PersonResponse setRoles(ObjectId callerId, ObjectId targetId, Set<Role> roles) {
        if (!roles.contains(Role.MEMBER)) {
            throw ProblemException.invalid("roles", "must include MEMBER");
        }
        boolean removesAdmin = !roles.contains(Role.ADMIN);
        User target = checkedTarget(callerId, targetId, Permissions.ROLES_GRANT, removesAdmin);
        if (target.roles().equals(roles)) {
            return response(target);
        }
        User saved = users.save(target.withRoles(roles, clock.instant()));
        if (target.isAdmin() && removesAdmin) {
            auth.endSessions(targetId);
        }
        return response(saved);
    }

    /** Deletes a user, their registrations (freeing their seats) and their sessions, together. */
    @MongoTransaction
    void delete(ObjectId callerId, ObjectId targetId) {
        checkedTarget(callerId, targetId, Permissions.USERS_MANAGE, true);
        registrations.deleteForUser(targetId);
        mongo.remove(Query.query(where("_id").is(targetId)), User.class);
        auth.endSessions(targetId);
    }

    /**
     * The checks every role change and delete shares, in the caller's transaction. The caller is
     * re-read from the database, not trusted from their token: an admin revoked a minute ago still
     * holds a token that says admin.
     */
    private User checkedTarget(ObjectId callerId, ObjectId targetId, String permission, boolean removesAdmin) {
        User caller = lockCaller(callerId);
        if (!Permissions.of(caller.roles()).contains(permission)) {
            throw ProblemException.forbidden();
        }
        User target = targetId.equals(callerId) ? caller : find(targetId);
        if (removesAdmin && target.isAdmin() && admins() == 1) {
            throw ProblemException.conflict("last-admin", "There must always be at least one admin");
        }
        if (target.id().equals(caller.id())) {
            throw new ProblemException(
                    HttpStatus.FORBIDDEN, "own-account", "Admins can't change their own role or delete themselves");
        }
        return target;
    }

    /**
     * Reads the caller and writes their document in the same step (bumps {@code version}), so a
     * concurrent transaction that changes the caller conflicts with this one: MongoDB's version of
     * {@code SELECT ... FOR UPDATE}. A caller who no longer exists gets 401, like a dead session.
     */
    private User lockCaller(ObjectId callerId) {
        User caller = mongo.findAndModify(
                Query.query(where("_id").is(callerId)),
                new Update().inc("version", 1),
                FindAndModifyOptions.options().returnNew(true),
                User.class);
        if (caller == null) {
            throw new InvalidRefreshTokenException();
        }
        return caller;
    }

    private long admins() {
        return mongo.count(Query.query(where("roles").is(Role.ADMIN)), User.class);
    }

    private User find(ObjectId id) {
        return users.findById(id).orElseThrow(() -> ProblemException.notFound("User"));
    }

    private PersonResponse response(User user) {
        return PersonResponse.of(
                user, registrations.countByUser(List.of(user.id())).getOrDefault(user.id(), 0));
    }
}
