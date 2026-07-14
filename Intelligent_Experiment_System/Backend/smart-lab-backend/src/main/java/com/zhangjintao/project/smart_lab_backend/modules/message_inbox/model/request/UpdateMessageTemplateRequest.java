package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新消息模板请求。字段为 null 时表示不更新该字段。
 */
@Data
public class UpdateMessageTemplateRequest {

	@Pattern(regexp = "(?s).*\\S.*", message = "模板名称不能为空")
	@Size(max = 128, message = "模板名称长度不能超过128")
	@JsonAlias("template_name")
	private String templateName;

	@Pattern(regexp = "(?s).*\\S.*", message = "消息类型不能为空")
	@Size(max = 128, message = "消息类型长度不能超过128")
	@JsonAlias("message_type")
	private String messageType;

	@Pattern(regexp = "(?s).*\\S.*", message = "标题模板不能为空")
	@Size(max = 255, message = "标题模板长度不能超过255")
	@JsonAlias("title_template")
	private String titleTemplate;

	@Pattern(regexp = "(?s).*\\S.*", message = "正文模板不能为空")
	@JsonAlias("content_template")
	private String contentTemplate;

	@JsonAlias("content_format")
	private ContentFormat contentFormat;

	@JsonAlias("param_schema")
	private JsonNode paramSchema;

	@JsonIgnore
	private boolean paramSchemaSpecified;

	@Min(value = 0, message = "模板状态只能是0或1")
	@Max(value = 1, message = "模板状态只能是0或1")
	private Integer status;

	@JsonAlias("param_schema")
	public void setParamSchema(JsonNode paramSchema) {
		this.paramSchema = paramSchema;
		this.paramSchemaSpecified = true;
	}
}
