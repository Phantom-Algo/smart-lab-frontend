<template>
  <div class="it-page">
    <div class="page-header">
      <h2>阈值配置</h2>
      <p>安全阈值设置、传感器绑定</p>
    </div>

    <!-- ===== 安全阈值设置 ===== -->
    <div class="section-card">
      <div class="section-header">
        <h3>安全阈值设置</h3>
        <div class="section-actions">
          <select v-model="selectedLab" class="tool-select">
            <option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option>
          </select>
          <button class="btn-reset" @click="resetDefaults">恢复默认</button>
        </div>
      </div>

      <div class="threshold-table">
        <div class="th-row th-head">
          <span class="th-col-icon"></span>
          <span class="th-col-name">传感器</span>
          <span class="th-col-range">阈值范围</span>
          <span class="th-col-current">当前实时值</span>
          <span class="th-col-status">状态</span>
          <span class="th-col-act">操作</span>
        </div>
        <div v-for="t in currentThresholds" :key="t.type" class="th-row">
          <span class="th-col-icon">
            <svg v-if="t.type==='temp'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>
            <svg v-else-if="t.type==='humidity'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>
            <svg v-else-if="t.type==='smoke'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
            <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          </span>
          <span class="th-col-name">{{ t.label }}</span>
          <span class="th-col-range">{{ t.rangeText }}</span>
          <span class="th-col-current">{{ t.currentValue }}</span>
          <span class="th-col-status">
            <span class="status-dot" :class="'dot-' + t.status"></span>
            {{ t.status === 'normal' ? '正常' : t.status === 'warning' ? '预警' : '告警' }}
          </span>
          <span class="th-col-act">
            <button class="act-link" @click="openThresholdEdit(t)">编辑</button>
          </span>
        </div>
      </div>
    </div>

    <!-- ===== 传感器绑定管理 ===== -->
    <div class="section-card">
      <div class="section-header">
        <h3>传感器绑定管理</h3>
        <button class="btn-add" @click="openBindCreate">+ 绑定传感器</button>
      </div>

      <div class="bind-table-wrap" v-if="bindings.length > 0">
        <table class="bind-table">
          <thead>
            <tr>
              <th>传感器编号</th>
              <th>类型</th>
              <th>绑定实验室</th>
              <th>绑定时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="b in bindings" :key="b.id">
              <td class="mono">{{ b.sensorNo }}</td>
              <td>{{ typeLabel(b.sensorType) }}</td>
              <td>{{ b.labName || '未绑定' }}</td>
              <td class="date-cell">{{ b.bindTime || '-' }}</td>
              <td>
                <span class="bind-status" :class="b.status === 'bound' ? 'bs-bound' : 'bs-unbound'">
                  {{ b.status === 'bound' ? '已绑定' : '未绑定' }}
                </span>
              </td>
              <td>
                <button v-if="b.status === 'bound'" class="act-link warn" @click="confirmUnbind(b)">解绑</button>
                <button v-else class="act-link" @click="openBindEdit(b)">绑定</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-state small">
        <p>暂无传感器绑定记录</p>
      </div>
    </div>

    <!-- ===== 编辑阈值弹窗 ===== -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showThresholdEdit" class="modal-overlay" @click.self="showThresholdEdit = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>编辑{{ editingThreshold?.label }}阈值</h3>
              <button class="modal-close" @click="showThresholdEdit = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>传感器类型</span><span>{{ editingThreshold?.label }}</span></div>
                <div class="ci-row"><span>当前实时值</span><span>{{ editingThreshold?.currentValue }}</span></div>
              </div>
              <div class="form-g" v-if="editingThreshold?.type === 'humidity'">
                <label>湿度下限 <span class="required">*</span></label>
                <div class="input-with-unit"><input v-model.number="thEdit.lower" type="number" class="form-input" min="0" max="100" /><span class="unit">%</span></div>
              </div>
              <div class="form-g">
                <label>{{ editingThreshold?.type === 'humidity' ? '湿度上限' : '告警阈值' }} <span class="required">*</span></label>
                <div class="input-with-unit"><input v-model.number="thEdit.upper" type="number" class="form-input" :min="0" /><span class="unit">{{ editingThreshold?.unit || '' }}</span></div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showThresholdEdit = false">取消</button>
              <button class="modal-btn primary" @click="saveThreshold" :disabled="!thEdit.upper">保存</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- ===== 绑定/解绑弹窗 ===== -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showBindModal" class="modal-overlay" @click.self="showBindModal = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>{{ bindMode === 'bind' ? '绑定传感器' : '确认解绑' }}</h3>
              <button class="modal-close" @click="showBindModal = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <template v-if="bindMode === 'unbind'">
                <p class="unbind-warn">确定要解除 <strong>{{ editingBinding?.sensorNo }}</strong> 与 <strong>{{ editingBinding?.labName }}</strong> 的绑定吗？</p>
              </template>
              <template v-else>
                <div class="form-g"><label>传感器编号 <span class="required">*</span></label><input v-model="bindForm.sensorNo" class="form-input" placeholder="如 SEN-004" /></div>
                <div class="form-g"><label>传感器类型 <span class="required">*</span></label><select v-model="bindForm.sensorType" class="form-input"><option value="" disabled>请选择</option><option value="temp_humidity">温湿度</option><option value="smoke">烟雾</option><option value="gas">气体</option></select></div>
                <div class="form-g"><label>绑定实验室 <span class="required">*</span></label><select v-model="bindForm.labName" class="form-input"><option value="" disabled>请选择</option><option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option></select></div>
              </template>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showBindModal = false">取消</button>
              <button v-if="bindMode === 'unbind'" class="modal-btn danger" @click="doUnbind">确认解绑</button>
              <button v-else class="modal-btn primary" @click="doBind" :disabled="!bindForm.sensorNo || !bindForm.sensorType || !bindForm.labName">确认绑定</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import api from '../../utils/api'
