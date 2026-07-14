package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class RealtimeEvent {

	String event;
	String source;
	RealtimeTargetType targetType;
	List<Long> targetUserIds;
	Object data;
	LocalDateTime occurredAt;
}