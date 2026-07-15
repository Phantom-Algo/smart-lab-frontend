<template>
  <div class="im-page">
    <div class="page-header">
      <div>
        <h2>物联网环境</h2>
        <p>传感器实时监测 / 趋势预警</p>
      </div>
      <div class="header-right">
        <span class="update-time">最后更新时间：{{ formatDateTime(overview?.updatedAt) }}</span>
        <button class="refresh-btn" type="button" :class="{ spinning: refreshing }" @click="manualRefresh">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <path d="M21 3v6h-6" />
          </svg>
          刷新
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

    <div class="monitor-layout">
      <aside class="filter-panel">
        <div class="filter-title">
          <span>筛选条件</span>
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 15l-6-6-6 6" />
          </svg>
        </div>

        <label class="field-label">校区</label>
        <select v-model="filterForm.campus" class="field-control">
          <option value="">全部校区</option>
          <option v-for="campus in filterOptions.campuses" :key="campus" :value="campus">{{ campus }}</option>
        </select>

        <label class="field-label">楼栋</label>
        <select v-model="filterForm.building" class="field-control">
          <option value="">全部楼栋</option>
          <option v-for="building in buildingOptions" :key="building" :value="building">{{ building }}</option>
        </select>

        <label class="field-label">实验室类型</label>
        <select v-model="filterForm.labTypeId" class="field-control">
          <option value="">全部类型</option>
          <option v-for="type in labTypes" :key="type.id" :value="type.id">{{ type.labTypeName }}</option>
        </select>

        <label class="field-label">实验室名称</label>
        <div class="search-row">
          <input
            v-model="filterForm.labName"
            class="field-control search-input"
            placeholder="搜索实验室名称"
            @keyup.enter="applyFilters"
          />
          <button class="filter-btn" type="button" title="筛选" @click="applyFilters">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 5h18" />
              <path d="M6 12h12" />
              <path d="M10 19h4" />
            </svg>
          </button>
        </div>

        <div class="lab-list-head">
          <span>实验室列表</span>
          <strong>{{ labs.length }}</strong>
        </div>
        <div class="lab-list">
          <button
            v-for="lab in labs"
            :key="lab.labId"
            class="lab-list-item"
            :class="{ active: selectedLabId === lab.labId }"
            type="button"
            @click="selectedLabId = lab.labId"
          >
            <span class="status-dot" :class="statusDotClass(lab.monitorStatus)"></span>
            <span class="lab-list-text">
              <strong>{{ labLocationTitle(lab) }}</strong>
              <em>{{ lab.labName }}</em>
            </span>
          </button>
          <div v-if="!loading && labs.length === 0" class="empty-list">暂无实验室</div>
        </div>
      </aside>

      <main class="monitor-main">
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon blue">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 21V7a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14" />
                <path d="M9 21v-6h6v6" />
                <path d="M8 9h1M12 9h1M16 9h1M8 13h1M16 13h1" />
              </svg>
            </div>
            <div>
              <span class="stat-label">在线监测实验室</span>
              <strong class="stat-value">{{ overview?.onlineMonitorLabs ?? 0 }} <small>/ {{ overview?.totalLabs ?? 0 }}</small></strong>
              <em class="stat-sub">绑定传感器可在线监测</em>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon green">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                <path d="M9 12l2 2 4-4" />
              </svg>
            </div>
            <div>
              <span class="stat-label">正常</span>
              <strong class="stat-value">{{ overview?.normalLabs ?? 0 }}</strong>
              <em class="stat-sub">{{ normalRateText }}</em>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon red">
              <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
                <path d="M12 9v4" />
                <path d="M12 17h.01" />
              </svg>
            </div>
            <div>
              <span class="stat-label">异常</span>
              <strong class="stat-value">{{ overview?.alertLabs ?? 0 }}</strong>
              <em class="stat-sub">{{ alertRateText }}</em>
            </div>
          </div>
          <div class="stat-card online-card">
            <span class="stat-label">设备在线率</span>
            <strong class="online-rate">{{ formatPercent(overview?.sensorOnlineRate ?? 0) }}</strong>
            <div class="rate-bar">
              <span :style="{ width: Math.min(100, overview?.sensorOnlineRate ?? 0) + '%' }"></span>
            </div>
            <em class="stat-sub">在线设备 {{ overview?.onlineSensors ?? 0 }} / {{ overview?.totalSensors ?? 0 }}</em>
          </div>
        </div>

        <div v-if="loading" class="loading-state">加载监测数据...</div>
        <div v-else-if="labs.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" width="46" height="46" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="9" />
            <path d="M8 12h8M12 8v8" />
          </svg>
          <p>当前筛选条件下暂无监测实验室</p>
        </div>

        <template v-else>
          <div class="chart-legend-panel" aria-label="折线颜色说明">
            <span class="legend-item">
              <i class="legend-line safe"></i>
              绿色：处于任一安全阈值范围内
            </span>
            <span class="legend-item">
              <i class="legend-line danger"></i>
              红色：超出全部安全阈值范围
            </span>
            <span class="legend-item">
              <i class="legend-line gap"></i>
              灰色虚线：相邻数据间隔超过 5 分钟
            </span>
          </div>

          <div class="lab-card-grid">
            <article
              v-for="lab in labs"
              :key="lab.labId"
              class="lab-card"
              :class="cardStatusClass(lab.monitorStatus)"
            >
            <div class="lab-card-head">
              <div class="lab-card-title">
                <span class="status-dot" :class="statusDotClass(lab.monitorStatus)"></span>
                <h3>{{ lab.labName }}</h3>
                <span class="status-badge" :class="badgeStatusClass(lab.monitorStatus)">
                  {{ lab.monitorStatusDesc }}
                </span>
              </div>
              <button class="detail-btn" type="button" @click="openDetail(lab)">
                详情
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 18l6-6-6-6" />
                </svg>
              </button>
            </div>

            <div class="indicator-list">
              <div v-for="indicator in lab.indicators" :key="indicator.indicatorId" class="indicator-row">
                <div class="indicator-meta">
                  <span class="indicator-icon" :class="iconKind(indicator.dataName)">
                    <svg v-if="iconKind(indicator.dataName) === 'temperature'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 14.76V4a2 2 0 1 0-4 0v10.76a4 4 0 1 0 4 0z" />
                    </svg>
                    <svg v-else-if="iconKind(indicator.dataName) === 'humidity'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 2.7l5.3 5.3a7.5 7.5 0 1 1-10.6 0L12 2.7z" />
                    </svg>
                    <svg v-else-if="iconKind(indicator.dataName) === 'smoke'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M5 16c2 0 2-2 4-2s2 2 4 2 2-2 4-2" />
                      <path d="M7 11c1.5 0 1.5-1.5 3-1.5s1.5 1.5 3 1.5 1.5-1.5 3-1.5" />
                      <path d="M10 6c1 0 1-1 2-1s1 1 2 1" />
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M4 14a8 8 0 0 1 16 0" />
                      <path d="M7 14h10" />
                      <path d="M9 18h6" />
                    </svg>
                  </span>
                  <span class="indicator-name">{{ displayIndicatorName(indicator) }}</span>
                </div>
                <div class="indicator-value">
                  <strong>{{ formatMetricValue(indicator.latestValue, indicator.unit) }}</strong>
                  <em>{{ formatSafeRange(indicator.safeRange, indicator.unit) }}</em>
                </div>
                <div class="mini-chart-wrap">
                  <svg class="mini-chart" :viewBox="chartViewBox" preserveAspectRatio="none">
                    <rect
                      v-for="(band, index) in safeBands(indicator)"
                      :key="'band-' + index"
                      :x="0"
                      :y="band.y"
                      :width="chartWidth"
                      :height="band.height"
                      class="safe-band"
                    />
                    <line
                      v-for="(line, index) in boundaryLines(indicator)"
                      :key="'line-' + index"
                      :x1="0"
                      :x2="chartWidth"
                      :y1="line.y"
                      :y2="line.y"
                      class="boundary-line"
                    />
                    <line
                      v-for="segment in chartSegments(indicator)"
                      :key="segment.key"
                      :x1="segment.x1"
                      :y1="segment.y1"
                      :x2="segment.x2"
                      :y2="segment.y2"
                      :class="['chart-line-segment', segment.tone]"
                    />
                    <circle
                      v-for="point in chartPoints(indicator)"
                      :key="point.key"
                      :cx="point.x"
                      :cy="point.y"
                      r="2"
                      :class="['chart-point', point.tone]"
                    />
                  </svg>
                  <span class="chart-time">{{ formatTime(indicator.detectedAt) }}</span>
                </div>
              </div>
              <div v-if="lab.indicators.length === 0" class="card-empty">该实验室类型暂未配置安全指标</div>
            </div>
            </article>
          </div>
        </template>
      </main>
    </div>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDetail" class="modal-overlay" @click.self="closeDetail">
          <div class="detail-modal">
            <div class="detail-head">
              <div>
                <h3>{{ detailLab?.labName }}</h3>
                <p>{{ detailLab ? labLocationTitle(detailLab) : '' }} · {{ detailLab?.labTypeName }}</p>
              </div>
              <button class="modal-close" type="button" @click="closeDetail">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div class="detail-body" v-if="detailLab">
              <div class="indicator-tabs">
                <button
                  v-for="indicator in detailLab.indicators"
                  :key="indicator.indicatorId"
                  type="button"
                  :class="{ active: selectedDetailDataName === indicator.dataName }"
                  @click="selectDetailIndicator(indicator.dataName)"
                >
                  {{ displayIndicatorName(indicator) }}
                </button>
              </div>

              <div v-if="selectedDetailIndicator" class="detail-chart-panel">
                <div class="detail-chart-top">
                  <div class="detail-chart-meta">
                    <strong>{{ displayIndicatorName(selectedDetailIndicator) }}趋势</strong>
                    <span>当前 {{ formatMetricValue(selectedDetailIndicator.latestValue, selectedDetailIndicator.unit) }} · 正常范围 {{ formatSafeRange(selectedDetailIndicator.safeRange, selectedDetailIndicator.unit) }}</span>
                  </div>
                  <div class="chart-legend compact" aria-label="折线颜色说明">
                    <span class="legend-item"><i class="legend-line safe"></i>安全范围内</span>
                    <span class="legend-item"><i class="legend-line danger"></i>超出阈值</span>
                    <span class="legend-item"><i class="legend-line gap"></i>数据中断</span>
                  </div>
                </div>
                <div ref="detailChartRef" class="detail-chart"></div>
              </div>

              <div class="log-table-wrap">
                <table class="log-table">
                  <thead>
                    <tr>
                      <th>采集时间</th>
                      <th>指标</th>
                      <th>值</th>
                      <th>安全范围</th>
                      <th>状态</th>
                      <th>传感器</th>
                      <th>事件号</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="log in detailLogs" :key="log.id">
                      <td>{{ formatDateTime(log.detectedAt) }}</td>
                      <td>{{ displayDataName(log.dataName) }}</td>
                      <td>{{ formatMetricValue(log.value, log.unit) }}</td>
                      <td>{{ formatSafeRange(log.safeRange, log.unit) }}</td>
                      <td>
                        <span class="log-status" :class="log.status === 2 ? 'danger' : 'normal'">
                          {{ log.statusDesc }}
                        </span>
                      </td>
                      <td>{{ log.sensorNo || '-' }}</td>
                      <td class="event-cell">{{ log.eventNo }}</td>
                    </tr>
                    <tr v-if="!detailLoading && detailLogs.length === 0">
                      <td colspan="7" class="empty-cell">暂无日志</td>
                    </tr>
                    <tr v-if="detailLoading">
                      <td colspan="7" class="empty-cell">加载中...</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="pagination">
                <button type="button" :disabled="detailPage <= 1 || detailLoading" @click="changeDetailPage(detailPage - 1)">上一页</button>
                <span>{{ detailPage }} / {{ detailTotalPages }}</span>
                <button type="button" :disabled="detailPage >= detailTotalPages || detailLoading" @click="changeDetailPage(detailPage + 1)">下一页</button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { CustomChart, LineChart, ScatterChart } from 'echarts/charts'
