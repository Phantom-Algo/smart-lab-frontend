package com.zhangjintao.project.smart_lab_backend.common.exception;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 处理业务异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException ex) {
		log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
		return buildResponse(ex.getHttpStatus(), ex.getCode(), ex.getMessage());
	}

	/**
	 * 处理参数校验异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResult<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
		return buildResponse(CommonErrorCode.PARAM_VALIDATION_FAILED, resolveBindingMessage(ex.getBindingResult()));
	}

	/**
	 * 处理参数绑定异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(BindException.class)
	public ResponseEntity<ApiResult<Void>> handleBindException(BindException ex) {
		return buildResponse(CommonErrorCode.PARAM_VALIDATION_FAILED, resolveBindingMessage(ex.getBindingResult()));
	}

	/**
	 * 处理参数约束异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResult<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
		String message = ex.getConstraintViolations().stream()
				.findFirst()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.orElse(CommonErrorCode.PARAM_VALIDATION_FAILED.getMessage());
		return buildResponse(CommonErrorCode.PARAM_VALIDATION_FAILED, message);
	}

	/**
	 * 处理缺少请求参数异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResult<Void>> handleMissingServletRequestParameterException(
			MissingServletRequestParameterException ex) {
		return buildResponse(CommonErrorCode.BAD_REQUEST, "缺少必要参数: " + ex.getParameterName());
	}

	/**
	 * 处理参数类型不匹配异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResult<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
		return buildResponse(CommonErrorCode.BAD_REQUEST, "参数类型错误: " + ex.getName());
	}

	/**
	 * 处理请求体格式错误异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResult<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
		return buildResponse(CommonErrorCode.BAD_REQUEST, "请求体格式错误");
	}

	/**
	 * 处理请求方法不支持异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResult<Void>> handleHttpRequestMethodNotSupportedException(
			HttpRequestMethodNotSupportedException ex) {
		return buildResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
	}

	/**
	 * 处理不支持的媒体类型异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiResult<Void>> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
		return buildResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
	}

	/**
	 * 处理资源未找到异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
	public ResponseEntity<ApiResult<Void>> handleNotFoundException(Exception ex) {
		return buildResponse(CommonErrorCode.NOT_FOUND);
	}

	/**
	 * 处理访问被拒绝异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResult<Void>> handleAccessDeniedException(AccessDeniedException ex) {
		return buildResponse(CommonErrorCode.FORBIDDEN);
	}

	/**
	 * 处理未知异常
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
		log.error("Unexpected exception", ex);
		return buildResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<ApiResult<Void>> buildResponse(ErrorCode errorCode) {
		return buildResponse(errorCode, errorCode.getMessage());
	}

	private ResponseEntity<ApiResult<Void>> buildResponse(ErrorCode errorCode, String message) {
		return buildResponse(errorCode.getHttpStatus(), errorCode.getCode(), message);
	}

	private ResponseEntity<ApiResult<Void>> buildResponse(HttpStatus httpStatus, int code, String message) {
		return ResponseEntity.status(httpStatus).body(ApiResult.fail(code, message));
	}

	private String resolveBindingMessage(BindingResult bindingResult) {
		if (bindingResult.hasFieldErrors()) {
			FieldError fieldError = bindingResult.getFieldErrors().get(0);
			return fieldError.getField() + ": " + fieldError.getDefaultMessage();
		}
		if (bindingResult.hasGlobalErrors()) {
			ObjectError objectError = bindingResult.getGlobalErrors().get(0);
			return objectError.getObjectName() + ": " + objectError.getDefaultMessage();
		}
		return CommonErrorCode.PARAM_VALIDATION_FAILED.getMessage();
	}
}
