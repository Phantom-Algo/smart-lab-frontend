package com.zhangjintao.project.smart_lab_backend.modules.material.service;

import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveApplyDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.ReceiveLogQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveDetailVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.ReceiveLogVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

/**
 * 领用管理服务接口 — FR-0003, FR-0004, FR-0005
 *
 * @author linbowei
 */
public interface MaterialReceiveService {

    /** 领用申请（FR-0003） */
    ReceiveApplyVO apply(ReceiveApplyDTO dto, Long applicantId);

    /** 取消申请（FR-0003） */
    void cancel(String applyNo, Long applicantId);

    /** 审批通过+自动出库（FR-0004：FIFO+临期优先选批） */
    ReceiveLogVO approve(String applyNo, Long approverId);

    /** 驳回申请（FR-0004） */
    void reject(String applyNo, Long approverId, String reason);

    /** 领用记录分页查询（FR-0005） */
    PageResult<ReceiveApplyVO> pageApplyRecords(ReceiveLogQueryDTO query);

    /** 领用详情（申请+出库+批次明细完整视图）（FR-0005） */
    ReceiveDetailVO getReceiveDetail(String applyNo);
}
