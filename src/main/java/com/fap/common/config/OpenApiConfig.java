package com.fap.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI fapOpenApi() {
		return new OpenAPI()
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
				.components(new Components()
						.addParameters("AcceptLanguageHeader", new Parameter()
								.in("header")
								.name("Accept-Language")
								.description("Response language. Use 'en' or 'vi'.")
								.example("vi"))
						.addSecuritySchemes("bearerAuth", new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.info(new Info()
						.title("FAP Backend API")
						.version("v1")
						.description("FAP backend service API"));
	}

	@Bean
	OperationCustomizer acceptLanguageHeaderCustomizer() {
		return (operation, handlerMethod) -> operation.addParametersItem(new Parameter()
				.in("header")
				.name("Accept-Language")
				.description("Response language. Use 'en' or 'vi'.")
				.example("en")
				.required(false));
	}
}
