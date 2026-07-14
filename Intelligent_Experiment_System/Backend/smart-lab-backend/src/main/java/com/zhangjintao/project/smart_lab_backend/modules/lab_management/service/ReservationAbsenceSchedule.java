package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationAbsenceSchedule {

	private final ReservationService reservationService;
	private final AtomicBoolean running = new AtomicBoolean(false);

	@Scheduled(fixedDelayString = "${lab-management.reservation.absence-scan-fixed-delay-ms:1800000}")
	public void markAbsentReservations() {
		if (!running.compareAndSet(false, true)) {
			return;
		}
		try {
			reservationService.markAbsentReservations();
		}
		catch (Exception ex) {
			log.warn("Reservation absence scan failed", ex);
		}
		finally {
			running.set(false);
		}
	}
}
