<template>
  <div class="lm-page">
    <div class="page-header">
      <h2>场地管理</h2>
      <p>实验室信息、查询与维护</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num blue">{{ labs.length }}</div>
        <div class="stat-label">实验室总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ openCount }}</div>
        <div class="stat-label">开放中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num orange">{{ maintainCount }}</div>
        <div class="stat-label">维护中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num red">{{ closedCount }}</div>
        <div class="stat-label">已关闭</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="searchName" placeholder="搜索名称..." class="search-input" @keyup.enter="pageNum = 1" />
        <select v-model="filterType" class="tool-select">
          <option value="">全部类型</option>
          <option v-for="t in labTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
        <select v-model="filterBuilding" class="tool-select">
          <option value="">全部楼栋</option>
          <option v-for="b in buildings" :key="b" :value="b">{{ b }}</option>
        </select>
        <select v-model="filterStatus" class="tool-select">
          <option value="">全部状态</option>
          <option :value="1">开放中</option>
          <option :value="2">维护中</option>
          <option :value="3">已关闭</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 间</span>
        <button class="add-btn" @click="openCreateDialog">+ 新增实验室</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="data-table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>编码</th>
            <th>名称</th>
            <th>类型</th>
            <th>位置</th>
            <th>容纳</th>
            <th>面积</th>
            <th>负责人</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in pagedList" :key="l.id">
            <td><code class="lab-code">{{ l.labCode }}</code></td>
            <td><span class="lab-name">{{ l.labName }}</span></td>
            <td><span class="type-tag" :class="'type-' + l.labTypeId">{{ l.labTypeName }}</span></td>
            <td class="location-cell">{{ l.campus }} · {{ l.building }} {{ l.floor }}F {{ l.roomNo }}</td>
            <td>{{ l.capacity }}人</td>
            <td>{{ l.areaM2 }} m²</td>
            <td>{{ l.dutyUserName || '--' }}</td>
            <td><span class="status-dot" :class="statusDotClass(l.runningStatus)"></span> {{ l.runningStatusName }}</td>
            <td class="action-cell">
              <button class="row-btn" @click="viewLab(l.id)">查看</button>
              <button class="row-btn" @click="editLab(l.id)">编辑</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="9" class="empty-cell">暂无实验室数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 新增/编辑/查看弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel wide">
            <div class="modal-header">
              <h3>{{ dialogTitle }}</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body" v-if="form">
              <div class="section-label">基本信息</div>
              <div class="form-row-2">
                <div class="form-g"><label>实验室编码 <span class="required">*</span></label><input v-model="form.labCode" class="form-input" :disabled="dialogMode !== 'create'" placeholder="如 LAB-2024-A305" /></div>
                <div class="form-g"><label>实验室名称 <span class="required">*</span></label><input v-model="form.labName" class="form-input" :disabled="dialogMode === 'view'" placeholder="如 B103 物理实验室" /></div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>实验室类型 <span class="required">*</span></label>
                  <select v-model="form.labTypeId" class="form-input" :disabled="dialogMode === 'view'">
                    <option :value="0" disabled>请选择类型</option>
                    <option v-for="t in labTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
                  </select>
                </div>
                <div class="form-g"><label>负责人</label>
                  <select v-model="form.dutyUserId" class="form-input" :disabled="dialogMode === 'view'">
                    <option :value="0">未指定</option>
                    <option v-for="d in dutyOptions" :key="d.id" :value="d.id">{{ d.name }}</option>
                  </select>
                </div>
              </div>

              <div class="section-label">位置信息</div>
              <div class="form-row-4">
                <div class="form-g"><label>校区</label><input v-model="form.campus" class="form-input" :disabled="dialogMode === 'view'" placeholder="如 主校区" /></div>
                <div class="form-g"><label>楼栋</label><input v-model="form.building" class="form-input" :disabled="dialogMode === 'view'" placeholder="如 B栋" /></div>
                <div class="form-g"><label>楼层</label><input v-model="form.floor" class="form-input" :disabled="dialogMode === 'view'" placeholder="如 1" /></div>
                <div class="form-g"><label>房间号</label><input v-model="form.roomNo" class="form-input" :disabled="dialogMode === 'view'" placeholder="如 103" /></div>
              </div>

              <div class="section-label">容量信息</div>
              <div class="form-row-3">
                <div class="form-g"><label>容纳人数</label><input v-model.number="form.capacity" type="number" class="form-input" :disabled="dialogMode === 'view'" min="1" /></div>
                <div class="form-g"><label>面积 (m²)</label><input v-model.number="form.areaM2" type="number" class="form-input" :disabled="dialogMode === 'view'" min="0" step="0.1" /></div>
                <div class="form-g"><label>运行状态</label>
                  <select v-model="form.runningStatus" class="form-input" :disabled="dialogMode === 'view' || dialogMode === 'create'">
                    <option :value="1">开放中</option><option :value="2">维护中</option><option :value="3">已关闭</option>
                  </select>
                </div>
              </div>

              <div class="form-g">
                <label>描述</label>
                <textarea v-model="form.description" class="form-input form-textarea" rows="2" :disabled="dialogMode === 'view'" placeholder="实验室设施、用途等描述..." />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">{{ dialogMode === 'view' ? '关闭' : '取消' }}</button>
              <button v-if="dialogMode !== 'view'" class="modal-btn primary" @click="submitForm" :disabled="!canSubmit">保存</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import type { LabStatus } from '../../utils/labApi'
