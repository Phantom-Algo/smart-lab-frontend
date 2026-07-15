<template>
  <div class="ls-page">
    <header class="page-header">
      <div>
        <h2>使用统计</h2>
        <p>预约申请、热门实验室与使用时段分析</p>
      </div>
      <button class="text-btn" type="button" :disabled="loading" @click="loadData">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 1 1-2.64-6.36" />
          <path d="M21 3v6h-6" />
        </svg>
        刷新
      </button>
    </header>

    <section class="overview-grid">
      <article v-for="card in summaryCards" :key="card.key" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong :class="card.className">{{ card.value }}</strong>
        <small>{{ card.hint }}</small>
      </article>

      <article class="stat-card hot-card">
        <div class="hot-head">
          <span class="stat-label">本月热门实验室 Top 5</span>
          <small>{{ monthReservationCount }} 次申请</small>
        </div>
        <div v-if="hotLabs.length > 0" class="hot-list">
          <div v-for="(lab, index) in hotLabs" :key="lab.labId" class="hot-item">
            <span class="hot-rank">{{ index + 1 }}</span>
            <div class="hot-main">
              <strong>{{ lab.labName }}</strong>
              <span>{{ lab.labTypeName || '-' }} · {{ formatLocation(lab) }}</span>
            </div>
            <span class="hot-count">{{ lab.reservationCount }}次</span>
          </div>
        </div>
        <div v-else class="empty-mini">{{ loading ? '加载中...' : '暂无预约数据' }}</div>
      </article>
    </section>

    <div v-if="loadError" class="state-box error">{{ loadError }}</div>

    <section v-else class="charts-grid">
      <article class="chart-card trend-card">
        <div class="chart-title">
          <span>近一年预约趋势</span>
          <em>按申请创建时间统计</em>
        </div>
        <div class="year-trend-panel" role="img" :aria-label="trendAriaLabel">
          <div class="year-trend-head">
            <div class="year-trend-metric">
              <span>预约次数</span>
              <strong>{{ yearlyReservationTotal }}次</strong>
            </div>
            <span class="year-trend-range">{{ trendRangeLabel }}</span>
          </div>

          <div class="year-trend-plot">
            <span class="axis-label axis-max">{{ chartMaxValue }}</span>
            <span class="axis-label axis-zero">0</span>
            <div class="plot-area">
              <span class="grid-line grid-top"></span>
              <span class="grid-line grid-baseline"></span>
              <div class="trend-bars">
                <div
                  v-for="item in monthlyTrend"
                  :key="item.month"
                  class="trend-bar-cell"
                  :title="`${formatFullMonth(item.month)}：${item.count}次`"
                >
                  <span
                    class="trend-bar"
                    :class="{ empty: item.count <= 0 }"
                    :style="{ height: barHeight(item.count, chartMaxValue) + '%' }"
                  ></span>
                </div>
              </div>
            </div>
            <span class="date-label date-start">{{ trendStartLabel }}</span>
            <span class="date-label date-end">{{ trendEndLabel }}</span>
          </div>
        </div>
      </article>

      <article class="chart-card">
        <div class="chart-title">
          <span>本月热门时段</span>
          <em>按预约开始时间分布</em>
        </div>
        <div class="bar-list">
          <div v-for="item in timeSlotDistribution" :key="item.slot" class="bar-row">
            <span class="bar-name">{{ item.slot }}</span>
            <div class="bar-track">
              <div class="bar-fill blue" :style="{ width: barWidth(item.count, maxTimeSlotCount) + '%' }"></div>
            </div>
            <span class="bar-value">{{ item.count }}次</span>
            <span class="bar-percent">{{ item.percent }}%</span>
          </div>
        </div>
      </article>

      <article class="chart-card">
        <div class="chart-title">
          <span>实验室类型分布</span>
          <em>本月预约申请</em>
        </div>
        <div v-if="labTypeDistribution.length > 0" class="bar-list">
          <div v-for="item in labTypeDistribution" :key="item.labTypeName" class="bar-row">
            <span class="bar-name">{{ item.labTypeName }}</span>
            <div class="bar-track">
              <div class="bar-fill green" :style="{ width: barWidth(item.count, maxLabTypeCount) + '%' }"></div>
            </div>
            <span class="bar-value">{{ item.count }}次</span>
            <span class="bar-percent">{{ item.percent }}%</span>
          </div>
        </div>
        <div v-else class="state-box">暂无类型分布数据</div>
      </article>

      <article class="chart-card">
        <div class="chart-title">
          <span>预约状态分布</span>
          <em>本月预约申请</em>
        </div>
        <div class="status-grid">
          <div v-for="item in statusDistribution" :key="item.status" class="status-item">
            <span class="status-badge" :class="statusClass(item.status)">{{ item.statusName }}</span>
            <strong>{{ item.count }}</strong>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  getLabUsageStats,
  type LabUsageHotLab,
  type LabUsageMonthlyTrend,
  type LabUsageStats,
} from '../../utils/labApi'

