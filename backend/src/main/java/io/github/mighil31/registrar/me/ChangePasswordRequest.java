package io.github.mighil31.registrar.me;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotNull @Size(min = 12, max = 64) String newPassword) {}
