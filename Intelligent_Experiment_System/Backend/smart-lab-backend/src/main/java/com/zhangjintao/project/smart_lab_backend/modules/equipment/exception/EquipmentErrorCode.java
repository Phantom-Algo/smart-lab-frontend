package com.zhangjintao.project.smart_lab_backend.modules.equipment.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 智能化设备管理子系统错误码枚举
 * <p>
 * 编码规则：
 * <ul>
 *   <li>50100-50199 设备相关</li>
 *   <li>50200-50299 借用申请相关</li>
 *   <li>50300-50399 借用日志相关</li>
 *   <li>50400-50499 维修相关</li>
 *   <li>50600-50699 告警相关</li>
 * </ul>
 *
 * @author linbowei
 */
public enum EquipmentErrorCode implements ErrorCode {

    // ========== 通用参数 50000-50099 ==========
    PARAM_INVALID(50000, "参数校验失败", HttpStatus.BAD_REQUEST),

    // ========== 设备相关 50100-50199 ==========
    DEVICE_NOT_FOUND(50100, "设备不存在", HttpStatus.NOT_FOUND),
    DEVICE_NO_DUPLICATE(50101, "设备编号已存在", HttpStatus.CONFLICT),
    DEVICE_NOT_AVAILABLE(50102, "设备不可用（非闲置状态）", HttpStatus.BAD_REQUEST),
    DEVICE_STATUS_INVALID(50103, "设备状态变更非法", HttpStatus.BAD_REQUEST),
    DEVICE_HAS_ACTIVE_BORROW(50104, "设备存在未归还记录，不可报废", HttpStatus.BAD_REQUEST),
    DEVICE_HAS_ACTIVE_SENSOR_BINDING(50105, "设备存在有效传感器绑定", HttpStatus.CONFLICT),
    DEVICE_SENSOR_BINDING_CONFLICT(50106, "设备传感器绑定关系冲突", HttpStatus.CONFLICT),

    // ========== 借用申请相关 50200-50299 ==========
    APPLY_NOT_FOUND(50200, "申请单不存在", HttpStatus.NOT_FOUND),
    APPLY_STATUS_INVALID(50201, "申请单状态不允许此操作", HttpStatus.BAD_REQUEST),
    APPLY_NOT_OWNER(50202, "只能取消自己的申请", HttpStatus.FORBIDDEN),
    APPLY_DEVICE_CONFLICT(50203, "该设备已有待审核申请", HttpStatus.CONFLICT),

    // ========== 借用日志相关 50300-50399 ==========
    BORROW_LOG_NOT_FOUND(50300, "借用记录不存在", HttpStatus.NOT_FOUND),
    BORROW_LOG_ALREADY_RETURNED(50301, "设备已归还", HttpStatus.BAD_REQUEST),
    BORROW_STATUS_INVALID(50302, "借用记录状态不允许此操作", HttpStatus.BAD_REQUEST),

    // ========== 维修相关 50400-50499 ==========
    REPAIR_NOT_FOUND(50400, "维修单不存在", HttpStatus.NOT_FOUND),
    REPAIR_STATUS_INVALID(50401, "维修单状态不允许此操作", HttpStatus.BAD_REQUEST),

    // ========== 告警相关 50600-50699 ==========
    ALERT_NOT_FOUND(50600, "告警记录不存在", HttpStatus.NOT_FOUND),
    ALERT_ALREADY_HANDLED(50601, "告警已处理，不可重复处理", HttpStatus.BAD_REQUEST),
    ALERT_HANDLER_REQUIRED(50602, "处理人ID不能为空", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    EquipmentErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }

    @Override
    public HttpStatus getHttpStatus() { return httpStatus; }
}
