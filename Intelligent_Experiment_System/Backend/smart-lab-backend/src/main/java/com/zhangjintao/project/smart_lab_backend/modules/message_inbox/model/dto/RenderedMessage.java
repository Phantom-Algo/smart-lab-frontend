package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderedMessage {

	private String title;

	private String content;

	private ContentFormat contentFormat;
}
