package com.zhangjintao.project.smart_lab_backend.modules.equipment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 设备状态枚举
 *
 * @author linbowei
 */
@Getter
@RequiredArgsConstructor
public enum DeviceStatus {
    PURCHASE_PENDING(1, "采购待入库"),
    IDLE(2, "闲置"),
    BORROWED(3, "借用中"),
    UNDER_REPAIR(4, "维修中"),
    SCRAPPED(5, "报废");

    private final int code;
    private final String desc;

    /**
     * 根据code获取枚举值
     */
    public static DeviceStatus fromCode(Integer code) {
        if (code == null) return null;
        for (DeviceStatus status : values()) {
            if (status.code == code) return status;
        }
        return null;
    }
}
