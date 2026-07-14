package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.BorrowApplyDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.BorrowApproveDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.BorrowLogQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.BorrowApplyVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.BorrowLogVO;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;

/**
 * 设备借用管理服务接口
 *
 * @author linbowei
 */
public interface DeviceBorrowService {

    /** FR-0004: 提交借用申请 */
    BorrowApplyVO apply(Long deviceId, BorrowApplyDTO dto);

    /** FR-0004: 取消/撤回申请 */
    void cancelApply(String applyNo);

    /** FR-0005: 审批申请（通过/驳回） */
    void approve(String applyNo, BorrowApproveDTO dto);

    /** FR-0006: 领用登记（outOperatorId从SecurityContext获取） */
    BorrowLogVO checkOut(String applyNo, Long outOperatorId);

    /** FR-0007: 归还登记（returnOperatorId从SecurityContext获取） */
    void checkIn(Long borrowLogId, String returnRemark, Long returnOperatorId);

    /** FR-0008: 借用记录查询 */
    PageResult<BorrowLogVO> listBorrowLogs(BorrowLogQueryDTO query);

    /** 当前用户的借用申请查询 */
    PageResult<BorrowApplyVO> listMyApplies(Long applicantId, Integer applyStatus, long pageNum, long pageSize);

    /** 管理员查询借用申请 */
    PageResult<BorrowApplyVO> listApplies(String applyNo, Long deviceId, Long applicantId,
                                          Integer applyStatus, long pageNum, long pageSize);

    /** FR-0009: 逾期检测（定时任务调用） */
    void detectOverdue();
}
