package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums;

/**
 * 消息分发失败阶段。
 */
public enum FailureStage {

	VALIDATE,
	RENDER,
	ROUTE,
	PERSIST,
	DISPATCH
}
