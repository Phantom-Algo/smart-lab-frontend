package com.zhangjintao.project.smart_lab_backend.modules.material.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 物资批次状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum BatchStatus {
    AVAILABLE(1, "在库可用"),
    EMPTY(2, "已清空"),
    SCRAPPED(3, "报废");

    private final int code;
    private final String desc;

    public static BatchStatus fromCode(Integer code) {
        if (code == null) return null;
        for (BatchStatus status : values()) {
            if (status.code == code) return status;
        }
        throw new IllegalArgumentException("未知批次状态码: " + code);
    }
}
