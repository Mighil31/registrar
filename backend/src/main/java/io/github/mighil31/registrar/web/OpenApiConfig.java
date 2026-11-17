package io.github.mighil31.registrar.web;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI document springdoc builds from the controllers, at {@code /v3/api-docs}. The web
 * client's TypeScript types are generated from it. Every operation needs a bearer token unless it
 * says otherwise ({@code AuthController} does), and every error is a {@code Problem}.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(title = "Registrar API", version = "v1"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(name = OpenApiConfig.BEARER, type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {

    static final String BEARER = "bearer";
    static final String PROBLEM = "Problem";

    /**
     * Errors come from one exception handler, not the controllers, so springdoc can't see them. This
     * gives every operation a {@code default} response: the ProblemDetail body {@link Problems} builds.
     */
    @Bean
    OpenApiCustomizer problemResponses() {
        return openApi -> {
            openApi.getComponents().addSchemas(PROBLEM, problemSchema());
            ApiResponse problem = new ApiResponse()
                    .description("Error (RFC 9457); switch on type, e.g. /problems/last-admin")
                    .content(new Content()
                            .addMediaType(
                                    org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    new MediaType().schema(new Schema<>().$ref(PROBLEM))));
            openApi.getPaths()
                    .values()
                    .forEach(path -> path.readOperations()
                            .forEach(operation -> operation.getResponses().addApiResponse("default", problem)));
        };
    }

    private static Schema<?> problemSchema() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema())
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("requestId", new StringSchema())
                .addProperty("errors", new MapSchema().additionalProperties(new StringSchema()))
                .addRequiredItem("type")
                .addRequiredItem("status");
    }
}
