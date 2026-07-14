package com.zhangjintao.project.smart_lab_backend.infrastructure.config;

import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.websocket.AuthTokenHandshakeInterceptor;
import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.websocket.RealtimeWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

	private final RealtimeWebSocketHandler realtimeWebSocketHandler;
	private final AuthTokenHandshakeInterceptor authTokenHandshakeInterceptor;

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(realtimeWebSocketHandler, "/ws/events")
				.addInterceptors(authTokenHandshakeInterceptor)
				.setAllowedOriginPatterns("*");
	}
}