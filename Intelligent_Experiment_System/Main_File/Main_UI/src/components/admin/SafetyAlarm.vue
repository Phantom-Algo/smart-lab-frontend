<template>
  <div class="sa-page">
    <div class="page-header">
      <h2>报警记录</h2>
      <p>物联网/耗材报警处理</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ alarms.length }}</div><div class="stat-label">总数</div></div>
      <div class="stat-card"><div class="stat-num red">{{ statusCount('pending') }}</div><div class="stat-label">未处理</div></div>
      <div class="stat-card"><div class="stat-num green">{{ statusCount('done') }}</div><div class="stat-label">已处理</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ todayCount }}</div><div class="stat-label">今日新增</div></div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key; pageNum = 1">
          {{ t.label }}<span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <select v-model="filterType" class="tool-select"><option value="">全部类型</option><option value="iot">物联网环境报警</option><option value="consumable">耗材药材报警</option></select>
      <select v-model="filterLevel" class="tool-select"><option value="">全部等级</option><option value="high">重要</option><option value="normal">一般</option></select>
    </div>

    <div class="card-list" v-if="pagedList.length > 0">
      <div v-for="a in pagedList" :key="a.id" class="alarm-card" :class="'card-' + a.handleStatus">
        <div class="card-left">
          <div class="card-header">
            <span class="level-tag" :class="'level-' + a.alarmLevel">{{ levelLabel(a.alarmLevel) }}</span>
            <span class="status-badge" :class="'badge-' + a.handleStatus">{{ a.handleStatus === 'pending' ? '未处理' : '已处理' }}</span>
            <span class="alarm-no">{{ a.alarmNo }}</span>
            <span class="alarm-time">{{ a.alarmTime }}</span>
          </div>
          <div class="card-body">
            <div class="card-row type-row">
              <span class="type-tag" :class="'type-' + a.alarmType">{{ typeLabel(a.alarmType) }}</span>
              <span class="ci-label">实验室</span><span class="ci-value">{{ a.labName }}</span>
            </div>
            <div class="card-row">
              <span class="ci-label">对象</span><span class="ci-value">{{ a.alarmObject }}</span>
            </div>
            <div class="card-row">
              <span class="ci-label">内容</span><span class="ci-value">{{ a.alarmContent }}</span>
            </div>
            <div class="card-row value-row">
              <span class="ci-label">数值</span>
              <span class="ci-value alarm-val">{{ a.alarmValue }}</span>
              <span class="vs-arrow">→</span>
              <span class="ci-label">阈值</span>
              <span class="ci-value threshold">{{ a.thresholdValue }}</span>
              <span class="exceed-tag" :class="a.alarmLevel === 'high' ? 'exceed-high' : 'exceed-normal'">
                {{ exceedText(a) }}
              </span>
            </div>
            <div class="card-row" v-if="a.handleStatus === 'done'">
              <span class="ci-label">处理人</span><span class="ci-value">{{ a.handlerName }}</span>
              <span class="ci-sep">|</span>
              <span class="ci-label">结果</span><span class="ci-value handle-result">{{ a.handleResult }}</span>
            </div>
          </div>
        </div>
        <div class="card-right">
          <button v-if="a.handleStatus === 'pending'" class="btn-primary" @click="openHandle(a)">处理</button>
          <button v-else class="btn-outline" @click="openDetail(a)">详情</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      <p>暂无报警记录</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 处理弹窗（未处理→已处理） -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showHandle" class="modal-overlay" @click.self="showHandle = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>处理报警</h3><button class="modal-close" @click="showHandle = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>报警编号</span><span>{{ currentAlarm?.alarmNo }}</span></div>
                <div class="ci-row"><span>报警类型</span><span>{{ typeLabel(currentAlarm?.alarmType || 'iot') }}</span></div>
                <div class="ci-row"><span>报警对象</span><span>{{ currentAlarm?.alarmObject }}</span></div>
                <div class="ci-row"><span>报警内容</span><span>{{ currentAlarm?.alarmContent }}</span></div>
                <div class="ci-row"><span>报警值</span><span class="alarm-val">{{ currentAlarm?.alarmValue }}</span></div>
                <div class="ci-row"><span>阈值</span><span class="threshold">{{ currentAlarm?.thresholdValue }}</span></div>
              </div>
              <div class="form-g"><label>处理结果 <span class="required">*</span></label><textarea v-model="handleForm.result" class="form-textarea" placeholder="请填写处理措施，如：已调整空调温度至25℃" rows="3"></textarea></div>
              <div class="form-g"><label>备注</label><input v-model="handleForm.remark" class="form-input" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showHandle = false">取消</button>
              <button class="modal-btn primary" @click="doHandle" :disabled="!handleForm.result.trim()">确认处理</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 详情弹窗（已处理只读） -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>报警详情</h3><button class="modal-close" @click="showDetail = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="detail-section">
                <div class="ds-title">报警信息</div>
                <div class="detail-grid">
                  <div class="dg-item"><span class="dg-label">编号</span><span>{{ currentAlarm?.alarmNo }}</span></div>
                  <div class="dg-item"><span class="dg-label">类型</span><span>{{ typeLabel(currentAlarm?.alarmType || 'iot') }}</span></div>
                  <div class="dg-item"><span class="dg-label">等级</span><span>{{ levelLabel(currentAlarm?.alarmLevel || 'normal') }}</span></div>
                  <div class="dg-item"><span class="dg-label">时间</span><span>{{ currentAlarm?.alarmTime }}</span></div>
                  <div class="dg-item"><span class="dg-label">实验室</span><span>{{ currentAlarm?.labName }}</span></div>
                  <div class="dg-item"><span class="dg-label">对象</span><span>{{ currentAlarm?.alarmObject }}</span></div>
                  <div class="dg-item"><span class="dg-label">内容</span><span>{{ currentAlarm?.alarmContent }}</span></div>
                  <div class="dg-item"><span class="dg-label">报警值</span><span class="alarm-val">{{ currentAlarm?.alarmValue }}</span></div>
                  <div class="dg-item"><span class="dg-label">阈值</span><span class="threshold">{{ currentAlarm?.thresholdValue }}</span></div>
                </div>
              </div>
              <div class="detail-section">
                <div class="ds-title">处理信息</div>
                <div class="detail-grid">
                  <div class="dg-item"><span class="dg-label">处理人</span><span>{{ currentAlarm?.handlerName }}</span></div>
                  <div class="dg-item"><span class="dg-label">处理结果</span><span class="handle-result">{{ currentAlarm?.handleResult }}</span></div>
                  <div class="dg-item full" v-if="currentAlarm?.remark"><span class="dg-label">备注</span><span>{{ currentAlarm?.remark }}</span></div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDetail = false">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { getUnifiedAlerts, handleDeviceAlert, handleMaterialAlert, type UnifiedAlert } from '../../utils/alertApi'

