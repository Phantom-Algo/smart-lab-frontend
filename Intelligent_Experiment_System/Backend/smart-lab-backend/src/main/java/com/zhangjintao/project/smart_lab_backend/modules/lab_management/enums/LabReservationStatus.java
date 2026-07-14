package com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum LabReservationStatus {

	PENDING_APPROVAL(1, "待审核"),
	APPROVED(2, "已通过"),
	REJECTED(3, "已驳回"),
	CANCELLED(4, "已取消"),
	CHECKED_IN(5, "已签到"),
	ABSENT(6, "已爽约");

	public static final Set<Integer> CANCELLABLE_BY_LAB_DELETED =
			Set.of(PENDING_APPROVAL.code, APPROVED.code);

	public static final Set<Integer> CANCELLABLE_BY_APPLICANT =
			Set.of(PENDING_APPROVAL.code, APPROVED.code);

	public static final Set<Integer> OCCUPANCY_STATUSES =
			Set.of(PENDING_APPROVAL.code, APPROVED.code, CHECKED_IN.code);

	private final Integer code;
	private final String description;

	public static LabReservationStatus fromCode(Integer code) {
		for (LabReservationStatus status : values()) {
			if (status.code.equals(code)) {
				return status;
			}
		}
		throw new IllegalArgumentException("未知的实验室预约状态：" + code);
	}
}
