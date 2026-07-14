package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RealtimeEnvelope {

	String event;
	String source;
	LocalDateTime occurredAt;
	Object data;
}