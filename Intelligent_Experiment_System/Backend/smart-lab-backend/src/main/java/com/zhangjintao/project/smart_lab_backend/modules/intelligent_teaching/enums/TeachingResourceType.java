package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeachingResourceType {

	PPT(1, "课件PPT"),
	WORD(2, "课件Word"),
	PDF(3, "课件PDF"),
	VIDEO_LINK(4, "视频链接"),
	REFERENCE_LINK(5, "参考链接"),
	OTHER(6, "其他");

	private final int code;
	private final String description;
}
