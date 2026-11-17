package io.github.mighil31.registrar.me;

/** The current password given to change it is wrong. */
public class WrongPasswordException extends RuntimeException {

    WrongPasswordException() {
        super("Current password is wrong", null, false, false);
    }
}
