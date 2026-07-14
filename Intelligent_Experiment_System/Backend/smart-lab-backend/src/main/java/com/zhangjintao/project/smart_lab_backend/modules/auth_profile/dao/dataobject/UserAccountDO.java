package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_account")
public class UserAccountDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String accountNo;

	private String username;

	private Long personId;

	private String passwordHash;

	private String passwordSalt;

	private Integer userType;

	private Integer accountStatus;

	private Integer failedLoginCount;

	private LocalDateTime lockExpiresAt;

	private LocalDateTime lastLoginAt;

	private String lastLoginIp;

	private LocalDateTime lastPasswordChangedAt;

	private Integer isPasswordResetRequired;

	private String extraData;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;
}
