package io.github.mighil31.registrar.event;

import io.github.mighil31.registrar.security.Caller;
import io.github.mighil31.registrar.web.PageResponse;
import io.github.mighil31.registrar.web.ProblemException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
class EventController {

    private final EventService events;

    EventController(EventService events) {
        this.events = events;
    }

    /** {@code scope=browse} (default) for everyone; {@code scope=all} for admins. */
    @GetMapping
    @PreAuthorize("hasAuthority('events:browse')")
    PageResponse<EventResponse> list(
            JwtAuthenticationToken auth,
            @RequestParam(defaultValue = "BROWSE") EventService.Scope scope,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Event.Mode mode,
            @RequestParam(required = false) Event.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return events.list(Caller.of(auth), scope, q, tag, mode, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('events:browse')")
    EventResponse get(JwtAuthenticationToken auth, @PathVariable String id) {
        return events.get(Caller.of(auth), ProblemException.id(id, "Event"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('events:manage')")
    ResponseEntity<EventResponse> create(JwtAuthenticationToken auth, @Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(events.create(Caller.of(auth), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('events:manage')")
    EventResponse update(@PathVariable String id, @Valid @RequestBody EventRequest request) {
        return events.update(ProblemException.id(id, "Event"), request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('events:manage')")
    EventResponse cancel(@PathVariable String id) {
        return events.setStatus(ProblemException.id(id, "Event"), Event.Status.CANCELLED);
    }

    @PostMapping("/{id}/uncancel")
    @PreAuthorize("hasAuthority('events:manage')")
    EventResponse uncancel(@PathVariable String id) {
        return events.setStatus(ProblemException.id(id, "Event"), Event.Status.SCHEDULED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('events:manage')")
    ResponseEntity<Void> delete(@PathVariable String id) {
        events.delete(ProblemException.id(id, "Event"));
        return ResponseEntity.noContent().build();
    }
}
