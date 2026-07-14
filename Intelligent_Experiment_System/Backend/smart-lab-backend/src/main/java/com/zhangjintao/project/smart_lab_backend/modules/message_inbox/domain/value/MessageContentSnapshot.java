package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.value;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息渲染结果快照。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageContentSnapshot {

	private String title;

	private String content;

	private ContentFormat contentFormat;

	public static MessageContentSnapshot of(String title, String content, ContentFormat contentFormat) {
		return MessageContentSnapshot.builder()
				.title(title)
				.content(content)
				.contentFormat(contentFormat)
				.build();
	}
}
