<template>
  <div class="sc-page">
    <div class="page-header">
      <h2>安全检查</h2>
      <p>创建检查任务、提交检查结果</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ tasks.length }}</div><div class="stat-label">任务总数</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ pendingCount }}</div><div class="stat-label">待检查</div></div>
      <div class="stat-card"><div class="stat-num green">{{ doneCount }}</div><div class="stat-label">已完成</div></div>
      <div class="stat-card"><div class="stat-num red">{{ abnormalCount }}</div><div class="stat-label">异常项</div></div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <div class="filter-tabs">
          <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key; pageNum = 1">
            {{ t.label }}<span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
          </div>
        </div>
        <select v-model="filterType" class="tool-select">
          <option value="">全部类型</option>
          <option value="routine">例行检查</option>
          <option value="special">专项检查</option>
        </select>
        <select v-model="filterLab" class="tool-select">
          <option value="">全部实验室</option>
          <option v-for="l in labOptions" :key="l.id" :value="l.id">{{ l.name }}</option>
        </select>
      </div>
      <div class="toolbar-right">
        <input v-model="searchTitle" placeholder="搜索标题..." class="search-input" @keyup.enter="pageNum = 1" />
        <button class="add-btn" @click="openCreateDialog">+ 创建任务</button>
      </div>
    </div>

    <div class="data-table-wrap">
      <table class="data-table">
        <thead><tr><th>编号</th><th>检查标题</th><th>类型</th><th>实验室</th><th>检查人</th><th>时间</th><th>结果</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="t in pagedList" :key="t.id">
            <td><code>{{ t.inspectionNo }}</code></td>
            <td><span class="task-title">{{ t.title }}</span></td>
            <td><span class="type-tag" :class="'type-' + t.inspectionType">{{ t.inspectionType === 'routine' ? '例行检查' : '专项检查' }}</span></td>
            <td>{{ t.labName }}</td>
            <td>{{ t.inspectorName }}</td>
            <td>{{ t.inspectionTime || '--' }}</td>
            <td><span class="result-tag" :class="'result-' + t.result">{{ resultLabel(t.result) }}</span></td>
            <td class="action-cell">
              <button v-if="t.result === 'pending'" class="row-btn primary" @click="openResultDialog(t)">填写结果</button>
              <button v-else class="row-btn" @click="openResultDialog(t)">查看</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="8" class="empty-cell">暂无检查任务</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 创建任务弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>创建检查任务</h3><button class="modal-close" @click="showCreate = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="form-g"><label>检查标题 <span class="required">*</span></label><input v-model="createForm.title" class="form-input" placeholder="如：7月化学实验室安全例行检查" /></div>
              <div class="form-row-2">
                <div class="form-g"><label>检查类型 <span class="required">*</span></label>
                  <select v-model="createForm.inspectionType" class="form-input"><option value="routine">例行检查</option><option value="special">专项检查</option></select>
                </div>
                <div class="form-g"><label>实验室 <span class="required">*</span></label>
                  <select v-model="createForm.labId" class="form-input"><option :value="0" disabled>请选择</option><option v-for="l in labOptions" :key="l.id" :value="l.id">{{ l.name }}</option></select>
                </div>
              </div>
              <div class="form-g"><label>检查人 <span class="required">*</span></label>
                <select v-model="createForm.inspectorId" class="form-input"><option :value="0" disabled>请选择</option><option v-for="p in inspectorOptions" :key="p.id" :value="p.id">{{ p.name }}</option></select>
              </div>
              <div class="form-g"><label>检查要求</label><textarea v-model="createForm.description" class="form-input form-textarea" rows="2" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showCreate = false">取消</button>
              <button class="modal-btn primary" @click="doCreate" :disabled="!canCreate">创建</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 填写/查看结果弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showResult && resultTarget" class="modal-overlay" @click.self="showResult = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>{{ resultTarget.result === 'pending' ? '填写检查结果' : '检查结果' }}</h3><button class="modal-close" @click="showResult = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>任务</span><span>{{ resultTarget.inspectionNo }} {{ resultTarget.title }}</span></div>
                <div class="ci-row"><span>实验室</span><span>{{ resultTarget.labName }}</span></div>
                <div class="ci-row"><span>检查人</span><span>{{ resultTarget.inspectorName }}</span></div>
              </div>
              <div class="form-g">
                <label>检查结果 <span class="required">*</span></label>
                <select v-model="resultForm.result" class="form-input" :disabled="resultTarget.result !== 'pending'"><option value="normal">正常</option><option value="abnormal">异常</option></select>
              </div>
              <div class="form-g">
                <label>问题描述</label>
                <textarea v-model="resultForm.problemDesc" class="form-input form-textarea" rows="2" placeholder="发现的问题..." :disabled="resultTarget.result !== 'pending'" />
              </div>
              <div class="form-g">
                <label>整改建议</label>
                <textarea v-model="resultForm.suggestion" class="form-input form-textarea" rows="2" placeholder="整改建议..." :disabled="resultTarget.result !== 'pending'" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showResult = false">{{ resultTarget.result === 'pending' ? '取消' : '关闭' }}</button>
              <button v-if="resultTarget.result === 'pending'" class="modal-btn primary" @click="doSubmitResult">提交结果</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