import {
  GraphicComponent,
  GridComponent,
  MarkAreaComponent,
  MarkLineComponent,
  TooltipComponent,
} from 'echarts/components'
import { graphic, init, use } from 'echarts/core'
import type {
  CustomSeriesRenderItemAPI,
  CustomSeriesRenderItemParams,
  EChartsOption,
  EChartsType,
} from 'echarts'
import { CanvasRenderer } from 'echarts/renderers'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  getIotMonitorLogs,
  getIotMonitorOverview,
  getLabFilterOptions,
  getMonitorLabTypes,
  type IotLabMonitorStatus,
  type IotMonitorIndicator,
  type IotMonitorLabCard,
  type IotMonitorLog,
  type IotMonitorOverview,
  type LabFilterOptions,
  type LabTypeOption,
} from '../../utils/iotMonitorApi'

use([
  CanvasRenderer,
  CustomChart,
  LineChart,
  ScatterChart,
  GridComponent,
  TooltipComponent,
  MarkAreaComponent,
  MarkLineComponent,
  GraphicComponent,
])

interface FilterState {
  campus: string
  building: string
  labTypeId: number | ''
  labName: string
}

interface ChartPoint {
  key: string
  x: number
  y: number
  value: number
  detectedAt: string | null
  status: number
  tone: ChartTone
}

type ChartTone = 'safe' | 'danger' | 'gap'

interface ChartSegment {
  key: string
  x1: number
  y1: number
  x2: number
  y2: number
  tone: ChartTone
}

interface DetailChartPoint {
  key: string
  time: number
  value: number
  detectedAt: string
  status: number
  tone: ChartTone
}

interface CompressedDetailPoint extends DetailChartPoint {
  displayX: number
  forceDateLabel: boolean
}

interface CompressedGap {
  key: string
  labelX: number
  startTime: number
  endTime: number
  durationMs: number
  crossedDay: boolean
}

interface DetailChartSegment {
  key: string
  startX: number
  startValue: number
  endX: number
  endValue: number
  tone: ChartTone
}

interface CompressedDetailAxis {
  points: CompressedDetailPoint[]
  segments: DetailChartSegment[]
  gaps: CompressedGap[]
  axisLabels: Map<number, string>
  maxDisplayX: number
}

interface SafeBand {
  y: number
  height: number
}

interface BoundaryLine {
  y: number
}

interface NormalizedSafeRange {
  lower: number
  upper: number
}

const chartWidth = 260
const chartHeight = 48
const chartPadding = 4
const chartViewBox = `0 0 ${chartWidth} ${chartHeight}`
const dataGapThresholdMs = 15 * 1000
const thresholdEpsilon = 1e-7
const safeLineColor = '#1fa85f'
const dangerLineColor = '#ef4444'
const gapLineColor = '#9aa6bc'
const safeAreaColor = 'rgba(31, 168, 95, .12)'
const thresholdLineColor = 'rgba(31, 168, 95, .45)'
const compressedGapSlotWidth = 3

// 轮询间隔时间，单位为毫秒 
const pollIntervalMs = 3000

const filterOptions = reactive<LabFilterOptions>({
  campuses: [],
  buildings: [],
  buildingByCampus: {},
})
const labTypes = ref<LabTypeOption[]>([])
const filterForm = reactive<FilterState>({
  campus: '',
  building: '',
  labTypeId: '',
  labName: '',
})
const activeFilters = reactive<FilterState>({
  campus: '',
  building: '',
  labTypeId: '',
  labName: '',
})

