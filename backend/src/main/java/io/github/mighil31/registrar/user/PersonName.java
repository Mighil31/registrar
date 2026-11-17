package io.github.mighil31.registrar.user;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The kitchensink port's name rules: 1 to 25 characters; letters with their accents, spaces and the
 * punctuation real names use (O'Brien, Jean-Luc, St. John); not only whitespace. Null passes: add
 * {@code @NotNull} where the field is required.
 */
@Size(min = 1, max = 25)
@Pattern(regexp = "[\\p{L}\\p{M} .'’-]*", message = "{name_validation_message}")
@Pattern(regexp = "(?sU)|.*\\S.*", message = "{jakarta.validation.constraints.NotBlank.message}")
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface PersonName {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
