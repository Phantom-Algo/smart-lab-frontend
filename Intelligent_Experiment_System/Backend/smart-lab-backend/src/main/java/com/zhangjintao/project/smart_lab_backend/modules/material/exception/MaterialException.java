package com.zhangjintao.project.smart_lab_backend.modules.material.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

/**
 * 物资管理子系统业务异常
 *
 * @author linbowei
 */
public class MaterialException extends BusinessException {

    public MaterialException(MaterialErrorCode errorCode) {
        super(errorCode);
    }

    public MaterialException(MaterialErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
