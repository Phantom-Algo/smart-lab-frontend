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

        <div v-else class="lab-card-grid">
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
                    <polyline
                      v-if="polylinePoints(indicator)"
                      :points="polylinePoints(indicator)"
                      :class="['chart-line', hasAlertPoint(indicator) ? 'danger' : 'normal']"
                    />
                    <circle
                      v-for="point in chartPoints(indicator)"
                      :key="point.key"
                      :cx="point.x"
                      :cy="point.y"
                      r="2"
                      :class="['chart-point', point.status === 2 ? 'danger' : 'normal']"
                    />
                  </svg>
                  <span class="chart-time">{{ formatTime(indicator.detectedAt) }}</span>
                </div>
              </div>
              <div v-if="lab.indicators.length === 0" class="card-empty">该实验室类型暂未配置安全指标</div>
            </div>
          </article>
        </div>
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
                <div class="detail-chart-meta">
                  <strong>{{ formatMetricValue(selectedDetailIndicator.latestValue, selectedDetailIndicator.unit) }}</strong>
                  <span>{{ selectedDetailIndicator.latestStatusDesc }} · {{ formatSafeRange(selectedDetailIndicator.safeRange, selectedDetailIndicator.unit) }}</span>
                </div>
                <svg class="detail-chart" :viewBox="chartViewBox" preserveAspectRatio="none">
                  <rect
                    v-for="(band, index) in safeBands(selectedDetailIndicator)"
                    :key="'detail-band-' + index"
                    :x="0"
                    :y="band.y"
                    :width="chartWidth"
                    :height="band.height"
                    class="safe-band"
                  />
                  <line
                    v-for="(line, index) in boundaryLines(selectedDetailIndicator)"
                    :key="'detail-line-' + index"
                    :x1="0"
                    :x2="chartWidth"
                    :y1="line.y"
                    :y2="line.y"
                    class="boundary-line"
                  />
                  <polyline
                    v-if="polylinePoints(selectedDetailIndicator)"
                    :points="polylinePoints(selectedDetailIndicator)"
                    :class="['chart-line', hasAlertPoint(selectedDetailIndicator) ? 'danger' : 'normal']"
                  />
                  <circle
                    v-for="point in chartPoints(selectedDetailIndicator)"
                    :key="'detail-' + point.key"
                    :cx="point.x"
                    :cy="point.y"
                    r="2.2"
                    :class="['chart-point', point.status === 2 ? 'danger' : 'normal']"
                  />
                </svg>
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
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
  status: number
}

interface SafeBand {
  y: number
  height: number
}

interface BoundaryLine {
  y: number
}

const chartWidth = 260
const chartHeight = 48
const chartPadding = 4
const chartViewBox = `0 0 ${chartWidth} ${chartHeight}`

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

onMounted(async () => {
  await Promise.all([loadFilterOptions(), loadLabTypes()])
  await refreshOverview()
  pollTimer = window.setInterval(() => {
    void refreshOverview(true)
  }, pollIntervalMs)
})