const MONTHLY_TREND_MONTHS = 12

const usageStats = ref<LabUsageStats | null>(null)
const loading = ref(false)
const loadError = ref('')

async function loadData(): Promise<void> {
  loading.value = true
  loadError.value = ''
  try {
    usageStats.value = await getLabUsageStats()
  } catch (error: any) {
    usageStats.value = null
    loadError.value = error?.message || '加载使用统计失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadData()
})

const todayReservationCount = computed(() => usageStats.value?.todayReservationCount ?? 0)
const weekReservationCount = computed(() => usageStats.value?.weekReservationCount ?? 0)
const monthReservationCount = computed(() => usageStats.value?.monthReservationCount ?? 0)
const hotLabs = computed(() => usageStats.value?.hotLabs ?? [])
const rawMonthlyTrend = computed(() => usageStats.value?.monthlyTrend ?? [])
const monthlyTrend = computed(() => normalizeMonthlyTrend(rawMonthlyTrend.value))
const timeSlotDistribution = computed(() => usageStats.value?.timeSlotDistribution ?? [])
const labTypeDistribution = computed(() => usageStats.value?.labTypeDistribution ?? [])
const statusDistribution = computed(() => usageStats.value?.statusDistribution ?? [])

const summaryCards = computed(() => [
  { key: 'today', label: '今日预约数', value: todayReservationCount.value, hint: '今日提交的预约申请', className: 'stat-blue' },
  { key: 'week', label: '本周预约数', value: weekReservationCount.value, hint: '本周提交的预约申请', className: 'stat-green' },
  { key: 'month', label: '本月预约数', value: monthReservationCount.value, hint: '本月提交的预约申请', className: 'stat-amber' },
])

const maxMonthlyCount = computed(() => maxCount(monthlyTrend.value.map(item => item.count)))
const chartMaxValue = computed(() => getNiceChartMax(maxMonthlyCount.value))
const maxTimeSlotCount = computed(() => maxCount(timeSlotDistribution.value.map(item => item.count)))
const maxLabTypeCount = computed(() => maxCount(labTypeDistribution.value.map(item => item.count)))
const yearlyReservationTotal = computed(() => monthlyTrend.value.reduce((sum, item) => sum + item.count, 0))
const trendStartLabel = computed(() => formatMonthEndpoint(monthlyTrend.value[0]?.month))
const trendEndLabel = computed(() => formatMonthEndpoint(monthlyTrend.value[monthlyTrend.value.length - 1]?.month))
const trendRangeLabel = computed(() => `${trendStartLabel.value} - ${trendEndLabel.value}`)
const trendAriaLabel = computed(() => `${trendRangeLabel.value}预约趋势，共${yearlyReservationTotal.value}次`)

function maxCount(values: number[]): number {
  return Math.max(...values, 1)
}

function barHeight(count: number, max: number): number {
  if (count <= 0) return 0
  return Math.max(4, Math.round(count / max * 100))
}

function barWidth(count: number, max: number): number {
  if (count <= 0) return 0
  return Math.max(8, Math.round(count / max * 100))
}

function formatLocation(lab: Pick<LabUsageHotLab, 'campus' | 'building' | 'floor' | 'roomNo'>): string {
  return `${lab.campus || '-'}-${lab.building || '-'}-${lab.floor ?? '-'}F-${lab.roomNo || '-'}`
}

