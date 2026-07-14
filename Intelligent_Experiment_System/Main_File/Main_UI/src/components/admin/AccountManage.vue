<template>
  <div class="am-page">
    <div class="page-header">
      <h2>账号管理</h2>
      <p>创建/启停账号、分配权限</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num">{{ accounts.length }}</div>
        <div class="stat-label">账号总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ activeCount }}</div>
        <div class="stat-label">正常启用</div>
      </div>
      <div class="stat-card">
        <div class="stat-num orange">{{ disabledCount }}</div>
        <div class="stat-label">已停用</div>
      </div>
      <div class="stat-card">
        <div class="stat-num red">{{ lockedCount }}</div>
        <div class="stat-label">锁定中</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="searchText" placeholder="搜索用户名..." class="search-input" @keyup.enter="pageNum = 1" />
        <select v-model="filterType" class="tool-select">
          <option value="">全部角色</option>
          <option :value="1">学生</option>
          <option :value="2">教师</option>
          <option :value="3">管理员</option>
        </select>
        <select v-model="filterStatus" class="tool-select">
          <option value="">全部状态</option>
          <option :value="1">正常</option>
          <option :value="2">停用</option>
          <option :value="3">锁定</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 个账号</span>
        <button class="add-btn" @click="openCreateDialog">+ 创建账号</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="data-table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>用户名</th><th>姓名</th><th>角色</th><th>状态</th><th>最后登录</th><th>登录IP</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="a in pagedList" :key="a.id">
            <td><span class="acct-name">{{ a.username }}</span></td>
            <td>{{ a.realName }}</td>
            <td><span class="type-tag" :class="'type-' + a.userType">{{ roleLabel(a.userType) }}</span></td>
            <td><span class="status-dot" :class="statusClass(a.status)"></span> {{ statusLabel(a.status) }}</td>
            <td>{{ a.lastLoginAt || '--' }}</td>
            <td>{{ a.lastLoginIp || '--' }}</td>
            <td class="action-cell">
              <button v-if="a.status === 1" class="row-btn danger" @click="toggleStatus(a)">停用</button>
              <button v-else class="row-btn" @click="toggleStatus(a)">启用</button>
              <button class="row-btn" @click="resetPwd(a)">重置密码</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="7" class="empty-cell">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 创建账号弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>创建账号</h3>
              <button class="modal-close" @click="showCreate = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>选择人员 <span class="required">*</span></label>
                <select v-model="form.personId" class="form-input">
                  <option :value="0" disabled>请选择人员</option>
                  <option v-for="p in personOptions" :key="p.id" :value="p.id">{{ p.name }} ({{ roleLabel(p.type) }}, {{ p.dept }})</option>
                </select>
              </div>
              <div class="form-g">
                <label>用户名 <span class="required">*</span></label>
                <input v-model="form.username" class="form-input" placeholder="请输入登录用户名" maxlength="64" />
              </div>
              <div class="form-g">
                <label>用户角色 <span class="required">*</span></label>
                <div class="radio-group">
                  <label class="radio-item"><input type="radio" v-model="form.userType" :value="1" /> 学生</label>
                  <label class="radio-item"><input type="radio" v-model="form.userType" :value="2" /> 教师</label>
                  <label class="radio-item"><input type="radio" v-model="form.userType" :value="3" /> 管理员</label>
                </div>
              </div>
              <div class="form-g">
                <label>初始密码</label>
                <div class="pwd-row">
                  <input v-model="form.password" class="form-input" type="text" placeholder="输入或生成随机密码" />
                  <button class="gen-btn" @click="generatePwd">生成随机</button>
                </div>
              </div>
              <div class="form-check">
                <input type="checkbox" v-model="form.forceReset" id="forceReset" />
                <label for="forceReset">首次登录需修改密码</label>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showCreate = false">取消</button>
              <button class="modal-btn primary" @click="submitCreate" :disabled="!canCreate">确认创建</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

// ========== Mock 数据 ==========
interface Account {
  id: number; username: string; realName: string; userType: number
  status: number; lastLoginAt: string; lastLoginIp: string
}
const accounts = ref<Account[]>([
  { id:1, username:'admin', realName:'王管理员', userType:3, status:1, lastLoginAt:'2026-07-09 10:19', lastLoginIp:'0:0:0:0:0:0:0:1' },
  { id:2, username:'zhang_stu', realName:'张同学', userType:1, status:1, lastLoginAt:'2026-07-08 08:15', lastLoginIp:'10.0.0.15' },
  { id:3, username:'li_teacher', realName:'李老师', userType:2, status:1, lastLoginAt:'2026-07-08 09:30', lastLoginIp:'192.168.1.50' },
  { id:4, username:'zhao_stu', realName:'赵同学', userType:1, status:2, lastLoginAt:'--', lastLoginIp:'--' },
  { id:5, username:'wang_stu', realName:'王同学', userType:1, status:1, lastLoginAt:'2026-07-07 14:00', lastLoginIp:'10.0.0.20' },
  { id:6, username:'sun_teacher', realName:'孙老师', userType:2, status:3, lastLoginAt:'--', lastLoginIp:'--' },
])

