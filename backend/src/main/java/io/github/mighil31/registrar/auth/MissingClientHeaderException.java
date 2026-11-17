package io.github.mighil31.registrar.auth;

/** A cookie-authenticated request without the client header: possibly a cross-site form post. */
public class MissingClientHeaderException extends RuntimeException {

    MissingClientHeaderException() {
        super("Missing X-Requested-With header", null, false, false);
    }
}
