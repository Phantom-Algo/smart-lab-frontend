package com.zhangjintao.project.smart_lab_backend.modules.equipment.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

/**
 * 设备管理子系统业务异常
 *
 * @author linbowei
 */
public class EquipmentException extends BusinessException {

    public EquipmentException(EquipmentErrorCode errorCode) {
        super(errorCode);
    }

    public EquipmentException(EquipmentErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
