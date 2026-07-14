package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.websocket;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

	private final RealtimeSessionRegistry sessionRegistry;

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		AuthenticatedUser authenticatedUser = (AuthenticatedUser) session.getAttributes()
				.get(RealtimeWebSocketAuth.AUTH_USER_ATTR);
		if (authenticatedUser == null) {
			session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
			return;
		}
		sessionRegistry.add(session);
		log.info("WebSocket 连接建立, sessionId={}, userId={}, onlineCount={}",
				session.getId(), authenticatedUser.getUserId(), sessionRegistry.onlineCount());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		if ("PING".equalsIgnoreCase(message.getPayload())) {
			safeSend(session, "PONG");
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessionRegistry.remove(session);
		log.info("WebSocket 连接关闭, sessionId={}, status={}, onlineCount={}",
				session.getId(), status, sessionRegistry.onlineCount());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		log.warn("WebSocket 连接异常, sessionId={}", session.getId(), exception);
		sessionRegistry.remove(session);
		if (session.isOpen()) {
			session.close(CloseStatus.SERVER_ERROR);
		}
	}

	private void safeSend(WebSocketSession session, String payload) {
		try {
			if (session.isOpen()) {
				session.sendMessage(new TextMessage(payload));
			}
		}
		catch (IOException ex) {
			log.warn("WebSocket 心跳响应失败, sessionId={}", session.getId(), ex);
		}
	}
}