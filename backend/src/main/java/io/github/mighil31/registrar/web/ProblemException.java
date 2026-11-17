package io.github.mighil31.registrar.web;

import java.util.Map;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;

/** An expected refusal, sent as ProblemDetail with a stable {@code type}. */
public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final Map<String, String> errors;

    public ProblemException(HttpStatus status, String type, String detail, Map<String, String> errors) {
        super(detail, null, false, false);
        this.status = status;
        this.type = type;
        this.errors = errors;
    }

    public ProblemException(HttpStatus status, String type, String detail) {
        this(status, type, detail, null);
    }

    public static ProblemException notFound(String what) {
        return new ProblemException(HttpStatus.NOT_FOUND, "not-found", what + " not found");
    }

    public static ProblemException forbidden() {
        return new ProblemException(HttpStatus.FORBIDDEN, "forbidden", "Access denied");
    }

    public static ProblemException conflict(String type, String detail) {
        return new ProblemException(HttpStatus.CONFLICT, type, detail);
    }

    public static ProblemException invalid(String field, String message) {
        return new ProblemException(
                HttpStatus.BAD_REQUEST, "validation-error", "Validation failed", Map.of(field, message));
    }

    /** A path id that isn't an ObjectId can't name anything: 404, not 400. */
    public static ObjectId id(String value, String what) {
        if (value == null || !ObjectId.isValid(value)) {
            throw notFound(what);
        }
        return new ObjectId(value);
    }

    HttpStatus status() {
        return status;
    }

    String type() {
        return type;
    }

    Map<String, String> errors() {
        return errors;
    }
}
