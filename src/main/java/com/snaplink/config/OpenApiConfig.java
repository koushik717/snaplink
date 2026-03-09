package com.snaplink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI snapLinkOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SnapLink API")
                        .description("High-Throughput URL Shortener API")
                        .version("1.0.0")
                        .contact(new Contact().name("SnapLink")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
                .schemaRequirement("ApiKeyAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-API-Key"));
    }
}
