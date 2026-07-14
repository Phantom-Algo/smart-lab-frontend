<template>
  <div class="ls-page">
    <div class="page-header">
      <h2>使用统计</h2>
      <p>实验室利用率、空闲时段分析 · {{ currentMonth }}</p>
    </div>

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

    <div class="charts-grid">
      <!-- 实验室利用率排行 -->
      <div class="chart-card">
        <div class="chart-title">实验室利用率排行</div>
        <div class="chart-body">
          <div v-for="(item, i) in usageRank" :key="i" class="bar-row">
            <span class="bar-rank">{{ i + 1 }}</span>
            <span class="bar-name">{{ item.name }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: item.rate + '%', background: item.color }"></div>
            </div>
            <span class="bar-val">{{ item.rate }}%</span>
          </div>
        </div>
      </div>

      <!-- 月度预约趋势 -->
      <div class="chart-card">
        <div class="chart-title">月度预约趋势</div>
        <div class="chart-body chart-columns">
          <div v-for="(m, i) in monthlyTrend" :key="i" class="col-item">
            <div class="col-bar-wrap">
              <div class="col-bar" :style="{ height: Math.round(barHeightPct(m.count) / 100 * 104) + 'px' }"></div>
            </div>
            <span class="col-val">{{ m.count }}次</span>
            <span class="col-label">{{ m.month }}</span>
          </div>
        </div>
      </div>

      <!-- 热门时段分布 -->
      <div class="chart-card">
        <div class="chart-title">热门时段分布</div>
        <div class="chart-body">
          <div v-for="(item, i) in timeSlotDist" :key="i" class="hbar-row">
            <span class="hbar-name">{{ item.slot }}</span>
            <div class="hbar-track">
              <div class="hbar-fill" :style="{ width: (item.pct / maxSlotPct * 100) + '%' }"></div>
            </div>
            <span class="hbar-val">{{ item.pct }}%</span>
          </div>
        </div>
      </div>

      <!-- 使用类型分布 -->
      <div class="chart-card">
        <div class="chart-title">使用类型分布</div>
        <div class="chart-body">
          <div v-for="(item, i) in typeDist" :key="i" class="type-row">
            <div class="type-dot" :style="{ background: item.color }"></div>
            <span class="type-name">{{ item.label }}</span>
            <div class="type-track">
              <div class="type-fill" :style="{ width: item.pct + '%', background: item.color }"></div>
            </div>
            <span class="type-pct">{{ item.pct }}%</span>
            <span class="type-count">{{ item.count }}次</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { getDashboardStats, type DashboardStats } from '../../utils/dashboardApi'

const currentMonth = '2026年7月'
const dashboardData = ref<DashboardStats | null>(null)

async function loadData() {
  try { dashboardData.value = await getDashboardStats() } catch(e) { console.error(e) }
}
onMounted(() => loadData())

const overviewCards = computed(() => {
  const d = dashboardData.value
  const topLab = (d?.labUsageList ?? [])[0]
  return [
    { label: '本月预约', value: `${d?.monthReservationCount ?? 0}次`, trend: '', trendUp: true, sub: '', color: 'blue' },
    { label: '实验室总数', value: `${d?.labCount ?? 0}间`, trend: '', trendUp: true, sub: '', color: 'green' },
    { label: '最热门', value: topLab?.name?.slice(0,6) ?? '—', trend: topLab?.name ?? '', trendUp: true, sub: `${topLab?.count ?? 0}台设备`, color: 'blue' },
    { label: '部门数', value: `${d?.departmentCount ?? 0}个`, trend: '', trendUp: false, sub: '', color: 'orange' },
  ]
})

const usageRank = computed(() =>
  (dashboardData.value?.labUsageList ?? []).slice(0, 5).map((item, i) => ({
    name: item.name, rate: Math.round(item.count / Math.max(dashboardData.value?.totalDeviceCount ?? 1, 1) * 100),
    color: ['#27ae60','#2ecc71','#f39c12','#1a73e8','#8e44ad'][i]
  }))
)

const monthlyTrend = computed(() =>
  (dashboardData.value?.monthlyReservationTrend ?? []).map(m => ({
    month: m.month?.slice(5) ?? m.month, count: m.count
  }))
)
const minCount = computed(() => monthlyTrend.value.length ? Math.min(...monthlyTrend.value.map(m => m.count)) : 0)
const maxCount = computed(() => monthlyTrend.value.length ? Math.max(...monthlyTrend.value.map(m => m.count)) : 1)
function barHeightPct(count: number): number { return maxCount.value === minCount.value ? 100 : (count - minCount.value) / (maxCount.value - minCount.value) * 85 + 15 }