import { isMockMode } from '../../stores/authStore'

type SensorTypeKey = 'temp' | 'humidity' | 'smoke' | 'gas'

interface ThresholdItem {
  type: SensorTypeKey; label: string; unit: string
  upper: number; lower: number; currentValue: string
  rangeText: string; status: 'normal' | 'warning' | 'alarm'
}

interface SensorBinding {
  id: number; sensorNo: string; sensorType: string
  labName: string | null; bindTime: string | null; status: 'bound' | 'unbound'
}

const labOptions = ['A305 分析化学实验室', 'B103 物理实验室', 'C202 计算机实验室', 'D108 生物培养实验室']
const selectedLab = ref(labOptions[0])

// Mock real-time values per lab
const labCurrentValues: Record<string, Record<SensorTypeKey, string>> = {
  'A305 分析化学实验室': { temp:'24.5°C', humidity:'52%', smoke:'0.2 mg/m³', gas:'0.1 ppm' },
  'B103 物理实验室': { temp:'32.8°C', humidity:'45%', smoke:'0.3 mg/m³', gas:'0.1 ppm' },
  'C202 计算机实验室': { temp:'26.1°C', humidity:'48%', smoke:'0.1 mg/m³', gas:'0.0 ppm' },
  'D108 生物培养实验室': { temp:'28.2°C', humidity:'65%', smoke:'0.1 mg/m³', gas:'0.2 ppm' },
}

// Default thresholds
const defaults: Omit<ThresholdItem, 'currentValue' | 'rangeText' | 'status'>[] = [
  { type:'temp', label:'温度', unit:'°C', upper:35, lower:0 },
  { type:'humidity', label:'湿度', unit:'%', upper:70, lower:30 },
  { type:'smoke', label:'烟雾', unit:'mg/m³', upper:0.5, lower:0 },
  { type:'gas', label:'气体', unit:'ppm', upper:0.5, lower:0 },
]

// Per-lab threshold overrides
const thresholdOverrides = reactive<Record<string, Record<SensorTypeKey, { upper: number; lower: number }>>>({})

function getThresholds(lab: string): ThresholdItem[] {
  const overrides = thresholdOverrides[lab] || {}
  const currentVals = labCurrentValues[lab] || labCurrentValues[labOptions[0]]
  return defaults.map(d => {
    const override = overrides[d.type]
    const upper = override?.upper ?? d.upper
    const lower = override?.lower ?? d.lower
    const currentStr = currentVals[d.type]
    const currentNum = parseFloat(currentStr)
    let status: ThresholdItem['status'] = 'normal'
    if (d.type === 'humidity') {
      if (currentNum > upper || currentNum < lower) status = 'alarm'
      else if (currentNum > upper - 5 || currentNum < lower + 5) status = 'warning'
    } else {
      if (currentNum >= upper) status = 'alarm'
      else if (currentNum >= upper * 0.8) status = 'warning'
    }
    let rangeText = ''
    if (d.type === 'humidity') rangeText = `下限 ${lower}% 上限 ${upper}%`
    else rangeText = `上限 ${upper}${d.unit}`
    return { ...d, upper, lower, currentValue: currentStr, rangeText, status }
  })
}