const overview = ref<IotMonitorOverview | null>(null)
const loading = ref(false)
const refreshing = ref(false)
const errorMessage = ref('')
const selectedLabId = ref<number | null>(null)
let pollTimer: number | undefined
let requestSeq = 0

const showDetail = ref(false)
const detailLab = ref<IotMonitorLabCard | null>(null)
const selectedDetailDataName = ref('')
const detailLogs = ref<IotMonitorLog[]>([])
const detailPage = ref(1)
const detailPageSize = 8
const detailTotal = ref(0)
const detailLoading = ref(false)
const detailChartRef = ref<HTMLDivElement | null>(null)
let detailChart: EChartsType | null = null
let detailChartResizeObserver: ResizeObserver | null = null

const labs = computed(() => overview.value?.labs ?? [])

const buildingOptions = computed(() => {
  if (!filterForm.campus) return filterOptions.buildings
  return filterOptions.buildingByCampus[filterForm.campus] ?? []
})

const normalRateText = computed(() => {
  const total = overview.value?.totalLabs ?? 0
  if (!total) return '0%'
  return `${Math.round(((overview.value?.normalLabs ?? 0) / total) * 100)}%`
})

const alertRateText = computed(() => {
  const total = overview.value?.totalLabs ?? 0
  if (!total) return '0%'
  return `${Math.round(((overview.value?.alertLabs ?? 0) / total) * 100)}%`
})

const selectedDetailIndicator = computed(() => {
  if (!detailLab.value) return null
  return detailLab.value.indicators.find(item => item.dataName === selectedDetailDataName.value) ?? null
})

const detailTotalPages = computed(() => Math.max(1, Math.ceil(detailTotal.value / detailPageSize)))

watch(() => filterForm.campus, () => {
  if (filterForm.building && !buildingOptions.value.includes(filterForm.building)) {
    filterForm.building = ''
  }
})

watch(selectedDetailIndicator, () => {
  if (showDetail.value) {
    void renderDetailChart()
  }
}, { deep: true })

watch(showDetail, visible => {
  if (visible) {
    void renderDetailChart()
  } else {
    disposeDetailChart()
  }
})

onMounted(async () => {
  await Promise.all([loadFilterOptions(), loadLabTypes()])
  await refreshOverview()
  pollTimer = window.setInterval(() => {
    void refreshOverview(true)
  }, pollIntervalMs)
})

onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
  disposeDetailChart()
})

async function loadFilterOptions() {
  try {
    const options = await getLabFilterOptions()
    filterOptions.campuses = options.campuses ?? []
    filterOptions.buildings = options.buildings ?? []
    filterOptions.buildingByCampus = options.buildingByCampus ?? {}
  } catch (error: any) {
    errorMessage.value = error?.message || '加载筛选条件失败'
  }
}

async function loadLabTypes() {
  try {
    labTypes.value = await getMonitorLabTypes()
  } catch (error: any) {
    errorMessage.value = error?.message || '加载实验室类型失败'
  }
}

function applyFilters() {
  activeFilters.campus = filterForm.campus
  activeFilters.building = filterForm.building
  activeFilters.labTypeId = filterForm.labTypeId
  activeFilters.labName = filterForm.labName.trim()
  void refreshOverview()
}

function manualRefresh() {
  void Promise.all([loadFilterOptions(), refreshOverview()])
}

async function refreshOverview(silent = false) {
  const seq = ++requestSeq
  if (silent) refreshing.value = true
  else loading.value = true
  try {
    const data = await getIotMonitorOverview(buildOverviewQuery())
    if (seq !== requestSeq) return
    overview.value = data
    errorMessage.value = ''
    if (selectedLabId.value == null && data.labs.length > 0) {
      selectedLabId.value = data.labs[0].labId
    }
    if (detailLab.value) {
      const updatedDetailLab = data.labs.find(lab => lab.labId === detailLab.value?.labId)
      if (updatedDetailLab) detailLab.value = updatedDetailLab
    }
  } catch (error: any) {
    errorMessage.value = error?.message || '加载监测数据失败'
  } finally {
    if (seq === requestSeq) {
      loading.value = false
      refreshing.value = false
    }
  }
}

function buildOverviewQuery() {
  const query: Record<string, string | number> = { historyLimit: 50 }
  if (activeFilters.campus) query.campus = activeFilters.campus
  if (activeFilters.building) query.building = activeFilters.building
  if (activeFilters.labTypeId !== '') query.labTypeId = activeFilters.labTypeId
  if (activeFilters.labName) query.labName = activeFilters.labName
  return query
}

async function openDetail(lab: IotMonitorLabCard) {
  detailLab.value = lab
  selectedDetailDataName.value = lab.indicators[0]?.dataName ?? ''
  detailPage.value = 1
  showDetail.value = true
  await renderDetailChart()
  await loadDetailLogs()
}

function closeDetail() {
  showDetail.value = false
  detailLab.value = null
  detailLogs.value = []
}

async function selectDetailIndicator(dataName: string) {
  selectedDetailDataName.value = dataName
  detailPage.value = 1
  await renderDetailChart()
  await loadDetailLogs()
}

async function changeDetailPage(page: number) {
  detailPage.value = Math.max(1, Math.min(page, detailTotalPages.value))
  await loadDetailLogs()
}

async function loadDetailLogs() {
  if (!detailLab.value) return
  detailLoading.value = true
  try {
    const page = await getIotMonitorLogs(detailLab.value.labId, {
      dataName: selectedDetailDataName.value || undefined,
      page: detailPage.value,
      pageSize: detailPageSize,
    })
    detailLogs.value = page.records ?? []
    detailTotal.value = page.total ?? 0
  } catch (error: any) {
    errorMessage.value = error?.message || '加载检测日志失败'
  } finally {
    detailLoading.value = false
  }
}

async function renderDetailChart() {
  await nextTick()
  const indicator = selectedDetailIndicator.value
  const el = detailChartRef.value
  if (!showDetail.value || !indicator || !el) return

  const chart = ensureDetailChart(el)
  chart.setOption(buildDetailChartOption(indicator), true)
  chart.resize()
}

function ensureDetailChart(el: HTMLDivElement) {
  if (detailChart && detailChart.getDom() === el) return detailChart
  disposeDetailChart()
  detailChart = init(el, undefined, { renderer: 'canvas' })
  detailChartResizeObserver = new ResizeObserver(() => {
    detailChart?.resize()
  })
  detailChartResizeObserver.observe(el)
  return detailChart
}

function disposeDetailChart() {
  detailChartResizeObserver?.disconnect()
  detailChartResizeObserver = null
  detailChart?.dispose()
  detailChart = null
}