function normalizeMonthlyTrend(items: LabUsageMonthlyTrend[]): LabUsageMonthlyTrend[] {
  const countByMonth = new Map(items.map(item => [item.month, Number(item.count) || 0]))
  return recentMonthKeys(MONTHLY_TREND_MONTHS).map(month => ({
    month,
    count: countByMonth.get(month) ?? 0,
  }))
}

function recentMonthKeys(monthCount: number): string[] {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth() - monthCount + 1, 1)
  return Array.from({ length: monthCount }, (_, index) => {
    const monthDate = new Date(start.getFullYear(), start.getMonth() + index, 1)
    const month = String(monthDate.getMonth() + 1).padStart(2, '0')
    return `${monthDate.getFullYear()}-${month}`
  })
}

function getNiceChartMax(max: number): number {
  if (max <= 5) return 5
  if (max <= 10) return 10
  if (max <= 20) return Math.ceil(max / 5) * 5
  if (max <= 100) return Math.ceil(max / 10) * 10
  return Math.ceil(max / 50) * 50
}

function formatFullMonth(month: string): string {
  const matched = month.match(/^(\d{4})-(\d{2})$/)
  if (!matched) return month
  return `${matched[1]}年${Number(matched[2])}月`
}

function formatMonthEndpoint(month = ''): string {
  const matched = month.match(/^(\d{4})-(\d{2})$/)
  if (!matched) return month
  return `${matched[1]}/${Number(matched[2])}`
}

function statusClass(status: number): string {
  if (status === 1) return 'badge-pending'
  if (status === 2 || status === 5) return 'badge-approved'
  if (status === 3) return 'badge-rejected'
  if (status === 4) return 'badge-cancelled'
  return 'badge-absent'
}
</script>

<style scoped>
.ls-page {
  height: 100%;
  overflow-y: auto;
  padding: 22px 24px 28px;
  color: #172033;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
}

.page-header p {
  margin: 5px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.text-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 10px;
  border: 0;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.text-btn:disabled {
  color: #9aa3b2;
  cursor: not-allowed;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(140px, 1fr)) minmax(360px, 2fr);
  gap: 10px;
  margin-bottom: 14px;
}

