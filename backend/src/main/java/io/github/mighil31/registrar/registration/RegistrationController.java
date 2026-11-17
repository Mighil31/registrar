package io.github.mighil31.registrar.registration;

import io.github.mighil31.registrar.event.Event;
import io.github.mighil31.registrar.security.Caller;
import io.github.mighil31.registrar.web.ProblemException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Members register and leave; admins see, add and remove attendees. */
@RestController
@RequestMapping("/api/v1")
class RegistrationController {

    private final RegistrationService registrations;

    RegistrationController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    /** One of my events, from the copies in the registration. */
    record MyRegistration(String eventId, String title, Instant startsAt, Event.Status status, Instant registeredAt) {

        static MyRegistration of(Registration r) {
            return new MyRegistration(
                    r.eventId().toHexString(),
                    r.event().title(),
                    r.event().startsAt(),
                    r.event().status(),
                    r.registeredAt());
        }
    }

    /** One row of an event's attendee list. Admins only: members see counts, not names. */
    record Attendee(
            String userId,
            String name,
            String email,
            Instant registeredAt,
            boolean addedByAdmin,
            boolean overCapacity) {

        static Attendee of(Registration r) {
            return new Attendee(
                    r.userId().toHexString(),
                    r.user().name(),
                    r.user().email(),
                    r.registeredAt(),
                    !r.addedBy().equals(r.userId()),
                    r.overCapacity());
        }
    }

    record AddRequest(
            @NotEmpty @Size(max = RegistrationService.MAX_BULK_ADD)
            List<String> userIds) {}

    enum When {
        UPCOMING,
        PAST
    }

    @PostMapping("/events/{id}/registrations/me")
    @PreAuthorize("hasAuthority('registrations:self')")
    ResponseEntity<MyRegistration> register(JwtAuthenticationToken auth, @PathVariable String id) {
        Registration registration =
                registrations.register(eventId(id), Caller.of(auth).id());
        return ResponseEntity.status(HttpStatus.CREATED).body(MyRegistration.of(registration));
    }

    @DeleteMapping("/events/{id}/registrations/me")
    @PreAuthorize("hasAuthority('registrations:self')")
    ResponseEntity<Void> leave(JwtAuthenticationToken auth, @PathVariable String id) {
        registrations.leave(eventId(id), Caller.of(auth).id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/registrations")
    @PreAuthorize("hasAuthority('registrations:self')")
    List<MyRegistration> mine(JwtAuthenticationToken auth, @RequestParam(defaultValue = "UPCOMING") When when) {
        return registrations.mine(Caller.of(auth).id(), when == When.UPCOMING).stream()
                .map(MyRegistration::of)
                .toList();
    }

    @GetMapping("/events/{id}/registrations")
    @PreAuthorize("hasAuthority('registrations:manage')")
    List<Attendee> attendees(@PathVariable String id) {
        return registrations.attendees(eventId(id)).stream().map(Attendee::of).toList();
    }

    @PostMapping("/events/{id}/registrations")
    @PreAuthorize("hasAuthority('registrations:manage')")
    RegistrationService.AddResult add(
            JwtAuthenticationToken auth, @PathVariable String id, @Valid @RequestBody AddRequest request) {
        return registrations.add(eventId(id), request.userIds(), Caller.of(auth).id());
    }

    @DeleteMapping("/events/{id}/registrations/{userId}")
    @PreAuthorize("hasAuthority('registrations:manage')")
    ResponseEntity<Void> remove(@PathVariable String id, @PathVariable String userId) {
        registrations.remove(eventId(id), ProblemException.id(userId, "Registration"));
        return ResponseEntity.noContent().build();
    }

    private static ObjectId eventId(String id) {
        return ProblemException.id(id, "Event");
    }
}
