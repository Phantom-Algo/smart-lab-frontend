package com.zhangjintao.project.smart_lab_backend.modules.equipment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 维修状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum RepairStatus {
    PENDING(1, "待处理"),
    PROCESSING(2, "维修中"),
    REPAIRED(3, "已修复"),
    ACCEPTED(4, "已验收");

    private final int code;
    private final String desc;

    public static RepairStatus fromCode(Integer code) {
        if (code == null) return null;
        for (RepairStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
