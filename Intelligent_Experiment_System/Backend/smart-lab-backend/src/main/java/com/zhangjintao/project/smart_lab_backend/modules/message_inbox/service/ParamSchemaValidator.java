package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import java.util.Map;

/**
 * 参数 Schema 校验接口
 */
public interface ParamSchemaValidator {

	/**
	 * 校验模板参数
	 *
	 * @param paramSchema    参数 Schema JSON
	 * @param titleTemplate  标题模板
	 * @param contentTemplate 正文模板
	 * @param params         实际参数
	 */
	void validate(String paramSchema, String titleTemplate, String contentTemplate, Map<String, Object> params);
}
