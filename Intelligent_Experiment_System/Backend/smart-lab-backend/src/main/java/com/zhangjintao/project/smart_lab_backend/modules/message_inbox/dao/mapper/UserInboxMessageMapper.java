package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.UserInboxMessageDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserInboxMessageMapper extends BaseMapper<UserInboxMessageDO> {

	@Insert("""
			<script>
			INSERT IGNORE INTO user_inbox_message
				(user_id, message_id, is_read, is_deleted, received_at)
			VALUES
			<foreach collection="list" item="item" separator=",">
				(#{item.userId}, #{item.messageId}, #{item.isRead}, #{item.isDeleted}, #{item.receivedAt})
			</foreach>
			</script>
			""")
	int insertIgnoreBatch(@Param("list") List<UserInboxMessageDO> list);

	@Select("""
			<script>
			SELECT
				uim.id AS inboxMessageId,
				uim.message_id AS messageId,
				me.title AS title,
				me.priority AS priority,
				me.message_type AS messageType,
				me.occurred_at AS sentAt,
				uim.received_at AS receivedAt,
				uim.is_read AS readFlag,
				uim.read_at AS readAt
			FROM user_inbox_message uim
			INNER JOIN msg_event me ON me.id = uim.message_id
			WHERE uim.user_id = #{userId}
				AND uim.is_deleted = 0
				<if test="priority != null">
					AND me.priority = #{priority}
				</if>
				<if test="messageType != null and messageType != ''">
					AND me.message_type = #{messageType}
				</if>
				<if test="isRead != null">
					AND uim.is_read = #{isRead}
				</if>
			ORDER BY uim.received_at DESC, uim.id DESC
			</script>
			""")
	Page<InboxMessageQueryDTO> selectInboxMessagePage(Page<InboxMessageQueryDTO> page, @Param("userId") Long userId,
			@Param("priority") Integer priority, @Param("messageType") String messageType,
			@Param("isRead") Integer isRead);

	@Select("""
			<script>
			SELECT id
			FROM user_inbox_message
			WHERE user_id = #{userId}
				AND is_deleted = 0
				AND is_read = 0
			ORDER BY received_at DESC, id DESC
			</script>
			""")
	List<Long> selectNewInboxMessageIds(@Param("userId") Long userId,
			@Param("lastViewedAt") LocalDateTime lastViewedAt);

	@Select("""
			SELECT
				uim.id AS inboxMessageId,
				uim.message_id AS messageId,
				me.title AS title,
				me.priority AS priority,
				me.message_type AS messageType,
				me.occurred_at AS sentAt,
				uim.received_at AS receivedAt,
				uim.is_read AS readFlag,
				uim.read_at AS readAt,
				me.content AS content,
				me.content_format AS contentFormat,
				me.action_url AS actionUrl
			FROM user_inbox_message uim
			INNER JOIN msg_event me ON me.id = uim.message_id
			WHERE uim.user_id = #{userId}
				AND uim.id = #{inboxMessageId}
				AND uim.is_deleted = 0
			""")
	InboxMessageQueryDTO selectActiveDetail(@Param("userId") Long userId,
			@Param("inboxMessageId") Long inboxMessageId);

	@Select("""
			<script>
			SELECT COUNT(*)
			FROM user_inbox_message
			WHERE user_id = #{userId}
				AND is_deleted = 0
				AND id IN
				<foreach collection="inboxMessageIds" item="id" open="(" separator="," close=")">
					#{id}
				</foreach>
			</script>
			""")
	long countActiveByUserIdAndIds(@Param("userId") Long userId,
			@Param("inboxMessageIds") List<Long> inboxMessageIds);

	@Update("""
			UPDATE user_inbox_message
			SET is_read = 1,
				read_at = #{readAt}
			WHERE user_id = #{userId}
				AND id = #{inboxMessageId}
				AND is_deleted = 0
				AND is_read = 0
			""")
	int markRead(@Param("userId") Long userId, @Param("inboxMessageId") Long inboxMessageId,
			@Param("readAt") LocalDateTime readAt);

	@Update("""
			UPDATE user_inbox_message
			SET is_deleted = 1,
				deleted_at = #{deletedAt}
			WHERE user_id = #{userId}
				AND id = #{inboxMessageId}
				AND is_deleted = 0
			""")
	int softDelete(@Param("userId") Long userId, @Param("inboxMessageId") Long inboxMessageId,
			@Param("deletedAt") LocalDateTime deletedAt);

	@Update("""
			<script>
			UPDATE user_inbox_message
			SET is_deleted = 1,
				deleted_at = #{deletedAt}
			WHERE user_id = #{userId}
				AND is_deleted = 0
				AND id IN
				<foreach collection="inboxMessageIds" item="id" open="(" separator="," close=")">
					#{id}
				</foreach>
			</script>
			""")
	int softDeleteBatch(@Param("userId") Long userId,
			@Param("inboxMessageIds") List<Long> inboxMessageIds,
			@Param("deletedAt") LocalDateTime deletedAt);
}
