package io.github.mighil31.registrar.user;

import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, ObjectId> {

    /** Expects a normalized (lower-case) email. */
    Optional<User> findByEmail(String email);

    boolean existsByRoles(Role role);
}
