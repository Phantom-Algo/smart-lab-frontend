package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service;

import java.util.List;

/**
 * 人员/用户模块提供的用户查询接口，消息模块只依赖该接口，不直接访问其它模块 DAO。
 */
public interface MessageRecipientUserPort {

	List<Long> listAllUserIds();

	List<Long> listStudentUserIds();

	List<Long> listTeacherUserIds();

	List<Long> listManagerUserIds();
}
