package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 管理员发布消息请求。
 */
@Data
public class PublishMessageRequest {

	@NotBlank(message = "消息类型不能为空")
	@Size(max = 128, message = "消息类型长度不能超过128")
	@JsonAlias("message_type")
	private String messageType;

	@NotBlank(message = "目标类型不能为空")
	@Size(max = 32, message = "目标类型长度不能超过32")
	@JsonAlias("target_type")
	private String targetType;

	@JsonAlias("target_user_ids")
	private List<Long> targetUserIds;

	@Size(max = 128, message = "模板编码长度不能超过128")
	@JsonAlias("template_code")
	private String templateCode;

	private Map<String, Object> params;

	@Size(max = 255, message = "消息标题长度不能超过255")
	private String title;

	private String content;

	@JsonAlias("content_format")
	private ContentFormat contentFormat;

	@Size(max = 512, message = "跳转地址长度不能超过512")
	@JsonAlias("action_url")
	private String actionUrl;

	@JsonIgnore
	@AssertTrue(message = "必须指定 templateCode 或 title/content")
	public boolean isMessageSourcePresent() {
		return hasTemplateCode() || (StringUtils.hasText(title) && StringUtils.hasText(content));
	}

	@JsonIgnore
	@AssertTrue(message = "templateCode 不能与 title/content 同时指定")
	public boolean isMessageSourceExclusive() {
		return !hasTemplateCode() || (!StringUtils.hasText(title) && !StringUtils.hasText(content));
	}

	@JsonIgnore
	@AssertTrue(message = "直接内容模式下不允许传入 params")
	public boolean isParamsOnlyForTemplate() {
		return hasTemplateCode() || params == null || params.isEmpty();
	}

	@JsonIgnore
	public boolean hasTemplateCode() {
		return StringUtils.hasText(templateCode);
	}
}
