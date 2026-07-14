package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime;

import com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.model.RealtimeEvent;

public interface RealtimePublisher {

	void publish(RealtimeEvent event);

	void publishAfterCommit(RealtimeEvent event);
}