const personOptions = [
  { id:1, name:'张同学', type:1, dept:'计算机科学与技术学院' },
  { id:2, name:'李老师', type:2, dept:'化学与材料学院' },
  { id:3, name:'赵同学', type:1, dept:'物理学院' },
  { id:4, name:'刘同学', type:1, dept:'计算机科学与技术学院' },
]

// ========== 筛选/分页 ==========
const searchText = ref('')
const filterType = ref<number | ''>('')
const filterStatus = ref<number | ''>('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = accounts.value
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(a => a.username.toLowerCase().includes(kw) || a.realName.includes(kw))
  }
  if (filterType.value !== '') list = list.filter(a => a.userType === filterType.value)
  if (filterStatus.value !== '') list = list.filter(a => a.status === filterStatus.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize))
const pagedList = computed(() => {
  const s = (pageNum.value - 1) * pageSize
  return filteredList.value.slice(s, s + pageSize)
})

const activeCount = computed(() => accounts.value.filter(a => a.status === 1).length)
const disabledCount = computed(() => accounts.value.filter(a => a.status === 2).length)
const lockedCount = computed(() => accounts.value.filter(a => a.status === 3).length)

// ========== 创建弹窗 ==========
const showCreate = ref(false)
const form = reactive({ personId: 0, username: '', userType: 1, password: '', forceReset: true })
const canCreate = computed(() => form.personId > 0 && form.username.trim().length > 0)

function openCreateDialog() {
  form.personId = 0; form.username = ''; form.userType = 1; form.password = ''; form.forceReset = true
  showCreate.value = true
}
function generatePwd() {
  const chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefghjkmnpqrstwxyz2345678'
  let pwd = ''
  for (let i = 0; i < 10; i++) pwd += chars[Math.floor(Math.random() * chars.length)]
  form.password = pwd
}
function submitCreate() {
  if (!canCreate.value) return
  const person = personOptions.find(p => p.id === form.personId)
  accounts.value.unshift({
    id: Date.now(), username: form.username.trim(), realName: person?.name || '',
    userType: form.userType, status: 1,
    lastLoginAt: '--', lastLoginIp: '--',
  })
  showCreate.value = false
}

// ========== 操作 ==========
function toggleStatus(a: Account) {
  a.status = a.status === 1 ? 2 : 1
}
function resetPwd(a: Account) {
  const newPwd = 'reset' + Math.random().toString(36).slice(2, 8)
  alert('已重置 ' + a.username + ' 的密码为: ' + newPwd + '\n（实际将调用后端 API）')
}

function roleLabel(t: number) { const m: Record<number,string> = {1:'学生',2:'教师',3:'管理员'}; return m[t] || '' }
function statusLabel(s: number) { const m: Record<number,string> = {1:'正常',2:'停用',3:'锁定'}; return m[s] || '' }
function statusClass(s: number) { return s === 1 ? 'active' : s === 2 ? 'inactive' : 'locked' }
</script>

<style scoped>
.am-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:110px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.green { color:#27ae60 }
.orange { color:#f39c12 }
.red { color:#e74c3c }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:160px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.data-table { width:100%; border-collapse:collapse }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04) }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.acct-name { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-1 { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-2 { background:rgba(26,115,232,.1); color:#1557b0 }
.type-3 { background:rgba(233,30,99,.1); color:#c2185b }
.status-dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:4px }
.status-dot.active { background:#27ae60 }
.status-dot.inactive { background:#e74c3c }
.status-dot.locked { background:#f39c12 }
.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.row-btn.danger { color:#e74c3c }
.row-btn.danger:hover { border-color:#e74c3c }
.empty-cell { text-align:center; color:#ccc; padding:40px }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<!-- 弹窗样式 -->
<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:500px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }

.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.radio-group { display:flex; gap:20px }
.radio-item { display:flex; align-items:center; gap:6px; font-size:14px; color:#333; cursor:pointer }
.radio-item input[type=radio] { accent-color:#1a73e8 }
.pwd-row { display:flex; gap:8px }
.pwd-row .form-input { flex:1 }
.gen-btn { padding:8px 14px; border:1px solid rgba(26,115,232,.3); border-radius:10px; background:rgba(26,115,232,.04); color:#1a73e8; font-size:12px; cursor:pointer; white-space:nowrap }
.gen-btn:hover { background:rgba(26,115,232,.1) }
.form-check { display:flex; align-items:center; gap:8px; font-size:13px; color:#555 }
.form-check input[type=checkbox] { accent-color:#1a73e8 }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
