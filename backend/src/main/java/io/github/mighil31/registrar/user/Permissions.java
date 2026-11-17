package io.github.mighil31.registrar.user;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * What each role may do. Endpoints check these permissions ({@code hasAuthority('events:manage')}),
 * never role names, so a new role is a change to this map only.
 */
public final class Permissions {

    public static final String PROFILE_SELF = "profile:self";
    public static final String EVENTS_BROWSE = "events:browse";
    public static final String REGISTRATIONS_SELF = "registrations:self";
    public static final String EVENTS_MANAGE = "events:manage";
    public static final String REGISTRATIONS_MANAGE = "registrations:manage";
    public static final String USERS_READ = "users:read";
    public static final String USERS_MANAGE = "users:manage";
    public static final String ROLES_GRANT = "roles:grant";
    public static final String DASHBOARD_READ = "dashboard:read";

    private static final Map<Role, Set<String>> BY_ROLE = Map.of(
            Role.MEMBER, Set.of(PROFILE_SELF, EVENTS_BROWSE, REGISTRATIONS_SELF),
            Role.ADMIN,
                    Set.of(EVENTS_MANAGE, REGISTRATIONS_MANAGE, USERS_READ, USERS_MANAGE, ROLES_GRANT, DASHBOARD_READ));

    private Permissions() {}

    /** The union of the roles' permissions, sorted (stable token claims). */
    public static Set<String> of(Collection<Role> roles) {
        Set<String> permissions = new TreeSet<>();
        roles.forEach(role -> permissions.addAll(BY_ROLE.get(role)));
        return permissions;
    }
}
