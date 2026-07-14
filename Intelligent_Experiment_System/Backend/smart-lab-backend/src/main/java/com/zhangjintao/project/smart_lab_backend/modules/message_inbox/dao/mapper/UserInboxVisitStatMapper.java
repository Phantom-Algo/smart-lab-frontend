package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.UserInboxVisitStatDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserInboxVisitStatMapper extends BaseMapper<UserInboxVisitStatDO> {

	@Select("""
			SELECT last_viewed_at
			FROM user_inbox_visit_stat
			WHERE user_id = #{userId}
			LIMIT 1
			""")
	LocalDateTime selectLastViewedAt(@Param("userId") Long userId);

	@Insert("""
			INSERT INTO user_inbox_visit_stat (user_id, last_viewed_at)
			VALUES (#{userId}, #{lastViewedAt})
			ON DUPLICATE KEY UPDATE
				last_viewed_at = GREATEST(last_viewed_at, VALUES(last_viewed_at)),
				updated_at = CURRENT_TIMESTAMP(3)
			""")
	int upsertLastViewedAt(@Param("userId") Long userId,
			@Param("lastViewedAt") LocalDateTime lastViewedAt);
}
