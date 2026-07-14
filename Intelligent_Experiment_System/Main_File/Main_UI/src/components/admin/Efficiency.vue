<template>
  <div class="ef-page">
    <div class="page-header">
      <div>
        <h2>效能看板</h2>
        <p>设备利用率、预约统计、故障率</p>
      </div>
      <span class="period-tag">2026年7月</span>
    </div>

    <div class="kpi-row">
      <div v-for="kpi in kpiCards" :key="kpi.label" class="kpi-card">
        <div class="kpi-icon" :style="{ background: kpi.bg, color: kpi.color }">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/></svg>
        </div>
        <div class="kpi-body">
          <span class="kpi-value">{{ kpi.value }}<span class="kpi-unit" v-if="kpi.unit">{{ kpi.unit }}</span></span>
          <span class="kpi-label">{{ kpi.label }}</span>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <!-- 实验室使用率排行 -->
      <div class="chart-card">
        <div class="chart-title">实验室使用率排行</div>
        <div class="bar-list">
          <div v-for="item in labUsageRank" :key="item.name" class="bar-row">
            <span class="bar-label">{{ item.name }}</span>
            <div class="bar-wrap">
              <div class="bar-fill" :class="'bar-' + item.color" :style="{ width: item.pct + '%' }"></div>
            </div>
            <span class="bar-val">{{ item.pct }}%</span>
          </div>
        </div>
      </div>

      <!-- 月度预约趋势 -->
      <div class="chart-card">
        <div class="chart-title">月度预约趋势</div>
        <div class="col-chart">
          <div v-for="m in monthlyReservations" :key="m.month" class="col-item">
            <div class="col-bar-wrap">
              <div class="col-bar" :style="{ height: colHeight(m.count, monthlyReservations) + 'px' }"></div>
            </div>
            <span class="col-label">{{ m.month }}</span>
            <span class="col-val">{{ m.count }}</span>
          </div>
        </div>
      </div>

      <!-- 设备故障率排行 -->
      <div class="chart-card">
        <div class="chart-title">设备故障率排行</div>
        <div class="bar-list">
          <div v-for="item in failureRank" :key="item.name" class="bar-row">
            <span class="bar-label">{{ item.name }}</span>
            <div class="bar-wrap">
              <div class="bar-fill bar-red" :style="{ width: item.pct * 10 + '%' }"></div>
            </div>
            <span class="bar-val">{{ item.pct }}%</span>
          </div>
        </div>
      </div>

      <!-- 安全事件趋势 -->
      <div class="chart-card">
        <div class="chart-title">安全事件趋势</div>
        <div class="col-chart">
          <div v-for="m in safetyTrend" :key="m.month" class="col-item">
            <div class="col-bar-wrap">
              <div class="col-bar bar-orange" :style="{ height: colHeight(m.count, safetyTrend) + 'px' }"></div>
            </div>
            <span class="col-label">{{ m.month }}</span>
            <span class="col-val">{{ m.count }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getDashboardStats, type DashboardStats } from '../../utils/dashboardApi'

const dashboardData = ref<DashboardStats | null>(null)

async function loadData() {
  try {
    dashboardData.value = await getDashboardStats()
  } catch (e) { console.error('Efficiency loadData error:', e) }
}
onMounted(() => loadData())

const labUsageRank = computed(() =>
  (dashboardData.value?.labUsageList ?? []).map(item => ({
    name: item.name, pct: dashboardData.value ? Math.round(item.count / Math.max(dashboardData.value.totalDeviceCount, 1) * 100) : 0, color: 'blue'
  }))
)

const monthlyReservations = computed(() =>
  (dashboardData.value?.monthlyReservationTrend ?? []).map(m => ({
    month: m.month?.slice(5) ?? m.month, count: m.count
  }))
)

const failureRank = computed(() => {
  const total = dashboardData.value?.totalDeviceCount ?? 1
  const repair = dashboardData.value?.repairDeviceCount ?? 0
  return [{ name: '全平台', pct: Math.round(repair / Math.max(total, 1) * 100) }]
})

const safetyTrend = computed(() =>
  (dashboardData.value?.monthlyBorrowTrend ?? []).slice(-6).map(m => ({
    month: m.month?.slice(5) ?? m.month, count: m.count
  }))
)

