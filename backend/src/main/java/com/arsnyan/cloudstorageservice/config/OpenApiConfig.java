package com.arsnyan.cloudstorageservice.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
    name = "Session",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "SESSION"
)
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("Cloud Storage Service API").version("1.0"))
            .addSecurityItem(new SecurityRequirement().addList("Session"));
    }

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(path -> {
            path.readOperations().forEach(operation -> {
                var responses = operation.getResponses();

                responses.addApiResponse(
                    "500",
                    new ApiResponse()
                        .description("Internal Server Error")
                        .content(new Content().addMediaType(
                            "application/json",
                            new MediaType()
                        ))
                );
            });
        });
    }
}
