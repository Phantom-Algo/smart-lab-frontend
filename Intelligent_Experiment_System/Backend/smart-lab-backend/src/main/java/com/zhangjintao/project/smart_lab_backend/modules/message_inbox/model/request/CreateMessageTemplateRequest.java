package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建消息模板请求。
 */
@Data
public class CreateMessageTemplateRequest {

	@NotBlank(message = "模板编码不能为空")
	@Size(max = 128, message = "模板编码长度不能超过128")
	@JsonAlias("template_code")
	private String templateCode;

	@NotBlank(message = "模板名称不能为空")
	@Size(max = 128, message = "模板名称长度不能超过128")
	@JsonAlias("template_name")
	private String templateName;

	@NotBlank(message = "消息类型不能为空")
	@Size(max = 128, message = "消息类型长度不能超过128")
	@JsonAlias("message_type")
	private String messageType;

	@NotBlank(message = "标题模板不能为空")
	@Size(max = 255, message = "标题模板长度不能超过255")
	@JsonAlias("title_template")
	private String titleTemplate;

	@NotBlank(message = "正文模板不能为空")
	@JsonAlias("content_template")
	private String contentTemplate;

	@JsonAlias("content_format")
	private ContentFormat contentFormat;

	@JsonAlias("param_schema")
	private JsonNode paramSchema;

	@Min(value = 0, message = "模板状态只能是0或1")
	@Max(value = 1, message = "模板状态只能是0或1")
	private Integer status;
}
