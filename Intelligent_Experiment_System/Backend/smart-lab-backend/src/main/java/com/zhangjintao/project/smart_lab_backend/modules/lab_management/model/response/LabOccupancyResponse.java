package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Value
@Builder
public class LabOccupancyResponse {

	Long labId;
	LocalDate date;
	List<OccupiedTime> occupiedTimes;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OccupiedTime {

		String reservationNo;
		LocalTime startTime;
		LocalTime endTime;
		Integer reservationStatus;
		String reservationStatusDesc;
	}
}
