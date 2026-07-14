package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabOpenStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabRunningStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;

public final class LabStatusTransitionPolicy {

	private LabStatusTransitionPolicy() {
	}

	public static void validateReservable(Lab lab) {
		if (lab.getOpenStatus() != LabOpenStatus.OPEN
				|| lab.getRunningStatus() == LabRunningStatus.DISABLED) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"当前实验室状态不允许创建预约");
		}
	}

	public static void applyCheckIn(Lab lab) {
		if (lab.getRunningStatus() != LabRunningStatus.IDLE || lab.getOpenStatus() != LabOpenStatus.OPEN) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"当前实验室状态不允许签到");
		}
		lab.setRunningStatus(LabRunningStatus.IN_USE);
		lab.setOpenStatus(LabOpenStatus.OPEN);
	}

	public static void applyCheckOut(Lab lab) {
		if (lab.getRunningStatus() != LabRunningStatus.IN_USE
				|| (lab.getOpenStatus() != LabOpenStatus.OPEN
				&& lab.getOpenStatus() != LabOpenStatus.TEMPORARY_CLOSED)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"当前实验室状态不允许签退");
		}
		lab.setRunningStatus(LabRunningStatus.IDLE);
	}

	public static void validateManualTransition(LabRunningStatus fromRunning, LabOpenStatus fromOpen,
			LabRunningStatus toRunning, LabOpenStatus toOpen, boolean runningStatusRequested) {
		validateCombination(toRunning, toOpen);
		if (fromRunning == toRunning && fromOpen == toOpen) {
			return;
		}
		if (fromOpen == LabOpenStatus.PERMANENT_CLOSED) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"实验室已永久关闭，不能再次变更状态");
		}
		if (runningStatusRequested && toRunning == LabRunningStatus.IN_USE
				&& fromRunning != LabRunningStatus.IN_USE) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"使用中状态只能由签到或管理员帮签产生");
		}
		if (!isAllowedManualTransition(fromRunning, fromOpen, toRunning, toOpen)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"不支持的实验室状态变更：" + fromRunning.getDescription() + "/" + fromOpen.getDescription()
							+ " -> " + toRunning.getDescription() + "/" + toOpen.getDescription());
		}
	}

	public static void validateCombination(LabRunningStatus runningStatus, LabOpenStatus openStatus) {
		if (runningStatus == LabRunningStatus.DISABLED && openStatus == LabOpenStatus.OPEN) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"停用的实验室不能保持正常开放状态");
		}
		if (runningStatus != LabRunningStatus.DISABLED && openStatus == LabOpenStatus.PERMANENT_CLOSED) {
			throw new LabManagementException(LabManagementErrorCode.LAB_STATUS_TRANSITION_INVALID,
					"永久关闭的实验室必须同时处于停用状态");
		}
	}

	private static boolean isAllowedManualTransition(LabRunningStatus fromRunning, LabOpenStatus fromOpen,
			LabRunningStatus toRunning, LabOpenStatus toOpen) {
		if (fromRunning == LabRunningStatus.IDLE && fromOpen == LabOpenStatus.OPEN) {
			return matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.PERMANENT_CLOSED);
		}
		if (fromRunning == LabRunningStatus.IDLE && fromOpen == LabOpenStatus.TEMPORARY_CLOSED) {
			return matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.PERMANENT_CLOSED);
		}
		if (fromRunning == LabRunningStatus.IN_USE && fromOpen == LabOpenStatus.OPEN) {
			return matches(toRunning, toOpen, LabRunningStatus.IN_USE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.OPEN);
		}
		if (fromRunning == LabRunningStatus.IN_USE && fromOpen == LabOpenStatus.TEMPORARY_CLOSED) {
			return matches(toRunning, toOpen, LabRunningStatus.IN_USE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.TEMPORARY_CLOSED);
		}
		if (fromRunning == LabRunningStatus.MAINTENANCE && fromOpen == LabOpenStatus.OPEN) {
			return matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.PERMANENT_CLOSED);
		}
		if (fromRunning == LabRunningStatus.MAINTENANCE && fromOpen == LabOpenStatus.TEMPORARY_CLOSED) {
			return matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.MAINTENANCE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.PERMANENT_CLOSED);
		}
		if (fromRunning == LabRunningStatus.DISABLED && fromOpen == LabOpenStatus.TEMPORARY_CLOSED) {
			return matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.TEMPORARY_CLOSED)
					|| matches(toRunning, toOpen, LabRunningStatus.IDLE, LabOpenStatus.OPEN)
					|| matches(toRunning, toOpen, LabRunningStatus.DISABLED, LabOpenStatus.PERMANENT_CLOSED);
		}
		return false;
	}

	private static boolean matches(LabRunningStatus running, LabOpenStatus open,
			LabRunningStatus expectedRunning, LabOpenStatus expectedOpen) {
		return running == expectedRunning && open == expectedOpen;
	}
}
