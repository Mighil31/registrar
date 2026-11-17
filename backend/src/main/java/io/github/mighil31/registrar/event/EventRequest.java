package io.github.mighil31.registrar.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * The event editor's form, for create and edit. {@code version} is required on edit: it's the
 * version the admin loaded, so a save over someone else's change is refused.
 */
public record EventRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 5000) String description,
        @NotNull Event.Format format,
        @Size(max = 200) String location,

        @Size(max = 500) @Pattern(regexp = "https://\\S+", message = "must be an https:// link")
        String meetingLink,

        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull Event.Mode mode,
        @Min(1) @Max(100_000) Integer capacity,

        @Size(max = 5)
        List<@Pattern(regexp = "[a-z0-9-]{1,20}", message = "use 1–20 lower-case letters, digits or -") String> tags,

        Long version) {}
