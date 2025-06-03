package org.example.config.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Coupon Service API", version = "v1"))
@SuppressWarnings("unused")
public class OpenApiConfig {
}