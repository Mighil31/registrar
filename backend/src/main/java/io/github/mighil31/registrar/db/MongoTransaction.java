package io.github.mighil31.registrar.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the method in one MongoDB transaction, and runs it again if MongoDB says that's safe.
 *
 * <p>MongoDB doesn't make a second writer wait: when two transactions write the same document (two
 * people taking the last seat), one fails with a write conflict and has to start over. Spring
 * applies the retry outside the transaction, so each attempt gets a fresh one. Methods using this
 * must be safe to run twice: database work only, no emails or calls to other services.
 *
 * <p>A commit whose reply was lost also re-runs the whole method. If that commit had in fact
 * succeeded, the second run sees its result (for example "already registered").
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
@Retryable(predicate = TransientTransactionError.class, maxRetries = 20, delay = 10, jitter = 5)
public @interface MongoTransaction {}
