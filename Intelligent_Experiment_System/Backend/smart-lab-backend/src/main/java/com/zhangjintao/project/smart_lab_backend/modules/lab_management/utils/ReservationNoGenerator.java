package com.zhangjintao.project.smart_lab_backend.modules.lab_management.utils;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public final class ReservationNoGenerator {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final int RANDOM_BOUND = (int) Math.pow(10, LabManagementConstants.RESERVATION_NO_RANDOM_DIGITS);

	private ReservationNoGenerator() {
	}

	public static String generate(LocalDate date) {
		int randomNumber = ThreadLocalRandom.current().nextInt(RANDOM_BOUND);
		return "LAB_RESV-" + DATE_FORMATTER.format(date) + String.format("%06d", randomNumber);
	}
}
