<template>
  <div class="dl-page">
    <div class="page-header">
      <h2>设备台账</h2>
      <p>设备信息录入、查询、编辑</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num">{{ devices.length }}</div>
        <div class="stat-label">设备总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ idleCount }}</div>
        <div class="stat-label">闲置可用</div>
      </div>
      <div class="stat-card">
        <div class="stat-num blue">{{ inUseCount }}</div>
        <div class="stat-label">借用中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num orange">{{ repairCount }}</div>
        <div class="stat-label">维修中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num red">{{ scrappedCount }}</div>
        <div class="stat-label">已报废</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="searchNo" placeholder="设备编号..." class="search-input" @keyup.enter="pageNum = 1" />
        <input v-model="searchName" placeholder="设备名称..." class="search-input" @keyup.enter="pageNum = 1" />
        <select v-model="filterStatus" class="tool-select">
          <option value="">全部状态</option>
          <option :value="1">采购待入库</option>
          <option :value="2">闲置</option>
          <option :value="3">借用中</option>
          <option :value="4">维修中</option>
          <option :value="5">报废</option>
        </select>
        <select v-model="filterLab" class="tool-select">
          <option value="">全部实验室</option>
          <option v-for="lab in labOptions" :key="lab.id" :value="lab.id">{{ lab.name }}</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 台设备</span>
        <button class="add-btn" @click="openCreateDialog">+ 新增设备</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="data-table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>设备编号</th>
            <th>设备名称</th>
            <th>规格参数</th>
            <th>所属实验室</th>
            <th>状态</th>
            <th>健康分</th>
            <th>购入日期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in pagedList" :key="d.id">
            <td><code class="device-no">{{ d.deviceNo }}</code></td>
            <td><span class="device-name">{{ d.name }}</span></td>
            <td class="spec-cell">
              <span v-if="d.spec" class="spec-summary">{{ specSummary(d.spec) }}</span>
              <span v-else class="no-spec">--</span>
            </td>
            <td>{{ d.labName }}</td>
            <td><span class="status-tag" :class="'status-' + d.deviceStatus">{{ d.deviceStatusName }}</span></td>
            <td>
              <div class="health-wrap">
                <div class="health-bar"><div class="health-fill" :class="healthClass(d.healthScore)" :style="{ width: d.healthScore + '%' }"></div></div>
                <span class="health-num" :class="healthClass(d.healthScore)">{{ d.healthScore }}</span>
              </div>
            </td>
            <td>{{ d.purchaseDate || '--' }}</td>
            <td class="action-cell">
              <button v-if="d.deviceStatus === 1" class="row-btn stock-in" @click="handleStockIn(d.id)">入库</button>
              <button class="row-btn" @click="viewDevice(d.id)">查看</button>
              <button class="row-btn" @click="editDevice(d.id)">编辑</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="8" class="empty-cell">暂无设备数据</td></tr>
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
              <!-- 基本信息 -->
              <div class="section-label">基本信息</div>
              <div class="form-row-2">
                <div class="form-g"><label>设备编号 <span class="required">*</span></label><input v-model="form.deviceNo" class="form-input" :disabled="dialogMode !== 'create'" placeholder="如 EQ-2024-001" /></div>
                <div class="form-g"><label>设备名称 <span class="required">*</span></label><input v-model="form.name" class="form-input" :disabled="dialogMode === 'view'" placeholder="请输入设备名称" /></div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>所属实验室 <span class="required">*</span></label>
                  <select v-model="form.labId" class="form-input" :disabled="dialogMode === 'view'">
                    <option :value="0" disabled>请选择实验室</option>
                    <option v-for="lab in labOptions" :key="lab.id" :value="lab.id">{{ lab.name }}</option>
                  </select>
                </div>
                <div class="form-g"><label>设备状态</label>
                  <select v-model="form.deviceStatus" class="form-input" disabled>
                    <option :value="1">采购待入库</option><option :value="2">闲置</option>
                    <option :value="3">借用中</option><option :value="4">维修中</option><option :value="5">报废</option>
                  </select>
                </div>
              </div>

              <!-- 采购信息 -->
              <div class="section-label">采购信息</div>
              <div class="form-row-3">
                <div class="form-g"><label>购入日期</label><input v-model="form.purchaseDate" type="date" class="form-input" :disabled="dialogMode === 'view'" /></div>
                <div class="form-g"><label>购置价格 (元)</label><input v-model.number="form.purchasePrice" type="number" class="form-input" :disabled="dialogMode === 'view'" placeholder="0.00" min="0" step="0.01" /></div>
                <div class="form-g"><label>保修到期日</label><input v-model="form.expireDate" type="date" class="form-input" :disabled="dialogMode === 'view'" /></div>
              </div>

              <!-- 规格参数 -->
              <div class="section-label">规格参数</div>
              <div class="spec-editor">
                <div v-for="(item, i) in specEntries" :key="i" class="spec-row">
                  <input v-model="item.key" class="spec-key" placeholder="参数名" :disabled="dialogMode === 'view'" />
                  <input v-model="item.value" class="spec-val" placeholder="参数值" :disabled="dialogMode === 'view'" />
                  <button v-if="dialogMode !== 'view'" class="spec-del" @click="specEntries.splice(i, 1)" :disabled="specEntries.length <= 1">✕</button>
                </div>
                <button v-if="dialogMode !== 'view'" class="spec-add-btn" @click="specEntries.push({ key: '', value: '' })">+ 添加参数</button>
              </div>

              <!-- 备注 & 健康分 -->
              <div class="form-row-2">
                <div class="form-g"><label>备注</label><textarea v-model="form.remark" class="form-input form-textarea" rows="2" :disabled="dialogMode === 'view'" placeholder="可选备注" /></div>
                <div class="form-g" v-if="dialogMode !== 'create'">
                  <label>健康分</label>
                  <div class="health-display"><span class="health-big" :class="healthClass(form.healthScore)">{{ form.healthScore }}</span><span class="health-unit">/ 100</span></div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">{{ dialogMode === 'view' ? '关闭' : '取消' }}</button>
              <button v-if="dialogMode !== 'view'" class="modal-btn primary" @click="submitForm" :disabled="!canSubmit">{{ dialogMode === 'create' ? '确认录入' : '保存修改' }}</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import type { DeviceStatus } from '../../utils/deviceApi'