.stat-card,
.chart-card {
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.stat-card {
  padding: 14px 14px 13px;
}

.stat-card strong {
  display: block;
  margin-top: 7px;
  font-size: 24px;
  line-height: 1;
  color: #111827;
}

.stat-card small,
.chart-title em,
.hot-main span {
  color: #7b8495;
  font-size: 12px;
  font-style: normal;
}

.stat-label {
  font-size: 12px;
  color: #697386;
}

.stat-blue {
  color: #2563eb !important;
}

.stat-green {
  color: #137a43 !important;
}

.stat-amber {
  color: #9a5b00 !important;
}

.hot-card {
  min-width: 0;
}

.hot-head,
.chart-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.hot-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.hot-rank {
  width: 22px;
  height: 22px;
  border-radius: 5px;
  background: #f2f4f7;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
  line-height: 22px;
  text-align: center;
}

.hot-main {
  min-width: 0;
}

.hot-main strong {
  display: block;
  margin: 0 0 2px;
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-main span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-count {
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.chart-card {
  min-width: 0;
  padding: 16px;
}

.trend-card {
  grid-column: 1 / -1;
}

.chart-title {
  margin-bottom: 14px;
}

.chart-title span {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.year-trend-panel {
  min-height: 304px;
  padding: 24px 28px 22px;
  border-radius: 8px;
  background: #f4f5f7;
}

.year-trend-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.year-trend-metric {
  display: flex;
  align-items: baseline;
  min-width: 0;
  gap: 12px;
}

.year-trend-metric span {
  color: #111827;
  font-size: 19px;
  font-weight: 800;
  line-height: 1.2;
}

.year-trend-metric strong {
  color: #667085;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.year-trend-range {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: #fff;
  color: #4b5563;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
}

.year-trend-plot {
  position: relative;
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr);
  grid-template-rows: minmax(190px, 1fr) 28px;
  min-height: 230px;
}

.axis-label,
.date-label {
  color: #667085;
  font-weight: 700;
  white-space: nowrap;
}

.axis-label {
  position: absolute;
  left: 0;
  font-size: 15px;
  line-height: 1;
}

.axis-max {
  top: 12px;
}

.axis-zero {
  bottom: 29px;
}

.plot-area {
  position: relative;
  grid-column: 2;
  grid-row: 1;
  min-width: 0;
}

.grid-line {
  position: absolute;
  left: 0;
  right: 0;
  z-index: 1;
  height: 1px;
  background: #cbd5e1;
  pointer-events: none;
}

.grid-top {
  top: 12px;
}

.grid-baseline {
  bottom: 0;
}

.trend-bars {
  position: absolute;
  top: 12px;
  right: 0;
  bottom: 1px;
  left: 0;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(12, minmax(10px, 1fr));
  align-items: end;
  gap: clamp(8px, 2.6vw, 30px);
  padding: 0 8px;
}

.trend-bar-cell {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 100%;
  min-width: 0;
}

.trend-bar {
  display: block;
  width: clamp(14px, 2.4vw, 26px);
  max-width: 26px;
  border-radius: 7px 7px 0 0;
  background: #2563eb;
  box-shadow: 0 5px 10px rgba(37, 99, 235, 0.2);
  transition: height 0.18s ease;
}

.trend-bar.empty {
  box-shadow: none;
}

.date-label {
  grid-column: 2;
  grid-row: 2;
  align-self: end;
  font-size: 15px;
  line-height: 1;
}

.date-start {
  justify-self: start;
}

.date-end {
  justify-self: end;
}

.bar-value,
.bar-percent {
  font-size: 12px;
  white-space: nowrap;
}

.bar-value {
  color: #111827;
  font-weight: 600;
}

.bar-percent {
  color: #7b8495;
}

.bar-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.bar-row {
  display: grid;
  grid-template-columns: 92px minmax(80px, 1fr) 44px 38px;
  align-items: center;
  gap: 10px;
}

.bar-name {
  overflow: hidden;
  color: #344054;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: #eef1f5;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
}

.bar-fill.blue {
  background: #2563eb;
}

.bar-fill.green {
  background: #137a43;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(110px, 1fr));
  gap: 10px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 0 10px;
  border: 1px solid #eef1f5;
  border-radius: 7px;
}

.status-item strong {
  color: #111827;
  font-size: 20px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  height: 24px;
  padding: 0 8px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-pending {
  background: #e8f0ff;
  color: #1d4ed8;
}

.badge-approved {
  background: #e8f7ef;
  color: #137a43;
}

.badge-rejected {
  background: #feeceb;
  color: #b42318;
}

.badge-cancelled {
  background: #f2f4f7;
  color: #667085;
}

.badge-absent {
  background: #fff7df;
  color: #9a5b00;
}

.state-box,
.empty-mini {
  padding: 22px 14px;
  color: #7b8495;
  font-size: 13px;
  text-align: center;
}

.state-box {
  border: 1px solid #e6e9ef;
  border-radius: 8px;
  background: #fff;
}

.state-box.error {
  color: #b42318;
  background: #fff7f7;
  border-color: #ffd5d2;
}

@media (max-width: 1180px) {
  .overview-grid,
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .ls-page {
    padding: 18px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .year-trend-panel {
    min-height: 272px;
    padding: 18px 16px 18px;
  }

  .year-trend-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;
  }

  .year-trend-metric {
    width: 100%;
    justify-content: space-between;
    gap: 10px;
  }

  .year-trend-metric span,
  .year-trend-metric strong {
    font-size: 16px;
  }

  .year-trend-plot {
    grid-template-columns: 38px minmax(0, 1fr);
    grid-template-rows: minmax(160px, 1fr) 24px;
    min-height: 196px;
  }

  .axis-label,
  .date-label {
    font-size: 12px;
  }

  .axis-zero {
    bottom: 25px;
  }

  .trend-bars {
    gap: 5px;
    padding: 0 3px;
  }

  .trend-bar {
    width: clamp(8px, 2.6vw, 14px);
  }

  .bar-row {
    grid-template-columns: 86px minmax(64px, 1fr) 42px;
  }

  .bar-percent {
    display: none;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
