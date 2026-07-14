package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeachingTaskPublishRequest {

	private LocalDateTime deadline;
}
