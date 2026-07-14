package com.zhangjintao.project.smart_lab_backend.modules.material.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 领用详情视图对象（申请+出库+批次明细完整视图） — TDD ReceiveDetailVO
 *
 * @author linbowei
 */
@Data
@Builder
public class ReceiveDetailVO {
    /** 申请信息 */
    private ReceiveApplyVO apply;
    /** 出库日志（审批通过后有值） */
    private ReceiveLogVO receiveLog;
}
