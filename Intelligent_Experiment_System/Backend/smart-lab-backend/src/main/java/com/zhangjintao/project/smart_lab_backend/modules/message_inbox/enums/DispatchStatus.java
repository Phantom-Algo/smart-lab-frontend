package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息分发状态。
 */
@Getter
@RequiredArgsConstructor
public enum DispatchStatus {

	PENDING(0, "待处理"),
	PROCESSING(1, "处理中"),
	COMPLETED(2, "已完成"),
	PARTIAL_FAILED(3, "部分失败"),
	FAILED(4, "失败");

	private final int code;
	private final String description;

	public static DispatchStatus fromCode(int code) {
		for (DispatchStatus status : values()) {
			if (status.code == code) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的消息分发状态：" + code);
	}
}