const kpiCards = computed(() => {
  const d = dashboardData.value
  return [
    { label: '实验室总数', value: `${d?.labCount ?? 0}`, unit: '间', color: '#1a73e8', bg: 'rgba(26,115,232,.1)' },
    { label: '设备总数', value: `${d?.totalDeviceCount ?? 0}`, unit: '台', color: '#27ae60', bg: 'rgba(39,174,96,.1)' },
    { label: '本月预约', value: `${d?.monthReservationCount ?? 0}`, unit: '次', color: '#8e44ad', bg: 'rgba(142,68,173,.1)' },
    { label: '本月借用', value: `${d?.monthBorrowCount ?? 0}`, unit: '次', color: '#f39c12', bg: 'rgba(243,156,18,.1)' },
    { label: '待处理告警', value: `${d?.alarms ?? 0}`, unit: '条', color: '#e74c3c', bg: 'rgba(231,76,60,.1)' },
  ]
})

const maxRsv = computed(() => {
  const arr = monthlyReservations.value
  return arr.length > 0 ? Math.max(...arr.map(m => m.count)) : 1
})
const maxSafety = computed(() => {
  const arr = safetyTrend.value
  return arr.length > 0 ? Math.max(...arr.map(m => m.count)) : 1
})

function colHeight(val: number, arr: { count: number }[]) {
  const vals = arr.map(a => a.count)
  const max = Math.max(...vals, 1)
  const min = Math.min(...vals)
  const range = max - min || 1
  return Math.round((val - min) / range * 80 + 20)
}
</script>

<style scoped>
.ef-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }
.period-tag { padding:6px 14px; background:rgba(26,115,232,.08); color:#1a73e8; border-radius:8px; font-size:13px; font-weight:500 }

.kpi-row { display:flex; gap:12px; margin-bottom:18px; flex-wrap:wrap }
.kpi-card { flex:1; min-width:150px; background:rgba(255,255,255,.6); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.04); border-radius:12px; padding:16px; display:flex; align-items:center; gap:12px }
.kpi-icon { width:40px; height:40px; border-radius:10px; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.kpi-body { display:flex; flex-direction:column; min-width:0 }
.kpi-value { font-size:22px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.kpi-unit { font-size:14px; font-weight:500; color:#888 }
.kpi-label { font-size:11px; color:#999; margin-top:1px }
.kpi-trend { display:flex; align-items:center; gap:2px; font-size:12px; font-weight:600; margin-left:auto; flex-shrink:0 }
.kpi-trend.up { color:#27ae60 }
.kpi-trend.down { color:#e74c3c }

.chart-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px }
@media (max-width:900px) { .chart-grid { grid-template-columns:1fr } }

.chart-card { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:18px 20px }
.chart-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:14px }

/* horizontal bar chart */
.bar-list { display:flex; flex-direction:column; gap:10px }
.bar-row { display:flex; align-items:center; gap:10px }
.bar-label { font-size:12px; color:#666; width:90px; flex-shrink:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.bar-wrap { flex:1; height:16px; background:rgba(0,0,0,.04); border-radius:4px; overflow:hidden }
.bar-fill { height:100%; border-radius:4px; transition:width .4s ease }
.bar-blue { background:linear-gradient(90deg,#1a73e8,#4a90d9) }
.bar-orange { background:linear-gradient(90deg,#f39c12,#e67e22) }
.bar-red { background:linear-gradient(90deg,#e74c3c,#c0392b) }
.bar-val { font-size:12px; font-weight:600; color:#555; width:36px; text-align:right; flex-shrink:0 }

/* column chart */
.col-chart { display:flex; align-items:flex-end; justify-content:space-around; gap:4px; height:130px; padding-top:8px }
.col-item { display:flex; flex-direction:column; align-items:center; gap:4px; flex:1 }
.col-bar-wrap { width:100%; max-width:32px; flex:1; display:flex; align-items:flex-end; position:relative }
.col-bar { width:100%; background:linear-gradient(180deg,#1a73e8,#4a90d9); border-radius:4px 4px 0 0; min-height:4px; transition:height .4s ease }
.col-bar.bar-orange { background:linear-gradient(180deg,#e74c3c,#c0392b) }
.col-label { font-size:10px; color:#888 }
.col-val { font-size:11px; font-weight:600; color:#555 }
</style>
