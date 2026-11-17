package io.github.mighil31.registrar.auth;

import io.github.mighil31.registrar.user.UserResponse;

/** The access token travels in the body (the client keeps it in memory); the refresh token in a cookie. */
public record AuthResponse(String accessToken, long expiresIn, UserResponse user) {}
