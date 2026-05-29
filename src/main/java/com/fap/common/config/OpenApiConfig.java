package com.fap.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "FAP Backend API",
				version = "v1",
				description = "FAP backend service API for authentication, training management, syllabus, quiz, notifications, settings, and audit logs.",
				contact = @Contact(name = "FAP Backend Team")),
		tags = {
				@Tag(name = "Authentication"),
				@Tag(name = "Users"),
				@Tag(name = "Roles and Permissions"),
				@Tag(name = "Syllabus"),
				@Tag(name = "Materials"),
				@Tag(name = "Training Programs"),
				@Tag(name = "Classes"),
				@Tag(name = "Training Sessions"),
				@Tag(name = "Training Feedback"),
				@Tag(name = "My Training"),
				@Tag(name = "My Learning"),
				@Tag(name = "Questions"),
				@Tag(name = "Quizzes"),
				@Tag(name = "Quiz Attempts"),
				@Tag(name = "Quiz Results"),
				@Tag(name = "Notifications"),
				@Tag(name = "Settings"),
				@Tag(name = "Audit Logs")
		})
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		in = SecuritySchemeIn.HEADER,
		scheme = "bearer",
		bearerFormat = "JWT")
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
						.addSecuritySchemes("bearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
								.type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.info(new io.swagger.v3.oas.models.info.Info()
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
