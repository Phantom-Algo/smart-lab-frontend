<template>
  <div class="ds-page">
    <div class="page-header">
      <h2>统计报表</h2>
      <p>使用率、故障率、健康画像 · {{ currentMonth }}</p>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <span v-if="loadError" class="err-msg">⚠ 数据加载失败，请检查后端服务</span>
      <span v-else-if="!dashboardData" class="loading-msg">加载中…</span>
      <button class="act-btn" @click="loadData">🔄 刷新数据</button>
      <button class="act-btn primary" :disabled="recalculating" @click="recalculateHealthScores">
        {{ recalculating ? '计算中…' : '📊 重算健康分' }}
      </button>
    </div>

    <!-- 概览卡片 -->
    <div class="stats-row">
      <div class="stat-card" v-for="c in overviewCards" :key="c.label">
        <div class="sc-top">
          <span class="sc-num" :class="c.color">{{ c.value }}</span>
          <span class="sc-trend" :class="c.trendUp ? 'up' : 'down'">{{ c.trend }}</span>
        </div>
        <div class="sc-label">{{ c.label }}</div>
        <div class="sc-sub">{{ c.sub }}</div>
      </div>
    </div>

    <!-- 图表区域 2x2 -->
    <div class="charts-grid">
      <!-- 使用率 TOP5 -->
      <div class="chart-card">
        <div class="chart-title">设备使用率 TOP 5</div>
        <div class="chart-body">
          <div v-for="(item, i) in usageTop5" :key="i" class="bar-row">
            <span class="bar-rank">{{ i + 1 }}</span>
            <span class="bar-name">{{ item.name }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: item.rate + '%', background: item.color }"></div>
            </div>
            <span class="bar-val">{{ item.rate }}%</span>
            <span class="bar-count">{{ item.count }}次</span>
          </div>
        </div>
      </div>

      <!-- 维修费用月度趋势 -->
      <div class="chart-card">
        <div class="chart-title">维修费用月度趋势</div>
        <div class="chart-body chart-columns">
          <div v-for="(m, i) in monthlyCosts" :key="i" class="col-item">
            <div class="col-bar-wrap">
              <div class="col-bar" :style="{ height: Math.round(costBarHeightPct(m.amount) / 100 * 114) + 'px' }"></div>
            </div>
            <span class="col-val">¥{{ (m.amount / 1000).toFixed(1) }}k</span>
            <span class="col-label">{{ m.month }}</span>
          </div>
        </div>
      </div>

      <!-- 故障分布（按实验室） -->
      <div class="chart-card">
        <div class="chart-title">故障分布（按实验室）</div>
        <div class="chart-body">
          <div v-for="(item, i) in failureByLab" :key="i" class="hbar-row">
            <span class="hbar-name">{{ item.lab }}</span>
            <div class="hbar-track">
              <div class="hbar-fill" :style="{ width: (item.count / maxFailure * 100) + '%' }"></div>
            </div>
            <span class="hbar-val">{{ item.count }}次</span>
          </div>
        </div>
      </div>

      <!-- 设备健康画像 -->
      <div class="chart-card">
        <div class="chart-title">设备健康画像</div>
        <div class="chart-body">
          <!-- 分段条 -->
          <div class="health-bar">
            <div class="hb-seg healthy" :style="{ flex: healthyCount }">
              <span>健康 ≥80</span><span>{{ healthyCount }}台</span>
            </div>
            <div class="hb-seg subhealthy" :style="{ flex: subhealthyCount }">
              <span>亚健康</span><span>{{ subhealthyCount }}台</span>
            </div>
            <div class="hb-seg risk" :style="{ flex: riskCount }">
              <span>&lt;60</span><span>{{ riskCount }}台</span>
            </div>
          </div>
          <!-- 预警列表 -->
          <div class="warn-title">报废预警（维修费 &gt; 净值 50%）</div>
          <div v-for="d in warningDevices" :key="d.id" class="warn-card">
            <div class="warn-left">
              <div class="warn-name">{{ d.name }}（{{ d.deviceNo }}）</div>
              <div class="warn-detail">
                净值 ¥{{ d.purchasePrice.toLocaleString() }} · 累计维修 ¥{{ d.totalRepairCost.toLocaleString() }}
                <span class="warn-pct">（{{ d.costRatio }}%）</span>
              </div>
            </div>
            <div class="warn-right">
              <span class="warn-score" :class="d.healthScore < 30 ? 'danger' : 'warn-color'">{{ d.healthScore }}</span>
              <span class="warn-tip">建议报废</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { getDashboardStats, getDeviceUsageTop, getCostStats, type DashboardStats, type DeviceUsageItem } from '../../utils/dashboardApi'
import { recalculateHealth, getDeviceHealthScores, type DeviceHealthItem } from '../../utils/deviceApi'

