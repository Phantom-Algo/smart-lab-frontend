package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfileDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long userId;

	private String realName;

	private Integer genderType;

	private String phone;

	private String email;

	private String avatarUrl;

	private String studentNo;

	private String employeeNo;

	private String departmentName;

	private String className;

	private String titleName;

	private Integer profileStatus;

	private String extraData;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;
}
