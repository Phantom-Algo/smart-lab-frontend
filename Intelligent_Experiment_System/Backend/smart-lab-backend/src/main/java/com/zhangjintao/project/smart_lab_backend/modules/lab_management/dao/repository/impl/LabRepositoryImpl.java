package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabLocation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabPageItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LabRepositoryImpl implements LabRepository {

	private final LabMapper labMapper;

	@Override
	public Optional<Lab> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labMapper.selectOne(new LambdaQueryWrapper<LabDO>()
						.eq(LabDO::getId, id)
						.isNull(LabDO::getDeletedAt)
						.last("limit 1")))
				.map(LabPersistenceConverter::toLab);
	}

	@Override
	public Optional<Lab> findByIdForUpdate(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labMapper.selectOne(new LambdaQueryWrapper<LabDO>()
						.eq(LabDO::getId, id)
						.isNull(LabDO::getDeletedAt)
						.last("limit 1 for update")))
				.map(LabPersistenceConverter::toLab);
	}

	@Override
	public Map<Long, Lab> mapByIdsIncludingDeleted(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Map.of();
		}
		List<Long> distinctIds = ids.stream()
				.filter(id -> id != null)
				.distinct()
				.toList();
		if (distinctIds.isEmpty()) {
			return Map.of();
		}
		return labMapper.selectBatchIds(distinctIds).stream()
				.map(LabPersistenceConverter::toLab)
				.collect(Collectors.toMap(Lab::getId, lab -> lab));
	}

	@Override
	public Lab save(Lab lab) {
		LabDO labDO = LabPersistenceConverter.toLabDO(lab);
		labMapper.insert(labDO);
		return findById(labDO.getId()).orElseGet(() -> LabPersistenceConverter.toLab(labDO));
	}

	@Override
	public Lab update(Lab lab) {
		labMapper.updateById(LabPersistenceConverter.toLabDO(lab));
		return findById(lab.getId()).orElse(lab);
	}

	@Override
	public void updateStatus(Lab lab) {
		labMapper.update(null, new LambdaUpdateWrapper<LabDO>()
				.eq(LabDO::getId, lab.getId())
				.isNull(LabDO::getDeletedAt)
				.set(LabDO::getRunningStatus, lab.getRunningStatus().getCode())
				.set(LabDO::getOpenStatus, lab.getOpenStatus().getCode()));
	}

	@Override
	public void softDelete(Lab lab) {
		labMapper.update(null, new LambdaUpdateWrapper<LabDO>()
				.eq(LabDO::getId, lab.getId())
				.isNull(LabDO::getDeletedAt)
				.set(LabDO::getRunningStatus, lab.getRunningStatus().getCode())
				.set(LabDO::getOpenStatus, lab.getOpenStatus().getCode())
				.set(LabDO::getDeletedAt, lab.getDeletedAt()));
	}

	@Override
	public PageResult<LabPageItemDTO> page(LabQueryCriteria criteria) {
		LabQueryCriteria safeCriteria = criteria == null ? LabQueryCriteria.builder().build() : criteria;
		long pageNum = safeCriteria.getPage() == null ? LabManagementConstants.DEFAULT_PAGE : safeCriteria.getPage();
		long pageSize = safeCriteria.getPageSize() == null ? LabManagementConstants.DEFAULT_PAGE_SIZE : safeCriteria.getPageSize();
		LambdaQueryWrapper<LabDO> wrapper = buildPageWrapper(safeCriteria);
		Page<LabDO> page = labMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
		List<LabPageItemDTO> records = page.getRecords().stream()
				.map(LabPersistenceConverter::toLabPageItemDTO)
				.toList();
		return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public boolean existsByLocation(LabLocation location, Long excludeId) {
		if (location == null || !location.isCompleteForUniqueCheck()) {
			return false;
		}
		LambdaQueryWrapper<LabDO> wrapper = new LambdaQueryWrapper<LabDO>()
				.eq(LabDO::getCampus, location.getCampus())
				.eq(LabDO::getBuilding, location.getBuilding())
				.eq(LabDO::getFloor, location.getFloor())
				.eq(LabDO::getRoomNo, location.getRoomNo())
				.isNull(LabDO::getDeletedAt);
		if (excludeId != null) {
			wrapper.ne(LabDO::getId, excludeId);
		}
		return labMapper.selectCount(wrapper) > 0;
	}

	@Override
	public boolean existsByLabTypeId(Long labTypeId) {
		if (labTypeId == null) {
			return false;
		}
		return labMapper.selectCount(new LambdaQueryWrapper<LabDO>()
				.eq(LabDO::getLabTypeId, labTypeId)
				.isNull(LabDO::getDeletedAt)) > 0;
	}

	private LambdaQueryWrapper<LabDO> buildPageWrapper(LabQueryCriteria criteria) {
		LambdaQueryWrapper<LabDO> wrapper = new LambdaQueryWrapper<LabDO>().isNull(LabDO::getDeletedAt);
		if (StringUtils.hasText(criteria.getKeyword())) {
			wrapper.like(LabDO::getLabName, criteria.getKeyword().trim());
		}
		wrapper.eq(StringUtils.hasText(criteria.getCampus()), LabDO::getCampus, criteria.getCampus());
		wrapper.eq(StringUtils.hasText(criteria.getBuilding()), LabDO::getBuilding, criteria.getBuilding());
		wrapper.eq(criteria.getFloor() != null, LabDO::getFloor, criteria.getFloor());
		wrapper.eq(StringUtils.hasText(criteria.getRoomNo()), LabDO::getRoomNo, criteria.getRoomNo());
		wrapper.eq(criteria.getLabTypeId() != null, LabDO::getLabTypeId, criteria.getLabTypeId());
		wrapper.eq(criteria.getRunningStatus() != null, LabDO::getRunningStatus, criteria.getRunningStatus());
		wrapper.eq(criteria.getOpenStatus() != null, LabDO::getOpenStatus, criteria.getOpenStatus());
		applyOrder(wrapper, criteria.getSortField(), criteria.getSortOrder());
		return wrapper;
	}

	private void applyOrder(LambdaQueryWrapper<LabDO> wrapper, String sortField, String sortOrder) {
		boolean asc = "asc".equalsIgnoreCase(sortOrder);
		ResolvedSortColumn orderColumn = resolveSortColumn(sortField);
		wrapper.orderBy(true, asc, orderColumn.column());
		if (!orderColumn.idColumn()) {
			wrapper.orderByDesc(LabDO::getId);
		}
	}

	private ResolvedSortColumn resolveSortColumn(String sortField) {
		String normalized = StringUtils.hasText(sortField) ? sortField.trim() : "created_at";
		normalized = normalized.replace("-", "_").toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "id" -> new ResolvedSortColumn(LabDO::getId, true);
			case "created_at", "createdat" -> new ResolvedSortColumn(LabDO::getCreatedAt, false);
			case "updated_at", "updatedat" -> new ResolvedSortColumn(LabDO::getUpdatedAt, false);
			case "lab_name", "labname" -> new ResolvedSortColumn(LabDO::getLabName, false);
			case "capacity" -> new ResolvedSortColumn(LabDO::getCapacity, false);
			case "area_m2", "aream2" -> new ResolvedSortColumn(LabDO::getAreaM2, false);
			case "floor" -> new ResolvedSortColumn(LabDO::getFloor, false);
			default -> throw new LabManagementException(LabManagementErrorCode.LAB_SORT_FIELD_INVALID,
					"不支持的实验室排序字段：" + sortField);
		};
	}

	private record ResolvedSortColumn(SFunction<LabDO, ?> column, boolean idColumn) {
	}
}