function buildDetailChartOption(indicator: IotMonitorIndicator): EChartsOption {
  const points = detailChartPoints(indicator)
  const compressedAxis = buildCompressedDetailAxis(points, indicator)
  const domain = chartDomain(indicator)
  const unit = indicator.unit || ''
  const safeAreas = detailSafeMarkAreas(indicator, domain)
  const thresholdLines = detailThresholdMarkLines(indicator)

  return {
    animation: false,
    grid: {
      left: 46,
      right: 32,
      top: 20,
      bottom: 52,
      containLabel: true,
    },
    tooltip: {
      trigger: 'item',
      confine: true,
      borderColor: '#dbe3ee',
      borderWidth: 1,
      backgroundColor: 'rgba(255, 255, 255, .96)',
      textStyle: {
        color: '#2f3a52',
        fontSize: 12,
      },
      formatter: params => formatDetailTooltip(params, indicator),
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: Math.max(1, compressedAxis.maxDisplayX),
      interval: 1,
      axisLine: { lineStyle: { color: '#dbe3ee' } },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: {
        showMinLabel: true,
        showMaxLabel: true,
        hideOverlap: true,
        color: '#71809a',
        fontSize: 12,
        lineHeight: 16,
        margin: 14,
        formatter: value => formatCompressedAxisLabel(Number(value), compressedAxis.axisLabels),
      },
    },
    yAxis: {
      type: 'value',
      min: domain.min,
      max: domain.max,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: '#71809a',
        formatter: value => `${trimNumber(Number(value))}${unit ? ` ${unit}` : ''}`,
      },
      splitLine: {
        lineStyle: {
          color: '#edf2f7',
          width: 1,
        },
      },
    },
    series: [
      {
        name: '安全阈值',
        type: 'line',
        data: compressedAxis.points.map(point => [point.displayX, point.value]),
        silent: true,
        symbol: 'none',
        lineStyle: { opacity: 0 },
        itemStyle: { opacity: 0 },
        markArea: safeAreas.length ? {
          silent: true,
          itemStyle: { color: safeAreaColor },
          data: safeAreas,
        } : undefined,
        markLine: thresholdLines.length ? {
          silent: true,
          symbol: 'none',
          lineStyle: {
            color: thresholdLineColor,
            type: 'dashed',
            width: 1.2,
          },
          label: {
            color: safeLineColor,
            fontSize: 11,
            fontWeight: 700,
            formatter: (params: any) => `${params.name} ${trimNumber(Number(params.value))}${unit}`,
          },
          data: thresholdLines,
        } : undefined,
        z: 1,
      },
      {
        name: '趋势线',
        type: 'custom',
        coordinateSystem: 'cartesian2d',
        data: compressedAxis.segments.map((segment, index) => [
          segment.startX,
          segment.startValue,
          segment.endX,
          segment.endValue,
          index,
        ]),
        renderItem: (params, api) => renderDetailSegment(params, api, compressedAxis.segments),
        silent: true,
        z: 5,
      },
      {
        name: '中断标注',
        type: 'custom',
        coordinateSystem: 'cartesian2d',
        data: compressedAxis.gaps.map((gap, index) => [gap.labelX, domain.min, index]),
        renderItem: (params, api) => renderGapLabel(params, api, compressedAxis.gaps),
        silent: true,
        z: 9,
      },
      {
        name: displayIndicatorName(indicator),
        type: 'scatter',
        data: compressedAxis.points.map(point => ({
          value: [point.displayX, point.value, point.detectedAt, point.status, point.tone],
          itemStyle: {
            color: detailToneColor(point.tone),
            borderColor: '#fff',
            borderWidth: 1.2,
            shadowBlur: 2,
            shadowColor: 'rgba(19, 35, 63, .14)',
          },
        })),
        encode: { x: 0, y: 1 },
        symbol: 'circle',
        symbolSize: 9,
        emphasis: {
          scale: 1.08,
          itemStyle: {
            borderColor: '#fff',
            borderWidth: 1.6,
            shadowBlur: 6,
            shadowColor: 'rgba(19, 35, 63, .2)',
          },
        },
        z: 8,
      },
    ],
    graphic: compressedAxis.points.length ? undefined : {
      type: 'text',
      left: 'center',
      top: 'middle',
      style: {
        text: '暂无趋势数据',
        fill: '#8b96aa',
        fontSize: 14,
        fontWeight: 600,
      },
    },
  } as EChartsOption
}

function renderDetailSegment(
  params: CustomSeriesRenderItemParams,
  api: CustomSeriesRenderItemAPI,
  segments: DetailChartSegment[],
) {
  const segment = segments[params.dataIndex]
  if (!segment) return null

  const start = api.coord([segment.startX, segment.startValue])
  const end = api.coord([segment.endX, segment.endValue])
  const coordSys = params.coordSys as unknown as { x: number; y: number; width: number; height: number }
  const clipped = graphic.clipPointsByRect([start, end], {
    x: coordSys.x,
    y: coordSys.y,
    width: coordSys.width,
    height: coordSys.height,
  })
  if (clipped.length < 2) return null

  return {
    type: 'line',
    shape: {
      x1: clipped[0][0],
      y1: clipped[0][1],
      x2: clipped[1][0],
      y2: clipped[1][1],
    },
    style: {
      stroke: detailToneColor(segment.tone),
      lineWidth: segment.tone === 'gap' ? 2.2 : 3,
      lineDash: segment.tone === 'gap' ? [7, 6] : undefined,
      lineCap: 'round',
      lineJoin: 'round',
    },
  }
}

function renderGapLabel(
  params: CustomSeriesRenderItemParams,
  api: CustomSeriesRenderItemAPI,
  gaps: CompressedGap[],
) {
  const gap = gaps[params.dataIndex]
  if (!gap) return null

  const coordSys = params.coordSys as unknown as { x: number; y: number; width: number; height: number }
  const point = api.coord([gap.labelX, Number(api.value(1))])
  const x = clamp(Number(point[0]), coordSys.x + 42, coordSys.x + coordSys.width - 42)
  const y = coordSys.y + coordSys.height - (gap.crossedDay ? 30 : 16)

  return {
    type: 'text',
    style: {
      x,
      y,
      text: formatGapAxisLabel(gap),
      align: 'center',
      verticalAlign: 'middle',
      fill: gapLineColor,
      fontSize: 11,
      fontWeight: 700,
      lineHeight: 15,
      backgroundColor: 'rgba(255, 255, 255, .9)',
      borderColor: 'rgba(154, 166, 188, .28)',
      borderWidth: 1,
      borderRadius: 4,
      padding: [3, 6],
    },
  }
}

function detailChartPoints(indicator: IotMonitorIndicator): DetailChartPoint[] {
  return (indicator.points ?? [])
    .map((point, index) => {
      const parsedTime = parseTimeMs(point.detectedAt)
      const tone: ChartTone = isPointInSafeRange(Number(point.value), indicator.safeRange, point.status) ? 'safe' : 'danger'
      return {
        key: `${point.logId}-${index}`,
        time: parsedTime ?? index,
        value: Number(point.value),
        detectedAt: point.detectedAt,
        status: point.status,
        tone,
      }
    })
    .sort((left, right) => left.time - right.time)
}

function buildCompressedDetailAxis(points: DetailChartPoint[], indicator: IotMonitorIndicator): CompressedDetailAxis {
  const compressedPoints: CompressedDetailPoint[] = []
  const gaps: CompressedGap[] = []

  for (let index = 0; index < points.length; index += 1) {
    const point = points[index]

    if (index === 0) {
      compressedPoints.push({ ...point, displayX: 0, forceDateLabel: false })
      continue
    }

    const previousPoint = points[index - 1]
    const previousCompressedPoint = compressedPoints[compressedPoints.length - 1]
    const interrupted = isDataInterrupted(previousPoint.detectedAt, point.detectedAt)
    const displayX = previousCompressedPoint.displayX + (interrupted ? compressedGapSlotWidth : 1)

    if (interrupted) {
      const startTime = parseTimeMs(previousPoint.detectedAt)
      const endTime = parseTimeMs(point.detectedAt)
      if (startTime != null && endTime != null) {
        gaps.push({
          key: `compressed-gap-${previousPoint.key}-${point.key}`,
          labelX: previousCompressedPoint.displayX + compressedGapSlotWidth / 2,
          startTime,
          endTime,
          durationMs: Math.abs(endTime - startTime),
          crossedDay: !isSameCalendarDay(startTime, endTime),
        })
      }
    }

    compressedPoints.push({
      ...point,
      displayX,
      forceDateLabel: interrupted,
    })
  }

  const segments = buildCompressedDetailSegments(compressedPoints, indicator)
  const axisLabels = buildCompressedAxisLabels(compressedPoints, gaps)

  return {
    points: compressedPoints,
    segments,
    gaps,
    axisLabels,
    maxDisplayX: compressedPoints.length ? compressedPoints[compressedPoints.length - 1].displayX : 0,
  }
}

