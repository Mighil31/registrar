package io.github.mighil31.registrar.auth;

/** Unknown email or wrong password: deliberately the same exception, so the response can't tell them apart. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email or password is wrong", null, false, false);
    }
}
