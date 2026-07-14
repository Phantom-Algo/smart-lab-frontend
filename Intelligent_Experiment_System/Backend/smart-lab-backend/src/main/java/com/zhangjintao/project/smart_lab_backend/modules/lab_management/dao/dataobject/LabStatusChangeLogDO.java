package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_status_change_log")
public class LabStatusChangeLogDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long labId;
	private Long operatorId;
	private Integer fromRunningStatus;
	private Integer toRunningStatus;
	private Integer fromOpenStatus;
	private Integer toOpenStatus;
	private String changeReason;
	private String triggerSource;
	private Integer forcedCheckoutCount;
	private Integer cancelledReservationCount;
	private LocalDateTime createdAt;
}
