package com.zhangjintao.project.smart_lab_backend.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> {

	private final List<T> records;
	private final long total;
	private final long pageNum;
	private final long pageSize;
	private final long pages;

	private PageResult(List<T> records, long total, long pageNum, long pageSize) {
		this.records = records == null ? List.of() : records;
		this.total = total;
		this.pageNum = pageNum;
		this.pageSize = pageSize;
		this.pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
	}

	public static <T> PageResult<T> of(List<T> records, long total, long pageNum, long pageSize) {
		return new PageResult<>(records, total, pageNum, pageSize);
	}

	public static <T> PageResult<T> empty(long pageNum, long pageSize) {
		return new PageResult<>(List.of(), 0, pageNum, pageSize);
	}

	public List<T> getRecords() {
		return records;
	}

	public long getTotal() {
		return total;
	}

	public long getPageNum() {
		return pageNum;
	}

	public long getPageSize() {
		return pageSize;
	}

	public long getPages() {
		return pages;
	}
}
