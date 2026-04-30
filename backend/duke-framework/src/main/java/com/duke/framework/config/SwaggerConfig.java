package com.duke.framework.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
public class SwaggerConfig {

    private final SwaggerProperties properties;

    public SwaggerConfig(SwaggerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title(properties.getTitle())
                        .description(properties.getDescription())
                        .version(properties.getVersion()));

        if (!properties.getServerUrl().isEmpty()) {
            openAPI.servers(List.of(new Server().url(properties.getServerUrl())));
        }

        if (properties.isEnableSecurity()) {
            openAPI.addSecurityItem(new SecurityRequirement().addList("Bearer"))
                    .components(new Components()
                            .addSecuritySchemes("Bearer", new SecurityScheme()
                                    .name("Authorization")
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")));
        }

        return openAPI;
    }
}