import { getDevicePage, createDevice, updateDevice, stockInDevice } from '../../utils/deviceApi'
import api from '../../utils/api'

// ========== 类型 ==========
interface Device {
  id: number; deviceNo: string; name: string
  spec: Record<string, unknown> | null; labId: number; labName: string
  purchaseDate: string | null; purchasePrice: number | null; expireDate: string | null
  deviceStatus: DeviceStatus; deviceStatusName: string
  healthScore: number; sensorId: number | null; remark: string | null
}

interface SpecEntry { key: string; value: string }

const labOptions = ref<{ id: number; name: string }[]>([])

async function loadLabOptions() {
  try {
    const res = await api.get('/lab-management/labs', { params: { pageNum: 1, pageSize: 200 } })
    labOptions.value = (res.data?.data?.records || []).map((l: any) => ({ id: l.id, name: l.labName }))
  } catch { labOptions.value = [] }
}

const devices = ref<Device[]>([])

async function loadDevices() {
  try {
    const res = await getDevicePage({ pageNum: 1, pageSize: 200 })
    devices.value = res.records.map(d => ({
      id: d.id, deviceNo: d.deviceNo, name: d.name,
      spec: null, labId: d.labId ?? 0, labName: (d as any).labName || '',
      purchaseDate: d.purchaseDate, purchasePrice: d.purchasePrice, expireDate: d.expireDate,
      deviceStatus: d.deviceStatus, deviceStatusName: (d as any).deviceStatusDesc || d.deviceStatusName,
      healthScore: d.healthScore, sensorId: d.sensorId ?? null, remark: d.remark
    }))
  } catch (e) { console.error('DeviceLedger loadDevices error:', e) }
}
onMounted(() => { loadDevices(); loadLabOptions() })

