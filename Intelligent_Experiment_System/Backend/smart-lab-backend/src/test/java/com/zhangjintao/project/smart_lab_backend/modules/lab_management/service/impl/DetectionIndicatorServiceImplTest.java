package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.DetectionIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRangeSegment;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.DetectionIndicatorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionIndicatorServiceImplTest {

	@Mock
	private DetectionIndicatorRepository detectionIndicatorRepository;

	@Mock
	private LabTypeRepository labTypeRepository;

	@Mock
	private DetectionIndicatorCacheManager detectionIndicatorCacheManager;

	@InjectMocks
	private DetectionIndicatorServiceImpl detectionIndicatorService;

	@Test
	void createSuccessShouldNormalizeFieldsValidateSafeRangeAndEvictCache() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType(1L)));
		when(detectionIndicatorRepository.existsByLabTypeIdAndIndicatorName(1L, "temperature", null))
				.thenReturn(false);
		when(detectionIndicatorRepository.save(any(DetectionIndicator.class))).thenAnswer(invocation -> {
			DetectionIndicator indicator = invocation.getArgument(0);
			indicator.setId(10L);
			return indicator;
		});

		DetectionIndicatorResponse response = detectionIndicatorService.create(DetectionIndicatorCreateCommand.builder()
				.labTypeId(1L)
				.indicatorName(" temperature ")
				.unit(" ℃ ")
				.safeRange(List.of(
						List.of(BigDecimal.valueOf(-10), BigDecimal.valueOf(30)),
						Arrays.asList(BigDecimal.valueOf(50), null)))
				.build());

		assertThat(response.getId()).isEqualTo(10L);
		assertThat(response.getIndicatorName()).isEqualTo("temperature");
		assertThat(response.getUnit()).isEqualTo("℃");
		assertThat(response.getSafeRange()).hasSize(2);
		ArgumentCaptor<DetectionIndicator> indicatorCaptor = ArgumentCaptor.forClass(DetectionIndicator.class);
		verify(detectionIndicatorRepository).save(indicatorCaptor.capture());
		assertThat(indicatorCaptor.getValue().getIndicatorName()).isEqualTo("temperature");
		assertThat(indicatorCaptor.getValue().getUnit()).isEqualTo("℃");
		assertThat(indicatorCaptor.getValue().getSafeRange().getSegments())
				.extracting(DetectionSafeRangeSegment::getMin)
				.containsExactly(BigDecimal.valueOf(-10), BigDecimal.valueOf(50));
		verify(detectionIndicatorCacheManager).evictByLabTypeId(1L);
	}

	@Test
	void createTypeNotFoundShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> detectionIndicatorService.create(DetectionIndicatorCreateCommand.builder()
				.labTypeId(1L)
				.indicatorName("temperature")
				.unit("℃")
				.safeRange(List.of(List.of(BigDecimal.ZERO, BigDecimal.TEN)))
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_TYPE_NOT_FOUND.getCode()));

		verify(detectionIndicatorRepository, never()).save(any());
		verify(detectionIndicatorCacheManager, never()).evictByLabTypeId(any());
	}

	@Test
	void createDuplicateNameShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType(1L)));
		when(detectionIndicatorRepository.existsByLabTypeIdAndIndicatorName(1L, "temperature", null))
				.thenReturn(true);

		assertThatThrownBy(() -> detectionIndicatorService.create(DetectionIndicatorCreateCommand.builder()
				.labTypeId(1L)
				.indicatorName("temperature")
				.unit("℃")
				.safeRange(List.of(List.of(BigDecimal.ZERO, BigDecimal.TEN)))
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(
								LabManagementErrorCode.DETECTION_INDICATOR_NAME_DUPLICATE.getCode()));

		verify(detectionIndicatorRepository, never()).save(any());
		verify(detectionIndicatorCacheManager, never()).evictByLabTypeId(any());
	}

	@Test
	void createInvalidSafeRangeShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType(1L)));

		assertThatThrownBy(() -> detectionIndicatorService.create(DetectionIndicatorCreateCommand.builder()
				.labTypeId(1L)
				.indicatorName("temperature")
				.unit("℃")
				.safeRange(List.of(List.of(BigDecimal.TEN, BigDecimal.ZERO)))
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(
								LabManagementErrorCode.DETECTION_SAFE_RANGE_INVALID.getCode()));

		verify(detectionIndicatorRepository, never()).existsByLabTypeIdAndIndicatorName(any(), any(), any());
		verify(detectionIndicatorRepository, never()).save(any());
	}

	@Test
	void createAllNullSafeRangeSegmentShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType(1L)));

		assertThatThrownBy(() -> detectionIndicatorService.create(DetectionIndicatorCreateCommand.builder()
				.labTypeId(1L)
				.indicatorName("temperature")
				.unit("℃")
				.safeRange(List.of(Arrays.asList(null, null)))
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(
								LabManagementErrorCode.DETECTION_SAFE_RANGE_INVALID.getCode()));

		verify(detectionIndicatorRepository, never()).save(any());
	}

	@Test
	void updateSuccessShouldAllowMoveLabTypeAndEvictOldAndNewCache() {
		DetectionIndicator currentIndicator = indicator(11L, 1L, "temperature", "℃");
		when(detectionIndicatorRepository.findById(11L)).thenReturn(Optional.of(currentIndicator));
		when(labTypeRepository.findById(2L)).thenReturn(Optional.of(labType(2L)));
		when(detectionIndicatorRepository.existsByLabTypeIdAndIndicatorName(2L, "humidity", 11L))
				.thenReturn(false);
		when(detectionIndicatorRepository.update(any(DetectionIndicator.class))).thenAnswer(invocation -> invocation.getArgument(0));

		DetectionIndicatorResponse response = detectionIndicatorService.update(11L,
				DetectionIndicatorUpdateCommand.builder()
						.labTypeId(2L)
						.indicatorName(" humidity ")
						.safeRange(List.of(Arrays.asList(null, BigDecimal.valueOf(80))))
						.build());

		assertThat(response.getLabTypeId()).isEqualTo(2L);
		assertThat(response.getIndicatorName()).isEqualTo("humidity");
		assertThat(response.getUnit()).isEqualTo("℃");
		assertThat(response.getSafeRange()).containsExactly(Arrays.asList(null, BigDecimal.valueOf(80)));
		ArgumentCaptor<DetectionIndicator> indicatorCaptor = ArgumentCaptor.forClass(DetectionIndicator.class);
		verify(detectionIndicatorRepository).update(indicatorCaptor.capture());
		assertThat(indicatorCaptor.getValue().getLabTypeId()).isEqualTo(2L);
		verify(detectionIndicatorCacheManager).evictByLabTypeId(1L);
		verify(detectionIndicatorCacheManager).evictByLabTypeId(2L);
	}

	@Test
	void updateNotFoundShouldThrowBusinessException() {
		when(detectionIndicatorRepository.findById(11L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> detectionIndicatorService.update(11L,
				DetectionIndicatorUpdateCommand.builder().indicatorName("humidity").build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(
								LabManagementErrorCode.DETECTION_INDICATOR_NOT_FOUND.getCode()));

		verify(detectionIndicatorRepository, never()).update(any());
		verify(detectionIndicatorCacheManager, never()).evictByLabTypeId(any());
	}

	@Test
	void deleteSuccessShouldEvictCache() {
		when(detectionIndicatorRepository.findById(11L)).thenReturn(Optional.of(indicator(11L, 1L, "temperature", "℃")));

		detectionIndicatorService.delete(11L);

		verify(detectionIndicatorRepository).deleteById(11L);
		verify(detectionIndicatorCacheManager).evictByLabTypeId(1L);
	}

	@Test
	void listByLabTypeIdShouldRequireTypeAndUseCache() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(labType(1L)));
		when(detectionIndicatorCacheManager.listByLabTypeId(eq(1L), any()))
				.thenReturn(List.of(indicator(11L, 1L, "temperature", "℃")));

		List<DetectionIndicatorResponse> responses = detectionIndicatorService.listByLabTypeId(1L);

		assertThat(responses).extracting(DetectionIndicatorResponse::getIndicatorName)
				.containsExactly("temperature");
	}

	private LabType labType(Long id) {
		return LabType.builder()
				.id(id)
				.labTypeName("化学实验室")
				.build();
	}

	private DetectionIndicator indicator(Long id, Long labTypeId, String indicatorName, String unit) {
		return DetectionIndicator.builder()
				.id(id)
				.labTypeId(labTypeId)
				.indicatorName(indicatorName)
				.unit(unit)
				.safeRange(DetectionSafeRange.builder()
						.segments(List.of(DetectionSafeRangeSegment.builder()
								.min(BigDecimal.ZERO)
								.max(BigDecimal.TEN)
								.build()))
						.build())
				.build();
	}
}
