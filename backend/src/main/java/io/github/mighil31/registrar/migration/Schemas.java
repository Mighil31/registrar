package io.github.mighil31.registrar.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;
import org.bson.Document;

/**
 * Creates a collection with a {@code $jsonSchema} validator: a backstop for writes that bypass the
 * app. It checks shape and types only, never more than the app's own validation, so a request the
 * API accepts can't fail at write time.
 */
final class Schemas {

    private Schemas() {}

    static void createWithValidator(MongoDatabase database, String collection, String jsonSchema) {
        database.createCollection(
                collection,
                new CreateCollectionOptions()
                        .validationOptions(new ValidationOptions()
                                .validator(new Document("$jsonSchema", Document.parse(jsonSchema)))
                                .validationLevel(ValidationLevel.STRICT)
                                .validationAction(ValidationAction.ERROR)));
    }
}
