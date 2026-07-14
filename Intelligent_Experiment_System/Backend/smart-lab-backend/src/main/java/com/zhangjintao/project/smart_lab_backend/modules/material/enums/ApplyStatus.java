package com.zhangjintao.project.smart_lab_backend.modules.material.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 物资领用申请状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum ApplyStatus {
    PENDING(1, "待审核"),
    APPROVED(2, "审批通过"),
    REJECTED(3, "已驳回"),
    CANCELLED(4, "已取消");

    private final int code;
    private final String desc;

    public static ApplyStatus fromCode(Integer code) {
        if (code == null) return null;
        for (ApplyStatus status : values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("未知申请状态码: " + code);
    }
}
