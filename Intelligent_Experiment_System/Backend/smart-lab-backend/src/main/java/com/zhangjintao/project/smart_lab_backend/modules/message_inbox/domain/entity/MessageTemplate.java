package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息模板领域实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplate {

	private Long id;

	private String templateCode;

	private String templateName;

	private String messageType;

	private String titleTemplate;

	private String contentTemplate;

	private ContentFormat contentFormat;

	private String paramSchema;

	private TemplateStatus status;

	private Long createdBy;

	private Long updatedBy;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