function buildCompressedDetailSegments(
  points: CompressedDetailPoint[],
  indicator: IotMonitorIndicator,
): DetailChartSegment[] {
  if (points.length < 2) return []

  const ranges = normalizeSafeRanges(indicator.safeRange)
  const segments: DetailChartSegment[] = []

  for (let index = 1; index < points.length; index += 1) {
    const prev = points[index - 1]
    const current = points[index]

    if (isDataInterrupted(prev.detectedAt, current.detectedAt)) {
      segments.push({
        key: `gap-${prev.key}-${current.key}`,
        startX: prev.displayX,
        startValue: prev.value,
        endX: current.displayX,
        endValue: current.value,
        tone: 'gap',
      })
      continue
    }

    segments.push(...splitDetailSegmentBySafety(prev, current, ranges))
  }

  return segments
}

function splitDetailSegmentBySafety(
  start: CompressedDetailPoint,
  end: CompressedDetailPoint,
  ranges: NormalizedSafeRange[],
): DetailChartSegment[] {
  if (ranges.length === 0) {
    return [{
      key: `detail-segment-${start.key}-${end.key}`,
      startX: start.displayX,
      startValue: start.value,
      endX: end.displayX,
      endValue: end.value,
      tone: start.tone === 'danger' || end.tone === 'danger' ? 'danger' : 'safe',
    }]
  }

  const cuts = [0, 1]
  const valueDelta = end.value - start.value

  if (Math.abs(valueDelta) > thresholdEpsilon) {
    for (const boundary of safeRangeBoundaries(ranges)) {
      const ratio = (boundary - start.value) / valueDelta
      if (ratio > thresholdEpsilon && ratio < 1 - thresholdEpsilon) {
        cuts.push(ratio)
      }
    }
  }

  const sortedCuts = uniqueSortedRatios(cuts)
  const result: DetailChartSegment[] = []

  for (let index = 1; index < sortedCuts.length; index += 1) {
    const from = sortedCuts[index - 1]
    const to = sortedCuts[index]
    if (to - from <= thresholdEpsilon) continue

    const mid = (from + to) / 2
    const midValue = interpolate(start.value, end.value, mid)
    const tone: ChartTone = isValueInSafeRanges(midValue, ranges) ? 'safe' : 'danger'

    result.push({
      key: `detail-segment-${start.key}-${end.key}-${index}`,
      startX: interpolate(start.displayX, end.displayX, from),
      startValue: interpolate(start.value, end.value, from),
      endX: interpolate(start.displayX, end.displayX, to),
      endValue: interpolate(start.value, end.value, to),
      tone,
    })
  }

  return result
}

function buildCompressedAxisLabels(points: CompressedDetailPoint[], gaps: CompressedGap[]) {
  const labels = new Map<number, string>()
  if (points.length === 0) return labels

  const labelStep = Math.max(1, Math.ceil(points.length / 6))
  const occupiedSlots: number[] = []
  const reserveLabel = (x: number, label: string, reservedDistance = 1.7) => {
    labels.set(x, label)
    occupiedSlots.push(x - reservedDistance, x, x + reservedDistance)
  }
  const canPlaceOptionalLabel = (x: number) => occupiedSlots.every(slot => Math.abs(slot - x) >= 1.45)

  for (const point of points) {
    if (point.forceDateLabel) {
      reserveLabel(point.displayX, formatPointAxisLabel(point, true), 2.2)
    }
  }

  points.forEach((point, index) => {
    if (point.forceDateLabel) return

    const requiredEdgeLabel = index === 0 || index === points.length - 1
    if (requiredEdgeLabel) {
      if (!labels.has(point.displayX) && canPlaceOptionalLabel(point.displayX)) {
        reserveLabel(point.displayX, formatPointAxisLabel(point, false))
      }
      return
    }

    if (index % labelStep === 0 && canPlaceOptionalLabel(point.displayX)) {
      reserveLabel(point.displayX, formatPointAxisLabel(point, false))
    }
  })

  return labels
}

function formatCompressedAxisLabel(value: number, labels: Map<number, string>) {
  const rounded = Math.round(value)
  if (Math.abs(value - rounded) > thresholdEpsilon) return ''
  return labels.get(rounded) ?? ''
}

function formatGapAxisLabel(gap: CompressedGap) {
  const crossedDayText = gap.crossedDay ? `\n跨至 ${formatShortDate(gap.endTime)}` : ''
  return `[中断 ${formatGapDuration(gap.durationMs)}]${crossedDayText}`
}

function formatPointAxisLabel(point: CompressedDetailPoint, includeDate: boolean) {
  if (includeDate) return `${formatShortDate(point.time)}\n${formatClockTime(point.time, false)}`
  return formatClockTime(point.time)
}