// ========== 筛选 & 分页 ==========
const searchNo = ref('')
const searchName = ref('')
const filterStatus = ref<DeviceStatus | ''>('')
const filterLab = ref<number | ''>('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = devices.value
  if (searchNo.value) list = list.filter(d => d.deviceNo.toLowerCase().includes(searchNo.value.toLowerCase()))
  if (searchName.value) list = list.filter(d => d.name.toLowerCase().includes(searchName.value.toLowerCase()))
  if (filterStatus.value !== '') list = list.filter(d => d.deviceStatus === filterStatus.value)
  if (filterLab.value !== '') list = list.filter(d => d.labId === filterLab.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => {
  const s = (pageNum.value - 1) * pageSize
  return filteredList.value.slice(s, s + pageSize)
})

const idleCount = computed(() => devices.value.filter(d => d.deviceStatus === 2).length)
const inUseCount = computed(() => devices.value.filter(d => d.deviceStatus === 3).length)
const repairCount = computed(() => devices.value.filter(d => d.deviceStatus === 4).length)
const scrappedCount = computed(() => devices.value.filter(d => d.deviceStatus === 5).length)

// ========== 弹窗 ==========
const showDialog = ref(false)
const dialogMode = ref<'create' | 'edit' | 'view'>('view')
const dialogTitle = computed(() => dialogMode.value === 'create' ? '新增设备' : dialogMode.value === 'edit' ? '编辑设备' : '设备详情')

interface DeviceForm { id: number; deviceNo: string; name: string; labId: number; deviceStatus: DeviceStatus; deviceStatusName: string; purchaseDate: string; purchasePrice: number | null; expireDate: string; healthScore: number; remark: string }
const form = ref<DeviceForm | null>(null)
const specEntries = ref<SpecEntry[]>([])
const canSubmit = computed(() => form.value && form.value.deviceNo.trim() && form.value.name.trim() && form.value.labId > 0)

async function handleStockIn(id: number) {
  if (!confirm('确认将该设备入库并转为"闲置"状态？')) return
  try {
    await stockInDevice(id)
    await loadDevices()
  } catch (e) { console.error('入库失败:', e); alert('入库失败，请重试') }
}

function openCreateDialog() {
  form.value = { id:0, deviceNo:'', name:'', labId:0, deviceStatus:1, deviceStatusName:'采购待入库', purchaseDate:'', purchasePrice:null, expireDate:'', healthScore:100, remark:'' }
  specEntries.value = [{ key: '', value: '' }]
  dialogMode.value = 'create'
  showDialog.value = true
}

function viewDevice(id: number) { openDetail(id, 'view') }
function editDevice(id: number) { openDetail(id, 'edit') }

function openDetail(id: number, mode: 'view' | 'edit') {
  const d = devices.value.find(d => d.id === id)
  if (!d) return
  form.value = { ...d, purchaseDate: d.purchaseDate || '', expireDate: d.expireDate || '', remark: d.remark || '' }
  specEntries.value = d.spec ? Object.entries(d.spec).map(([k, v]) => ({ key: k, value: String(v) })) : [{ key: '', value: '' }]
  dialogMode.value = mode
  showDialog.value = true
}

async function submitForm() {
  if (!form.value || !canSubmit.value) return
  const f = form.value
  const specObj: Record<string, unknown> = {}
  specEntries.value.forEach(e => { if (e.key.trim()) specObj[e.key.trim()] = e.value.trim() })

  try {
    if (dialogMode.value === 'create') {
      await createDevice({
        deviceNo: f.deviceNo.trim(),
        name: f.name.trim(),
        spec: Object.keys(specObj).length > 0 ? specObj : undefined,
        labId: f.labId,
        purchaseDate: f.purchaseDate || undefined,
        purchasePrice: f.purchasePrice ?? undefined,
        expireDate: f.expireDate || undefined,
        remark: f.remark || undefined,
      })
    } else {
      await updateDevice({
        id: f.id,
        name: f.name.trim(),
        spec: Object.keys(specObj).length > 0 ? specObj : undefined,
        labId: f.labId,
        purchaseDate: f.purchaseDate || undefined,
        purchasePrice: f.purchasePrice ?? undefined,
        expireDate: f.expireDate || undefined,
        remark: f.remark || undefined,
      })
    }
    showDialog.value = false
    await loadDevices()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '操作失败')
  }
}

