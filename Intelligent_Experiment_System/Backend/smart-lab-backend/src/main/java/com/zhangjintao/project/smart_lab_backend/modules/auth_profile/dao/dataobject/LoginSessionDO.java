package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_session")
public class LoginSessionDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long userId;

	private String sessionNo;

	private String accessTokenHash;

	private String refreshTokenHash;

	private Integer sessionStatus;

	private LocalDateTime loginAt;

	private LocalDateTime expireAt;

	private LocalDateTime logoutAt;

	private String ipAddress;

	private String deviceType;

	private String userAgent;

	private String extraData;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;
}
