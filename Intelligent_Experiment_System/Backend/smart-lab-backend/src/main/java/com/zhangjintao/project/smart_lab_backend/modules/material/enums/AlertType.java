package com.zhangjintao.project.smart_lab_backend.modules.material.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 物资预警类型枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum AlertType {
    LOW_STOCK(1, "低库存"),
    NEAR_EXPIRY(2, "临期"),
    EXPIRED(3, "过期"),
    OVER_RECEIVE(4, "超量领用");

    private final int code;
    private final String desc;

    public static AlertType fromCode(Integer code) {
        if (code == null) return null;
        for (AlertType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("未知预警类型码: " + code);
    }
}
