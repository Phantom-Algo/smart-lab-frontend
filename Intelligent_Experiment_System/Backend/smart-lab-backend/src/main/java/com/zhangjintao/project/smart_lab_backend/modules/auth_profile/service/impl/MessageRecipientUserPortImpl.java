package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageRecipientUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageRecipientUserPortImpl implements MessageRecipientUserPort {

	private final UserAccountRepository userAccountRepository;

	@Override
	public List<Long> listAllUserIds() {
		return userAccountRepository.listActiveUserIds();
	}

	@Override
	public List<Long> listStudentUserIds() {
		return userAccountRepository.listActiveUserIdsByUserType(UserType.STUDENT.getCode());
	}

	@Override
	public List<Long> listTeacherUserIds() {
		return userAccountRepository.listActiveUserIdsByUserType(UserType.TEACHER.getCode());
	}

	@Override
	public List<Long> listManagerUserIds() {
		return userAccountRepository.listActiveUserIdsByUserType(UserType.ADMIN.getCode());
	}
}
