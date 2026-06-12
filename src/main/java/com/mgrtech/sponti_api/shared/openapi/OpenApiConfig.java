package com.mgrtech.sponti_api.shared.openapi;

import com.mgrtech.sponti_api.shared.error.ApiErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    private static final String APPLICATION_JSON = "application/json";
    private static final String ERROR_RESPONSE_SCHEMA = "ApiError";
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    OpenAPI spontiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sponti API")
                        .version("v1")
                        .description("""
                                Private social matching API for Sponti.

                                Contract notes for mobile clients:
                                - DTO field names are stable within v1.
                                - Errors use RFC 9457 ProblemDetail JSON plus stable `code` and `path` properties.
                                - Clients should branch on `code`, not on localized or human-readable `detail`.
                                """)
                        .contact(new Contact().name("MGR Tech"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .schemaRequirement(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));

    }

    @Bean
    OpenApiCustomizer apiContractCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(ERROR_RESPONSE_SCHEMA, errorSchema());

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().forEach((path, pathItem) ->
                    pathItem.readOperations().forEach(operation -> hardenOperation(path, operation))
            );
        };
    }

    private void hardenOperation(String path, Operation operation) {
        if (operation.getResponses() == null) {
            return;
        }

        operation.getResponses().addApiResponse("400", errorResponse(
                "Invalid request. Stable codes include VALIDATION_ERROR, MALFORMED_JSON, and INVALID_REQUEST.",
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                "Invalid request",
                path
        ));
        operation.getResponses().addApiResponse("500", errorResponse(
                "Unexpected server error. Mobile clients should show a generic retry message.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.INTERNAL_SERVER_ERROR,
                "Internal server error",
                path
        ));

        if (requiresBearer(operation)) {
            operation.getResponses().addApiResponse("401", errorResponse(
                    "Authentication is missing, expired, or invalid.",
                    HttpStatus.UNAUTHORIZED,
                    ApiErrorCode.AUTHENTICATION_REQUIRED,
                    "Authentication required",
                    path
            ));
        }
    }

    private boolean requiresBearer(Operation operation) {
        return operation.getSecurity() == null || !operation.getSecurity().isEmpty();
    }

    private ApiResponse errorResponse(
            String description,
            HttpStatus status,
            ApiErrorCode code,
            String detail,
            String path
    ) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(
                        APPLICATION_JSON,
                        new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + ERROR_RESPONSE_SCHEMA))
                                .addExamples(code.value(), errorExample(status, code, detail, path))
                ));
    }

    private Example errorExample(HttpStatus status, ApiErrorCode code, String detail, String path) {
        return new Example()
                .summary(code.value())
                .value(Map.of(
                        "type", "about:blank",
                        "title", status.getReasonPhrase(),
                        "status", status.value(),
                        "detail", detail,
                        "code", code.value(),
                        "path", path
                ));
    }

    private Schema<?> errorSchema() {
        return new ObjectSchema()
                .description("Stable error response. `code` is the mobile-safe discriminator.")
                .addProperty("type", new StringSchema().example("about:blank"))
                .addProperty("title", new StringSchema().example("Bad Request"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Invalid request"))
                .addProperty("code", new StringSchema()
                        .description("Stable API error code. Mobile clients should branch on this value.")
                        ._enum(Arrays.stream(ApiErrorCode.values()).map(ApiErrorCode::value).toList())
                        .example(ApiErrorCode.VALIDATION_ERROR.value()))
                .addProperty("path", new StringSchema().example("/api/v1/auth/login"));
    }
}
