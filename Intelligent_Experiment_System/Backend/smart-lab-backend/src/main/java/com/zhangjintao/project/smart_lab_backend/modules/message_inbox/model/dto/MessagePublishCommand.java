package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessagePublishCommand {

	private String eventNo;

	private String sourceSystem;

	private String messageType;

	private TargetType targetType;

	private List<Long> targetUserIds;

	/**
	 * 模板ID
	 * 
	 * @deprecated 业务子系统发布消息时不建议使用 templateId 数据库自增ID，请优先使用 templateCode
	 */
	@Deprecated
	private Long templateId;

	/**
	 * 模版编码
	 * 
	 * @apiNote 业务子系统发布消息时建议使用 templateCode 模板编码，且模板编码一般与业务子系统的消息类型相同
	 */
	private String templateCode;

	private TempMessageTemplate tempTemplate;

	private Map<String, Object> params;

	private String actionUrl;

	private LocalDateTime occurredAt;
}
