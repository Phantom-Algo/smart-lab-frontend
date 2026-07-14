<template>
  <div class="user-layout">
    <!-- 左侧边栏 -->
    <aside class="user-sidebar">
      <div class="sidebar-profile">
        <div class="profile-avatar"><span class="profile-avatar-text">{{ currentUser.realName.charAt(0) }}</span></div>
        <div class="profile-username">{{ currentUser.realName }}</div>
        <div class="profile-role" :class="roleBadgeClass">{{ roleLabel }}</div>
      </div>
      <nav class="sidebar-tabs">
        <div v-for="tab in visibleTabs" :key="tab.key" class="sidebar-tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
          <span class="tab-label">{{ tab.label }}</span>
        </div>
      </nav>
      <div class="sidebar-footer">
        <button class="back-btn" @click="$emit('back')">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          回到首页
        </button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="user-content">
      <!-- ===== 用户信息 ===== -->
      <div v-if="activeTab === 'info'" class="tab-panel">
        <h3 class="panel-title">用户信息</h3>
        <div class="info-cards">
          <div class="info-row-2">
            <div class="info-card">
              <div class="info-card-title">基本信息</div>
              <div class="info-grid cols-3">
                <div class="info-item"><span class="info-label">真实姓名</span><span class="info-value">{{ currentUser.realName }}</span></div>
                <div class="info-item"><span class="info-label">性别</span><span class="info-value">{{ genderLabel(currentUser.genderType) }}</span></div>
                <div class="info-item"><span class="info-label">手机号</span><span class="info-value">{{ currentUser.phone || '未设置' }}</span></div>
                <div class="info-item"><span class="info-label">邮箱</span><span class="info-value">{{ currentUser.email || '未设置' }}</span></div>
              </div>
            </div>
            <div class="info-card">
              <div class="info-card-title">身份信息</div>
              <div class="info-grid cols-3">
                <div class="info-item"><span class="info-label">用户类型</span><span class="info-value">{{ roleLabel }}</span></div>
                <div v-if="currentUser.employeeNo" class="info-item"><span class="info-label">工号</span><span class="info-value">{{ currentUser.employeeNo }}</span></div>
                <div v-if="currentUser.studentNo" class="info-item"><span class="info-label">学号</span><span class="info-value">{{ currentUser.studentNo }}</span></div>
                <div class="info-item"><span class="info-label">院系/部门</span><span class="info-value">{{ currentUser.departmentName || '未设置' }}</span></div>
                <div v-if="currentUser.className" class="info-item"><span class="info-label">班级</span><span class="info-value">{{ currentUser.className }}</span></div>
                <div v-if="currentUser.titleName" class="info-item"><span class="info-label">职称</span><span class="info-value">{{ currentUser.titleName }}</span></div>
              </div>
            </div>
          </div>
          <div class="info-card info-card-wide">
            <div class="info-card-title">账号信息</div>
            <div class="info-grid cols-4">
              <div class="info-item"><span class="info-label">登录账号</span><span class="info-value">{{ currentUser.username }}</span></div>
              <div class="info-item"><span class="info-label">账号编号</span><span class="info-value">{{ currentUser.accountNo }}</span></div>
              <div class="info-item"><span class="info-label">账号状态</span><span class="info-value status-active">正常</span></div>
              <div class="info-item" v-if="currentUser.lastLoginAt">
                <span class="info-label">最后登录</span><span class="info-value">{{ currentUser.lastLoginAt }}</span>
              </div>
              <div class="info-item" v-if="currentUser.lastLoginIp">
                <span class="info-label">最后登录IP</span><span class="info-value">{{ currentUser.lastLoginIp }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ===== 设置 ===== -->
      <div v-if="activeTab === 'settings'" class="tab-panel">
        <h3 class="panel-title">账号设置</h3>
        <div class="settings-list">
          <div class="settings-item"><div class="settings-item-info"><div class="settings-item-title">修改密码</div><div class="settings-item-desc">定期更换密码以保证账号安全</div></div><button class="settings-btn" @click="showChangePwd = true">修改</button></div>
          <div class="settings-item"><div class="settings-item-info"><div class="settings-item-title">手机号</div><div class="settings-item-desc">{{ currentUser.phone || '未绑定' }}</div></div><button class="settings-btn">绑定</button></div>
          <div class="settings-item"><div class="settings-item-info"><div class="settings-item-title">邮箱</div><div class="settings-item-desc">{{ currentUser.email || '未绑定' }}</div></div><button class="settings-btn">绑定</button></div>
        </div>
      </div>

      <!-- ===== 隐私 ===== -->
      <div v-if="activeTab === 'privacy'" class="tab-panel">
        <h3 class="panel-title">隐私设置</h3>
        <div class="settings-list">
          <div class="settings-item"><div class="settings-item-info"><div class="settings-item-title">个人信息可见范围</div><div class="settings-item-desc">控制其他用户可以看到您的哪些信息</div></div><select class="settings-select"><option>仅自己</option><option>同院系可见</option><option>所有人可见</option></select></div>
          <div class="settings-item"><div class="settings-item-info"><div class="settings-item-title">实验记录可见性</div><div class="settings-item-desc">控制您的实验记录对谁可见</div></div><select class="settings-select"><option>仅导师可见</option><option>课题组成员可见</option><option>所有人可见</option></select></div>
        </div>
      </div>
    </main>

    <!-- 修改密码弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showChangePwd" class="popup-overlay" @click.self="showChangePwd = false">
          <div class="popup-panel">
            <div class="popup-header"><h3>修改密码</h3><button class="popup-close" @click="showChangePwd = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="popup-body">
              <div class="form-group"><label>当前密码</label><input v-model="pwdForm.current" type="password" class="form-input" placeholder="请输入当前密码" /></div>
              <div class="form-group"><label>新密码</label><input v-model="pwdForm.newPwd" type="password" class="form-input" placeholder="请输入新密码" /></div>
              <div class="form-group"><label>确认新密码</label><input v-model="pwdForm.confirm" type="password" class="form-input" placeholder="请再次输入新密码" /></div>
            </div>
            <div class="popup-footer"><button class="popup-btn cancel" @click="showChangePwd = false">取消</button><button class="popup-btn primary" @click="handleChangePwd" :disabled="pwdLoading">{{ pwdLoading ? '修改中...' : '确认修改' }}</button></div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { userRole, roleLabel, isMockMode } from '../stores/authStore'
import { getMyProfile, changePassword, type MyProfileResponse } from '../utils/authApi'

defineEmits(['back'])

const activeTab = ref('info')
const showChangePwd = ref(false)
const pwdLoading = ref(false)

const roleMap: Record<number, string> = { 1: '学生', 2: '教师', 3: '管理员' }

// 真实 profile 数据（API 模式）
const apiProfile = ref<MyProfileResponse | null>(null)

onMounted(async () => {
  if (!isMockMode.value) {
    try { apiProfile.value = await getMyProfile() } catch { /* fallback to mock */ }
  }
})

// 三套模拟用户数据（mock 模式）
const userProfiles: Record<number, any> = {
  1: { realName: '张同学', genderType: 1, phone: '139****1234', email: 'zhang@stu.edu.cn', studentNo: 'S20240001', employeeNo: '', departmentName: '计算机科学与技术学院', className: '计科2401班', titleName: '', username: 'zhang_stu', accountNo: 'ACC-2024-0101', lastLoginAt: '2026-07-08 08:15:00', lastLoginIp: '10.0.0.15' },
  2: { realName: '李老师', genderType: 2, phone: '136****5678', email: 'li@lab.edu.cn', studentNo: '', employeeNo: 'E20240005', departmentName: '化学与材料学院', className: '', titleName: '副教授', username: 'li_teacher', accountNo: 'ACC-2024-0005', lastLoginAt: '2026-07-08 09:30:00', lastLoginIp: '192.168.1.50' },
  3: { realName: '王管理员', genderType: 1, phone: '138****9012', email: 'wang@lab.edu.cn', studentNo: '', employeeNo: 'E20240001', departmentName: '实验室管理中心', className: '', titleName: '高级实验师', username: 'admin', accountNo: 'ACC-2024-0001', lastLoginAt: '2026-07-08 09:30:00', lastLoginIp: '192.168.1.100' },
}

const currentUser = computed(() => {
  if (apiProfile.value) {
    const p = apiProfile.value
    return {
      realName: p.realName || p.username,
      genderType: p.genderType,
      phone: p.phone || '',
      email: p.email || '',
      studentNo: p.studentNo || '',
      employeeNo: p.employeeNo || '',
      departmentName: p.departmentName || '',
      className: p.className || '',
      titleName: p.titleName || '',
      username: p.username,
      accountNo: p.userId ? 'ACC-2024-' + String(p.userId).padStart(4,'0') : '',
      lastLoginAt: '',
      lastLoginIp: '',
    }
  }
  return userProfiles[userRole.value] || userProfiles[3]
})

// 管理员可管理的账号列表（模拟）- 已移至独立的 AccountManage 组件

const visibleTabs = computed(() => {
  const tabs = [
    { key: 'info', label: '用户信息' },
    { key: 'settings', label: '设置' },
    { key: 'privacy', label: '隐私' },
  ]
  return tabs
})

const roleBadgeClass = computed(() => {
  if (userRole.value === 1) return 'role-student'
  if (userRole.value === 2) return 'role-teacher'
  return 'role-admin'
})

function genderLabel(type: number): string { const m: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }; return m[type] || '未知' }

