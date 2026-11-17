package io.github.mighil31.registrar.me;

import io.github.mighil31.registrar.user.PersonName;
import io.github.mighil31.registrar.user.PhoneNumber;

/** Both optional: only the fields sent change. Email is the sign-in identity and can't change here. */
public record UpdateProfileRequest(
        @PersonName String name, @PhoneNumber String phoneNumber) {}