const currentMonth = '2026年7月'
const dashboardData = ref<DashboardStats | null>(null)
const deviceUsageTop = ref<DeviceUsageItem[]>([])
const monthlyCosts = ref<{ month: string; amount: number }[]>([])
const healthScores = ref<DeviceHealthItem[]>([])
const loadError = ref(false)
const recalculating = ref(false)
interface WarningDevice {
  id: number
  name: string
  deviceNo: string
  purchasePrice: number
  totalRepairCost: number
  costRatio: number
  healthScore: number
}

async function loadData() {
  loadError.value = false
  try {
    const [data, usage, healthList] = await Promise.all([
      getDashboardStats(),
      getDeviceUsageTop(),
      getDeviceHealthScores().catch(() => [] as DeviceHealthItem[]),
    ])
    dashboardData.value = data
    deviceUsageTop.value = usage
    healthScores.value = healthList

    // 加载月度维修费用（近6个月）
    const now = new Date()
    const labels: string[] = []
    const promises: Promise<any>[] = []
    for (let i = 5; i >= 0; i--) {
      const m = new Date(now.getFullYear(), now.getMonth() - i, 1)
      labels.push(String(m.getMonth() + 1).padStart(2, '0') + '月')
      const start = new Date(m.getFullYear(), m.getMonth(), 1).toISOString()
      const end = new Date(m.getFullYear(), m.getMonth() + 1, 0, 23, 59, 59).toISOString()
      promises.push(getCostStats(start, end).then(r => r.totalCost).catch(() => 0))
    }
    const results = await Promise.all(promises)
    monthlyCosts.value = labels.map((month, i) => ({ month, amount: results[i] }))
  } catch(e) {
    console.error('[DeviceStats] load error:', e)
    loadError.value = true
  }
}

async function recalculateHealthScores() {
  recalculating.value = true
  try {
    await recalculateHealth()
    alert('健康分重算完成')
    await loadData()
  } catch(e) {
    console.error('[DeviceStats] recalculate error:', e)
    alert('健康分重算失败')
  } finally {
    recalculating.value = false
  }
}

onMounted(() => loadData())

const overviewCards = computed(() => {
  const d = dashboardData.value
  return [
    { label: '设备总数', value: `${d?.totalDeviceCount ?? 0}台`, trend: '', trendUp: true, sub: '', color: 'blue' },
    { label: '本月借用', value: `${d?.monthBorrowCount ?? 0}次`, trend: '', trendUp: true, sub: '', color: 'green' },
    { label: '维修中', value: `${d?.repairDeviceCount ?? 0}台`, trend: '', trendUp: false, sub: '', color: 'orange' },
    { label: '累计维修', value: `${d?.totalRepairCount ?? 0}次`, trend: '', trendUp: false, sub: '', color: 'orange' },
    { label: '待处理告警', value: `${d?.alarms ?? 0}条`, trend: '', trendUp: true, sub: '', color: 'red' },
  ]
})

const usageTop5 = computed(() =>
  deviceUsageTop.value.map((item, i) => ({
    name: item.deviceName,
    rate: item.usageRate,
    count: item.borrowCount,
    color: ['#27ae60','#2ecc71','#f39c12','#1a73e8','#8e44ad'][i]
  }))
)

const minCost = computed(() => monthlyCosts.value.length ? Math.min(...monthlyCosts.value.map(m => m.amount)) : 0)
const maxCost = computed(() => monthlyCosts.value.length ? Math.max(...monthlyCosts.value.map(m => m.amount)) : 1)
function costBarHeightPct(amount: number): number { return maxCost.value === minCost.value ? 100 : (amount - minCost.value) / (maxCost.value - minCost.value) * 85 + 15 }

const failureByLab = computed(() =>
  (dashboardData.value?.labUsageList ?? []).slice(0, 5).map(item => ({ lab: item.name, count: item.count }))
)
const maxFailure = computed(() => Math.max(...failureByLab.value.map(f => f.count), 1))

const healthyCount = computed(() => {
  const bd = dashboardData.value?.deviceStatusBreakdown ?? []
  return bd.filter(s => s.status === 1 || s.status === 2).reduce((s, i) => s + i.count, 0)
})
const subhealthyCount = computed(() => {
  const bd = dashboardData.value?.deviceStatusBreakdown ?? []
  return bd.filter(s => s.status === 3).reduce((s, i) => s + i.count, 0)
})
const riskCount = computed(() => {
  const bd = dashboardData.value?.deviceStatusBreakdown ?? []
  return bd.filter(s => s.status === 4 || s.status === 5).reduce((s, i) => s + i.count, 0)
})

const warningDevices = computed<WarningDevice[]>(() => {
  return healthScores.value
    .filter(h => h.scrapAdvisory)
    .map(h => ({
      id: h.deviceId,
      name: h.deviceName,
      deviceNo: '',
      purchasePrice: 0,
      totalRepairCost: 0,
      costRatio: 0,
      healthScore: h.healthScore,
    }))
})
</script>

