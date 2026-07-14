package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabScheduleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

@Mapper
public interface LabScheduleMapper extends BaseMapper<LabScheduleDO> {

	@Insert("""
			<script>
			INSERT INTO lab_schedule (lab_id, day_of_week, open_time, close_time)
			VALUES
			<foreach collection="schedules" item="schedule" separator=",">
				(#{schedule.labId}, #{schedule.dayOfWeek}, #{schedule.openTime}, #{schedule.closeTime})
			</foreach>
			ON DUPLICATE KEY UPDATE
				open_time = VALUES(open_time),
				close_time = VALUES(close_time),
				updated_at = CURRENT_TIMESTAMP
			</script>
			""")
	int upsertBatch(@Param("schedules") List<LabScheduleDO> schedules);
}