import { getLabPage } from '../../utils/labApi'
import { isMockMode } from '../../stores/authStore'

interface Lab {
  id: number; labCode: string; labName: string; labTypeId: number; labTypeName: string
  campus: string; building: string; floor: string; roomNo: string
  capacity: number; areaM2: number; dutyUserId: number | null; dutyUserName: string
  runningStatus: LabStatus; runningStatusName: string; description: string | null
}

const labTypes = [
  { id:1, name:'化学实验室' }, { id:2, name:'物理实验室' },
  { id:3, name:'生物实验室' }, { id:4, name:'计算机实验室' }, { id:5, name:'综合实验室' },
]
const buildings = ['A栋','B栋','C栋','G栋']
const dutyOptions = [
  { id:1, name:'王管理员' }, { id:2, name:'李老师' }, { id:3, name:'孙老师' },
]

// Mock 数据：后端不可用时使用
const MOCK_LABS: Lab[] = [
  { id:1, labCode:'LAB-2024-A305', labName:'A305 有机化学实验室', labTypeId:1, labTypeName:'化学实验室', campus:'主校区', building:'A栋', floor:'3', roomNo:'305', capacity:30, areaM2:85, dutyUserId:2, dutyUserName:'李老师', runningStatus:1, runningStatusName:'开放中', description:'配备通风橱、旋转蒸发仪等' },
  { id:2, labCode:'LAB-2024-B103', labName:'B103 物理实验室', labTypeId:2, labTypeName:'物理实验室', campus:'主校区', building:'B栋', floor:'1', roomNo:'103', capacity:40, areaM2:120, dutyUserId:3, dutyUserName:'孙老师', runningStatus:1, runningStatusName:'开放中', description:'光学、电磁学实验设备齐全' },
  { id:3, labCode:'LAB-2024-C202', labName:'C202 计算机实验室', labTypeId:4, labTypeName:'计算机实验室', campus:'主校区', building:'C栋', floor:'2', roomNo:'202', capacity:50, areaM2:100, dutyUserId:1, dutyUserName:'王管理员', runningStatus:1, runningStatusName:'开放中', description:'50台高性能工作站' },
  { id:4, labCode:'LAB-2024-A201', labName:'A201 生物实验室', labTypeId:3, labTypeName:'生物实验室', campus:'主校区', building:'A栋', floor:'2', roomNo:'201', capacity:25, areaM2:90, dutyUserId:2, dutyUserName:'李老师', runningStatus:2, runningStatusName:'维护中', description:'显微镜室，部分设备维护' },
  { id:5, labCode:'LAB-2024-G101', labName:'G101 综合实验室', labTypeId:5, labTypeName:'综合实验室', campus:'分校区', building:'G栋', floor:'1', roomNo:'101', capacity:60, areaM2:200, dutyUserId:null, dutyUserName:'', runningStatus:1, runningStatusName:'开放中', description:'大型综合实验场地，支持多学科' },
  { id:6, labCode:'LAB-2024-B205', labName:'B205 物理实验室(二)', labTypeId:2, labTypeName:'物理实验室', campus:'主校区', building:'B栋', floor:'2', roomNo:'205', capacity:35, areaM2:110, dutyUserId:3, dutyUserName:'孙老师', runningStatus:3, runningStatusName:'已关闭', description:'暑期关闭维护' },
  { id:7, labCode:'LAB-2024-A102', labName:'A102 化学分析室', labTypeId:1, labTypeName:'化学实验室', campus:'主校区', building:'A栋', floor:'1', roomNo:'102', capacity:20, areaM2:75, dutyUserId:2, dutyUserName:'李老师', runningStatus:1, runningStatusName:'开放中', description:'色谱、质谱分析设备' },
  { id:8, labCode:'LAB-2024-C305', labName:'C305 人工智能实验室', labTypeId:4, labTypeName:'计算机实验室', campus:'主校区', building:'C栋', floor:'3', roomNo:'305', capacity:40, areaM2:95, dutyUserId:1, dutyUserName:'王管理员', runningStatus:1, runningStatusName:'开放中', description:'GPU服务器、深度学习平台' },
]

