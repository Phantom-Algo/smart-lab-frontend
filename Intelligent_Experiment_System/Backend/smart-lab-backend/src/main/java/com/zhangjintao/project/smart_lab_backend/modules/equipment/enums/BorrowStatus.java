package com.zhangjintao.project.smart_lab_backend.modules.equipment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 借用状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum BorrowStatus {
    BORROWING(1, "借出中"),
    RETURNED(2, "已归还"),
    OVERDUE_UNRETURNED(3, "逾期未还");

    private final int code;
    private final String desc;

    public static BorrowStatus fromCode(Integer code) {
        if (code == null) return null;
        for (BorrowStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
