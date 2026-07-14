<template>
  <div class="pm-page">
    <div class="page-header">
      <h2>人员档案</h2>
      <p>学生/教师/管理员信息管理</p>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="query.personName" placeholder="搜索姓名..." class="search-input" @keyup.enter="loadPersons" />
        <select v-model="query.personType" class="tool-select" @change="loadPersons">
          <option :value="undefined">全部类型</option>
          <option :value="1">学生</option>
          <option :value="2">教师</option>
          <option :value="3">管理员</option>
        </select>
        <select v-model="query.personStatus" class="tool-select" @change="loadPersons">
          <option :value="undefined">全部状态</option>
          <option :value="1">正常</option>
          <option :value="2">停用</option>
          <option :value="3">冻结</option>
        </select>
        <button class="query-btn" @click="loadPersons">查询</button>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ personTotal }} 人</span>
        <button class="add-btn" @click="openAddDialog">+ 新增人员</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="data-table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>姓名</th><th>编号</th><th>类型</th><th>性别</th><th>部门</th><th>状态</th><th>手机号</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in personList" :key="p.id">
            <td><span class="person-name">{{ p.personName }}</span></td>
            <td><code>{{ p.personNo }}</code></td>
            <td><span class="type-tag" :class="'type-' + p.personType">{{ p.personTypeName }}</span></td>
            <td>{{ p.genderType === 1 ? '男' : p.genderType === 2 ? '女' : '-' }}</td>
            <td>{{ p.departmentName || '-' }}</td>
            <td><span class="status-dot" :class="p.personStatus === 1 ? 'active' : 'inactive'"></span> {{ p.personStatusName }}</td>
            <td>{{ p.phone || '-' }}</td>
            <td class="action-cell">
              <button class="row-btn" @click="viewPerson(p.id)">查看</button>
              <button class="row-btn" @click="editPerson(p.id)">编辑</button>
            </td>
          </tr>
          <tr v-if="personList.length === 0"><td colspan="8" class="empty-cell">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="personTotal > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--; loadPersons()">上一页</button>
      <span>{{ pageNum }} / {{ Math.ceil(personTotal / pageSize) }}</span>
      <button :disabled="pageNum >= Math.ceil(personTotal / pageSize)" @click="pageNum++; loadPersons()">下一页</button>
    </div>

    <!-- 弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel wide">
            <div class="modal-header">
              <h3>{{ dialogMode === 'view' ? '人员详情' : '编辑人员' }}</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body" v-if="dialogPerson">
              <div class="form-row">
                <div class="form-g"><label>姓名</label><input v-model="dialogPerson.personName" class="form-input" :disabled="dialogMode === 'view'" /></div>
                <div class="form-g"><label>类型</label>
                  <select v-model="dialogPerson.personType" class="form-input" :disabled="dialogMode === 'view'">
                    <option :value="1">学生</option><option :value="2">教师</option><option :value="3">管理员</option>
                  </select>
                </div>
                <div class="form-g"><label>性别</label>
                  <select v-model="dialogPerson.genderType" class="form-input" :disabled="dialogMode === 'view'">
                    <option :value="0">未知</option><option :value="1">男</option><option :value="2">女</option>
                  </select>
                </div>
              </div>
              <div class="form-row">
                <div class="form-g"><label>部门</label><input v-model="dialogPerson.departmentName" class="form-input" disabled /></div>
                <div class="form-g"><label>岗位</label><input v-model="dialogPerson.positionName" class="form-input" disabled /></div>
                <div class="form-g"><label>状态</label><span class="status-text">{{ dialogPerson.personStatusName }}</span></div>
              </div>
              <div class="form-row">
                <div class="form-g"><label>手机号</label><input v-model="dialogPerson.phone" class="form-input" :disabled="dialogMode === 'view'" /></div>
                <div class="form-g"><label>邮箱</label><input v-model="dialogPerson.email" class="form-input" :disabled="dialogMode === 'view'" /></div>
              </div>
              <div v-if="dialogPerson.accountInfo" class="account-card">
                <div class="acct-title">关联账号</div>
                <div class="acct-row"><span>用户名</span><span>{{ dialogPerson.accountInfo.username }}</span></div>
                <div class="acct-row"><span>状态</span><span :class="dialogPerson.accountInfo.accountStatus === 1 ? 'green' : 'red'">{{ dialogPerson.accountInfo.accountStatusName }}</span></div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">关闭</button>
              <button v-if="dialogMode === 'edit'" class="modal-btn primary">保存</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { isMockMode } from '../../stores/authStore'
import { getPersonPage, getPersonDetail, type PersonListItem, type PersonDetail } from '../../utils/personnelApi'

const query = reactive({ personName: '', personType: undefined as number | undefined, personStatus: undefined as number | undefined })
const personList = ref<PersonListItem[]>([])
const personTotal = ref(0)
const pageNum = ref(1)
const pageSize = 10

async function loadPersons(): Promise<void> {
  if (isMockMode.value) return
  try {
    const result = await getPersonPage({ pageNum: pageNum.value, pageSize, ...query })
    personList.value = result.records
    personTotal.value = result.total
  } catch { /* ignore */ }
}

onMounted(() => { loadPersons() })

const showDialog = ref(false)
const dialogMode = ref<'view' | 'edit'>('view')
const dialogPerson = ref<PersonDetail | null>(null)

async function viewPerson(id: number): Promise<void> {
  try { dialogPerson.value = await getPersonDetail(id); dialogMode.value = 'view'; showDialog.value = true } catch { alert('获取详情失败') }
}
async function editPerson(id: number): Promise<void> {
  try { dialogPerson.value = await getPersonDetail(id); dialogMode.value = 'edit'; showDialog.value = true } catch { alert('获取详情失败') }
}
function openAddDialog(): void {
  dialogPerson.value = { id:0, personNo:'', personName:'', personType:1, personTypeName:'学生', genderType:0, departmentName:'', positionName:'', personStatus:1, personStatusName:'正常', phone:'', email:'', studentNo:'', employeeNo:'', className:'', titleName:'', extraData:{} }
  dialogMode.value = 'edit'; showDialog.value = true
}
</script>

<style scoped>
.pm-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:160px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.query-btn { padding:8px 18px; border:1px solid rgba(0,0,0,.1); border-radius:10px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.data-table { width:100%; border-collapse:collapse }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04) }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.person-name { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-1 { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-2 { background:rgba(26,115,232,.1); color:#1557b0 }
.type-3 { background:rgba(233,30,99,.1); color:#c2185b }
.status-dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:4px }
.status-dot.active { background:#27ae60 }
.status-dot.inactive { background:#e74c3c }
.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#888; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.empty-cell { text-align:center; color:#ccc; padding:40px }
code { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

/* modal */
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.3); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:600px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-panel.wide { max-width:640px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1 }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500 }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }

.form-row { display:flex; gap:14px; margin-bottom:14px }
.form-g { flex:1; display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:12px; font-weight:500; color:#888 }
.form-input { padding:9px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; outline:none; background:rgba(255,255,255,.7) }
.form-input:disabled { background:rgba(0,0,0,.03); color:#999 }

.account-card { background:rgba(0,0,0,.02); border-radius:10px; padding:14px 16px; margin-top:8px }
.acct-title { font-size:13px; font-weight:600; color:#555; margin-bottom:8px }
.acct-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#666 }
.green { color:#27ae60 }
.red { color:#e74c3c }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