const timeSlotDist = [
  { slot: '08:00-10:00', pct: 0 }, { slot: '10:00-12:00', pct: 0 },
  { slot: '14:00-16:00', pct: 0 }, { slot: '16:00-18:00', pct: 0 },
  { slot: '18:00-20:00', pct: 0 },
]
const maxSlotPct = computed(() => Math.max(...timeSlotDist.map(t => t.pct), 1))

const typeDist = computed(() => {
  const total = dashboardData.value?.monthReservationCount ?? 1
  return [
    { label: '实验教学', pct: Math.round(60), count: Math.round(total * 0.6), color: '#27ae60' },
    { label: '科研项目', pct: Math.round(25), count: Math.round(total * 0.25), color: '#1a73e8' },
    { label: '考试测评', pct: Math.round(10), count: Math.round(total * 0.1), color: '#f39c12' },
    { label: '其他', pct: Math.round(5), count: Math.round(total * 0.05), color: '#bbb' },
  ]
})
</script>

<style scoped>
.ls-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:20px; flex-wrap:wrap }
.stat-card { flex:1; min-width:130px; background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:16px 18px }
.sc-top { display:flex; align-items:baseline; gap:8px; margin-bottom:4px }
.sc-num { font-size:22px; font-weight:700; color:#1a1a2e }
.sc-num.blue { color:#1a73e8 }
.sc-num.green { color:#27ae60 }
.sc-num.orange { color:#f39c12 }
.sc-trend { font-size:12px; font-weight:500 }
.sc-trend.up { color:#e74c3c }
.sc-trend.down { color:#27ae60 }
.sc-label { font-size:13px; color:#666; font-weight:500 }
.sc-sub { font-size:11px; color:#bbb; margin-top:2px }

.charts-grid { display:grid; grid-template-columns:1fr 1fr; gap:16px }
.chart-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.04); border-radius:14px; padding:18px 20px }
.chart-title { font-size:14px; font-weight:600; color:#555; margin-bottom:14px }

/* 利用率排行 */
.bar-row { display:flex; align-items:center; gap:8px; margin-bottom:12px }
.bar-rank { width:18px; font-size:12px; font-weight:700; color:#bbb; text-align:center }
.bar-name { width:110px; font-size:12px; color:#555; text-align:right; flex-shrink:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.bar-track { flex:1; height:10px; border-radius:5px; background:rgba(0,0,0,.05); overflow:hidden }
.bar-fill { height:100%; border-radius:5px; transition:width .5s; min-width:4px }
.bar-val { width:36px; font-size:12px; font-weight:600; color:#333; text-align:right }

/* 月度趋势 */
.chart-columns { display:flex; align-items:flex-end; justify-content:space-around; height:140px; padding-top:8px }
.col-item { display:flex; flex-direction:column; align-items:center; gap:6px; flex:1 }
.col-bar-wrap { flex:1; position:relative; width:100% }
.col-bar { position:absolute; bottom:0; left:50%; transform:translateX(-50%); width:100%; max-width:32px; border-radius:5px 5px 0 0; background:linear-gradient(to top,#e67e22,#f39c12); min-height:4px; transition:height .4s }
.col-val { font-size:10px; color:#888 }
.col-label { font-size:11px; color:#aaa }

/* 热门时段 */
.hbar-row { display:flex; align-items:center; gap:10px; margin-bottom:12px }
.hbar-name { width:90px; font-size:12px; color:#555; text-align:right; flex-shrink:0 }
.hbar-track { flex:1; height:16px; border-radius:8px; background:rgba(0,0,0,.04); overflow:hidden }
.hbar-fill { height:100%; border-radius:8px; background:linear-gradient(90deg,#1a73e8,#4a90d9); transition:width .5s; min-width:4px }
.hbar-val { width:36px; font-size:12px; font-weight:600; color:#1557b0 }

/* 类型分布 */
.type-row { display:flex; align-items:center; gap:8px; margin-bottom:14px }
.type-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0 }
.type-name { width:36px; font-size:12px; color:#555 }
.type-track { flex:1; height:10px; border-radius:5px; background:rgba(0,0,0,.05); overflow:hidden }
.type-fill { height:100%; border-radius:5px; transition:width .5s; min-width:4px }
.type-pct { width:36px; font-size:12px; font-weight:600; color:#333; text-align:right }
.type-count { width:32px; font-size:11px; color:#aaa; text-align:right }

@media (max-width: 1024px) {
  .charts-grid { grid-template-columns:1fr }
}
</style>
