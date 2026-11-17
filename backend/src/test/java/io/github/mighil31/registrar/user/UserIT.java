package io.github.mighil31.registrar.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.mighil31.registrar.Api;
import io.github.mighil31.registrar.ApiIT;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuppressWarnings({"rawtypes", "unchecked"})
class UserIT extends ApiIT {

    private static final Random RANDOM = new Random();

    @Test
    void searchesByNameOrEmailPrefixAndFiltersByRole() {
        String family = family();
        Account member = member("Ana " + family);
        Account other = member("Ben " + family);
        Account promoted = newAdmin("Cy " + family);
        register(id(event()), member);

        assertThat(people("q=ana " + family.toLowerCase()))
                .extracting(p -> p.get("id"), p -> p.get("eventCount"))
                .containsExactly(tuple(member.id(), 1));
        assertThat(people("q=" + member.email().substring(0, 12).toUpperCase()))
                .extracting(p -> p.get("id"))
                .containsExactly(member.id());
        assertThat(people("q=" + family + "&role=ADMIN"))
                .extracting(p -> p.get("id"))
                .containsExactly(promoted.id());
        assertThat(people("q=" + family + "&role=MEMBER"))
                .extracting(p -> p.get("id"))
                .containsExactly(member.id(), other.id());
        assertThat(people("q=" + family).getFirst())
                .containsEntry("name", "Ana " + family)
                .containsEntry("roles", List.of("MEMBER"))
                .doesNotContainKey("passwordHash");
    }

    @Test
    void listsInPages() {
        String family = family();
        for (String first : List.of("Ann", "Bob", "Cat")) {
            member(first + " " + family);
        }

        Map page = api.get("/api/v1/users?q=" + family + "&page=1&size=2", admin.auth())
                .getBody();

        assertThat(page).containsEntry("total", 3).containsEntry("page", 1).containsEntry("size", 2);
        assertThat((List<Map>) page.get("items")).extracting(p -> p.get("name")).containsExactly("Cat " + family);
    }

    @Test
    void grantWorksFromTheNextTokenAndRevokeEndsTheirSessions() {
        Account member = member();
        assertThat(api.get("/api/v1/users", member.auth()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> granted = setRoles(member.id(), admin, "MEMBER", "ADMIN");
        assertThat(granted.getBody()).containsEntry("roles", List.of("MEMBER", "ADMIN"));
        ResponseEntity<Map> signIn =
                api.post("/api/v1/auth/signin", Map.of("email", member.email(), "password", PASSWORD));
        String adminToken = (String) signIn.getBody().get("accessToken");
        assertThat(api.get("/api/v1/users", Api.bearer(adminToken)).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> revoked = setRoles(member.id(), admin, "MEMBER");
        assertThat(revoked.getBody()).containsEntry("roles", List.of("MEMBER"));
        ResponseEntity<Map> refresh = api.post("/api/v1/auth/refresh", null, Api.withCookie(Api.refreshToken(signIn)));
        assertThat(refresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aRevokedAdminsOldTokenCantChangeRolesOrDelete() {
        Account demoted = newAdmin("Dee Moted");
        setRoles(demoted.id(), admin, "MEMBER");
        Account victim = member();

        // The token still says admin for up to 10 minutes; these endpoints check the database.
        assertThat(setRoles(victim.id(), demoted, "MEMBER", "ADMIN").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.delete("/api/v1/users/" + victim.id(), demoted.auth()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(getPerson(victim.id())).containsEntry("roles", List.of("MEMBER"));
    }

    @Test
    void adminsCantChangeOrDeleteThemselves() {
        Account second = newAdmin("Sec Ond");

        ResponseEntity<Map> revoke = setRoles(second.id(), second, "MEMBER");
        ResponseEntity<Map> delete = api.delete("/api/v1/users/" + second.id(), second.auth());

        assertThat(revoke.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(type(revoke)).isEqualTo("/problems/own-account");
        assertThat(type(delete)).isEqualTo("/problems/own-account");
        assertThat(getPerson(second.id())).containsEntry("roles", List.of("MEMBER", "ADMIN"));
    }

    @Test
    void theLastAdminCantBeRevokedOrDeleted() {
        for (Map other : people("role=ADMIN&size=100")) {
            if (!other.get("id").equals(admin.id())) {
                setRoles((String) other.get("id"), admin, "MEMBER");
            }
        }

        ResponseEntity<Map> revoke = setRoles(admin.id(), admin, "MEMBER");
        ResponseEntity<Map> delete = api.delete("/api/v1/users/" + admin.id(), admin.auth());

        assertThat(revoke.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(type(revoke)).isEqualTo("/problems/last-admin");
        assertThat(type(delete)).isEqualTo("/problems/last-admin");
        assertThat(people("role=ADMIN")).extracting(p -> p.get("id")).containsExactly(admin.id());
    }

    @Test
    void deletingAUserFreesTheirSeatsAndEndsTheirSessions() {
        String full = id(event(Map.of("capacity", 1)));
        String other = id(event());
        Account member = member();
        register(full, member);
        register(other, member);
        assertThat(api.list("/api/v1/users/" + member.id() + "/registrations", admin.auth())
                        .getBody())
                .hasSize(2);

        assertThat(api.delete("/api/v1/users/" + member.id(), admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getEvent(full, admin)).containsEntry("registeredCount", 0);
        assertThat(getEvent(other, admin)).containsEntry("registeredCount", 0);
        assertThat(api.list("/api/v1/events/" + full + "/registrations", admin.auth())
                        .getBody())
                .isEmpty();
        assertThat(register(full, member()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(api.get("/api/v1/users/" + member.id(), admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.get("/api/v1/me", member.auth()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(api.delete("/api/v1/users/" + member.id(), admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rolesMustIncludeMemberAndNameARealUser() {
        Account member = member();

        ResponseEntity<Map> adminOnly = setRoles(member.id(), admin, "ADMIN");
        ResponseEntity<Map> none = setRoles(member.id(), admin);

        assertThat(adminOnly.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, String>) adminOnly.getBody().get("errors")).containsOnlyKeys("roles");
        assertThat(none.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(setRoles("0123456789abcdef01234567", admin, "MEMBER").getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.get("/api/v1/users/not-an-id", admin.auth()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(api.get("/api/v1/users/0123456789abcdef01234567/registrations", admin.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void membersCantSeeOrChangePeople() {
        Account member = member();
        Account other = member();

        assertThat(api.get("/api/v1/users", member.auth()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.get("/api/v1/users/" + other.id(), member.auth()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.get("/api/v1/users/" + other.id() + "/registrations", member.auth())
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(setRoles(member.id(), member, "MEMBER", "ADMIN").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.delete("/api/v1/users/" + other.id(), member.auth()).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(api.get("/api/v1/users").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private List<Map> people(String query) {
        return (List<Map>)
                api.get("/api/v1/users?" + query, admin.auth()).getBody().get("items");
    }

    private Map getPerson(String id) {
        return api.get("/api/v1/users/" + id, admin.auth()).getBody();
    }

    /** A surname no other test uses, so searches find only this test's people. */
    private static String family() {
        StringBuilder name = new StringBuilder("Q");
        for (int i = 0; i < 10; i++) {
            name.append((char) ('a' + RANDOM.nextInt(26)));
        }
        return name.toString();
    }
}
