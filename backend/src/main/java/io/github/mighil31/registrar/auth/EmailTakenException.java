package io.github.mighil31.registrar.auth;

public class EmailTakenException extends RuntimeException {

    EmailTakenException() {
        super("Email taken", null, false, false);
    }
}