// ========== 工具函数 ==========
function specSummary(spec: Record<string, unknown>): string {
  return Object.entries(spec).slice(0, 2).map(([k, v]) => `${k}:${v}`).join('，') + (Object.keys(spec).length > 2 ? '…' : '')
}
function healthClass(s: number): string {
  if (s >= 80) return 'hl-healthy'
  if (s >= 60) return 'hl-warn'
  return 'hl-danger'
}
function statusLabel(s: DeviceStatus): string {
  const m: Record<number, string> = { 1:'采购待入库', 2:'闲置', 3:'借用中', 4:'维修中', 5:'报废' }
  return m[s] || ''
}
</script>

<style scoped>
.dl-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.green { color:#27ae60 }
.blue { color:#1a73e8 }
.orange { color:#f39c12 }
.red { color:#e74c3c }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px; flex-wrap:wrap }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:140px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; min-width:900px }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04); white-space:nowrap }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.device-no { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }
.device-name { font-weight:600; color:#1a1a2e }
.spec-cell { max-width:160px; overflow:hidden; text-overflow:ellipsis }
.spec-summary { font-size:11px; color:#999 }
.no-spec { color:#ccc }

.status-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.status-1 { background:rgba(158,158,158,.12); color:#757575 }
.status-2 { background:rgba(76,175,80,.1); color:#2e7d32 }
.status-3 { background:rgba(26,115,232,.1); color:#1557b0 }
.status-4 { background:rgba(243,156,18,.1); color:#e67e22 }
.status-5 { background:rgba(231,76,60,.1); color:#c0392b }

.health-wrap { display:flex; align-items:center; gap:6px }
.health-bar { width:60px; height:6px; border-radius:3px; background:rgba(0,0,0,.06); overflow:hidden }
.health-fill { height:100%; border-radius:3px; transition:width .3s }
.hl-healthy { background:#27ae60 }
.hl-warn { background:#f39c12 }
.hl-danger { background:#e74c3c }
.health-num { font-size:12px; font-weight:600 }
.health-num.hl-healthy { color:#27ae60 }
.health-num.hl-warn { color:#f39c12 }
.health-num.hl-danger { color:#e74c3c }

.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.row-btn.stock-in { border-color:#27ae60; color:#27ae60 }
.row-btn.stock-in:hover { background:rgba(39,174,96,.06) }
.empty-cell { text-align:center; color:#ccc; padding:40px }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<!-- 弹窗样式（非scoped） -->
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
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-input:disabled { background:rgba(0,0,0,.03); color:#999 }
.form-textarea { resize:vertical; min-height:60px }

.spec-editor { display:flex; flex-direction:column; gap:6px }
.spec-row { display:flex; gap:8px; align-items:center }
.spec-key { flex:1; padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; outline:none; background:rgba(255,255,255,.7) }
.spec-key:focus { border-color:#1a73e8 }
.spec-val { flex:2; padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; outline:none; background:rgba(255,255,255,.7) }
.spec-val:focus { border-color:#1a73e8 }
.spec-del { width:28px; height:28px; border:1px solid rgba(231,76,60,.2); border-radius:6px; background:rgba(231,76,60,.04); color:#e74c3c; font-size:12px; cursor:pointer; display:flex; align-items:center; justify-content:center }
.spec-del:hover { background:rgba(231,76,60,.1) }
.spec-add-btn { align-self:flex-start; padding:6px 14px; border:1px dashed rgba(26,115,232,.3); border-radius:8px; background:transparent; color:#1a73e8; font-size:12px; cursor:pointer }
.spec-add-btn:hover { background:rgba(26,115,232,.04) }
.spec-key:disabled,.spec-val:disabled { background:rgba(0,0,0,.03); color:#999 }

.health-display { display:flex; align-items:baseline; gap:4px; padding-top:4px }
.health-big { font-size:28px; font-weight:700 }
.health-unit { font-size:13px; color:#999 }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
