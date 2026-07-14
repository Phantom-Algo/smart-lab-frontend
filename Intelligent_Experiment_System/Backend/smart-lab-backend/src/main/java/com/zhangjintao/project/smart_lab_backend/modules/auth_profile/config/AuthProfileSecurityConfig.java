package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.exception.CommonErrorCode;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProfileProperties.class)
public class AuthProfileSecurityConfig {

	private final AuthTokenAuthenticationFilter authTokenAuthenticationFilter;
	private final ObjectMapper objectMapper;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
		httpSecurity
				.csrf(csrf -> csrf.disable())
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(
								"/api/auth/login",
								"/api/iot/sensor-data",
								"/api/iot/labs/*/summary",
								"/ws/events",
								// Unity 3D 大屏展示需要的公开 API（实验室/设备/物资查询 + 预约提交）
								"/api/lab-management/labs",
								"/api/lab-management/reservations",
								"/api/equipment/devices",
								"/api/equipment/devices/*",
								"/api/equipment/borrows",
								"/api/equipment/repairs",
								"/api/equipment/alerts",
								"/api/material/materials",
								"/api/material/materials/*",
								"/api/material/receives",
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/v3/api-docs.yaml",
								"/error")
						.permitAll()
						.anyRequest()
						.authenticated())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.authenticationEntryPoint((request, response, authException) ->
								writeErrorResponse(response, CommonErrorCode.UNAUTHORIZED))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								writeErrorResponse(response, CommonErrorCode.FORBIDDEN)))
				.addFilterBefore(authTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return httpSecurity.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	private void writeErrorResponse(HttpServletResponse response, CommonErrorCode errorCode) throws java.io.IOException {
		response.setStatus(errorCode.getHttpStatus().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ApiResult.fail(errorCode));
	}
}