const labs = ref<Lab[]>([])

async function loadLabs() {
  // Mock 模式或后端不可用时使用本地数据
  if (isMockMode.value) { labs.value = [...MOCK_LABS]; return }
  try {
    const res = await getLabPage({ pageNum: 1, pageSize: 200 })
    if (res.records?.length) {
      labs.value = res.records.map(l => ({
        id: l.id, labCode: (l as any).labCode || `LAB-${l.id}`, labName: l.labName,
        labTypeId: l.labTypeId, labTypeName: l.labTypeName || '',
        campus: l.campus || '', building: l.building || '', floor: (l as any).floor || '', roomNo: (l as any).roomNo || '',
        capacity: l.capacity, areaM2: l.areaM2 ?? 0,
        dutyUserId: l.dutyUserId, dutyUserName: l.dutyUserName || '',
        runningStatus: l.runningStatus, runningStatusName: (l as any).runningStatusDesc || l.runningStatusName || '',
        description: l.description
      }))
    } else {
      labs.value = [...MOCK_LABS]
    }
  } catch { labs.value = [...MOCK_LABS] }
}
onMounted(() => { loadLabs() })

// 筛选 & 分页
const searchName = ref('')
const filterType = ref<number | ''>('')
const filterBuilding = ref('')
const filterStatus = ref<LabStatus | ''>('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = labs.value
  if (searchName.value) list = list.filter(l => l.labName.toLowerCase().includes(searchName.value.toLowerCase()))
  if (filterType.value !== '') list = list.filter(l => l.labTypeId === filterType.value)
  if (filterBuilding.value) list = list.filter(l => l.building === filterBuilding.value)
  if (filterStatus.value !== '') list = list.filter(l => l.runningStatus === filterStatus.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const openCount = computed(() => labs.value.filter(l => l.runningStatus === 1).length)
const maintainCount = computed(() => labs.value.filter(l => l.runningStatus === 2).length)
const closedCount = computed(() => labs.value.filter(l => l.runningStatus === 3).length)

// 弹窗
const showDialog = ref(false)
const dialogMode = ref<'create' | 'edit' | 'view'>('view')
const dialogTitle = computed(() => dialogMode.value === 'create' ? '新增实验室' : dialogMode.value === 'edit' ? '编辑实验室' : '实验室详情')

interface LabForm { id: number; labCode: string; labName: string; labTypeId: number; campus: string; building: string; floor: string; roomNo: string; capacity: number; areaM2: number; dutyUserId: number | null; runningStatus: LabStatus; description: string }
const form = ref<LabForm | null>(null)
const canSubmit = computed(() => form.value && form.value.labCode.trim() && form.value.labName.trim() && form.value.labTypeId > 0)

function openCreateDialog() {
  form.value = { id:0, labCode:'', labName:'', labTypeId:0, campus:'', building:'', floor:'', roomNo:'', capacity:20, areaM2:0, dutyUserId:null, runningStatus:1, description:'' }
  dialogMode.value = 'create'; showDialog.value = true
}
function viewLab(id: number) { openDetail(id, 'view') }
function editLab(id: number) { openDetail(id, 'edit') }
function openDetail(id: number, mode: 'view' | 'edit') {
  const l = labs.value.find(l => l.id === id)
  if (!l) return
  form.value = { ...l, description: l.description || '' }
  dialogMode.value = mode; showDialog.value = true
}

function submitForm() {
  if (!form.value || !canSubmit.value) return
  const f = form.value
  const typeName = labTypes.find(t => t.id === f.labTypeId)?.name || ''
  const dutyName = (f.dutyUserId && dutyOptions.find(d => d.id === f.dutyUserId)?.name) || ''
  const statusNames: Record<number, string> = { 1:'开放中', 2:'维护中', 3:'已关闭' }

  if (dialogMode.value === 'create') {
    labs.value.unshift({
      id: Date.now(), labCode: f.labCode.trim(), labName: f.labName.trim(),
      labTypeId: f.labTypeId, labTypeName: typeName,
      campus: f.campus, building: f.building, floor: f.floor, roomNo: f.roomNo,
      capacity: f.capacity, areaM2: f.areaM2, dutyUserId: f.dutyUserId || null,
      dutyUserName: dutyName, runningStatus: 1, runningStatusName: '开放中',
      description: f.description || null,
    })
  } else {
    const idx = labs.value.findIndex(l => l.id === f.id)
    if (idx >= 0) {
      const l = labs.value[idx]
      l.labName = f.labName.trim(); l.labTypeId = f.labTypeId; l.labTypeName = typeName
      l.campus = f.campus; l.building = f.building; l.floor = f.floor; l.roomNo = f.roomNo
      l.capacity = f.capacity; l.areaM2 = f.areaM2
      l.dutyUserId = f.dutyUserId || null; l.dutyUserName = dutyName
      l.runningStatus = f.runningStatus; l.runningStatusName = statusNames[f.runningStatus]
      l.description = f.description || null
    }
  }
  showDialog.value = false
}

function statusDotClass(s: LabStatus): string {
  return s === 1 ? 'dot-open' : s === 2 ? 'dot-maintain' : 'dot-closed'
}
</script>

<style scoped>
.lm-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 }
.green { color:#27ae60 }
.orange { color:#f39c12 }
.red { color:#e74c3c }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px; flex-wrap:wrap }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:140px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; min-width:900px }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04); white-space:nowrap }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.lab-code { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }
.lab-name { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-1 { background:rgba(231,76,60,.08); color:#c0392b }
.type-2 { background:rgba(26,115,232,.08); color:#1557b0 }
.type-3 { background:rgba(39,174,96,.08); color:#1e8449 }
.type-4 { background:rgba(142,68,173,.08); color:#7d3c98 }
.type-5 { background:rgba(243,156,18,.08); color:#b9770e }
.location-cell { font-size:12px; color:#777 }
.status-dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:4px }
.dot-open { background:#27ae60 }
.dot-maintain { background:#f39c12 }
.dot-closed { background:#e74c3c }
.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.empty-cell { text-align:center; color:#ccc; padding:40px }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:640px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-panel.wide { max-width:680px }
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

.section-label { font-size:13px; font-weight:600; color:#888; padding-bottom:4px; border-bottom:1px solid rgba(0,0,0,.05); margin-bottom:-6px }
.form-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.form-row-3 { display:grid; grid-template-columns:1fr 1fr 1fr; gap:14px }
.form-row-4 { display:grid; grid-template-columns:1fr 1fr 1fr 1fr; gap:14px }
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
