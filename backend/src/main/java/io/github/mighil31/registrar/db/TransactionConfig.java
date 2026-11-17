package io.github.mighil31.registrar.db;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Spring Boot doesn't create a MongoDB transaction manager; {@code @Transactional} needs one. */
@Configuration
@EnableResilientMethods
class TransactionConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