type AlarmType = 'iot' | 'consumable'
type AlarmLevel = 'high' | 'normal'
type HandleStatus = 'pending' | 'done'

interface Alarm {
  id: string; alarmNo: string; alarmType: AlarmType; alarmLevel: AlarmLevel
  labName: string; alarmObject: string; alarmContent: string
  alarmValue: string; thresholdValue: string; alarmTime: string
  handleStatus: HandleStatus; handlerName: string | null
  handleResult: string | null; remark: string | null
}

const alarms = ref<Alarm[]>([])
const loadError = ref(false)
const loading = ref(true)

const activeStatus = ref<HandleStatus | 'all'>('all')
const filterType = ref('')
const filterLevel = ref('')
const pageNum = ref(1)
const pageSize = 6

async function loadAlarms() {
  loading.value = true
  loadError.value = false
  try {
    const statusFilter = activeStatus.value === 'pending' ? 0 : activeStatus.value === 'done' ? 1 : undefined
    const typeParam = (filterType.value || '') as '' | 'iot' | 'consumable'
    const result = await getUnifiedAlerts({
      pageNum: 1, pageSize: 500,
      type: typeParam,
      status: statusFilter,
    })
    alarms.value = result.records.map(r => ({
      ...r,
      id: r.id,
      alarmNo: r.alarmNo,
      alarmType: r.alarmType as AlarmType,
      alarmLevel: r.alarmLevel as AlarmLevel,
      handleStatus: r.handleStatus as HandleStatus,
    }))
    pageNum.value = 1
  } catch (e) {
    console.error('[SafetyAlarm] load error:', e)
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => loadAlarms())

const statusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: alarms.value.length },
  { key: 'pending' as const, label: '未处理', count: alarms.value.filter(a => a.handleStatus === 'pending').length },
  { key: 'done' as const, label: '已处理', count: alarms.value.filter(a => a.handleStatus === 'done').length },
])

const todayStr = new Date().toISOString().slice(0, 10)
const todayCount = computed(() =>
  alarms.value.filter(a => a.alarmTime.startsWith(todayStr)).length
)

