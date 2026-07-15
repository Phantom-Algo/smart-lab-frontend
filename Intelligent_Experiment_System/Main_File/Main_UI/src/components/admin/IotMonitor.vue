<template>
  <div class="im-page">
    <div class="page-header">
      <div>
        <h2>物联网环境</h2>
        <p>传感器实时监测、告警管理</p>
      </div>
      <div class="header-right">
        <span class="update-time">最后更新 {{ lastUpdate }}</span>
        <button class="refresh-btn" @click="doDetectOffline">检测离线传感器</button>
        <button class="refresh-btn" @click="doRefresh" :class="{ spinning: refreshing }">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          刷新
        </button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ labData.length }}</div><div class="stat-label">监测中</div></div>
      <div class="stat-card"><div class="stat-num green">{{ statusCount('normal') }}</div><div class="stat-label">正常</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ statusCount('warning') }}</div><div class="stat-label">预警</div></div>
      <div class="stat-card"><div class="stat-num red">{{ statusCount('alarm') }}</div><div class="stat-label">告警</div></div>
    </div>

    <div class="filter-bar">
      <select v-model="filterLab" class="tool-select"><option value="">全部实验室</option><option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option></select>
    </div>

    <div class="card-grid" v-if="filteredLabs.length > 0">
      <div v-for="lab in filteredLabs" :key="lab.id" class="lab-card" :class="'status-' + lab.overallStatus">
        <div class="card-top">
          <div class="card-lab-info">
            <span class="lab-status-dot" :class="'dot-' + lab.overallStatus"></span>
            <span class="lab-name">{{ lab.labName }}</span>
          </div>
          <span class="lab-update">{{ lab.updateAgo }}</span>
        </div>
        <div class="sensors">
          <div class="sensor-row" v-for="s in lab.sensors" :key="s.type">
            <div class="sensor-icon" :class="'icon-' + s.status">
              <svg v-if="s.type==='temperature'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>
              <svg v-else-if="s.type==='humidity'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>
              <svg v-else-if="s.type==='smoke'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            </div>
            <span class="sensor-label">{{ s.label }}</span>
            <span class="sensor-value" :class="'val-' + s.status">{{ s.displayValue }}</span>
            <div class="sensor-bar-wrap">
              <div class="sensor-bar" :class="'bar-' + s.status" :style="{ width: s.barPct + '%' }"></div>
              <div class="sensor-threshold-marker" v-if="s.threshold > 0" :style="{ left: Math.min(100, (s.value / s.threshold) * 100) + '%' }"></div>
            </div>
            <span class="sensor-status" :class="'text-' + s.status">{{ statusLabel(s.status) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
      <p>暂无监测数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { detectOfflineSensors, getLabMonitorData, type LabMonitorData } from '../../utils/sensorApi'
import { REALTIME_EVENT_NAME, type RealtimeEnvelope } from '../../utils/wsClient'

type SensorStatus = 'normal' | 'warning' | 'alarm'

interface SensorDisplay {
  type: string; label: string; value: number; unit: string
  threshold: number; status: SensorStatus
  displayValue: string; barPct: number
}

interface LabDisplay {
  id: number; labName: string; overallStatus: SensorStatus
  updateAgo: string; sensors: SensorDisplay[]
}

const labData = ref<LabDisplay[]>([])
const filterLab = ref('')
const refreshing = ref(false)
const loadError = ref(false)
const lastUpdate = ref('加载中…')

const labOptions = computed(() => labData.value.map(l => l.labName))

const filteredLabs = computed(() => {
  if (!filterLab.value) return labData.value
  return labData.value.filter(l => l.labName === filterLab.value)
})

function statusCount(s: SensorStatus) { return labData.value.filter(l => l.overallStatus === s).length }
function statusLabel(s: SensorStatus) { return s === 'normal' ? '正常' : s === 'warning' ? '预警' : '告警' }

function toDisplay(data: LabMonitorData[]): LabDisplay[] {
  return data.map(lab => {
    const sensors = (lab.sensors ?? []).map(s => ({
      ...s,
      status: s.status as SensorStatus,
      displayValue: s.value + ' ' + s.unit,
    }))
    return {
      id: lab.labId,
      labName: lab.labName,
      overallStatus: lab.overallStatus as SensorStatus,
      updateAgo: formatAgo(lab.updateAgo),
      sensors,
    }
  })
}

function formatAgo(seconds: number): string {
  if (seconds < 60) return seconds + '秒前'
  if (seconds < 3600) return Math.floor(seconds / 60) + '分钟前'
  return Math.floor(seconds / 3600) + '小时前'
}

async function loadData() {
  loadError.value = false
  try {
    const data = await getLabMonitorData()
    labData.value = toDisplay(data)
    const now = new Date()
    lastUpdate.value = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0') + ':' + now.getSeconds().toString().padStart(2,'0')
  } catch(e) {
    console.error('[IotMonitor] load error:', e)
    loadError.value = true
    lastUpdate.value = '加载失败'
  }
}

async function doDetectOffline() {
  try {
    const count = await detectOfflineSensors()
    alert(`离线检测完成，${count} 个传感器状态已变更`)
    await loadData()
  } catch(e) {
    console.error('[IotMonitor] detect offline error:', e)
    alert('离线检测失败')
  }
}

function doRefresh() {
  refreshing.value = true
  loadData().finally(() => { refreshing.value = false })
}

/** 处理 WebSocket 实时推送的传感器数据更新 */
function handleRealtimeEvent(event: Event) {
  const envelope = (event as CustomEvent<RealtimeEnvelope>).detail
  if (envelope.event === 'sensor.data.updated' || envelope.event === 'sensor.status_changed') {
    loadData()
  }
}

onMounted(() => {
  loadData()
  window.addEventListener(REALTIME_EVENT_NAME, handleRealtimeEvent)
})

onUnmounted(() => {
  window.removeEventListener(REALTIME_EVENT_NAME, handleRealtimeEvent)
})
</script>

<style scoped>
.im-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }
.header-right { display:flex; align-items:center; gap:12px; flex-shrink:0 }
.update-time { font-size:12px; color:#999 }
.refresh-btn { display:flex; align-items:center; gap:5px; padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:10px; background:#fff; font-size:13px; color:#555; cursor:pointer; transition:.2s }
.refresh-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.refresh-btn.spinning svg { animation:spin .6s linear }
@keyframes spin { to { transform:rotate(360deg) } }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:80px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .green { color:#27ae60 } .orange { color:#f39c12 } .red { color:#e74c3c }

.filter-bar { margin-bottom:16px }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }

.card-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px }
@media (max-width:900px) { .card-grid { grid-template-columns:1fr } }

.lab-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden; transition:box-shadow .2s }
.lab-card:hover { box-shadow:0 4px 16px rgba(0,0,0,.06) }
.lab-card.status-normal { border-left:3px solid #27ae60 }
.lab-card.status-warning { border-left:3px solid #f39c12 }
.lab-card.status-alarm { border-left:3px solid #e74c3c }

.card-top { display:flex; align-items:center; justify-content:space-between; padding:14px 16px 10px }
.card-lab-info { display:flex; align-items:center; gap:8px }
.lab-status-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0 }
.dot-normal { background:#27ae60 } .dot-warning { background:#f39c12 } .dot-alarm { background:#e74c3c; animation:pulse 1.5s infinite }
@keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.4} }
.lab-name { font-weight:600; color:#1a1a2e; font-size:14px }
.lab-update { font-size:11px; color:#aaa }

.sensors { padding:0 16px 14px; display:flex; flex-direction:column; gap:6px }
.sensor-row { display:flex; align-items:center; gap:8px }
.sensor-icon { width:24px; height:24px; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.sensor-icon svg { stroke-width:1.5 }
.icon-normal { color:#888 } .icon-warning { color:#f39c12 } .icon-alarm { color:#e74c3c }
.sensor-label { font-size:12px; color:#888; width:28px; flex-shrink:0 }
.sensor-value { font-size:13px; font-weight:500; width:64px; flex-shrink:0; text-align:right }
.val-normal { color:#333 } .val-warning { color:#e67e22 } .val-alarm { color:#e74c3c; font-weight:600 }

.sensor-bar-wrap { flex:1; height:6px; background:rgba(0,0,0,.05); border-radius:3px; overflow:hidden; position:relative; min-width:60px }
.sensor-bar { height:100%; border-radius:3px; transition:width .5s ease }
.bar-normal { background:#27ae60 } .bar-warning { background:#f39c12 } .bar-alarm { background:#e74c3c }
.sensor-threshold-marker { position:absolute; top:0; bottom:0; width:2px; background:rgba(0,0,0,.2); border-radius:1px }

.sensor-status { font-size:11px; font-weight:500; width:30px; flex-shrink:0 }
.text-normal { color:#27ae60 } .text-warning { color:#e67e22 } .text-alarm { color:#e74c3c }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }
</style>