// 修改密码
const pwdForm = reactive({ current: '', newPwd: '', confirm: '' })
async function handleChangePwd(): Promise<void> {
  if (!pwdForm.current || !pwdForm.newPwd) return
  if (pwdForm.newPwd !== pwdForm.confirm) { alert('两次新密码不一致'); return }
  pwdLoading.value = true
  try {
    await changePassword({ oldPassword: pwdForm.current, newPassword: pwdForm.newPwd, confirmPassword: pwdForm.confirm })
    showChangePwd.value = false
    pwdForm.current = ''; pwdForm.newPwd = ''; pwdForm.confirm = ''
    alert('密码修改成功')
  } catch { alert('密码修改失败') }
  finally { pwdLoading.value = false }
}
</script>

<style scoped>
.user-layout{ display:flex; height:100% }
.user-sidebar{ width:240px; flex-shrink:0; display:flex; flex-direction:column; border-right:1px solid rgba(0,0,0,.06); background:rgba(255,255,255,.1) }
.sidebar-profile{ padding:32px 20px 20px; text-align:center; border-bottom:1px solid rgba(0,0,0,.06) }
.profile-avatar{ width:72px; height:72px; border-radius:50%; margin:0 auto 12px; background:linear-gradient(135deg,#4A90D9,#357ABD); display:flex; align-items:center; justify-content:center }
.profile-avatar-text{ font-size:28px; font-weight:600; color:#fff }
.profile-username{ font-size:16px; font-weight:600; color:#1a1a2e; margin-bottom:4px }
.profile-role{ font-size:12px; font-weight:500; padding:2px 12px; border-radius:10px; display:inline-block }
.role-admin{ background:rgba(233,30,99,.12); color:#c2185b }
.role-teacher{ background:rgba(26,115,232,.1); color:#1557b0 }
.role-student{ background:rgba(76,175,80,.12); color:#2e7d32 }

.sidebar-tabs{ flex:1; padding:4px 12px; display:flex; flex-direction:column; gap:2px }
.sidebar-tab{ display:flex; align-items:center; padding:10px 12px; border-radius:8px; cursor:pointer; transition:all .15s; color:#555; font-size:14px; user-select:none }
.sidebar-tab:hover{ background:rgba(26,115,232,.06); color:#1a73e8 }
.sidebar-tab.active{ background:rgba(26,115,232,.1); color:#1a73e8; font-weight:500 }
.sidebar-footer{ padding:16px 12px; border-top:1px solid rgba(0,0,0,.06) }
.back-btn{ display:flex; align-items:center; justify-content:center; gap:6px; width:100%; padding:10px 0; border:none; border-radius:8px; background:rgba(0,0,0,.04); color:#666; font-size:13px; cursor:pointer; transition:all .15s }
.back-btn:hover{ background:rgba(26,115,232,.08); color:#1a73e8 }

.user-content{ flex:1; overflow-y:auto; padding:24px 32px }
.tab-panel{ }
.panel-title{ font-size:16px; font-weight:600; color:#1a1a2e; margin:0 0 20px }
.info-cards{ display:flex; flex-direction:column; gap:16px }
.info-row-2{ display:grid; grid-template-columns:1fr 1fr; gap:16px }
.info-card{ background:rgba(255,255,255,.6); border-radius:10px; padding:20px }
.info-card-wide{ }
.info-card-title{ font-size:14px; font-weight:600; color:#1a73e8; margin-bottom:14px; padding-bottom:10px; border-bottom:1px solid rgba(0,0,0,.06) }
.info-grid{ display:grid; grid-template-columns:repeat(2,1fr); gap:12px 24px }
.info-grid.cols-3{ grid-template-columns:repeat(3,1fr) }
.info-grid.cols-4{ grid-template-columns:repeat(4,1fr) }
.info-item{ display:flex; flex-direction:column; gap:2px; min-width:0 }
.info-label{ font-size:12px; color:#999; white-space:nowrap }
.info-value{ font-size:14px; color:#333; font-weight:500; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.status-active{ color:#27ae60 }

.settings-list{ max-width:560px; display:flex; flex-direction:column; gap:2px }
.settings-item{ display:flex; align-items:center; justify-content:space-between; padding:16px 20px; background:rgba(255,255,255,.6); border-radius:10px }
.settings-item-info{ flex:1 }
.settings-item-title{ font-size:14px; font-weight:500; color:#222; margin-bottom:2px }
.settings-item-desc{ font-size:12px; color:#999 }
.settings-btn{ padding:6px 18px; border:1px solid rgba(0,0,0,.12); border-radius:8px; background:#fff; color:#555; font-size:13px; cursor:pointer; transition:all .15s }
.settings-btn:hover{ border-color:#1a73e8; color:#1a73e8 }
.settings-select{ padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; color:#555; outline:none; background:#fff }

/* 弹窗 */
.popup-overlay{ position:fixed; inset:0; background:rgba(0,0,0,.3); display:flex; align-items:center; justify-content:center; z-index:1000 }
.popup-panel{ background:#fff; border-radius:16px; width:440px; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.popup-header{ display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.popup-header h3{ margin:0; font-size:17px; color:#1a1a2e }
.popup-close{ background:none; border:none; cursor:pointer; color:#999; padding:4px; border-radius:6px }
.popup-close:hover{ background:rgba(0,0,0,.05); color:#333 }
.popup-body{ padding:20px 24px }
.form-group{ display:flex; flex-direction:column; gap:6px; margin-bottom:14px }
.form-group label{ font-size:13px; font-weight:500; color:#555 }
.form-input{ padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:14px; outline:none; transition:border .15s }
.form-input:focus{ border-color:#1a73e8 }
.popup-footer{ padding:0 24px 20px; display:flex; justify-content:flex-end; gap:10px }
.popup-btn{ padding:10px 24px; border-radius:8px; font-size:14px; cursor:pointer; border:none; transition:all .15s }
.popup-btn.cancel{ background:rgba(0,0,0,.04); color:#666 }
.popup-btn.cancel:hover{ background:rgba(0,0,0,.08) }
.popup-btn.primary{ background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.popup-btn.primary:hover{ box-shadow:0 4px 12px rgba(26,115,232,.3) }
.popup-fade-enter-active,.popup-fade-leave-active{ transition:all .2s ease }
.popup-fade-enter-from,.popup-fade-leave-to{ opacity:0 }
</style>