const filteredList = computed(() => {
  let list = alarms.value
  if (activeStatus.value !== 'all') list = list.filter(a => a.handleStatus === activeStatus.value)
  if (filterType.value) list = list.filter(a => a.alarmType === filterType.value)
  if (filterLevel.value) list = list.filter(a => a.alarmLevel === filterLevel.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function statusCount(s: HandleStatus) { return alarms.value.filter(a => a.handleStatus === s).length }
function typeLabel(t: AlarmType) { return t === 'iot' ? '物联网环境报警' : '耗材药材报警' }
function levelLabel(l: AlarmLevel) { return l === 'high' ? '重要' : '一般' }
function exceedText(a: Alarm) {
  return a.alarmLevel === 'high' ? '超标' : '异常'
}

// 处理弹窗
const showHandle = ref(false)
const currentAlarm = ref<Alarm | null>(null)
const handleForm = reactive({ result: '', remark: '' })
function openHandle(a: Alarm) { currentAlarm.value = a; handleForm.result = ''; handleForm.remark = ''; showHandle.value = true }
async function doHandle() {
  if (!currentAlarm.value || !handleForm.result.trim()) return
  const a = currentAlarm.value
  try {
    if (a.alarmType === 'iot') {
      const devId = parseInt(a.id.replace('dev-', ''))
      if (!isNaN(devId)) await handleDeviceAlert(devId, handleForm.result.trim())
    } else {
      const matId = parseInt(a.id.replace('mat-', ''))
      if (!isNaN(matId)) await handleMaterialAlert(matId, handleForm.result.trim())
    }
    a.handleStatus = 'done'
    a.handleResult = handleForm.result.trim()
    a.remark = handleForm.remark.trim() || null
    showHandle.value = false
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '处理失败')
  }
}

// 详情弹窗
const showDetail = ref(false)
function openDetail(a: Alarm) { currentAlarm.value = a; showDetail.value = true }

// 切换筛选时重新加载
import { watch } from 'vue'
watch([activeStatus, filterType], () => { loadAlarms() })
</script>

<style scoped>
.sa-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:90px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .red { color:#e74c3c } .orange { color:#f39c12 } .green { color:#27ae60 }

.filter-bar { display:flex; align-items:center; gap:10px; margin-bottom:18px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer; margin-left:auto }

.card-list { display:flex; flex-direction:column; gap:12px }
.alarm-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:stretch; overflow:hidden }
.alarm-card.card-pending { border-left:3px solid #e74c3c }
.alarm-card.card-done { border-left:3px solid #27ae60; opacity:.82 }

.card-left { flex:1; padding:16px 20px; min-width:0 }
.card-right { display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:8px; padding:16px 20px; flex-shrink:0; min-width:100px; border-left:1px solid rgba(0,0,0,.04) }

.card-header { display:flex; align-items:center; gap:8px; margin-bottom:8px }
.level-tag { padding:2px 6px; border-radius:3px; font-size:10px; font-weight:700; color:#fff }
.level-high { background:#e74c3c } .level-normal { background:#f39c12 }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.badge-pending { background:rgba(231,76,60,.1); color:#e74c3c }
.badge-done { background:rgba(39,174,96,.1); color:#27ae60 }
.alarm-no { font-size:13px; font-weight:600; color:#1a1a2e }
.alarm-time { font-size:12px; color:#999; margin-left:auto }

.card-body { display:flex; flex-direction:column; gap:5px }
.card-row { display:flex; align-items:baseline; gap:6px; font-size:13px; flex-wrap:wrap }
.type-row { margin-bottom:2px }
.type-tag { padding:1px 6px; border-radius:3px; font-size:11px; font-weight:500 }
.type-iot { background:rgba(26,115,232,.08); color:#1a73e8 }
.type-consumable { background:rgba(142,68,173,.08); color:#8e44ad }
.ci-label { color:#999; min-width:44px; flex-shrink:0 }
.ci-value { color:#333 }
.ci-sep { color:#ddd }
.value-row { margin:4px 0; padding:6px 10px; background:rgba(0,0,0,.02); border-radius:6px; align-items:center; display:inline-flex; width:fit-content }
.alarm-val { color:#e74c3c; font-weight:600 }
.threshold { color:#1a73e8; font-weight:500 }
.vs-arrow { color:#ccc; font-size:12px }
.exceed-tag { margin-left:6px; padding:1px 6px; border-radius:3px; font-size:10px; font-weight:600 }
.exceed-high { background:rgba(231,76,60,.12); color:#e74c3c }
.exceed-normal { background:rgba(243,156,18,.12); color:#e67e22 }
.handle-result { color:#27ae60 }

.btn-primary { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }
.btn-primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.btn-outline { padding:9px 20px; border:1px solid rgba(0,0,0,.12); border-radius:10px; background:#fff; color:#555; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-outline:hover { border-color:#aaa }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
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
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px; display:flex; flex-direction:column; gap:4px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; resize:vertical; width:100%; box-sizing:border-box }
.form-textarea:focus { border-color:#1a73e8 }

.detail-section { margin-bottom:6px }
.ds-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px; padding-bottom:6px; border-bottom:1px solid rgba(0,0,0,.06) }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px 16px }
.dg-item { display:flex; justify-content:space-between; font-size:13px; color:#555; padding:4px 0 }
.dg-item.full { grid-column:1 / -1 }
.dg-label { color:#999 }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
