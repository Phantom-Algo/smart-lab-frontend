package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TeachingResourceReorderRequest {

	@NotEmpty
	private List<Item> items;

	@Data
	public static class Item {

		private Long resourceId;
		private Integer sortNo;
	}
}