const currentThresholds = computed(() => getThresholds(selectedLab.value))

function resetDefaults() {
  delete thresholdOverrides[selectedLab.value]
}

// Threshold edit modal
const showThresholdEdit = ref(false)
const editingThreshold = ref<ThresholdItem | null>(null)
const thEdit = reactive({ upper: 0, lower: 0 })

function openThresholdEdit(t: ThresholdItem) {
  editingThreshold.value = t
  thEdit.upper = t.upper
  thEdit.lower = t.lower
  showThresholdEdit.value = true
}

function saveThreshold() {
  if (!editingThreshold.value) return
  const lab = selectedLab.value
  const type = editingThreshold.value.type
  if (!thresholdOverrides[lab]) thresholdOverrides[lab] = {} as any
  if (!thresholdOverrides[lab][type]) thresholdOverrides[lab][type] = { upper: 0, lower: 0 }
  thresholdOverrides[lab][type].upper = thEdit.upper
  thresholdOverrides[lab][type].lower = thEdit.lower
  showThresholdEdit.value = false
}

// Bindings
const MOCK_BINDINGS: SensorBinding[] = [
  { id:1, sensorNo:'SEN-001', sensorType:'temp_humidity', labName:'A305 分析化学实验室', bindTime:'2026-06-15 09:30', status:'bound' },
  { id:2, sensorNo:'SEN-002', sensorType:'smoke', labName:'B103 物理实验室', bindTime:'2026-06-20 14:00', status:'bound' },
  { id:3, sensorNo:'SEN-003', sensorType:'gas', labName:'C202 计算机实验室', bindTime:'2026-07-01 10:15', status:'bound' },
  { id:4, sensorNo:'SEN-004', sensorType:'temp_humidity', labName:'D108 生物培养实验室', bindTime:'2026-07-05 08:45', status:'bound' },
  { id:5, sensorNo:'SEN-005', sensorType:'smoke', labName:null, bindTime:null, status:'unbound' },
  { id:6, sensorNo:'SEN-006', sensorType:'gas', labName:null, bindTime:null, status:'unbound' },
]

const bindings = ref<SensorBinding[]>([])

async function loadBindings() {
  if (isMockMode.value) { bindings.value = [...MOCK_BINDINGS]; return }
  try {
    const res = await api.get('/iot/sensors', { params: { pageNum: 1, pageSize: 100 } })
    if (res.data?.data?.records?.length) {
      bindings.value = res.data.data.records.map((s: any) => ({
        id: s.id, sensorNo: s.sensorNo || '', sensorType: s.sensorTypeName || '',
        labName: s.targetName || null, bindTime: s.createdAt || null, status: (s.runningStatus === 1 ? 'bound' : 'unbound') as SensorBinding['status']
      }))
    } else bindings.value = [...MOCK_BINDINGS]
  } catch { /* API 不可用，请检查后端服务 */ }
}

onMounted(() => loadBindings())

function typeLabel(t: string) {
  const m: Record<string,string> = { temp_humidity:'温湿度', smoke:'烟雾', gas:'气体' }
  return m[t] || t
}

// Bind modal
const showBindModal = ref(false)
const bindMode = ref<'bind' | 'unbind'>('bind')
const editingBinding = ref<SensorBinding | null>(null)
const bindForm = reactive({ sensorNo:'', sensorType:'', labName:'' })

