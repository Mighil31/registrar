package io.github.mighil31.registrar.auth;

/** Missing, expired, revoked or already used. The client must sign in again. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Session expired; sign in again", null, false, false);
    }
}
