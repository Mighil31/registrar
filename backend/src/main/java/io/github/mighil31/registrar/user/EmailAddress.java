package io.github.mighil31.registrar.user;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The port's email rules: {@code @Email}, plus a domain with a dot and a final label of 2+
 * characters ({@code a@gmail} is a typo, not an address; an IP literal stays valid).
 */
@NotEmpty
@Email
@Pattern(regexp = "(?s)|.*@(?:\\[.*]|.+\\.[^.@]{2,})", message = "{jakarta.validation.constraints.Email.message}")
@Constraint(validatedBy = {})
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface EmailAddress {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
