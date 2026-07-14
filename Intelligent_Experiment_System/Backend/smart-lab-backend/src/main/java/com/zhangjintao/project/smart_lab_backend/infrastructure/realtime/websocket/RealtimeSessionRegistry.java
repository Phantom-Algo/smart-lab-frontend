package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RealtimeSessionRegistry {

	private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public void add(WebSocketSession session) {
		sessions.put(session.getId(), session);
	}

	public void remove(WebSocketSession session) {
		if (session != null) {
			sessions.remove(session.getId());
		}
	}

	public void broadcast(String payload) {
		sessions.values().forEach(session -> safeSend(session, payload));
	}

	public void sendToUsers(Collection<Long> userIds, String payload) {
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		Set<Long> targets = Set.copyOf(userIds);
		sessions.values().forEach(session -> {
			Object userId = session.getAttributes().get(RealtimeWebSocketAuth.USER_ID_ATTR);
			if (userId instanceof Long targetUserId && targets.contains(targetUserId)) {
				safeSend(session, payload);
			}
		});
	}

	public int onlineCount() {
		return sessions.size();
	}

	private void safeSend(WebSocketSession session, String payload) {
		if (session == null || !session.isOpen()) {
			remove(session);
			return;
		}
		try {
			session.sendMessage(new TextMessage(payload));
		}
		catch (IOException ex) {
			log.warn("WebSocket 推送失败, sessionId={}", session.getId(), ex);
			remove(session);
		}
	}
}