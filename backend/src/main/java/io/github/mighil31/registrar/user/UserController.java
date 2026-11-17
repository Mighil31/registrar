package io.github.mighil31.registrar.user;

import io.github.mighil31.registrar.security.Caller;
import io.github.mighil31.registrar.web.PageResponse;
import io.github.mighil31.registrar.web.ProblemException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** People, for admins. A user's registrations are in {@code RegistrationController}. */
@RestController
@RequestMapping("/api/v1/users")
class UserController {

    private final UserService users;

    UserController(UserService users) {
        this.users = users;
    }

    /** The whole set of roles the user should have; MEMBER is required. */
    record RolesRequest(@NotEmpty Set<Role> roles) {}

    /** {@code role=ADMIN}: admins; {@code role=MEMBER}: everyone who isn't an admin; none: all. */
    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    PageResponse<PersonResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return users.list(q, role, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    PersonResponse get(@PathVariable String id) {
        return users.get(userId(id));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('roles:grant')")
    PersonResponse setRoles(
            JwtAuthenticationToken auth, @PathVariable String id, @Valid @RequestBody RolesRequest request) {
        return users.setRoles(Caller.of(auth).id(), userId(id), request.roles());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('users:manage')")
    ResponseEntity<Void> delete(JwtAuthenticationToken auth, @PathVariable String id) {
        users.delete(Caller.of(auth).id(), userId(id));
        return ResponseEntity.noContent().build();
    }

    private static ObjectId userId(String id) {
        return ProblemException.id(id, "User");
    }
}
