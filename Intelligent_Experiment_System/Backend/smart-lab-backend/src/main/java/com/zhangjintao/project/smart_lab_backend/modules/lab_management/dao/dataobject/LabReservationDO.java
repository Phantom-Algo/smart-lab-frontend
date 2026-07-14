package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("lab_reservation")
public class LabReservationDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String reservationNo;
	private Long labId;
	private Long applicantId;
	private LocalDate reservationDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private String purpose;
	private String courseName;
	private Integer reservationStatus;
	private Long approverId;
	private LocalDateTime approveTime;
	private String approveComment;
	private LocalDateTime checkInAt;
	private LocalDateTime checkOutAt;
	private String cancelReason;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
