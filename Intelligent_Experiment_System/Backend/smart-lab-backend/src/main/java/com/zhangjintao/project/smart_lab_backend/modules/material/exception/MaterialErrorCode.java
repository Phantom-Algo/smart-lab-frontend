package com.zhangjintao.project.smart_lab_backend.modules.material.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 物资管理子系统错误码枚举
 * <p>
 * 编码规则（避开equipment的50000-50699）：
 * <ul>
 *   <li>50700-50799 通用/参数</li>
 *   <li>50800-50899 物资管理</li>
 *   <li>50900-50999 批次管理</li>
 *   <li>51000-51099 领用管理</li>
 *   <li>51100-51199 预警管理</li>
 * </ul>
 *
 * @author linbowei
 */
public enum MaterialErrorCode implements ErrorCode {

    // ========== 物资管理 50800-50899 ==========
    MATERIAL_NOT_FOUND(50800, "物资不存在", HttpStatus.NOT_FOUND),
    MATERIAL_NO_DUPLICATE(50801, "物资编号重复", HttpStatus.CONFLICT),
    MATERIAL_HAS_STOCK(50802, "物资尚有库存，无法删除", HttpStatus.BAD_REQUEST),
    MATERIAL_HAS_PENDING_APPLY(50803, "物资有进行中的领用申请，无法删除", HttpStatus.BAD_REQUEST),

    // ========== 批次管理 50900-50999 ==========
    BATCH_NOT_FOUND(50900, "批次不存在", HttpStatus.NOT_FOUND),
    BATCH_NOT_AVAILABLE(50901, "批次不可用（已清空或已报废）", HttpStatus.BAD_REQUEST),

    // ========== 领用管理 51000-51099 ==========
    APPLY_NOT_FOUND(51000, "申请单不存在", HttpStatus.NOT_FOUND),
    APPLY_STATUS_INVALID(51001, "申请单状态不允许此操作", HttpStatus.BAD_REQUEST),
    STOCK_INSUFFICIENT(51002, "库存不足", HttpStatus.BAD_REQUEST),
    APPLY_DUPLICATE_PENDING(51003, "该物资已有待审核的领用申请", HttpStatus.CONFLICT),
    PERSON_NOT_FOUND(51004, "人员不存在", HttpStatus.NOT_FOUND),

    // ========== 预警管理 51100-51199 ==========
    ALERT_NOT_FOUND(51100, "预警记录不存在", HttpStatus.NOT_FOUND),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    MaterialErrorCode(int code, String message, HttpStatus httpStatus) {
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