onUnmounted(() => {
  if (pollTimer) window.clearInterval(pollTimer)
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

function chartPoints(indicator: IotMonitorIndicator): ChartPoint[] {
  const points = indicator.points ?? []
  if (points.length === 0) return []
  const domain = chartDomain(indicator)
  const width = chartWidth - chartPadding * 2
  return points.map((point, index) => ({
    key: `${point.logId}-${index}`,
    x: points.length === 1 ? chartWidth - chartPadding : chartPadding + (index / (points.length - 1)) * width,
    y: scaleY(point.value, domain.min, domain.max),
    status: point.status,
  }))
}

function polylinePoints(indicator: IotMonitorIndicator): string {
  return chartPoints(indicator).map(point => `${point.x.toFixed(1)},${point.y.toFixed(1)}`).join(' ')
}

function safeBands(indicator: IotMonitorIndicator): SafeBand[] {
  const domain = chartDomain(indicator)
  return (indicator.safeRange ?? []).map(range => {
    const min = range[0] == null ? domain.min : clamp(Number(range[0]), domain.min, domain.max)
    const max = range[1] == null ? domain.max : clamp(Number(range[1]), domain.min, domain.max)
    const y1 = scaleY(max, domain.min, domain.max)
    const y2 = scaleY(min, domain.min, domain.max)
    return { y: Math.min(y1, y2), height: Math.max(1, Math.abs(y2 - y1)) }
  })
}

function boundaryLines(indicator: IotMonitorIndicator): BoundaryLine[] {
  const domain = chartDomain(indicator)
  const values = new Set<number>()
  for (const range of indicator.safeRange ?? []) {
    if (range[0] != null) values.add(Number(range[0]))
    if (range[1] != null) values.add(Number(range[1]))
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
  for (const range of indicator.safeRange ?? []) {
    if (range[0] != null) values.push(Number(range[0]))
    if (range[1] != null) values.push(Number(range[1]))
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

function hasAlertPoint(indicator: IotMonitorIndicator) {
  return indicator.points?.some(point => point.status === 2) ?? false
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
.mini-chart-wrap { position:relative; min-width:0; height:42px; background:linear-gradient(90deg,rgba(39,174,96,.08),rgba(39,174,96,.04)); border-radius:2px; overflow:hidden }
.mini-chart { width:100%; height:100%; display:block }
.chart-time { position:absolute; right:4px; bottom:2px; color:#78859a; font-size:11px }
.safe-band { fill:rgba(37,182,107,.14) }
.boundary-line { stroke:rgba(70,112,92,.28); stroke-width:1; stroke-dasharray:4 3 }
.chart-line { fill:none; stroke-width:1.8 }
.chart-line.normal { stroke:#1fa85f }
.chart-line.danger { stroke:#e64c4c }
.chart-point.normal { fill:#1fa85f }
.chart-point.danger { fill:#e64c4c }
.card-empty { padding:20px 0; color:#8c96a8; text-align:center; font-size:13px }

.modal-overlay { position:fixed; inset:0; z-index:1000; background:rgba(10,18,34,.35); backdrop-filter:blur(4px); display:flex; align-items:center; justify-content:center; padding:24px }
.detail-modal { width:min(1060px,96vw); max-height:88vh; overflow:hidden; background:#fff; border-radius:10px; box-shadow:0 24px 80px rgba(8,20,44,.28); display:flex; flex-direction:column }
.detail-head { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:18px 22px; border-bottom:1px solid #eef1f6 }
.detail-head h3 { margin:0; font-size:18px; color:#121b30 }
.detail-head p { margin:5px 0 0; color:#7a8497; font-size:13px }
.modal-close { width:34px; height:34px; border:none; border-radius:8px; background:#f4f6fa; color:#59657a; display:flex; align-items:center; justify-content:center; cursor:pointer }
.modal-close:hover { color:#e64c4c; background:#fff0f0 }
.detail-body { padding:18px 22px 20px; overflow-y:auto }
.indicator-tabs { display:flex; align-items:center; gap:8px; flex-wrap:wrap; margin-bottom:14px }
.indicator-tabs button { border:1px solid #dbe2ec; border-radius:999px; background:#fff; color:#4b5870; padding:7px 13px; font-size:13px; cursor:pointer }
.indicator-tabs button.active { border-color:#2475ff; color:#2475ff; background:#f0f5ff; font-weight:700 }
.detail-chart-panel { border:1px solid #eef1f6; border-radius:8px; padding:12px 14px; margin-bottom:14px; background:#fbfcff }
.detail-chart-meta { display:flex; align-items:baseline; gap:12px; margin-bottom:8px }
.detail-chart-meta strong { color:#121b30; font-size:18px }
.detail-chart-meta span { color:#7a8497; font-size:13px }
.detail-chart { width:100%; height:126px; display:block; background:linear-gradient(90deg,rgba(39,174,96,.08),rgba(39,174,96,.03)); border-radius:4px }
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
