package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.constants.MessageInboxConstants;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.RenderedMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.ParamSchemaValidator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.TemplateRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染服务实现
 */
@Service
@RequiredArgsConstructor
public class TemplateRenderServiceImpl implements TemplateRenderService {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(MessageInboxConstants.MESSAGE_PLACEHOLDER_REGEX);

	private final ParamSchemaValidator paramSchemaValidator;

	@Override
	public RenderedMessage render(MessageTemplateSnapshot template, Map<String, Object> params) {
		paramSchemaValidator.validate(template.getParamSchema(), template.getTitleTemplate(), template.getContentTemplate(), params);
		ContentFormat contentFormat = template.getContentFormat() == null ? ContentFormat.TEXT : template.getContentFormat();
		return RenderedMessage.builder()
				.title(replacePlaceholders(template.getTitleTemplate(), params, contentFormat))
				.content(replacePlaceholders(template.getContentTemplate(), params, contentFormat))
				.contentFormat(contentFormat)
				.build();
	}

	private String replacePlaceholders(String template, Map<String, Object> params, ContentFormat contentFormat) {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
		StringBuilder builder = new StringBuilder();
		while (matcher.find()) {
			Object value = params.get(matcher.group(1));
			String replacement = value == null ? "" : String.valueOf(value);
			if (contentFormat == ContentFormat.HTML) {
				replacement = HtmlUtils.htmlEscape(replacement);
			}
			matcher.appendReplacement(builder, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(builder);
		return builder.toString();
	}
}
