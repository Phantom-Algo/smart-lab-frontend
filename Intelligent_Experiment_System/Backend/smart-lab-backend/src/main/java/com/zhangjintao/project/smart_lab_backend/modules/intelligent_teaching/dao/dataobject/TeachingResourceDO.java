package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teaching_resource")
public class TeachingResourceDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String resourceNo;
	private Long courseId;
	private String resourceName;
	private Integer resourceType;
	private String fileUrl;
	private String externalUrl;
	private Long fileSize;
	private Integer sortNo;
	private Long uploadedBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