function formatGapDuration(durationMs: number) {
  const totalSeconds = Math.max(1, Math.round(durationMs / 1000))
  const days = Math.floor(totalSeconds / 86400)
  const hours = Math.floor((totalSeconds % 86400) / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60

  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${pad2(minutes)}分`
  if (minutes > 0) return `${minutes}分${seconds > 0 ? `${seconds}秒` : ''}`
  return `${seconds}秒`
}

function detailSafeMarkAreas(indicator: IotMonitorIndicator, domain: { min: number; max: number }) {
  return normalizeSafeRanges(indicator.safeRange).map(range => {
    const lower = range.lower === Number.NEGATIVE_INFINITY ? domain.min : clamp(range.lower, domain.min, domain.max)
    const upper = range.upper === Number.POSITIVE_INFINITY ? domain.max : clamp(range.upper, domain.min, domain.max)
    return [{ yAxis: lower }, { yAxis: upper }]
  })
}

function detailThresholdMarkLines(indicator: IotMonitorIndicator) {
  const lines = new Map<number, string>()
  for (const range of normalizeSafeRanges(indicator.safeRange)) {
    if (Number.isFinite(range.lower)) lines.set(range.lower, '下限')
    if (Number.isFinite(range.upper)) lines.set(range.upper, '上限')
  }
  return Array.from(lines, ([value, name]) => ({ yAxis: value, name }))
}

function formatDetailTooltip(params: any, indicator: IotMonitorIndicator) {
  const value = Array.isArray(params.value) ? params.value : []
  const detectedAt = value[2] as string | undefined
  const metricValue = Number(value[1])
  const tone = value[4] as ChartTone | undefined
  const statusText = tone === 'danger' ? '超出阈值' : '安全范围内'

  return [
    `<strong>${displayIndicatorName(indicator)}</strong>`,
    `采集时间：${formatDateTime(detectedAt)}`,
    `采集值：${formatMetricValue(metricValue, indicator.unit)}`,
    `安全范围：${formatSafeRange(indicator.safeRange, indicator.unit)}`,
    `状态：${statusText}`,
  ].join('<br/>')
}

function formatClockTime(value: number, withSeconds = true) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const time = `${pad2(date.getHours())}:${pad2(date.getMinutes())}`
  return withSeconds ? `${time}:${pad2(date.getSeconds())}` : time
}

function formatShortDate(value: number) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getMonth() + 1}.${date.getDate()}`
}

function isSameCalendarDay(left: number, right: number) {
  const leftDate = new Date(left)
  const rightDate = new Date(right)
  return leftDate.getFullYear() === rightDate.getFullYear()
    && leftDate.getMonth() === rightDate.getMonth()
    && leftDate.getDate() === rightDate.getDate()
}

function pad2(value: number) {
  return value.toString().padStart(2, '0')
}

function detailToneColor(tone: ChartTone) {
  if (tone === 'danger') return dangerLineColor
  if (tone === 'gap') return gapLineColor
  return safeLineColor
}

function chartPoints(indicator: IotMonitorIndicator): ChartPoint[] {
  const points = indicator.points ?? []
  if (points.length === 0) return []
  const domain = chartDomain(indicator)
  const width = chartWidth - chartPadding * 2
  return points.map((point, index) => ({
    key: `${point.logId}-${index}`,
    x: points.length === 1 ? chartWidth - chartPadding : chartPadding + (index / (points.length - 1)) * width,
    y: scaleY(point.value, domain.min, domain.max),
    value: Number(point.value),
    detectedAt: point.detectedAt,
    status: point.status,
    tone: isPointInSafeRange(Number(point.value), indicator.safeRange, point.status) ? 'safe' : 'danger',
  }))
}

function chartSegments(indicator: IotMonitorIndicator): ChartSegment[] {
  const points = chartPoints(indicator)
  if (points.length < 2) return []

  const ranges = normalizeSafeRanges(indicator.safeRange)
  const segments: ChartSegment[] = []

  for (let index = 1; index < points.length; index += 1) {
    const prev = points[index - 1]
    const current = points[index]

    if (isDataInterrupted(prev.detectedAt, current.detectedAt)) {
      segments.push({
        key: `gap-${prev.key}-${current.key}`,
        x1: prev.x,
        y1: prev.y,
        x2: current.x,
        y2: current.y,
        tone: 'gap',
      })
      continue
    }

    segments.push(...splitSegmentBySafety(prev, current, ranges))
  }

  return segments
}

function splitSegmentBySafety(start: ChartPoint, end: ChartPoint, ranges: NormalizedSafeRange[]): ChartSegment[] {
  if (ranges.length === 0) {
    return [{
      key: `segment-${start.key}-${end.key}`,
      x1: start.x,
      y1: start.y,
      x2: end.x,
      y2: end.y,
      tone: start.tone === 'danger' || end.tone === 'danger' ? 'danger' : 'safe',
    }]
  }

  const cuts = [0, 1]
  const valueDelta = end.value - start.value

  if (Math.abs(valueDelta) > thresholdEpsilon) {
    for (const boundary of safeRangeBoundaries(ranges)) {
      const ratio = (boundary - start.value) / valueDelta
      if (ratio > thresholdEpsilon && ratio < 1 - thresholdEpsilon) {
        cuts.push(ratio)
      }
    }
  }

  const sortedCuts = uniqueSortedRatios(cuts)
  const result: ChartSegment[] = []

  for (let index = 1; index < sortedCuts.length; index += 1) {
    const from = sortedCuts[index - 1]
    const to = sortedCuts[index]
    if (to - from <= thresholdEpsilon) continue

    const mid = (from + to) / 2
    const midValue = interpolate(start.value, end.value, mid)
    const tone: ChartTone = isValueInSafeRanges(midValue, ranges) ? 'safe' : 'danger'

    result.push({
      key: `segment-${start.key}-${end.key}-${index}`,
      x1: interpolate(start.x, end.x, from),
      y1: interpolate(start.y, end.y, from),
      x2: interpolate(start.x, end.x, to),
      y2: interpolate(start.y, end.y, to),
      tone,
    })
  }

  return result
}

function safeBands(indicator: IotMonitorIndicator): SafeBand[] {
  const domain = chartDomain(indicator)
  return normalizeSafeRanges(indicator.safeRange).map(range => {
    const min = range.lower === Number.NEGATIVE_INFINITY ? domain.min : clamp(range.lower, domain.min, domain.max)
    const max = range.upper === Number.POSITIVE_INFINITY ? domain.max : clamp(range.upper, domain.min, domain.max)
    const y1 = scaleY(max, domain.min, domain.max)
    const y2 = scaleY(min, domain.min, domain.max)
    return { y: Math.min(y1, y2), height: Math.max(1, Math.abs(y2 - y1)) }
  })
}

function boundaryLines(indicator: IotMonitorIndicator): BoundaryLine[] {
  const domain = chartDomain(indicator)
  const values = new Set<number>()
  for (const range of normalizeSafeRanges(indicator.safeRange)) {
    if (Number.isFinite(range.lower)) values.add(range.lower)
    if (Number.isFinite(range.upper)) values.add(range.upper)
  }
  return Array.from(values)
    .filter(value => value >= domain.min && value <= domain.max)
    .map(value => ({ y: scaleY(value, domain.min, domain.max) }))
}

function chartDomain(indicator: IotMonitorIndicator): { min: number; max: number } {
  const values: number[] = []
  for (const point of indicator.points ?? []) {
    values.push(Number(point.value))
  }
  for (const range of normalizeSafeRanges(indicator.safeRange)) {
    if (Number.isFinite(range.lower)) values.push(range.lower)
    if (Number.isFinite(range.upper)) values.push(range.upper)
  }
  if (values.length === 0) return { min: 0, max: 1 }
  let min = Math.min(...values)
  let max = Math.max(...values)
  if (min === max) {
    min -= 1
    max += 1
  }
  const padding = Math.max((max - min) * 0.12, 0.1)
  return { min: min - padding, max: max + padding }
}

function scaleY(value: number, min: number, max: number) {
  const ratio = (max - value) / (max - min)
  return chartPadding + ratio * (chartHeight - chartPadding * 2)
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value))
}

function normalizeSafeRanges(safeRange: Array<Array<number | null>> | null | undefined): NormalizedSafeRange[] {
  return (safeRange ?? [])
    .map(range => {
      const rawLower = range[0]
      const rawUpper = range[1]
      if (rawLower == null && rawUpper == null) return null

      const lower = rawLower == null ? Number.NEGATIVE_INFINITY : Number(rawLower)
      const upper = rawUpper == null ? Number.POSITIVE_INFINITY : Number(rawUpper)
      if (!Number.isFinite(lower) && lower !== Number.NEGATIVE_INFINITY) return null
      if (!Number.isFinite(upper) && upper !== Number.POSITIVE_INFINITY) return null

      return lower <= upper ? { lower, upper } : { lower: upper, upper: lower }
    })
    .filter((range): range is NormalizedSafeRange => range != null)
}

function isPointInSafeRange(value: number, safeRange: Array<Array<number | null>> | null | undefined, status: number) {
  const ranges = normalizeSafeRanges(safeRange)
  if (ranges.length === 0) return status !== 2
  return isValueInSafeRanges(value, ranges)
}

function isValueInSafeRanges(value: number, ranges: NormalizedSafeRange[]) {
  return ranges.some(range => value >= range.lower - thresholdEpsilon && value <= range.upper + thresholdEpsilon)
}

function safeRangeBoundaries(ranges: NormalizedSafeRange[]) {
  const values = new Set<number>()
  for (const range of ranges) {
    if (Number.isFinite(range.lower)) values.add(range.lower)
    if (Number.isFinite(range.upper)) values.add(range.upper)
  }
  return Array.from(values)
}

function uniqueSortedRatios(values: number[]) {
  return values
    .filter(value => value >= 0 && value <= 1)
    .sort((a, b) => a - b)
    .filter((value, index, sorted) => index === 0 || Math.abs(value - sorted[index - 1]) > thresholdEpsilon)
}

function interpolate(start: number, end: number, ratio: number) {
  return start + (end - start) * ratio
}

function isDataInterrupted(start: string | null | undefined, end: string | null | undefined) {
  const startTime = parseTimeMs(start)
  const endTime = parseTimeMs(end)
  if (startTime == null || endTime == null) return false
  return Math.abs(endTime - startTime) > dataGapThresholdMs
}

function parseTimeMs(value: string | null | undefined) {
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const time = new Date(normalized).getTime()
  return Number.isFinite(time) ? time : null
}

function formatMetricValue(value: number | null | undefined, unit: string) {
  if (value == null) return '--'
  return `${trimNumber(value)} ${unit || ''}`.trim()
}

function trimNumber(value: number) {
  return Number(value).toFixed(3).replace(/\.?0+$/, '')
}

