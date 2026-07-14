package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetectionSafeRangeTest {

	@Test
	void containsShouldSupportClosedAndOpenSegments() {
		DetectionSafeRange safeRange = DetectionSafeRange.builder()
				.segments(List.of(
						DetectionSafeRangeSegment.builder()
								.min(BigDecimal.valueOf(18))
								.max(BigDecimal.valueOf(30))
								.build(),
						DetectionSafeRangeSegment.builder()
								.min(BigDecimal.valueOf(50))
								.max(null)
								.build()))
				.build();

		assertThat(safeRange.contains(BigDecimal.valueOf(18))).isTrue();
		assertThat(safeRange.contains(BigDecimal.valueOf(45))).isFalse();
		assertThat(safeRange.contains(BigDecimal.valueOf(60))).isTrue();
	}

	@Test
	void containsShouldValidateAllSegmentsBeforeMatching() {
		DetectionSafeRange safeRange = DetectionSafeRange.builder()
				.segments(List.of(
						DetectionSafeRangeSegment.builder()
								.min(BigDecimal.ZERO)
								.max(BigDecimal.TEN)
								.build(),
						DetectionSafeRangeSegment.builder()
								.min(BigDecimal.TEN)
								.max(BigDecimal.ZERO)
								.build()))
				.build();

		assertThatThrownBy(() -> safeRange.contains(BigDecimal.ONE))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("下限不能大于上限");
	}

	@Test
	void toExternalValueShouldKeepNullBoundaries() {
		DetectionSafeRange safeRange = DetectionSafeRange.builder()
				.segments(List.of(DetectionSafeRangeSegment.builder()
						.min(null)
						.max(BigDecimal.valueOf(80))
						.build()))
				.build();

		assertThat(safeRange.toExternalValue()).containsExactly(Arrays.asList(null, BigDecimal.valueOf(80)));
	}
}
