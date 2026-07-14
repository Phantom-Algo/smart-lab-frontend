package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("person")
public class PersonDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String personNo;
	private String personName;
	private Integer personType;
	private Integer genderType;
	private String phone;
	private String email;
	private String idCardNoMasked;
	private String studentNo;
	private String employeeNo;
	private Long departmentId;
	private Long positionId;
	private String className;
	private String titleName;
	private Integer personStatus;
	private LocalDateTime entryAt;
	private LocalDateTime leaveAt;
	private Integer sourceType;
	private String description;
	private String extraData;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
