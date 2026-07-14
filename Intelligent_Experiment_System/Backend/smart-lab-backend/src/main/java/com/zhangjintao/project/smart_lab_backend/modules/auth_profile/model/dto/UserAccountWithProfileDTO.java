package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserAccountWithProfileDTO {

	UserAccountDO userAccount;
	UserProfileDO userProfile;
}
