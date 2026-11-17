package io.github.mighil31.registrar.auth;

import io.github.mighil31.registrar.user.EmailAddress;
import io.github.mighil31.registrar.user.PersonName;
import io.github.mighil31.registrar.user.PhoneNumber;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Password: 12 to 64 characters (bcrypt reads at most 72 bytes; 64 characters stays clear of it). */
public record SignUpRequest(
        @NotNull @PersonName String name,
        @NotNull @EmailAddress String email,
        @NotNull @PhoneNumber String phoneNumber,
        @NotNull @Size(min = 12, max = 64) String password) {}
