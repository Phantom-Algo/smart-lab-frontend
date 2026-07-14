package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DeadLetterStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.FailureStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 死信消息领域实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeadLetter {

	private Long id;

	private Long eventId;

	private String eventNo;

	private FailureStage failureStage;

	private String errorCode;

	private String errorMessage;

	private String rawPayload;

	private boolean retryable;

	private DeadLetterStatus status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