<style scoped>
.ds-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }
.action-bar { display:flex; align-items:center; gap:10px; margin-bottom:16px; padding:10px 16px; background:rgba(255,255,255,.5); border-radius:10px; border:1px solid rgba(0,0,0,.04) }
.err-msg { font-size:13px; color:#e74c3c; font-weight:500 }
.loading-msg { font-size:13px; color:#999 }
.act-btn { padding:8px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; color:#555; cursor:pointer; white-space:nowrap; transition:.15s }
.act-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.act-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; border:none }
.act-btn.primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.act-btn:disabled { opacity:.5; cursor:not-allowed }

/* 概览卡片 */
.stats-row { display:flex; gap:12px; margin-bottom:20px; flex-wrap:wrap }
.stat-card { flex:1; min-width:130px; background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:16px 18px }
.sc-top { display:flex; align-items:baseline; gap:8px; margin-bottom:4px }
.sc-num { font-size:22px; font-weight:700; color:#1a1a2e }
.sc-num.blue { color:#1a73e8 }
.sc-num.green { color:#27ae60 }
.sc-num.orange { color:#f39c12 }
.sc-num.red { color:#e74c3c }
.sc-trend { font-size:12px; font-weight:500 }
.sc-trend.up { color:#e74c3c }
.sc-trend.down { color:#27ae60 }
.sc-label { font-size:13px; color:#666; font-weight:500 }
.sc-sub { font-size:11px; color:#bbb; margin-top:2px }

/* 2x2 图表网格 */
.charts-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px }
.chart-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.04); border-radius:14px; padding:18px 20px }
.chart-title { font-size:14px; font-weight:600; color:#555; margin-bottom:14px }
.chart-body { }

/* 使用率 TOP5 横向条形图 */
.bar-row { display:flex; align-items:center; gap:8px; margin-bottom:12px }
.bar-rank { width:18px; font-size:12px; font-weight:700; color:#bbb; text-align:center }
.bar-name { width:80px; font-size:12px; color:#555; text-align:right; flex-shrink:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.bar-track { flex:1; height:10px; border-radius:5px; background:rgba(0,0,0,.05); overflow:hidden }
.bar-fill { height:100%; border-radius:5px; transition:width .5s; min-width:4px }
.bar-val { width:36px; font-size:12px; font-weight:600; color:#333; text-align:right }
.bar-count { width:32px; font-size:11px; color:#aaa; text-align:right }

/* 月度费用柱状图 */
.chart-columns { display:flex; align-items:flex-end; justify-content:space-around; height:150px; padding-top:8px }
.col-item { display:flex; flex-direction:column; align-items:center; gap:6px; flex:1 }
.col-bar-wrap { flex:1; position:relative; width:100% }
.col-bar { position:absolute; bottom:0; left:50%; transform:translateX(-50%); width:100%; max-width:32px; border-radius:5px 5px 0 0; background:linear-gradient(to top,#1a73e8,#4a90d9); min-height:4px; transition:height .4s }
.col-val { font-size:10px; color:#888 }
.col-label { font-size:11px; color:#aaa }

/* 故障分布 横向条形图 */
.hbar-row { display:flex; align-items:center; gap:10px; margin-bottom:12px }
.hbar-name { width:100px; font-size:12px; color:#555; text-align:right; flex-shrink:0 }
.hbar-track { flex:1; height:16px; border-radius:8px; background:rgba(0,0,0,.04); overflow:hidden }
.hbar-fill { height:100%; border-radius:8px; background:linear-gradient(90deg,#e74c3c,#ec7063); transition:width .5s; min-width:4px }
.hbar-val { width:36px; font-size:12px; font-weight:600; color:#c0392b }

/* 健康画像 */
.health-bar { display:flex; height:36px; border-radius:8px; overflow:hidden; margin-bottom:14px }
.hb-seg { display:flex; align-items:center; justify-content:center; gap:6px; font-size:11px; font-weight:600; color:#fff; min-width:0 }
.hb-seg.healthy { background:linear-gradient(135deg,#27ae60,#2ecc71) }
.hb-seg.subhealthy { background:linear-gradient(135deg,#f39c12,#e67e22) }
.hb-seg.risk { background:linear-gradient(135deg,#e74c3c,#c0392b) }

.warn-title { font-size:12px; font-weight:600; color:#e67e22; margin-bottom:10px }
.warn-card { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; background:rgba(231,76,60,.03); border:1px solid rgba(231,76,60,.1); border-radius:10px; margin-bottom:8px }
.warn-left { min-width:0 }
.warn-name { font-size:13px; font-weight:600; color:#333; margin-bottom:2px }
.warn-detail { font-size:11px; color:#888 }
.warn-pct { color:#e74c3c; font-weight:600 }
.warn-right { display:flex; flex-direction:column; align-items:center; flex-shrink:0; margin-left:12px }
.warn-score { font-size:22px; font-weight:700 }
.warn-score.danger { color:#e74c3c }
.warn-score.warn-color { color:#f39c12 }
.warn-tip { font-size:10px; color:#e74c3c; font-weight:500 }

@media (max-width: 1024px) {
  .charts-grid { grid-template-columns:1fr }
}
</style>
