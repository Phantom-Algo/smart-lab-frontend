package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AccountBindingDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.CreatePersonAccountCommand;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.PersonProfileSyncCommand;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface PersonAccountPort {

	AccountBindingDTO createAccount(CreatePersonAccountCommand command);

	void disableAccountByPersonId(Long personId, String reason);

	void enableAccountByPersonId(Long personId);

	Optional<AccountBindingDTO> getAccountBindingByPersonId(Long personId);

	Optional<AccountBindingDTO> getAccountBindingByUserId(Long userId);

	Map<Long, AccountBindingDTO> getAccountBindingsByPersonIds(Collection<Long> personIds);

	void syncProfileByPerson(PersonProfileSyncCommand command);
}