function formatPercent(value: number) {
  return `${trimNumber(value)}%`
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

function formatTime(value: string | null | undefined) {
  if (!value) return '--:--'
  return value.replace('T', ' ').slice(11, 16)
}

function formatSafeRange(safeRange: Array<Array<number | null>>, unit: string) {
  if (!safeRange?.length) return '未配置'
  return safeRange.map(range => {
    const lower = range[0]
    const upper = range[1]
    if (lower == null && upper == null) return '未配置'
    if (lower == null) return `≤ ${trimNumber(Number(upper))}${unit}`
    if (upper == null) return `≥ ${trimNumber(Number(lower))}${unit}`
    return `${trimNumber(Number(lower))} ~ ${trimNumber(Number(upper))}${unit}`
  }).join('；')
}

function labLocationTitle(lab: IotMonitorLabCard) {
  return [lab.campus, lab.building, lab.roomNo].filter(Boolean).join('-') || '未配置位置'
}

function displayIndicatorName(indicator: IotMonitorIndicator) {
  return displayDataName(indicator.displayName || indicator.dataName)
}

function displayDataName(dataName: string) {
  const normalized = dataName.toLowerCase()
  const labelMap: Record<string, string> = {
    temperature: '温度',
    temp: '温度',
    humidity: '湿度',
    smoke: '烟雾',
    gas: '气体',
    gas_concentration: '气体',
    combustible_gas: '气体',
  }
  return labelMap[normalized] || dataName
}

function iconKind(dataName: string) {
  const normalized = dataName.toLowerCase()
  if (normalized.includes('temp') || normalized.includes('temperature')) return 'temperature'
  if (normalized.includes('humidity')) return 'humidity'
  if (normalized.includes('smoke')) return 'smoke'
  return 'gas'
}

function statusDotClass(status: IotLabMonitorStatus) {
  return {
    'dot-normal': status === 'NORMAL',
    'dot-alert': status === 'ALERT',
    'dot-muted': status === 'NO_SENSOR' || status === 'NO_DATA',
  }
}

function cardStatusClass(status: IotLabMonitorStatus) {
  return {
    'card-normal': status === 'NORMAL',
    'card-alert': status === 'ALERT',
    'card-muted': status === 'NO_SENSOR' || status === 'NO_DATA',
  }
}

function badgeStatusClass(status: IotLabMonitorStatus) {
  return {
    normal: status === 'NORMAL',
    danger: status === 'ALERT',
    muted: status === 'NO_SENSOR' || status === 'NO_DATA',
  }
}
</script>

<style scoped>
.im-page { height:100%; overflow-y:auto; padding:20px 24px; color:#182033 }
.page-header { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; margin-bottom:18px }
.page-header h2 { margin:0; font-size:22px; line-height:1.2; font-weight:700; color:#111a33 }
.page-header p { margin:6px 0 0; font-size:13px; color:#7b8498 }
.header-right { display:flex; align-items:center; gap:12px; flex-shrink:0 }
.update-time { font-size:12px; color:#7b8498 }
.refresh-btn { display:flex; align-items:center; gap:6px; padding:8px 14px; border:1px solid rgba(26,37,64,.12); border-radius:8px; background:rgba(255,255,255,.82); color:#3a455d; font-size:13px; cursor:pointer; transition:.18s }
.refresh-btn:hover { border-color:#2475ff; color:#2475ff }
.refresh-btn.spinning svg { animation:spin .7s linear infinite }
@keyframes spin { to { transform:rotate(360deg) } }
.error-banner { margin-bottom:14px; padding:10px 12px; border:1px solid rgba(221,56,56,.2); border-radius:8px; background:rgba(255,241,241,.9); color:#bd2d2d; font-size:13px }

.monitor-layout { display:grid; grid-template-columns:280px minmax(0,1fr); gap:18px; align-items:start }
.filter-panel { background:rgba(255,255,255,.72); border:1px solid rgba(20,37,70,.08); border-radius:8px; padding:18px; box-shadow:0 8px 24px rgba(22,35,70,.05) }
.filter-title { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; color:#10215c; font-weight:700 }
.field-label { display:block; margin:14px 0 7px; font-size:13px; color:#4e5a72 }
.field-control { width:100%; height:38px; border:1px solid rgba(24,36,64,.14); border-radius:7px; background:#fff; padding:0 10px; color:#1d273b; font-size:13px; outline:none }
.field-control:focus { border-color:#2475ff; box-shadow:0 0 0 3px rgba(36,117,255,.1) }
.search-row { display:grid; grid-template-columns:minmax(0,1fr) 38px; gap:8px }
.search-input { min-width:0 }
.filter-btn { width:38px; height:38px; border:1px solid rgba(36,117,255,.2); border-radius:7px; background:#f5f8ff; color:#2475ff; display:flex; align-items:center; justify-content:center; cursor:pointer }
.filter-btn:hover { background:#eaf1ff }
.lab-list-head { display:flex; align-items:center; justify-content:space-between; margin:22px 0 10px; color:#10215c; font-size:14px; font-weight:700 }
.lab-list-head strong { color:#4b65a1 }
.lab-list { display:flex; flex-direction:column; gap:6px; max-height:430px; overflow-y:auto; padding-right:2px }
.lab-list-item { border:1px solid transparent; background:transparent; border-radius:7px; padding:9px 8px; display:grid; grid-template-columns:10px minmax(0,1fr); gap:9px; align-items:center; text-align:left; cursor:pointer }
.lab-list-item:hover,.lab-list-item.active { background:#f3f7ff; border-color:rgba(36,117,255,.1) }
.lab-list-text { min-width:0; display:flex; flex-direction:column; gap:3px }
.lab-list-text strong { color:#17233c; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.lab-list-text em { color:#7a859b; font-size:12px; font-style:normal; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.empty-list { padding:18px 0; text-align:center; font-size:13px; color:#9ba5b8 }

.monitor-main { min-width:0 }
.stats-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:14px; margin-bottom:18px }
.stat-card { min-height:108px; border:1px solid rgba(20,37,70,.08); border-radius:8px; background:rgba(255,255,255,.76); display:flex; align-items:center; gap:16px; padding:16px 18px; box-shadow:0 8px 24px rgba(22,35,70,.04) }
.stat-icon { width:52px; height:52px; border-radius:50%; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.stat-icon.blue { color:#2475ff; background:#eaf1ff }
.stat-icon.green { color:#24b66b; background:#e9f9ef }
.stat-icon.red { color:#e64c4c; background:#fff0f0 }
.stat-label { display:block; margin-bottom:6px; color:#34405a; font-size:13px; font-weight:700 }
.stat-value { display:block; color:#111a33; font-size:26px; line-height:1; font-weight:800 }
.stat-value small { color:#6f7a91; font-size:16px; font-weight:600 }
.stat-sub { display:block; margin-top:7px; color:#7b8498; font-size:12px; font-style:normal }
.online-card { display:block }
.online-rate { display:block; color:#22b05f; font-size:30px; line-height:1; font-weight:800 }
.rate-bar { width:100%; height:8px; margin:12px 0 8px; border-radius:999px; background:#e7edf4; overflow:hidden }
.rate-bar span { display:block; height:100%; border-radius:999px; background:linear-gradient(90deg,#28b76b,#55c47a) }

.loading-state,.empty-state { min-height:260px; border:1px dashed rgba(25,42,75,.16); border-radius:8px; display:flex; align-items:center; justify-content:center; flex-direction:column; gap:10px; color:#8b96aa; background:rgba(255,255,255,.45) }
.empty-state p { margin:0; font-size:14px }
.chart-legend-panel { display:flex; align-items:center; flex-wrap:wrap; gap:14px 18px; margin:0 0 14px; padding:10px 12px; border:1px solid rgba(20,37,70,.08); border-radius:8px; background:rgba(255,255,255,.78); box-shadow:0 8px 24px rgba(22,35,70,.04) }
.chart-legend,.legend-item { display:flex; align-items:center; gap:7px }
.chart-legend { flex-wrap:wrap; justify-content:flex-end; gap:10px 14px }
.legend-item { color:#536079; font-size:12px; line-height:1.2; white-space:nowrap }
.legend-line { display:inline-block; width:24px; height:0; border-top:2px solid currentColor; border-radius:999px; flex-shrink:0 }
.legend-line.safe { color:#1fa85f }
.legend-line.danger { color:#ef4444 }
.legend-line.gap { color:#9aa6bc; border-top-style:dashed }
.lab-card-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px }
.lab-card { border:1px solid rgba(20,37,70,.08); border-left-width:3px; border-radius:8px; background:rgba(255,255,255,.74); padding:18px 20px; box-shadow:0 8px 24px rgba(22,35,70,.04) }
.lab-card.card-normal { border-left-color:#24b66b }
.lab-card.card-alert { border-left-color:#e64c4c }
.lab-card.card-muted { border-left-color:#9aa6bc }
.lab-card-head { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:14px }
.lab-card-title { display:flex; align-items:center; gap:9px; min-width:0 }
.lab-card-title h3 { margin:0; color:#17233c; font-size:16px; font-weight:800; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.status-dot { width:9px; height:9px; border-radius:50%; flex-shrink:0 }
.dot-normal { background:#24b66b }
.dot-alert { background:#e64c4c }
.dot-muted { background:#9aa6bc }
.status-badge { flex-shrink:0; padding:3px 9px; border-radius:999px; font-size:12px; font-weight:700 }
.status-badge.normal { color:#159653; background:#e8f8ef }
.status-badge.danger { color:#dc3939; background:#fff0f0 }
.status-badge.muted { color:#6c7890; background:#eef2f7 }
.detail-btn { display:flex; align-items:center; gap:3px; border:none; background:transparent; color:#667188; font-weight:700; font-size:13px; cursor:pointer }
.detail-btn:hover { color:#2475ff }
.indicator-list { display:flex; flex-direction:column; gap:11px }
.indicator-row { display:grid; grid-template-columns:94px 96px minmax(160px,1fr); gap:12px; align-items:center }
.indicator-meta { display:flex; align-items:center; gap:8px; min-width:0 }
.indicator-icon { width:24px; height:24px; display:flex; align-items:center; justify-content:center; flex-shrink:0; color:#2475ff }
.indicator-icon.humidity { color:#18a5ff }
.indicator-icon.smoke { color:#7b61ff }
.indicator-icon.gas { color:#2475ff }
.indicator-name { color:#33405c; font-size:13px; font-weight:700; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.indicator-value { min-width:0 }
.indicator-value strong { display:block; color:#15203a; font-size:14px; font-weight:800; white-space:nowrap }
.indicator-value em { display:block; margin-top:3px; color:#7a859b; font-size:11px; font-style:normal; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.mini-chart-wrap { position:relative; min-width:0; height:58px; background:#fff; border:1px solid #edf2f7; border-radius:6px; overflow:hidden }
.mini-chart { width:100%; height:100%; display:block }
.chart-time { position:absolute; right:6px; bottom:4px; color:#78859a; font-size:11px; line-height:1; padding:2px 4px; border-radius:4px; background:rgba(255,255,255,.76) }
.safe-band { fill:rgba(37,182,107,.13) }
.boundary-line { stroke:rgba(46,119,91,.46); stroke-width:1; stroke-dasharray:5 4; vector-effect:non-scaling-stroke }
.chart-line-segment { fill:none; stroke-width:2; stroke-linecap:round; stroke-linejoin:round; vector-effect:non-scaling-stroke }
.chart-line-segment.safe { stroke:#1fa85f }
.chart-line-segment.danger { stroke:#ef4444 }
.chart-line-segment.gap { stroke:#9aa6bc; stroke-width:1.6; stroke-dasharray:5 4 }
.chart-point { stroke:#fff; stroke-width:1.2; vector-effect:non-scaling-stroke }
.chart-point.safe { fill:#1fa85f }
.chart-point.danger { fill:#ef4444 }
.card-empty { padding:20px 0; color:#8c96a8; text-align:center; font-size:13px }

.modal-overlay { position:fixed; inset:0; z-index:1000; background:rgba(10,18,34,.35); backdrop-filter:blur(4px); display:flex; align-items:center; justify-content:center; padding:24px }
.detail-modal { width:min(1420px,96vw); max-height:90vh; overflow:hidden; background:#fff; border-radius:10px; box-shadow:0 24px 80px rgba(8,20,44,.28); display:flex; flex-direction:column }
.detail-head { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:18px 22px; border-bottom:1px solid #eef1f6 }
.detail-head h3 { margin:0; font-size:18px; color:#121b30 }
.detail-head p { margin:5px 0 0; color:#7a8497; font-size:13px }
.modal-close { width:34px; height:34px; border:none; border-radius:8px; background:#f4f6fa; color:#59657a; display:flex; align-items:center; justify-content:center; cursor:pointer }
.modal-close:hover { color:#e64c4c; background:#fff0f0 }
.detail-body { padding:18px 22px 20px; overflow-y:auto }
.indicator-tabs { display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:14px }
.indicator-tabs button { border:1px solid #dbe2ec; border-radius:999px; background:#fff; color:#4b5870; padding:7px 13px; font-size:13px; cursor:pointer }
.indicator-tabs button.active { border-color:#2475ff; color:#2475ff; background:#f0f5ff; font-weight:700 }
.detail-chart-panel { border:1px solid #e3e9f2; border-radius:8px; padding:16px 18px 18px; margin-bottom:14px; background:#fff; box-shadow:0 10px 28px rgba(22,35,70,.05) }
.detail-chart-top { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; margin-bottom:12px }
.detail-chart-meta { min-width:0; display:flex; flex-direction:column; gap:5px }
.detail-chart-meta strong { color:#121b30; font-size:17px; line-height:1.2 }
.detail-chart-meta span { color:#7a8497; font-size:13px; line-height:1.35 }
.chart-legend.compact { padding-top:2px }
.detail-chart { width:100%; height:320px; display:block; background:linear-gradient(180deg,#fff 0%,#fbfdff 100%); border:1px solid #edf2f7; border-radius:6px }
.log-table-wrap { overflow-x:auto; border:1px solid #eef1f6; border-radius:8px }
.log-table { width:100%; border-collapse:collapse; min-width:880px }
.log-table th,.log-table td { padding:10px 12px; border-bottom:1px solid #eef1f6; text-align:left; font-size:13px; color:#34405a }
.log-table th { background:#f7f9fc; color:#657188; font-weight:800 }
.log-table tr:last-child td { border-bottom:none }
.log-status { display:inline-flex; padding:3px 8px; border-radius:999px; font-size:12px; font-weight:700 }
.log-status.normal { color:#159653; background:#e8f8ef }
.log-status.danger { color:#dc3939; background:#fff0f0 }
.event-cell { max-width:190px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#69758b }
.empty-cell { text-align:center!important; color:#9aa4b5!important; padding:28px 12px!important }
.pagination { display:flex; align-items:center; justify-content:flex-end; gap:10px; margin-top:14px; color:#6e788b; font-size:13px }
.pagination button { border:1px solid #dbe2ec; background:#fff; border-radius:7px; padding:7px 12px; color:#41506a; cursor:pointer }
.pagination button:disabled { opacity:.45; cursor:not-allowed }
.modal-fade-enter-active,.modal-fade-leave-active { transition:opacity .18s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }

@media (max-width:1180px) {
  .stats-grid { grid-template-columns:repeat(2,minmax(0,1fr)) }
  .lab-card-grid { grid-template-columns:1fr }
}
@media (max-width:920px) {
  .monitor-layout { grid-template-columns:1fr }
  .filter-panel { position:relative }
  .page-header { flex-direction:column }
  .header-right { width:100%; justify-content:space-between }
}
@media (max-width:640px) {
  .im-page { padding:16px }
  .stats-grid { grid-template-columns:1fr }
  .indicator-row { grid-template-columns:1fr; gap:7px }
  .mini-chart-wrap { height:54px }
  .modal-overlay { padding:12px }
}
</style>
