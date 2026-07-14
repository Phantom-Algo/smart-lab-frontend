<template>
  <div class="pos-page">
    <div class="page-header">
      <h2>岗位管理</h2>
      <p>维护岗位信息并关联部门</p>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <select v-model="filterType" class="tool-select">
          <option value="">全部类型</option>
          <option :value="1">学生岗位</option>
          <option :value="2">教师岗位</option>
          <option :value="3">管理岗位</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 个岗位</span>
        <button class="add-btn" @click="openAddDialog">+ 新增岗位</button>
      </div>
    </div>

    <div class="data-table-wrap">
      <table class="data-table">
        <thead>
          <tr><th>岗位名称</th><th>编号</th><th>类型</th><th>所属部门</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="pos in filteredList" :key="pos.id">
            <td><span class="pos-name">{{ pos.positionName }}</span></td>
            <td><code>{{ pos.positionNo }}</code></td>
            <td><span class="type-tag" :class="'type-' + pos.positionType">{{ pos.positionTypeName }}</span></td>
            <td>{{ pos.departmentName }}</td>
            <td><span class="status-dot" :class="pos.positionStatus === 1 ? 'active' : 'inactive'"></span> {{ pos.positionStatus === 1 ? '正常' : '停用' }}</td>
          </tr>
          <tr v-if="filteredList.length === 0"><td colspan="5" class="empty-cell">暂无数据</td></tr>
        </tbody>
      </table>
    </div>

    <!-- 新增岗位弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>新增岗位</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>岗位名称 <span class="required">*</span></label>
                <input v-model="form.positionName" class="form-input" placeholder="如 本科生、副教授" maxlength="128" />
              </div>
              <div class="form-row-2">
                <div class="form-g">
                  <label>岗位类型 <span class="required">*</span></label>
                  <select v-model="form.positionType" class="form-input">
                    <option :value="1">学生岗位</option>
                    <option :value="2">教师岗位</option>
                    <option :value="3">管理岗位</option>
                  </select>
                </div>
                <div class="form-g">
                  <label>岗位编码</label>
                  <input v-model="form.positionCode" class="form-input" placeholder="选填" maxlength="64" />
                </div>
              </div>
              <div class="form-g">
                <label>所属部门 <span class="required">*</span></label>
                <select v-model="form.departmentId" class="form-input">
                  <option :value="0" disabled>请选择部门</option>
                  <option v-for="d in deptList" :key="d.id" :value="d.id">{{ d.departmentName }}</option>
                </select>
              </div>
              <div class="form-g">
                <label>描述</label>
                <textarea v-model="form.description" class="form-input form-textarea" placeholder="岗位职责描述（选填）" rows="2" maxlength="255"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">取消</button>
              <button class="modal-btn primary" @click="submitAdd" :disabled="!canSubmit">确认创建</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { isMockMode } from '../../stores/authStore'
import { getPositions, createPosition, getDepartmentTree, type PositionItem, type DepartmentItem } from '../../utils/personnelApi'

const filterType = ref<number | ''>('')
const positionList = ref<PositionItem[]>([])
const deptList = ref<DepartmentItem[]>([])

const filteredList = computed(() => {
  if (filterType.value === '') return positionList.value
  return positionList.value.filter(p => p.positionType === filterType.value)
})

// 扁平化部门列表供选择
function flattenDepts(items: DepartmentItem[]): DepartmentItem[] {
  let result: DepartmentItem[] = []
  for (const d of items) {
    result.push(d)
    if (d.children) result = result.concat(flattenDepts(d.children))
  }
  return result
}

async function loadData() {
  if (isMockMode.value) return
  try {
    const [posRes, tree] = await Promise.all([getPositions(), getDepartmentTree()])
    positionList.value = posRes.records
    deptList.value = flattenDepts(tree)
  } catch { /* ignore */ }
}
loadData()

// 新增弹窗
const showDialog = ref(false)
const form = reactive({
  positionName: '',
  positionType: 1,
  positionCode: '',
  departmentId: 0,
  description: '',
})

const canSubmit = computed(() => form.positionName.trim() && form.departmentId > 0)

function openAddDialog() {
  form.positionName = ''
  form.positionType = 1
  form.positionCode = ''
  form.departmentId = 0
  form.description = ''
  showDialog.value = true
}

async function submitAdd() {
  if (!canSubmit.value) return
  try {
    await createPosition({
      positionName: form.positionName.trim(),
      positionType: form.positionType,
      departmentId: form.departmentId,
    })
    showDialog.value = false
    const posRes = await getPositions()
    positionList.value = posRes.records
  } catch { alert('创建失败，请重试') }
}
</script>

<style scoped>
.pos-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px }
.toolbar-right { display:flex; align-items:center; gap:12px }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }
.add-btn:hover { opacity:.9 }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.data-table { width:100%; border-collapse:collapse }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04) }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.pos-name { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-1 { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-2 { background:rgba(26,115,232,.1); color:#1557b0 }
.type-3 { background:rgba(233,30,99,.1); color:#c2185b }
.status-dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:4px }
.status-dot.active { background:#27ae60 }
.status-dot.inactive { background:#e74c3c }
.empty-cell { text-align:center; color:#ccc; padding:40px }
code { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }
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
.form-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:14px }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
