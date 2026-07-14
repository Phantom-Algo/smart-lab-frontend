package com.zhangjintao.project.smart_lab_backend.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

	public static final int SUCCESS_CODE = 0;
	public static final String SUCCESS_MESSAGE = "success";

	private final int code;
	private final String message;
	private final T data;
	private final Map<String, Object> metadata;

	private ApiResult(int code, String message, T data) {
		this(code, message, data, defaultMetadata());
	}

	private ApiResult(int code, String message, T data, Map<String, Object> metadata) {
		this.code = code;
		this.message = message;
		this.data = data;
		this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
	}

	public static ApiResult<Void> success() {
		return new ApiResult<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
	}

	public static <T> ApiResult<T> success(T data) {
		return new ApiResult<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public static <T> ApiResult<T> success(String message, T data) {
		return new ApiResult<>(SUCCESS_CODE, message, data);
	}

	public static ApiResult<Void> fail(ErrorCode errorCode) {
		return fail(errorCode.getCode(), errorCode.getMessage());
	}

	public static ApiResult<Void> fail(ErrorCode errorCode, String message) {
		return fail(errorCode.getCode(), message);
	}

	public static ApiResult<Void> fail(int code, String message) {
		return new ApiResult<>(code, message, null);
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public T getData() {
		return data;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	private static Map<String, Object> defaultMetadata() {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("timestamp", System.currentTimeMillis());
		return metadata;
	}
}
