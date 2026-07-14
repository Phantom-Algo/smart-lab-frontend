package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.config;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserProfileRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.AccountStatus;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.RandomSecretUtils;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 测试数据初始化器：启动时自动插入 3 个测试用户（学生 / 教师 / 管理员）。
 * <p>
 * 同时插入 person → user_account → user_profile 三条记录，
 * 仅在用户不存在时插入，保证幂等。
 * </p>
 * <p>
 * <b>测试账号一览</b>：
 * <table>
 *   <tr><th>用户名</th><th>密码</th><th>角色</th><th>姓名</th></tr>
 *   <tr><td>student001</td><td>Test123456</td><td>学生</td><td>张同学</td></tr>
 *   <tr><td>teacher001</td><td>Test123456</td><td>教师</td><td>李老师</td></tr>
 *   <tr><td>admin001</td><td>Test123456</td><td>管理员</td><td>王管理员</td></tr>
 * </table>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataInitializer {

	private final PersonRepository personRepository;
	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;
	private final PasswordEncoder passwordEncoder;

	private static final String DEFAULT_PASSWORD = "Test123456";

	@PostConstruct
	@Transactional
	public void initTestUsers() {
		ensureTestUser("student001", DEFAULT_PASSWORD, UserType.STUDENT,
				"张同学", "2024001", null, "计算机科学与技术", "计科2401班", null);
		ensureTestUser("teacher001", DEFAULT_PASSWORD, UserType.TEACHER,
				"李老师", null, "T2024001", "计算机科学与技术", null, "副教授");
		ensureTestUser("admin001", DEFAULT_PASSWORD, UserType.ADMIN,
				"王管理员", null, "A2024001", "实验室管理中心", null, null);
		log.info("测试用户初始化完成：student001 / teacher001 / admin001，密码均为 {}", DEFAULT_PASSWORD);
	}

	private void ensureTestUser(String username, String rawPassword, UserType userType,
	                            String realName, String studentNo, String employeeNo,
	                            String departmentName, String className, String titleName) {
		if (userAccountRepository.findByUsername(username).isPresent()) {
			return; // 已存在，跳过
		}

		// 1. 插入人员档案（person）
		PersonDO person = new PersonDO();
		person.setPersonNo("P-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		person.setPersonName(realName);
		person.setPersonType(userType.getCode());
		person.setGenderType(userType == UserType.TEACHER ? 1 : 2); // 男/女错开
		person.setStudentNo(studentNo);
		person.setEmployeeNo(employeeNo);
		person.setDepartmentId(0L);  // 占位：未关联具体部门
		person.setClassName(className);
		person.setTitleName(titleName);
		person.setPersonStatus(1);   // 正常
		person.setSourceType(1);     // 手工新增
		person.setCreatedAt(LocalDateTime.now());
		person.setUpdatedAt(LocalDateTime.now());
		personRepository.insert(person);

		// 2. 插入用户账号（user_account）
		String salt = RandomSecretUtils.randomToken(12);
		String passwordHash = passwordEncoder.encode(rawPassword + salt);
		String accountNo = (userType == UserType.STUDENT ? "STU-" : userType == UserType.TEACHER ? "TCH-" : "ADM-")
				+ UUID.randomUUID().toString().substring(0, 8).toUpperCase();

		UserAccountDO account = new UserAccountDO();
		account.setAccountNo(accountNo);
		account.setUsername(username);
		account.setPersonId(person.getId());
		account.setPasswordHash(passwordHash);
		account.setPasswordSalt(salt);
		account.setUserType(userType.getCode());
		account.setAccountStatus(AccountStatus.NORMAL.getCode());
		account.setFailedLoginCount(0);
		account.setIsPasswordResetRequired(0);
		account.setCreatedAt(LocalDateTime.now());
		account.setUpdatedAt(LocalDateTime.now());
		userAccountRepository.insert(account);

		// 3. 插入个人信息（user_profile）
		UserProfileDO profile = new UserProfileDO();
		profile.setUserId(account.getId());
		profile.setRealName(realName);
		profile.setGenderType(userType == UserType.TEACHER ? 1 : 2);
		profile.setDepartmentName(departmentName);
		profile.setStudentNo(studentNo);
		profile.setEmployeeNo(employeeNo);
		profile.setClassName(className);
		profile.setTitleName(titleName);
		profile.setProfileStatus(1);
		profile.setCreatedAt(LocalDateTime.now());
		profile.setUpdatedAt(LocalDateTime.now());
		userProfileRepository.insert(profile);

		log.info("已创建测试用户: username={}, userType={}, realName={}, personId={}",
				username, userType.getDescription(), realName, person.getId());
	}
}