function openBindCreate() {
  bindMode.value = 'bind'
  editingBinding.value = null
  bindForm.sensorNo = ''; bindForm.sensorType = ''; bindForm.labName = ''
  showBindModal.value = true
}
function openBindEdit(b: SensorBinding) {
  bindMode.value = 'bind'
  editingBinding.value = b
  bindForm.sensorNo = b.sensorNo; bindForm.sensorType = b.sensorType; bindForm.labName = b.labName || ''
  showBindModal.value = true
}
function confirmUnbind(b: SensorBinding) {
  bindMode.value = 'unbind'
  editingBinding.value = b
  showBindModal.value = true
}
function doBind() {
  if (!bindForm.sensorNo || !bindForm.sensorType || !bindForm.labName) return
  if (editingBinding.value) {
    const b = bindings.value.find(b => b.id === editingBinding.value!.id)
    if (b) { b.labName = bindForm.labName; b.status = 'bound'; b.bindTime = new Date().toISOString().slice(0,16).replace('T',' ') }
  } else {
    const newId = Math.max(...bindings.value.map(b => b.id), 0) + 1
    bindings.value.push({
      id: newId, sensorNo: bindForm.sensorNo, sensorType: bindForm.sensorType,
      labName: bindForm.labName, bindTime: new Date().toISOString().slice(0,16).replace('T',' '), status:'bound'
    })
  }
  showBindModal.value = false
}
function doUnbind() {
  if (!editingBinding.value) return
  const b = bindings.value.find(b => b.id === editingBinding.value!.id)
  if (b) { b.labName = null; b.bindTime = null; b.status = 'unbound' }
  showBindModal.value = false
}
</script>

<style scoped>
.it-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.section-card { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:18px 20px; margin-bottom:16px }
.section-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px }
.section-header h3 { margin:0; font-size:15px; font-weight:600; color:#1a1a2e }
.section-actions { display:flex; align-items:center; gap:10px }
.tool-select { padding:7px 10px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.btn-reset { padding:7px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; color:#888; cursor:pointer }
.btn-reset:hover { border-color:#e74c3c; color:#e74c3c }

/* Threshold table */
.threshold-table { display:flex; flex-direction:column }
.th-row { display:flex; align-items:center; gap:12px; padding:10px 8px; border-bottom:1px solid rgba(0,0,0,.03) }
.th-row:last-child { border-bottom:none }
.th-head { border-bottom:1px solid rgba(0,0,0,.06); padding:8px 8px }
.th-head span { font-size:12px; color:#888; font-weight:600 }
.th-col-icon { width:28px; flex-shrink:0; display:flex; align-items:center; color:#888 }
.th-col-name { width:56px; flex-shrink:0; font-size:14px; font-weight:500; color:#1a1a2e }
.th-col-range { flex:1; font-size:13px; color:#555 }
.th-col-current { width:100px; flex-shrink:0; font-size:13px; color:#666; font-weight:500 }
.th-col-status { width:72px; flex-shrink:0; font-size:12px; color:#555; display:flex; align-items:center; gap:4px }
.th-col-act { width:56px; flex-shrink:0 }

.status-dot { width:6px; height:6px; border-radius:50%; flex-shrink:0 }
.dot-normal { background:#27ae60 } .dot-warning { background:#f39c12 } .dot-alarm { background:#e74c3c }

.act-link { padding:5px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; color:#555; cursor:pointer }
.act-link:hover { border-color:#1a73e8; color:#1a73e8 }
.act-link.warn:hover { border-color:#e74c3c; color:#e74c3c }

/* Bind table */
.bind-table-wrap { overflow:hidden }
.bind-table { width:100%; border-collapse:collapse; font-size:13px }
.bind-table thead { background:rgba(0,0,0,.02) }
.bind-table th { padding:10px 14px; text-align:left; font-weight:600; color:#888; font-size:12px; border-bottom:1px solid rgba(0,0,0,.06); white-space:nowrap }
.bind-table td { padding:9px 14px; border-bottom:1px solid rgba(0,0,0,.03); color:#333 }
.mono { font-family:'SF Mono','Menlo',monospace; font-size:12px; color:#666 }
.date-cell { color:#888; font-size:12px }
.bind-status { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:500 }
.bs-bound { background:rgba(39,174,96,.1); color:#27ae60 }
.bs-unbound { background:rgba(0,0,0,.05); color:#999 }

.btn-add { padding:8px 16px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }
.btn-add:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }

.empty-state.small { padding:24px 0 }
.empty-state p { margin:0; font-size:14px; color:#bbb; text-align:center }

.input-with-unit { display:flex; align-items:center; gap:6px }
.input-with-unit .form-input { flex:1 }
.unit { font-size:14px; color:#888 }
.unbind-warn { font-size:14px; color:#555; line-height:1.6; margin:0 }
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
.modal-btn.danger { background:linear-gradient(135deg,#e74c3c,#c0392b); color:#fff }
.modal-btn.danger:hover { box-shadow:0 4px 12px rgba(231,76,60,.3) }
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px; display:flex; flex-direction:column; gap:4px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
