package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants.MessageInboxConstants;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.ParamSchemaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数 Schema 校验实现
 */
@Component
@RequiredArgsConstructor
public class ParamSchemaValidatorImpl implements ParamSchemaValidator {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(MessageInboxConstants.MESSAGE_PLACEHOLDER_REGEX);

	private final ObjectMapper objectMapper;

	@Override
	public void validate(String paramSchema, String titleTemplate, String contentTemplate, Map<String, Object> params) {
		Map<String, Object> actualParams = params == null ? Map.of() : params;
		Set<String> placeholders = extractPlaceholders(titleTemplate + "\n" + contentTemplate);
		if (!StringUtils.hasText(paramSchema)) {
			if (!actualParams.isEmpty() || !placeholders.isEmpty()) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID,
						"param_schema 为空时不允许传入参数或使用模板占位符");
			}
			return;
		}
		JsonNode schemaRoot = readSchema(paramSchema);
		JsonNode propertiesNode = schemaRoot.path("properties");
		if (!propertiesNode.isObject()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "param_schema.properties 必须是对象");
		}
		validatePlaceholdersDeclared(placeholders, propertiesNode);
		validateRequired(schemaRoot.path("required"), actualParams);
		validateAdditionalProperties(schemaRoot.path("additionalProperties").asBoolean(true), propertiesNode, actualParams);
		validatePropertyValues(propertiesNode, actualParams);
		validatePlaceholdersProvided(placeholders, actualParams);
	}

	private JsonNode readSchema(String paramSchema) {
		try {
			return objectMapper.readTree(paramSchema);
		}
		catch (Exception ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "param_schema 不是合法 JSON");
		}
	}

	private void validatePlaceholdersDeclared(Set<String> placeholders, JsonNode propertiesNode) {
		for (String placeholder : placeholders) {
			if (!propertiesNode.has(placeholder)) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID,
						"模板占位符未在 param_schema.properties 中声明：" + placeholder);
			}
		}
	}

	private void validateRequired(JsonNode requiredNode, Map<String, Object> params) {
		if (!requiredNode.isMissingNode() && !requiredNode.isArray()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "param_schema.required 必须是数组");
		}
		for (JsonNode requiredItem : requiredNode) {
			String paramName = requiredItem.asText();
			if (!params.containsKey(paramName) || params.get(paramName) == null) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "缺少必填模板参数：" + paramName);
			}
		}
	}

	private void validateAdditionalProperties(boolean additionalProperties, JsonNode propertiesNode, Map<String, Object> params) {
		if (additionalProperties) {
			return;
		}
		for (String paramName : params.keySet()) {
			if (!propertiesNode.has(paramName)) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "存在未声明模板参数：" + paramName);
			}
		}
	}

	private void validatePropertyValues(JsonNode propertiesNode, Map<String, Object> params) {
		Iterator<Map.Entry<String, JsonNode>> fields = propertiesNode.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> field = fields.next();
			String paramName = field.getKey();
			if (!params.containsKey(paramName) || params.get(paramName) == null) {
				continue;
			}
			validateOneValue(paramName, params.get(paramName), field.getValue());
		}
	}

	private void validateOneValue(String paramName, Object value, JsonNode propertySchema) {
		String type = propertySchema.path("type").asText();
		switch (type) {
			case "string" -> validateString(paramName, value, propertySchema);
			case "integer" -> validateInteger(paramName, value, propertySchema);
			case "number" -> validateNumber(paramName, value, propertySchema);
			case "boolean" -> validateBoolean(paramName, value);
			case "date" -> validateDate(paramName, value, propertySchema);
			case "datetime" -> validateDateTime(paramName, value, propertySchema);
			default -> throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID,
					"不支持的模板参数类型：" + type);
		}
		validateEnum(paramName, value, propertySchema);
	}

	private void validateString(String paramName, Object value, JsonNode propertySchema) {
		if (!(value instanceof String stringValue)) {
			throw typeError(paramName, "string");
		}
		if (propertySchema.has("minLength") && stringValue.length() < propertySchema.get("minLength").asInt()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数长度过短：" + paramName);
		}
		if (propertySchema.has("maxLength") && stringValue.length() > propertySchema.get("maxLength").asInt()) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数长度过长：" + paramName);
		}
	}

	private void validateInteger(String paramName, Object value, JsonNode propertySchema) {
		if (!(value instanceof Number number) || BigDecimal.valueOf(number.doubleValue()).stripTrailingZeros().scale() > 0) {
			throw typeError(paramName, "integer");
		}
		validateNumberRange(paramName, BigDecimal.valueOf(number.longValue()), propertySchema);
	}

	private void validateNumber(String paramName, Object value, JsonNode propertySchema) {
		if (!(value instanceof Number number)) {
			throw typeError(paramName, "number");
		}
		validateNumberRange(paramName, BigDecimal.valueOf(number.doubleValue()), propertySchema);
	}

	private void validateBoolean(String paramName, Object value) {
		if (!(value instanceof Boolean)) {
			throw typeError(paramName, "boolean");
		}
	}

	private void validateDate(String paramName, Object value, JsonNode propertySchema) {
		if (!(value instanceof String stringValue)) {
			throw typeError(paramName, "date");
		}
		try {
			DateTimeFormatter formatter = propertySchema.has("format")
					? DateTimeFormatter.ofPattern(propertySchema.get("format").asText())
					: DateTimeFormatter.ISO_DATE;
			LocalDate.parse(stringValue, formatter);
		}
		catch (DateTimeParseException | IllegalArgumentException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数日期格式错误：" + paramName);
		}
	}

	private void validateDateTime(String paramName, Object value, JsonNode propertySchema) {
		if (!(value instanceof String stringValue)) {
			throw typeError(paramName, "datetime");
		}
		try {
			DateTimeFormatter formatter = propertySchema.has("format")
					? DateTimeFormatter.ofPattern(propertySchema.get("format").asText())
					: DateTimeFormatter.ISO_DATE_TIME;
			LocalDateTime.parse(stringValue, formatter);
		}
		catch (DateTimeParseException | IllegalArgumentException ex) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数日期时间格式错误：" + paramName);
		}
	}

	private void validateNumberRange(String paramName, BigDecimal value, JsonNode propertySchema) {
		if (propertySchema.has("min") && value.compareTo(propertySchema.get("min").decimalValue()) < 0) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数小于最小值：" + paramName);
		}
		if (propertySchema.has("max") && value.compareTo(propertySchema.get("max").decimalValue()) > 0) {
			throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数大于最大值：" + paramName);
		}
	}

	private void validateEnum(String paramName, Object value, JsonNode propertySchema) {
		JsonNode enumNode = propertySchema.path("enum");
		if (!enumNode.isArray()) {
			return;
		}
		for (JsonNode item : enumNode) {
			if (String.valueOf(value).equals(item.asText())) {
				return;
			}
		}
		throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板参数不在枚举范围内：" + paramName);
	}

	private void validatePlaceholdersProvided(Set<String> placeholders, Map<String, Object> params) {
		for (String placeholder : placeholders) {
			if (!params.containsKey(placeholder) || params.get(placeholder) == null) {
				throw new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID, "模板占位符缺少参数值：" + placeholder);
			}
		}
	}

	private Set<String> extractPlaceholders(String templateText) {
		Set<String> placeholders = new HashSet<>();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateText);
		while (matcher.find()) {
			placeholders.add(matcher.group(1));
		}
		return placeholders;
	}

	private MessageInboxException typeError(String paramName, String expectedType) {
		return new MessageInboxException(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID,
				"模板参数类型错误：" + paramName + " 应为 " + expectedType);
	}
}
