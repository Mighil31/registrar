package io.github.mighil31.registrar.auth;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank String email, @NotBlank String password) {}