interface Task {
  id: number; inspectionNo: string; title: string; inspectionType: string
  labId: number; labName: string; inspectorId: number; inspectorName: string
  inspectionTime: string | null; result: 'pending' | 'normal' | 'abnormal'
  problemDesc: string | null; suggestion: string | null; description: string | null
}

const labOptions = [
  { id:1, name:'A305 分析化学实验室' }, { id:2, name:'B103 物理实验室' },
  { id:3, name:'D108 生物培养实验室' }, { id:4, name:'C202 计算机实验室' },
]
const inspectorOptions = [{ id:1, name:'王管理员' }, { id:2, name:'李老师' }]

const tasks = ref<Task[]>([
  { id:1, inspectionNo:'CHK-2024-001', title:'7月化学实验室安全例行检查', inspectionType:'routine', labId:1, labName:'A305 分析化学实验室', inspectorId:1, inspectorName:'王管理员', inspectionTime:null, result:'pending', problemDesc:null, suggestion:null, description:'检查通风橱、试剂存放、消防器材' },
  { id:2, inspectionNo:'CHK-2024-002', title:'物理实验室用电安全专项检查', inspectionType:'special', labId:2, labName:'B103 物理实验室', inspectorId:1, inspectorName:'王管理员', inspectionTime:null, result:'pending', problemDesc:null, suggestion:null, description:'重点检查大功率设备线路和接地' },
  { id:3, inspectionNo:'CHK-2024-003', title:'生物实验室生物安全例行检查', inspectionType:'routine', labId:3, labName:'D108 生物培养实验室', inspectorId:2, inspectorName:'李老师', inspectionTime:null, result:'pending', problemDesc:null, suggestion:null, description:null },
  { id:4, inspectionNo:'CHK-2024-004', title:'6月计算机实验室安全检查', inspectionType:'routine', labId:4, labName:'C202 计算机实验室', inspectorId:1, inspectorName:'王管理员', inspectionTime:'2026-06-28', result:'normal', problemDesc:null, suggestion:null, description:null },
  { id:5, inspectionNo:'CHK-2024-005', title:'化学实验室危化品专项检查', inspectionType:'special', labId:1, labName:'A305 分析化学实验室', inspectorId:1, inspectorName:'王管理员', inspectionTime:'2026-06-20', result:'abnormal', problemDesc:'浓硫酸存放柜锁具损坏，未双人双锁管理', suggestion:'立即更换锁具，严格执行双人双锁制度', description:null },
  { id:6, inspectionNo:'CHK-2024-006', title:'6月物理实验室例行检查', inspectionType:'routine', labId:2, labName:'B103 物理实验室', inspectorId:2, inspectorName:'李老师', inspectionTime:'2026-06-15', result:'normal', problemDesc:null, suggestion:null, description:null },
])

const activeStatus = ref<'all'|'pending'|'done'>('all')
const filterType = ref('')
const filterLab = ref<number|''>('')
const searchTitle = ref('')
const pageNum = ref(1)
const pageSize = 8

