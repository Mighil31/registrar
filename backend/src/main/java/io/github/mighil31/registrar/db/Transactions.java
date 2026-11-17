package io.github.mighil31.registrar.db;

import com.mongodb.MongoException;
import java.util.function.Supplier;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs work in one MongoDB transaction. Two transactions touching the same document (two people
 * taking the last seat) make one of them fail with a write conflict, labelled
 * {@code TransientTransactionError}; that one is retried from the start, as MongoDB recommends.
 */
@Component
public class Transactions {

    static final int MAX_ATTEMPTS = 20;

    private final TransactionTemplate template;

    Transactions(MongoDatabaseFactory factory) {
        this.template = new TransactionTemplate(new MongoTransactionManager(factory));
    }

    public <T> T run(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return template.execute(status -> work.get());
            } catch (RuntimeException e) {
                if (attempt >= MAX_ATTEMPTS || !isTransient(e)) {
                    throw e;
                }
            }
        }
    }

    public void run(Runnable work) {
        run(() -> {
            work.run();
            return null;
        });
    }

    static boolean isTransient(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof MongoException mongo
                    && (mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
                            || mongo.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL))) {
                return true;
            }
        }
        return false;
    }
}
