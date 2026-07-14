package com.zhangjintao.project.smart_lab_backend.modules.material.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 预警处理状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum AlertStatus {
    PENDING(0, "待处理"),
    HANDLED(1, "已处理");

    private final int code;
    private final String desc;

    public static AlertStatus fromCode(Integer code) {
        if (code == null) return null;
        for (AlertStatus status : values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("未知预警状态码: " + code);
    }
}