const statusTabs = computed(() => [
  { key:'all' as const, label:'全部', count:tasks.value.length },
  { key:'pending' as const, label:'待检查', count:tasks.value.filter(t => t.result === 'pending').length },
  { key:'done' as const, label:'已完成', count:tasks.value.filter(t => t.result !== 'pending').length },
])

const filteredList = computed(() => {
  let list = tasks.value
  if (activeStatus.value === 'pending') list = list.filter(t => t.result === 'pending')
  if (activeStatus.value === 'done') list = list.filter(t => t.result !== 'pending')
  if (filterType.value) list = list.filter(t => t.inspectionType === filterType.value)
  if (filterLab.value !== '') list = list.filter(t => t.labId === filterLab.value)
  if (searchTitle.value) list = list.filter(t => t.title.includes(searchTitle.value))
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const pendingCount = computed(() => tasks.value.filter(t => t.result === 'pending').length)
const doneCount = computed(() => tasks.value.filter(t => t.result !== 'pending').length)
const abnormalCount = computed(() => tasks.value.filter(t => t.result === 'abnormal').length)

function resultLabel(r: string): string { const m: Record<string,string> = { pending:'待检查', normal:'正常', abnormal:'异常' }; return m[r] || r }

// 创建弹窗
const showCreate = ref(false)
const createForm = reactive({ title:'', inspectionType:'routine', labId:0, inspectorId:0, description:'' })
const canCreate = computed(() => createForm.title.trim() && createForm.labId > 0 && createForm.inspectorId > 0)
function openCreateDialog() { createForm.title=''; createForm.inspectionType='routine'; createForm.labId=0; createForm.inspectorId=0; createForm.description=''; showCreate.value=true }
function doCreate() {
  if (!canCreate.value) return
  tasks.value.unshift({
    id:Date.now(), inspectionNo:'CHK-' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '-' + String(tasks.value.length+1).padStart(3,'0'),
    title:createForm.title.trim(), inspectionType:createForm.inspectionType,
    labId:createForm.labId, labName:labOptions.find(l=>l.id===createForm.labId)?.name||'',
    inspectorId:createForm.inspectorId, inspectorName:inspectorOptions.find(p=>p.id===createForm.inspectorId)?.name||'',
    inspectionTime:null, result:'pending', problemDesc:null, suggestion:null, description:createForm.description||null,
  })
  showCreate.value = false
}

// 结果弹窗
const showResult = ref(false)
const resultTarget = ref<Task | null>(null)
const resultForm = reactive({ result:'normal' as 'normal'|'abnormal', problemDesc:'', suggestion:'' })
function openResultDialog(t: Task) {
  resultTarget.value = t
  resultForm.result = t.result === 'pending' ? 'normal' : t.result
  resultForm.problemDesc = t.problemDesc || ''
  resultForm.suggestion = t.suggestion || ''
  showResult.value = true
}
function doSubmitResult() {
  if (!resultTarget.value) return
  const t = resultTarget.value
  t.result = resultForm.result; t.problemDesc = resultForm.problemDesc || null
  t.suggestion = resultForm.suggestion || null; t.inspectionTime = '2026-07-09'
  showResult.value = false
}
</script>

<style scoped>
.sc-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .green { color:#27ae60 } .orange { color:#f39c12 } .red { color:#e74c3c }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px; flex-wrap:wrap }
.toolbar-right { display:flex; align-items:center; gap:12px }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:150px; background:rgba(255,255,255,.7) }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.data-table { width:100%; border-collapse:collapse }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04) }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.task-title { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-routine { background:rgba(26,115,232,.1); color:#1557b0 }
.type-special { background:rgba(243,156,18,.1); color:#e67e22 }
.result-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.result-normal { background:rgba(39,174,96,.1); color:#27ae60 }
.result-abnormal { background:rgba(231,76,60,.1); color:#e74c3c }
.result-pending { background:rgba(0,0,0,.05); color:#999 }

.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer; white-space:nowrap }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.row-btn.primary { background:#1a73e8; color:#fff; border:none }
.row-btn.primary:hover { background:#1557b0 }
.empty-cell { text-align:center; color:#ccc; padding:40px }
code { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
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
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
.form-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-input:disabled { background:rgba(0,0,0,.03); color:#999 }
.form-textarea { resize:vertical; min-height:60px }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
