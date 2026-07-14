<template>
  <div class="dm-page">
    <div class="page-header">
      <h2>部门管理</h2>
      <p>维护实验室组织部门结构</p>
    </div>

    <div class="dept-layout">
      <div class="dept-tree-panel">
        <div class="dept-tree-header">
          <span>组织架构</span>
          <button class="add-btn-sm" @click="openAddDialog()">+ 新增</button>
        </div>
        <div class="dept-tree" v-if="deptTree.length > 0">
          <div v-for="dept in deptTree" :key="dept.id" class="dept-node">
            <div class="dept-node-row" :class="{ selected: selectedDept?.id === dept.id }" @click="selectedDept = dept">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3h18v18H3z M3 9h18 M9 3v18"/></svg>
              <span>{{ dept.departmentName }}</span>
              <span class="dept-code">{{ dept.departmentCode }}</span>
            </div>
            <div v-if="dept.children" class="dept-children">
              <div v-for="child in dept.children" :key="child.id" class="dept-node-row child" :class="{ selected: selectedDept?.id === child.id }" @click="selectedDept = child">
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3h18v18H3z M3 9h18 M9 3v18"/></svg>
                <span>{{ child.departmentName }}</span>
                <span class="dept-code">{{ child.departmentCode }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无部门数据</div>
      </div>

      <div class="dept-detail-panel" v-if="selectedDept">
        <div class="detail-header">
          <div class="detail-icon">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M3 21h18"/><path d="M5 21V7l8-4v18"/><path d="M19 21V11l-6-4"/></svg>
          </div>
          <div class="detail-header-text">
            <h4>{{ selectedDept.departmentName }}</h4>
            <span class="status-badge" :class="selectedDept.departmentStatus === 1 ? 'on' : 'off'">
              <span class="status-dot"></span>{{ selectedDept.departmentStatus === 1 ? '正常' : '停用' }}
            </span>
          </div>
        </div>

        <div class="detail-cards">
          <div class="info-card">
            <div class="info-card-icon code-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16,18 22,12 16,6"/><polyline points="8,6 2,12 8,18"/></svg>
            </div>
            <div class="info-card-body">
              <span class="info-label">部门编码</span>
              <span class="info-value code">{{ selectedDept.departmentCode || '—' }}</span>
            </div>
          </div>
          <div class="info-card">
            <div class="info-card-icon leader-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            </div>
            <div class="info-card-body">
              <span class="info-label">负责人</span>
              <span class="info-value" :class="{ muted: !selectedDept.leaderPersonName }">{{ selectedDept.leaderPersonName || '未设置' }}</span>
            </div>
          </div>
          <div class="info-card" v-if="selectedDept.description">
            <div class="info-card-icon desc-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z"/><polyline points="14,2 14,8 20,8"/></svg>
            </div>
            <div class="info-card-body">
              <span class="info-label">描述</span>
              <span class="info-value desc-text">{{ selectedDept.description }}</span>
            </div>
          </div>
        </div>

        <div class="detail-footer">
          <button class="action-btn primary" @click="openAddDialog(true)">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            添加子部门
          </button>
        </div>
      </div>
      <div v-else class="dept-detail-panel empty-detail">
        <span>选择左侧部门查看详情</span>
      </div>
    </div>

    <!-- 新增部门弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>{{ dialogParentId ? '添加子部门' : '新增部门' }}</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>部门名称 <span class="required">*</span></label>
                <input v-model="form.departmentName" class="form-input" placeholder="请输入部门名称" maxlength="128" />
              </div>
              <div class="form-g">
                <label>部门编码</label>
                <input v-model="form.departmentCode" class="form-input" placeholder="如 CS、CHEM（选填）" maxlength="64" />
              </div>
              <div class="form-g">
                <label>上级部门</label>
                <select v-model="form.parentId" class="form-input">
                  <option :value="undefined">无（顶级部门）</option>
                  <option v-for="d in flatDeptList" :key="d.id" :value="d.id">{{ d.departmentName }}</option>
                </select>
              </div>
              <div class="form-g">
                <label>描述</label>
                <textarea v-model="form.description" class="form-input form-textarea" placeholder="部门职能描述（选填）" rows="2" maxlength="255"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">取消</button>
              <button class="modal-btn primary" @click="submitAdd" :disabled="!form.departmentName.trim()">确认创建</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { isMockMode } from '../../stores/authStore'
import { getDepartmentTree, createDepartment, type DepartmentItem } from '../../utils/personnelApi'

const deptTree = ref<DepartmentItem[]>([])
const selectedDept = ref<DepartmentItem | null>(null)

// 扁平化部门列表（用于上级部门选择）
function flattenDepts(items: DepartmentItem[]): DepartmentItem[] {
  let result: DepartmentItem[] = []
  for (const d of items) {
    result.push(d)
    if (d.children) result = result.concat(flattenDepts(d.children))
  }
  return result
}
const flatDeptList = ref<DepartmentItem[]>([])

async function loadTree() {
  if (isMockMode.value) return
  try {
    deptTree.value = await getDepartmentTree()
    flatDeptList.value = flattenDepts(deptTree.value)
  } catch { /* ignore */ }
}
loadTree()

// 新增弹窗
const showDialog = ref(false)
const dialogParentId = ref<number | undefined>(undefined)
const form = reactive({
  departmentName: '',
  departmentCode: '',
  parentId: undefined as number | undefined,
  description: '',
})

function openAddDialog(asChild = false) {
  form.departmentName = ''
  form.departmentCode = ''
  form.description = ''
  if (asChild && selectedDept.value) {
    dialogParentId.value = selectedDept.value.id
    form.parentId = selectedDept.value.id
  } else {
    dialogParentId.value = undefined
    form.parentId = undefined
  }
  showDialog.value = true
}

async function submitAdd() {
  if (!form.departmentName.trim()) return
  try {
    await createDepartment({
      departmentName: form.departmentName.trim(),
      departmentCode: form.departmentCode.trim() || undefined,
      parentId: form.parentId,
    })
    showDialog.value = false
    await loadTree()
  } catch { alert('创建失败，请重试') }
}
</script>

<style scoped>
.dm-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.dept-layout { display:grid; grid-template-columns:280px 1fr; gap:20px; height:calc(100% - 60px) }

.dept-tree-panel { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; padding:16px; overflow-y:auto }
.dept-tree-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px }
.dept-tree-header span { font-size:14px; font-weight:600; color:#1a1a2e }
.add-btn-sm { padding:5px 14px; border:none; border-radius:8px; background:rgba(26,115,232,.1); color:#1a73e8; font-size:12px; cursor:pointer }
.add-btn-sm:hover { background:rgba(26,115,232,.18) }

.dept-node { margin-bottom:4px }
.dept-node-row { display:flex; align-items:center; gap:8px; padding:8px 10px; border-radius:8px; cursor:pointer; transition:.15s; font-size:13px; color:#333 }
.dept-node-row:hover { background:rgba(0,0,0,.04) }
.dept-node-row.selected { background:rgba(26,115,232,.08); color:#1a73e8; font-weight:500 }
.dept-node-row svg { flex-shrink:0; color:#aaa }
.dept-node-row.selected svg { color:#1a73e8 }
.dept-code { font-size:11px; color:#aaa; margin-left:auto }
.dept-children { margin-left:16px; border-left:1px solid rgba(0,0,0,.06); padding-left:8px }
.dept-node-row.child { font-size:13px }

.dept-detail-panel { 
  background:#fff; 
  border-radius:16px; 
  padding:0; 
  overflow:hidden;
  box-shadow:0 1px 3px rgba(0,0,0,.04), 0 4px 16px rgba(0,0,0,.04);
  display:flex; flex-direction:column;
}
.detail-header {
  display:flex; align-items:center; gap:16px;
  padding:24px 24px 20px;
  background:linear-gradient(135deg, #f8faff 0%, #eef2ff 100%);
  border-bottom:1px solid rgba(0,0,0,.04);
}
.detail-icon {
  width:52px; height:52px; border-radius:14px;
  background:linear-gradient(135deg,#1a73e8,#4a90d9);
  display:flex; align-items:center; justify-content:center;
  color:#fff; flex-shrink:0;
  box-shadow:0 4px 12px rgba(26,115,232,.25);
}
.detail-header-text { flex:1; min-width:0 }
.detail-header-text h4 {
  margin:0 0 6px; font-size:18px; font-weight:700; color:#1a1a2e;
  overflow:hidden; text-overflow:ellipsis; white-space:nowrap;
}
.status-badge {
  display:inline-flex; align-items:center; gap:6px;
  font-size:12px; font-weight:500; padding:4px 12px; border-radius:20px;
}
.status-badge.on { background:rgba(39,174,96,.1); color:#27ae60 }
.status-badge.off { background:rgba(231,76,60,.1); color:#e74c3c }
.status-badge .status-dot {
  width:6px; height:6px; border-radius:50%;
}
.status-badge.on .status-dot { background:#27ae60 }
.status-badge.off .status-dot { background:#e74c3c }

.detail-cards { 
  padding:16px 24px; 
  display:flex; flex-direction:column; gap:10px;
  flex:1;
}
.info-card {
  display:flex; align-items:flex-start; gap:14px;
  padding:14px 16px;
  border-radius:12px;
  background:rgba(0,0,0,.015);
  transition:.15s;
}
.info-card:hover { background:rgba(0,0,0,.03) }
.info-card-icon {
  width:38px; height:38px; border-radius:10px;
  display:flex; align-items:center; justify-content:center;
  flex-shrink:0;
}
.code-icon { background:rgba(26,115,232,.08); color:#1a73e8 }
.leader-icon { background:rgba(230,126,34,.08); color:#e67e22 }
.desc-icon { background:rgba(39,174,96,.08); color:#27ae60 }

.info-card-body { flex:1; min-width:0 }
.info-label { display:block; font-size:11px; font-weight:500; color:#999; text-transform:uppercase; letter-spacing:.5px; margin-bottom:3px }
.info-value { font-size:14px; font-weight:600; color:#1a1a2e; word-break:break-all }
.info-value.code { font-family:'SF Mono','Fira Code',monospace; font-size:13px; font-weight:500; color:#555 }
.info-value.muted { color:#bbb; font-weight:400 }
.info-value.desc-text { font-size:13px; font-weight:400; color:#666; line-height:1.5 }

.detail-footer {
  padding:16px 24px 20px;
  border-top:1px solid rgba(0,0,0,.04);
}
.action-btn.primary {
  display:inline-flex; align-items:center; gap:6px;
  padding:10px 22px;
  border:none; border-radius:10px;
  background:linear-gradient(135deg,#1a73e8,#4a90d9);
  color:#fff; font-size:13px; font-weight:500; cursor:pointer;
  transition:.2s;
  box-shadow:0 2px 8px rgba(26,115,232,.2);
}
.action-btn.primary:hover { 
  box-shadow:0 4px 16px rgba(26,115,232,.35);
  transform:translateY(-1px);
}

.empty-hint { padding:40px 20px; text-align:center; color:#bbb; font-size:13px }
.empty-detail { display:flex; align-items:center; justify-content:center; color:#bbb; font-size:13px }
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
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { resize:vertical }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
