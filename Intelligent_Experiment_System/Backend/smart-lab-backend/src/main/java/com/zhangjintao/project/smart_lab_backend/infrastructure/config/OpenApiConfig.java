package com.zhangjintao.project.smart_lab_backend.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI smartLabOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Smart Lab Backend API")
						.description("Smart Lab 后端接口文档")
						.version("0.0.1"));
	}
}
