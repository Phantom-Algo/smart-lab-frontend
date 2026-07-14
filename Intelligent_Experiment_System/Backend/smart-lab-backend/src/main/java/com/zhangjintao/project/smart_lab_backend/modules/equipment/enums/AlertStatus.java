package com.zhangjintao.project.smart_lab_backend.modules.equipment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 告警处理状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum AlertStatus {
    UNHANDLED(0, "未处理"),
    HANDLED(1, "已处理");

    private final int code;
    private final String desc;

    public static AlertStatus fromCode(Integer code) {
        if (code == null) return null;
        for (AlertStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
