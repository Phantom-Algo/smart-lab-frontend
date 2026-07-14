<template>
  <div class="im-page">
    <div class="page-header">
      <div>
        <h2>物联网环境</h2>
        <p>传感器实时监测、告警管理</p>
      </div>
      <div class="header-right">
        <span class="update-time">最后更新 {{ lastUpdate }}</span>
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
              <svg v-if="s.type==='temp'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 14.76V3.5a2.5 2.5 0 0 0-5 0v11.26a4.5 4.5 0 1 0 5 0z"/></svg>
              <svg v-else-if="s.type==='humidity'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>
              <svg v-else-if="s.type==='smoke'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
              <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            </div>
            <span class="sensor-label">{{ s.label }}</span>
            <span class="sensor-value" :class="'val-' + s.status">{{ s.displayValue }}</span>
            <div class="sensor-bar-wrap">
              <div class="sensor-bar" :class="'bar-' + s.status" :style="{ width: s.barPct + '%' }"></div>
              <div class="sensor-threshold-marker" v-if="s.thresholdPct > 0" :style="{ left: s.thresholdPct + '%' }"></div>
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
import { ref, computed, reactive } from 'vue'

type SensorStatus = 'normal' | 'warning' | 'alarm'
type SensorType = 'temp' | 'humidity' | 'smoke' | 'gas'

interface SensorData {
  type: SensorType; label: string; value: number; unit: string
  threshold: number; thresholdPct: number; status: SensorStatus
  displayValue: string; barPct: number
}

interface LabSensor {
  id: number; labName: string; overallStatus: SensorStatus
  updateAgo: string; sensors: SensorData[]
}

const labOptions = ['A305 分析化学实验室', 'B103 物理实验室', 'C202 计算机实验室', 'D108 生物培养实验室']

function randomVal(base: number, range: number) { return +(base + (Math.random() - 0.5) * range).toFixed(1) }
function ago() { return Math.floor(Math.random() * 30) + 1 + '秒前' }

function generateData(): LabSensor[] {
  return [
    { id:1, labName:'A305 分析化学实验室', overallStatus:'normal', updateAgo:ago(), sensors:[
      { type:'temp', label:'温度', value:randomVal(24,3), unit:'°C', threshold:35, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'humidity', label:'湿度', value:randomVal(52,8), unit:'%', threshold:70, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'smoke', label:'烟雾', value:randomVal(0.2,0.2), unit:'mg/m³', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
      { type:'gas', label:'气体', value:randomVal(0.1,0.1), unit:'ppm', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
    ]},
    { id:2, labName:'B103 物理实验室', overallStatus:'warning', updateAgo:ago(), sensors:[
      { type:'temp', label:'温度', value:randomVal(33,3), unit:'°C', threshold:35, thresholdPct:70, status:'warning', displayValue:'', barPct:0 },
      { type:'humidity', label:'湿度', value:randomVal(45,6), unit:'%', threshold:70, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'smoke', label:'烟雾', value:randomVal(0.3,0.2), unit:'mg/m³', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
      { type:'gas', label:'气体', value:randomVal(0.1,0.1), unit:'ppm', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
    ]},
    { id:3, labName:'C202 计算机实验室', overallStatus:'normal', updateAgo:ago(), sensors:[
      { type:'temp', label:'温度', value:randomVal(26,2), unit:'°C', threshold:35, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'humidity', label:'湿度', value:randomVal(48,5), unit:'%', threshold:70, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'smoke', label:'烟雾', value:randomVal(0.1,0.1), unit:'mg/m³', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
      { type:'gas', label:'气体', value:randomVal(0.0,0.1), unit:'ppm', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
    ]},
    { id:4, labName:'D108 生物培养实验室', overallStatus:'normal', updateAgo:ago(), sensors:[
      { type:'temp', label:'温度', value:randomVal(28,2), unit:'°C', threshold:35, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'humidity', label:'湿度', value:randomVal(65,5), unit:'%', threshold:70, thresholdPct:70, status:'normal', displayValue:'', barPct:0 },
      { type:'smoke', label:'烟雾', value:randomVal(0.1,0.1), unit:'mg/m³', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
      { type:'gas', label:'气体', value:randomVal(0.2,0.1), unit:'ppm', threshold:0.5, thresholdPct:50, status:'normal', displayValue:'', barPct:0 },
    ]},
  ]
}

function enrichData(data: LabSensor[]): LabSensor[] {
  return data.map(lab => {
    const sensors = lab.sensors.map(s => {
      let pct = Math.min(100, Math.round(s.value / s.threshold * 100))
      if (s.type === 'humidity') pct = Math.round(s.value)
      let status: SensorStatus = 'normal'
      if (s.value >= s.threshold) status = 'alarm'
      else if (s.value >= s.threshold * 0.8) status = 'warning'
      if (s.type === 'humidity') {
        if (s.value > 70 || s.value < 30) status = 'alarm'
        else if (s.value > 65 || s.value < 35) status = 'warning'
        else status = 'normal'
      }
      return {
        ...s,
        status,
        displayValue: s.value + ' ' + s.unit,
        barPct: pct,
      }
    })
    const hasAlarm = sensors.some(s => s.status === 'alarm')
    const hasWarning = sensors.some(s => s.status === 'warning')
    const overallStatus: SensorStatus = hasAlarm ? 'alarm' : hasWarning ? 'warning' : 'normal'
    return { ...lab, sensors, overallStatus }
  })
}

const labData = ref<LabSensor[]>(enrichData(generateData()))
const filterLab = ref('')
const refreshing = ref(false)
const lastUpdate = ref('刚刚')

const filteredLabs = computed(() => {
  if (!filterLab.value) return labData.value
  return labData.value.filter(l => l.labName === filterLab.value)
})

function statusCount(s: SensorStatus) { return labData.value.filter(l => l.overallStatus === s).length }
function statusLabel(s: SensorStatus) { return s === 'normal' ? '正常' : s === 'warning' ? '预警' : '告警' }

function doRefresh() {
  refreshing.value = true
  setTimeout(() => {
    labData.value = enrichData(generateData())
    const now = new Date()
    lastUpdate.value = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0') + ':' + now.getSeconds().toString().padStart(2,'0')
    refreshing.value = false
  }, 600)
}
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
