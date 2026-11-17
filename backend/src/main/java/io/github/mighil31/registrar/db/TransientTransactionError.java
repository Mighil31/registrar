package io.github.mighil31.registrar.db;

import com.mongodb.MongoException;
import java.lang.reflect.Method;
import org.springframework.resilience.retry.MethodRetryPredicate;

/**
 * True when MongoDB says a transaction is safe to run again: a write conflict with another
 * transaction ({@code TransientTransactionError}), or a commit whose result was lost
 * ({@code UnknownTransactionCommitResult}). Checks the labels rather than Spring's exception types.
 */
public class TransientTransactionError implements MethodRetryPredicate {

    @Override
    public boolean shouldRetry(Method method, Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof MongoException mongo
                    && (mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                            || mongo.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL))) {
                return true;
            }
        }
        return false;
    }
}
