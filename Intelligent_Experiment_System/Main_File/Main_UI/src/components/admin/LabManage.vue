<template>
  <div class="lm-page">
    <header class="page-header">
      <div>
        <h2>场地管理</h2>
        <p>实验室信息、开放安排与状态维护</p>
      </div>
      <button class="primary-btn" type="button" @click="openCreateDialog">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14" />
        </svg>
        新增实验室
      </button>
    </header>

    <section class="overview-grid">
      <article v-for="card in statCards" :key="card.key" class="stat-card">
        <span class="stat-label">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
      </article>
    </section>

    <section class="filter-panel">
      <div class="keyword-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <input
          v-model="query.keyword"
          placeholder="搜索实验室名称 / 类型"
          @input="onKeywordInput"
          @keyup.enter="reloadFromFirstPage"
        />
      </div>
      <select v-model.number="query.labTypeId" class="filter-select" @change="reloadFromFirstPage">
        <option :value="0">全部类型</option>
        <option v-for="type in labTypes" :key="type.id" :value="type.id">{{ type.labTypeName }}</option>
      </select>
      <select v-model="query.campus" class="filter-select" @change="onCampusChange">
        <option value="">全部校区</option>
        <option v-for="campus in filterOptions.campuses" :key="campus" :value="campus">{{ campus }}</option>
      </select>
      <select v-model="query.building" class="filter-select" @change="reloadFromFirstPage">
        <option value="">全部楼栋</option>
        <option v-for="building in availableBuildings" :key="building" :value="building">{{ building }}</option>
      </select>
      <select v-model.number="query.runningStatus" class="filter-select" @change="reloadFromFirstPage">
        <option :value="0">全部运行状态</option>
        <option v-for="item in runningStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <select v-model.number="query.openStatus" class="filter-select" @change="reloadFromFirstPage">
        <option :value="0">全部开放状态</option>
        <option v-for="item in openStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
      <button class="ghost-btn" type="button" @click="resetFilters">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M3 12a9 9 0 1 0 3-6.7" />
          <path d="M3 4v5h5" />
        </svg>
        重置
      </button>
    </section>

    <section class="table-section">
      <div class="table-toolbar">
        <span>共 {{ total }} 间实验室</span>
        <button class="text-btn" type="button" :disabled="loading" @click="loadPage">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12a9 9 0 1 1-2.64-6.36" />
            <path d="M21 3v6h-6" />
          </svg>
          刷新
        </button>
      </div>

      <div v-if="pageError" class="state-box error">{{ pageError }}</div>
      <div v-else class="table-wrap">
        <table class="lab-table">
          <thead>
            <tr>
              <th>实验室类型</th>
              <th>实验室名称</th>
              <th>实验室位置</th>
              <th>可容纳人数</th>
              <th>面积</th>
              <th>负责人</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="empty-cell">加载中...</td>
            </tr>
            <tr v-else-if="labs.length === 0">
              <td colspan="8" class="empty-cell">暂无实验室数据</td>
            </tr>
            <template v-else>
              <tr v-for="lab in labs" :key="lab.id">
                <td><span class="type-pill">{{ lab.labTypeName || '-' }}</span></td>
                <td class="strong-cell">{{ lab.labName }}</td>
                <td>{{ formatLocation(lab) }}</td>
                <td>{{ lab.capacity ?? 0 }} 人</td>
                <td>{{ formatArea(lab.areaM2) }}</td>
                <td>{{ lab.dutyUserName || '-' }}</td>
                <td>
                  <div class="status-pair">
                    <span class="status-tag" :class="runningStatusClass(lab.runningStatus)">
                      {{ runningStatusLabel(lab.runningStatus) }}
                    </span>
                    <span class="status-tag" :class="openStatusClass(lab.openStatus)">
                      {{ openStatusLabel(lab.openStatus) }}
                    </span>
                  </div>
                </td>
                <td>
                  <div class="row-actions">
                    <button class="link-btn" type="button" @click="openDetail(lab.id)">详情</button>
                    <button class="link-btn" type="button" @click="openEditDialog(lab.id)">编辑</button>
                    <button class="link-btn danger" type="button" @click="deleteOneLab(lab)">删除</button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <button type="button" :disabled="query.page <= 1 || loading" @click="changePage(query.page - 1)">上一页</button>
        <span>{{ query.page }} / {{ totalPages }}</span>
        <button type="button" :disabled="query.page >= totalPages || loading" @click="changePage(query.page + 1)">下一页</button>
      </div>
    </section>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
          <div class="modal-panel detail-panel">
            <div class="modal-header">
              <div>
                <h3>实验室详情</h3>
                <p v-if="detailLab">{{ detailLab.labName }}</p>
              </div>
              <button class="icon-btn" type="button" @click="detailVisible = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div v-if="detailLoading" class="modal-body state-box">加载中...</div>
            <div v-else-if="detailLab" class="modal-body">
              <div class="detail-grid">
                <div><span>实验室类型</span><strong>{{ detailLab.labTypeName || '-' }}</strong></div>
                <div><span>实验室名称</span><strong>{{ detailLab.labName }}</strong></div>
                <div><span>实验室位置</span><strong>{{ formatLocation(detailLab) }}</strong></div>
                <div><span>可容纳人数</span><strong>{{ detailLab.capacity ?? 0 }} 人</strong></div>
                <div><span>面积</span><strong>{{ formatArea(detailLab.areaM2) }}</strong></div>
                <div><span>负责人</span><strong>{{ detailLab.dutyUserName || '-' }}</strong></div>
              </div>
              <div class="status-pair detail-status">
                <span class="status-tag" :class="runningStatusClass(detailLab.runningStatus)">
                  {{ runningStatusLabel(detailLab.runningStatus) }}
                </span>
                <span class="status-tag" :class="openStatusClass(detailLab.openStatus)">
                  {{ openStatusLabel(detailLab.openStatus) }}
                </span>
              </div>
              <section class="info-section">
                <h4>描述</h4>
                <p>{{ detailLab.description || '暂无描述' }}</p>
              </section>
              <section v-if="detailLab.runningStatus === 2" class="info-section">
                <h4>当前使用信息</h4>
                <div v-if="currentUsageLoading" class="muted-text">加载中...</div>
                <div v-else-if="currentUsages.length === 0" class="muted-text">暂无当前使用记录</div>
                <template v-else>
                  <div v-for="usage in currentUsages" :key="usage.reservationNo" class="usage-card">
                    <div><span>预约人</span><strong>{{ usage.applicantName }}</strong></div>
                    <div><span>签到时间</span><strong>{{ formatDateTime(usage.checkInAt) }}</strong></div>
                    <div><span>使用目的</span><strong>{{ usage.purpose || '-' }}</strong></div>
                    <div><span>课程</span><strong>{{ usage.courseName || '-' }}</strong></div>
                  </div>
                </template>
              </section>
              <section class="info-section">
                <h4>开放时间</h4>
                <div v-if="detailLab.weeklySchedules.length === 0" class="muted-text">暂无开放时间</div>
                <div v-else class="schedule-list">
                  <span v-for="item in detailLab.weeklySchedules" :key="item.id">
                    {{ weekLabel(item.dayOfWeek) }} {{ formatTime(item.openTime) }}-{{ formatTime(item.closeTime) }}
                  </span>
                </div>
              </section>
              <section class="info-section">
                <h4>临时不开放时间</h4>
                <div v-if="detailCloseSchedules.length === 0" class="muted-text">暂无临时关闭安排</div>
                <template v-else>
                  <div v-for="item in detailCloseSchedules" :key="item.id" class="close-item">
                    <strong>{{ item.closeTypeDesc }}</strong>
                    <span>{{ formatDateTime(item.startTime) }} 至 {{ formatDateTime(item.endTime) }}</span>
                    <em>{{ item.reason || '未填写原因' }}</em>
                  </div>
                </template>
              </section>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="editorVisible" class="modal-overlay" @click.self="closeEditor">
          <div class="modal-panel editor-panel">
            <div class="modal-header">
              <div>
                <h3>{{ editorMode === 'create' ? '新增实验室' : '编辑实验室' }}</h3>
                <p>{{ editorMode === 'create' ? '创建后默认停用并临时关闭' : '维护基础信息、开放安排与状态' }}</p>
              </div>
              <button class="icon-btn" type="button" @click="closeEditor">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6 6 18M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div class="modal-body">
              <div v-if="editorLoading" class="state-box">加载中...</div>
              <template v-else>
                <section class="form-section">
                  <h4>基础信息</h4>
                  <div class="form-grid two">
                    <label>
                      <span>实验室名称</span>
                      <input v-model.trim="form.labName" class="form-input" maxlength="128" placeholder="请输入实验室名称" />
                    </label>
                    <label>
                      <span>所属类型</span>
                      <select v-model.number="form.labTypeId" class="form-input">
                        <option :value="0" disabled>请选择类型</option>
                        <option v-for="type in labTypes" :key="type.id" :value="type.id">{{ type.labTypeName }}</option>
                      </select>
                    </label>
                  </div>
                  <div class="form-grid four">
                    <label><span>校区</span><input v-model.trim="form.campus" class="form-input" maxlength="64" /></label>
                    <label><span>楼栋</span><input v-model.trim="form.building" class="form-input" maxlength="64" /></label>
                    <label><span>楼层</span><input v-model="form.floor" class="form-input" type="number" min="1" max="99" /></label>
                    <label><span>房间号</span><input v-model.trim="form.roomNo" class="form-input" maxlength="32" /></label>
                  </div>
                  <div class="form-grid three">
                    <label>
                      <span>最大容纳人数</span>
                      <input v-model="form.capacity" class="form-input" type="number" min="0" />
                    </label>
                    <label>
                      <span>面积</span>
                      <input v-model="form.areaM2" class="form-input" type="number" min="0" step="0.1" />
                    </label>
                    <div class="person-picker">
                      <span class="field-label">负责人</span>
                      <div v-if="selectedDutyUser" class="selected-person">
                        <span>{{ selectedDutyUser.personName }}</span>
                        <button type="button" @click="clearDutyUser">移除</button>
                      </div>
                      <div class="person-search">
                        <input
                          v-model="dutyKeyword"
                          class="form-input"
                          placeholder="搜索姓名 / 用户名 / 人员编号"
                          @input="onDutyInput"
                          @focus="onDutyInput"
                          @blur="hideDutyDropdown"
                        />
                        <div v-if="dutyDropdownVisible" class="person-dropdown">
                        <div v-if="dutyLoading" class="dropdown-state">搜索中...</div>
                          <template v-else>
                            <button
                              v-for="person in dutyResults"
                              :key="person.userId"
                              type="button"
                              @mousedown.prevent="selectDutyUser(person)"
                            >
                              <strong>{{ person.personName }}</strong>
                              <span>@{{ person.username }} · {{ person.personNo }}</span>
                            </button>
                          </template>
                          <div v-if="!dutyLoading && dutyResults.length === 0 && dutyKeyword.trim()" class="dropdown-state">
                            无匹配人员
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <label class="block-label">
                    <span>描述</span>
                    <textarea v-model.trim="form.description" class="form-input" rows="3" maxlength="512" />
                  </label>
                </section>

                <section v-if="editorMode === 'create'" class="form-section compact-section">
                  <label class="check-line">
                    <input v-model="form.immediateOpen" type="checkbox" />
                    <span>创建后立即开放为空闲 / 开放中</span>
                  </label>
                </section>

                <section class="form-section">
                  <div class="section-title-row">
                    <h4>开放时间</h4>
                    <button class="small-btn" type="button" @click="addScheduleRow">添加</button>
                  </div>
                  <div v-if="scheduleRows.length === 0" class="muted-text">暂无开放时间</div>
                  <div v-for="(row, index) in scheduleRows" :key="row.clientId" class="inline-row schedule-row">
                    <select v-model.number="row.dayOfWeek" class="form-input">
                      <option v-for="day in weekOptions" :key="day.value" :value="day.value">{{ day.label }}</option>
                    </select>
                    <input v-model="row.openTime" class="form-input" type="time" />
                    <input v-model="row.closeTime" class="form-input" type="time" />
                    <button class="icon-text danger" type="button" @click="removeScheduleRow(index)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18M8 6V4h8v2M10 11v6M14 11v6M6 6l1 14h10l1-14" />
                      </svg>
                      删除
                    </button>
                  </div>
                </section>

                <section class="form-section">
                  <div class="section-title-row">
                    <h4>临时关闭时间</h4>
                    <button class="small-btn" type="button" @click="addCloseRow">添加</button>
                  </div>
                  <div v-if="closeRows.length === 0" class="muted-text">暂无临时关闭安排</div>
                  <div v-for="(row, index) in closeRows" :key="row.clientId" class="close-editor-row">
                    <select v-model.number="row.closeType" class="form-input">
                      <option v-for="type in closeTypeOptions" :key="type.value" :value="type.value">{{ type.label }}</option>
                    </select>
                    <input v-model="row.startTime" class="form-input" type="datetime-local" />
                    <input v-model="row.endTime" class="form-input" type="datetime-local" />
                    <input v-model.trim="row.reason" class="form-input" maxlength="255" placeholder="原因" />
                    <button class="icon-text danger" type="button" @click="removeCloseRow(index)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18M8 6V4h8v2M10 11v6M14 11v6M6 6l1 14h10l1-14" />
                      </svg>
                      删除
                    </button>
                  </div>
                </section>

                <section v-if="editorMode === 'edit'" class="form-section">
                  <h4>状态转换</h4>
                  <div v-if="transitionLoading" class="muted-text">加载状态转换中...</div>
                  <template v-else>
                    <div class="current-status-line" v-if="editingLab">
                      当前状态：
                      <span>{{ runningStatusLabel(editingLab.runningStatus) }}</span>
                      <span>{{ openStatusLabel(editingLab.openStatus) }}</span>
                    </div>
                    <label class="block-label">
                      <span>状态变更原因</span>
                      <input v-model.trim="statusReason" class="form-input" maxlength="255" placeholder="例如：设备维保 / 节假日临时关闭" />
                    </label>
                    <div v-if="statusTransitions.length === 0" class="muted-text">当前状态无可用转换</div>
                    <div v-else class="transition-list">
                      <button
                        v-for="option in statusTransitions"
                        :key="option.runningStatus + '-' + option.openStatus"
                        type="button"
                        :disabled="statusChanging"
                        @click="changeStatus(option)"
                      >
                        <strong>{{ option.actionLabel }}</strong>
                        <span>{{ runningStatusLabel(option.runningStatus) }} / {{ openStatusLabel(option.openStatus) }}</span>
                      </button>
                    </div>
                  </template>
                </section>
              </template>
            </div>

            <footer class="modal-footer">
              <span v-if="saveError" class="save-error">{{ saveError }}</span>
              <button class="ghost-btn" type="button" @click="closeEditor">取消</button>
              <button class="primary-btn" type="button" :disabled="!canSubmit || saving" @click="submitEditor">
                {{ saving ? '保存中...' : '保存' }}
              </button>
            </footer>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createLab,
  createLabCloseSchedule,
  deleteLab,
  deleteLabCloseSchedule,
  deleteLabSchedule,
  getLabCloseSchedules,
  getLabCurrentUsage,
  getLabDetail,
  getLabFilterOptions,
  getLabPage,
  getLabStats,
  getLabStatusTransitions,
  getLabTypes,
  saveLabSchedules,
  updateLab,
  updateLabCloseSchedule,
  updateLabStatus,
  type ConflictedReservation,
  type LabCloseScheduleItem,
  type LabCurrentUsage,
  type LabDetail,
  type LabFilterOptions,
  type LabListItem,
  type LabOpenStatus,
  type LabRunningStatus,
  type LabScheduleItem,
  type LabStats,
  type LabStatusTransitionOption,
  type LabTypeItem,
} from '../../utils/labApi'
import { searchRecipients, type RecipientSearchResult } from '../../utils/messageApi'

type EditorMode = 'create' | 'edit'

interface QueryState {
  keyword: string
  labTypeId: number
  campus: string
  building: string
  runningStatus: number
  openStatus: number
  page: number
  pageSize: number
}

interface LabForm {
  id: number
  labName: string
  labTypeId: number
  campus: string
  building: string
  floor: string
  roomNo: string
  capacity: string
  areaM2: string
  dutyUserId: number | null
  description: string
  immediateOpen: boolean
}

interface ScheduleRow {
  clientId: string
  id: number | null
  dayOfWeek: number
  openTime: string
  closeTime: string
}

interface CloseRow {
  clientId: string
  id: number | null
  closeType: number
  startTime: string
  endTime: string
  reason: string
}

const runningStatusOptions: Array<{ value: LabRunningStatus; label: string }> = [
  { value: 1, label: '空闲' },
  { value: 2, label: '使用中' },
  { value: 3, label: '维护中' },
  { value: 4, label: '停用中' },
]

const openStatusOptions: Array<{ value: LabOpenStatus; label: string }> = [
  { value: 1, label: '开放中' },
  { value: 2, label: '临时关闭' },
  { value: 3, label: '永久关闭' },
]

const weekOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

const closeTypeOptions = [
  { value: 1, label: '节假日' },
  { value: 2, label: '设备维保' },
  { value: 3, label: '临时关闭' },
  { value: 4, label: '其他' },
]

const labs = ref<LabListItem[]>([])
const total = ref(0)
const loading = ref(false)
const pageError = ref('')
const labTypes = ref<LabTypeItem[]>([])
const stats = ref<LabStats>({
  total: 0,
  runningStatusCounts: {},
  openStatusCounts: {},
})
const filterOptions = ref<LabFilterOptions>({
  campuses: [],
  buildings: [],
  buildingByCampus: {},
})

const query = reactive<QueryState>({
  keyword: '',
  labTypeId: 0,
  campus: '',
  building: '',
  runningStatus: 0,
  openStatus: 0,
  page: 1,
  pageSize: 10,
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailLab = ref<LabDetail | null>(null)
const detailCloseSchedules = ref<LabCloseScheduleItem[]>([])
const currentUsages = ref<LabCurrentUsage[]>([])
const currentUsageLoading = ref(false)

const editorVisible = ref(false)
const editorMode = ref<EditorMode>('create')
const editorLoading = ref(false)
const saving = ref(false)
const saveError = ref('')
const editingLab = ref<LabDetail | null>(null)
const originalScheduleIds = ref<number[]>([])
const originalCloseIds = ref<number[]>([])
const scheduleRows = ref<ScheduleRow[]>([])
const closeRows = ref<CloseRow[]>([])

const form = reactive<LabForm>({
  id: 0,
  labName: '',
  labTypeId: 0,
  campus: '',
  building: '',
  floor: '',
  roomNo: '',
  capacity: '0',
  areaM2: '',
  dutyUserId: null,
  description: '',
  immediateOpen: false,
})

const selectedDutyUser = ref<RecipientSearchResult | null>(null)
const dutyKeyword = ref('')
const dutyResults = ref<RecipientSearchResult[]>([])
const dutyLoading = ref(false)
const dutyDropdownVisible = ref(false)

const transitionLoading = ref(false)
const statusChanging = ref(false)
const statusReason = ref('')
const statusTransitions = ref<LabStatusTransitionOption[]>([])

let keywordTimer: ReturnType<typeof setTimeout> | null = null
let dutyTimer: ReturnType<typeof setTimeout> | null = null

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)))

const availableBuildings = computed(() => {
  if (query.campus && filterOptions.value.buildingByCampus[query.campus]) {
    return filterOptions.value.buildingByCampus[query.campus]
  }
  return filterOptions.value.buildings
})

const statCards = computed(() => [
  { key: 'total', label: '实验室总数', value: stats.value.total ?? 0 },
  ...runningStatusOptions.map(item => ({
    key: `running-${item.value}`,
    label: item.label,
    value: stats.value.runningStatusCounts?.[item.value] ?? 0,
  })),
  ...openStatusOptions.map(item => ({
    key: `open-${item.value}`,
    label: item.label,
    value: stats.value.openStatusCounts?.[item.value] ?? 0,
  })),
])

const canSubmit = computed(() => {
  return form.labName.trim().length > 0 && form.labTypeId > 0 && !saving.value
})

onMounted(async () => {
  await Promise.all([loadStaticOptions(), loadStats(), loadPage()])
})

async function loadStaticOptions() {
  try {
    const [types, options] = await Promise.all([getLabTypes(), getLabFilterOptions()])
    labTypes.value = types
    filterOptions.value = options
  } catch (error: any) {
    pageError.value = error?.message || '加载筛选条件失败'
  }
}

async function loadStats() {
  try {
    stats.value = await getLabStats()
  } catch (error: any) {
    pageError.value = error?.message || '加载实验室统计失败'
  }
}

async function loadPage() {
  loading.value = true
  pageError.value = ''
  try {
    const page = await getLabPage({
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      labTypeId: query.labTypeId || undefined,
      campus: query.campus || undefined,
      building: query.building || undefined,
      runningStatus: query.runningStatus ? query.runningStatus as LabRunningStatus : undefined,
      openStatus: query.openStatus ? query.openStatus as LabOpenStatus : undefined,
      sortField: 'created_at',
      sortOrder: 'desc',
    })
    labs.value = page.records || []
    total.value = Number(page.total || 0)
  } catch (error: any) {
    labs.value = []
    total.value = 0
    pageError.value = error?.message || '加载实验室列表失败'
  } finally {
    loading.value = false
  }
}

function onKeywordInput() {
  if (keywordTimer) window.clearTimeout(keywordTimer)
  keywordTimer = window.setTimeout(() => {
    void reloadFromFirstPage()
  }, 320)
}

async function reloadFromFirstPage() {
  query.page = 1
  await loadPage()
}

async function onCampusChange() {
  if (query.building && !availableBuildings.value.includes(query.building)) {
    query.building = ''
  }
  await reloadFromFirstPage()
}

async function resetFilters() {
  query.keyword = ''
  query.labTypeId = 0
  query.campus = ''
  query.building = ''
  query.runningStatus = 0
  query.openStatus = 0
  await reloadFromFirstPage()
}

async function changePage(page: number) {
  query.page = page
  await loadPage()
}

async function openDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detailLab.value = null
  detailCloseSchedules.value = []
  currentUsages.value = []
  try {
    const lab = await getLabDetail(id)
    detailLab.value = lab
    const closeSchedulesPromise = getLabCloseSchedules(id)
    const usagePromise = lab.runningStatus === 2 ? loadCurrentUsage(id) : Promise.resolve()
    detailCloseSchedules.value = await closeSchedulesPromise
    await usagePromise
  } catch (error: any) {
    alert(error?.message || '加载实验室详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function loadCurrentUsage(id: number) {
  currentUsageLoading.value = true
  try {
    currentUsages.value = await getLabCurrentUsage(id)
  } catch (error: any) {
    alert(error?.message || '加载当前使用信息失败')
  } finally {
    currentUsageLoading.value = false
  }
}

function openCreateDialog() {
  editorMode.value = 'create'
  editorVisible.value = true
  editorLoading.value = false
  saveError.value = ''
  editingLab.value = null
  resetForm()
  scheduleRows.value = []
  closeRows.value = []
  originalScheduleIds.value = []
  originalCloseIds.value = []
  statusTransitions.value = []
  statusReason.value = ''
}

async function openEditDialog(id: number) {
  editorMode.value = 'edit'
  editorVisible.value = true
  editorLoading.value = true
  saveError.value = ''
  try {
    const [lab, closeSchedules] = await Promise.all([getLabDetail(id), getLabCloseSchedules(id)])
    editingLab.value = lab
    fillForm(lab)
    scheduleRows.value = lab.weeklySchedules.map(toScheduleRow)
    closeRows.value = closeSchedules.map(toCloseRow)
    originalScheduleIds.value = lab.weeklySchedules.map(item => item.id)
    originalCloseIds.value = closeSchedules.map(item => item.id)
    await loadTransitions(id)
  } catch (error: any) {
    alert(error?.message || '加载编辑数据失败')
    editorVisible.value = false
  } finally {
    editorLoading.value = false
  }
}

function closeEditor() {
  editorVisible.value = false
}

function resetForm() {
  form.id = 0
  form.labName = ''
  form.labTypeId = 0
  form.campus = ''
  form.building = ''
  form.floor = ''
  form.roomNo = ''
  form.capacity = '0'
  form.areaM2 = ''
  form.dutyUserId = null
  form.description = ''
  form.immediateOpen = false
  selectedDutyUser.value = null
  dutyKeyword.value = ''
  dutyResults.value = []
  dutyDropdownVisible.value = false
}

function fillForm(lab: LabDetail) {
  form.id = lab.id
  form.labName = lab.labName
  form.labTypeId = lab.labTypeId
  form.campus = lab.campus || ''
  form.building = lab.building || ''
  form.floor = lab.floor == null ? '' : String(lab.floor)
  form.roomNo = lab.roomNo || ''
  form.capacity = String(lab.capacity ?? 0)
  form.areaM2 = lab.areaM2 == null ? '' : String(lab.areaM2)
  form.dutyUserId = lab.dutyUserId
  form.description = lab.description || ''
  form.immediateOpen = false
  selectedDutyUser.value = lab.dutyUserId
    ? { userId: lab.dutyUserId, personName: lab.dutyUserName || `用户#${lab.dutyUserId}`, username: '', personNo: '', userType: 0 }
    : null
}

async function submitEditor() {
  if (!canSubmit.value) return
  saving.value = true
  saveError.value = ''
  try {
    if (editorMode.value === 'create') {
      const created = await createLab(buildLabPayload())
      await saveSchedules(created.id)
      await saveCloseSchedules(created.id)
      if (form.immediateOpen) {
        await updateLabStatus(created.id, {
          runningStatus: 1,
          openStatus: 1,
          changeReason: '新增实验室后立即开放',
        })
      }
    } else {
      await updateLab({ id: form.id, ...buildLabPayload() })
      await saveSchedules(form.id)
      await saveCloseSchedules(form.id)
    }
    editorVisible.value = false
    await Promise.all([loadStats(), loadStaticOptions(), loadPage()])
  } catch (error: any) {
    saveError.value = error?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

function buildLabPayload() {
  return {
    labName: form.labName.trim(),
    labTypeId: form.labTypeId,
    campus: optionalText(form.campus),
    building: optionalText(form.building),
    floor: optionalNumber(form.floor),
    roomNo: optionalText(form.roomNo),
    capacity: numberOrZero(form.capacity),
    areaM2: optionalNumber(form.areaM2),
    dutyUserId: form.dutyUserId || undefined,
    description: optionalText(form.description),
  }
}

async function saveSchedules(labId: number) {
  const validRows = scheduleRows.value.filter(row => row.dayOfWeek && row.openTime && row.closeTime)
  const currentIds = new Set(validRows.map(row => row.id).filter((id): id is number => id != null))
  for (const id of originalScheduleIds.value) {
    if (!currentIds.has(id)) {
      await runScheduleDelete(labId, id, false)
    }
  }
  if (validRows.length > 0) {
    await runScheduleSave(labId, false)
  }
}

async function runScheduleSave(labId: number, force: boolean) {
  const result = await saveLabSchedules(labId, {
    schedules: scheduleRows.value
      .filter(row => row.dayOfWeek && row.openTime && row.closeTime)
      .map(row => ({ dayOfWeek: row.dayOfWeek, openTime: row.openTime, closeTime: row.closeTime })),
    forceCancelConflictedReservations: force,
  })
  if (!result.applied && confirmConflict('开放时间调整会影响已有预约，是否取消这些预约并继续？', result.conflictedReservations)) {
    await runScheduleSave(labId, true)
  } else if (!result.applied) {
    throw new Error('开放时间调整已取消')
  }
}

async function runScheduleDelete(labId: number, scheduleId: number, force: boolean) {
  const result = await deleteLabSchedule(labId, scheduleId, force)
  if (!result.applied && confirmConflict('删除开放时间会影响已有预约，是否取消这些预约并继续？', result.conflictedReservations)) {
    await runScheduleDelete(labId, scheduleId, true)
  } else if (!result.applied) {
    throw new Error('开放时间删除已取消')
  }
}

async function saveCloseSchedules(labId: number) {
  const validRows = closeRows.value.filter(row => row.closeType && row.startTime && row.endTime)
  const currentIds = new Set(validRows.map(row => row.id).filter((id): id is number => id != null))
  for (const id of originalCloseIds.value) {
    if (!currentIds.has(id)) {
      await deleteLabCloseSchedule(labId, id)
    }
  }
  for (const row of validRows) {
    if (row.id) {
      await runCloseScheduleUpdate(labId, row, false)
    } else {
      await runCloseScheduleCreate(labId, row, false)
    }
  }
}

async function runCloseScheduleCreate(labId: number, row: CloseRow, force: boolean) {
  const result = await createLabCloseSchedule(labId, closePayload(row, force))
  if (!result.applied && confirmConflict('临时关闭时间会影响已有预约，是否取消这些预约并继续？', result.conflictedReservations)) {
    await runCloseScheduleCreate(labId, row, true)
  } else if (!result.applied) {
    throw new Error('临时关闭时间保存已取消')
  }
}

async function runCloseScheduleUpdate(labId: number, row: CloseRow, force: boolean) {
  if (!row.id) return
  const result = await updateLabCloseSchedule(labId, row.id, closePayload(row, force))
  if (!result.applied && confirmConflict('临时关闭时间会影响已有预约，是否取消这些预约并继续？', result.conflictedReservations)) {
    await runCloseScheduleUpdate(labId, row, true)
  } else if (!result.applied) {
    throw new Error('临时关闭时间保存已取消')
  }
}

function closePayload(row: CloseRow, force: boolean) {
  return {
    closeType: row.closeType,
    startTime: row.startTime,
    endTime: row.endTime,
    reason: optionalText(row.reason) || undefined,
    forceCancelConflictedReservations: force,
  }
}

async function loadTransitions(id: number) {
  transitionLoading.value = true
  try {
    const response = await getLabStatusTransitions(id)
    statusTransitions.value = response.options || []
  } catch (error: any) {
    alert(error?.message || '加载状态转换失败')
  } finally {
    transitionLoading.value = false
  }
}

async function changeStatus(option: LabStatusTransitionOption) {
  if (!editingLab.value) return
  if (!statusReason.value.trim()) {
    alert('请先填写状态变更原因')
    return
  }
  if (option.requiresPermanentCloseConfirm && !window.confirm('永久关闭后不能再次变更状态，确定继续吗？')) {
    return
  }
  statusChanging.value = true
  try {
    const response = await updateLabStatus(editingLab.value.id, {
      runningStatus: option.runningStatus,
      openStatus: option.openStatus,
      changeReason: statusReason.value.trim(),
      confirmPermanentClose: option.requiresPermanentCloseConfirm,
    })
    if (!response.applied) {
      const confirmed = confirmConflict(response.message || '状态变更需要确认，是否继续？', response.conflictedReservations)
      if (!confirmed) return
      const retry = await updateLabStatus(editingLab.value.id, {
        runningStatus: option.runningStatus,
        openStatus: option.openStatus,
        changeReason: statusReason.value.trim(),
        forceFinishCurrentUsage: option.mayNeedForceFinishCurrentUsage,
        forceCancelFutureReservations: option.mayNeedForceCancelFutureReservations,
        confirmPermanentClose: option.requiresPermanentCloseConfirm,
      })
      if (!retry.applied) {
        throw new Error(retry.message || '状态变更未生效')
      }
      fillForm(retry.lab)
      editingLab.value = retry.lab
    } else {
      fillForm(response.lab)
      editingLab.value = response.lab
    }
    statusReason.value = ''
    await Promise.all([loadTransitions(editingLab.value.id), loadStats(), loadPage()])
  } catch (error: any) {
    alert(error?.message || '状态变更失败')
  } finally {
    statusChanging.value = false
  }
}

async function deleteOneLab(lab: LabListItem) {
  if (!window.confirm(`确定删除「${lab.labName}」吗？相关未来预约会被自动取消。`)) return
  try {
    await deleteLab(lab.id)
    await Promise.all([loadStats(), loadStaticOptions(), loadPage()])
  } catch (error: any) {
    alert(error?.message || '删除实验室失败')
  }
}

function addScheduleRow() {
  const used = new Set(scheduleRows.value.map(row => row.dayOfWeek))
  const day = weekOptions.find(item => !used.has(item.value))?.value ?? 1
  scheduleRows.value.push({ clientId: makeClientId(), id: null, dayOfWeek: day, openTime: '08:00', closeTime: '18:00' })
}

function removeScheduleRow(index: number) {
  scheduleRows.value.splice(index, 1)
}

function addCloseRow() {
  closeRows.value.push({
    clientId: makeClientId(),
    id: null,
    closeType: 3,
    startTime: '',
    endTime: '',
    reason: '',
  })
}

function removeCloseRow(index: number) {
  closeRows.value.splice(index, 1)
}

function toScheduleRow(item: LabScheduleItem): ScheduleRow {
  return {
    clientId: makeClientId(),
    id: item.id,
    dayOfWeek: item.dayOfWeek,
    openTime: formatTime(item.openTime),
    closeTime: formatTime(item.closeTime),
  }
}

function toCloseRow(item: LabCloseScheduleItem): CloseRow {
  return {
    clientId: makeClientId(),
    id: item.id,
    closeType: item.closeType,
    startTime: toDateTimeLocal(item.startTime),
    endTime: toDateTimeLocal(item.endTime),
    reason: item.reason || '',
  }
}

async function onDutyInput() {
  if (dutyTimer) window.clearTimeout(dutyTimer)
  const keyword = dutyKeyword.value.trim()
  if (!keyword) {
    dutyResults.value = []
    dutyDropdownVisible.value = false
    return
  }
  dutyDropdownVisible.value = true
  dutyTimer = window.setTimeout(async () => {
    dutyLoading.value = true
    try {
      dutyResults.value = await searchRecipients(keyword, 12)
    } catch {
      dutyResults.value = []
    } finally {
      dutyLoading.value = false
    }
  }, 260)
}

function selectDutyUser(person: RecipientSearchResult) {
  selectedDutyUser.value = person
  form.dutyUserId = person.userId
  dutyKeyword.value = ''
  dutyResults.value = []
  dutyDropdownVisible.value = false
}

function clearDutyUser() {
  selectedDutyUser.value = null
  form.dutyUserId = null
}

function hideDutyDropdown() {
  window.setTimeout(() => {
    dutyDropdownVisible.value = false
  }, 180)
}

function confirmConflict(message: string, conflicts: ConflictedReservation[]): boolean {
  const detail = conflicts?.length
    ? '\n\n受影响预约：\n' + conflicts
      .map(item => `${item.reservationNo} ${item.reservationDate} ${formatTime(item.startTime)}-${formatTime(item.endTime)} ${item.reservationStatusDesc}`)
      .join('\n')
    : ''
  return window.confirm(message + detail)
}

function formatLocation(lab: Pick<LabListItem, 'campus' | 'building' | 'floor' | 'roomNo'>): string {
  return `${lab.campus || '-'}-${lab.building || '-'}-${lab.floor ?? '-'}F-${lab.roomNo || '-'}`
}

function formatArea(value: number | null | undefined): string {
  return value == null ? '-' : `${value} m²`
}

function runningStatusLabel(status: number): string {
  return runningStatusOptions.find(item => item.value === status)?.label || '-'
}

function openStatusLabel(status: number): string {
  return openStatusOptions.find(item => item.value === status)?.label || '-'
}

function runningStatusClass(status: number): string {
  if (status === 1) return 'status-idle'
  if (status === 2) return 'status-use'
  if (status === 3) return 'status-maintain'
  return 'status-disabled'
}

function openStatusClass(status: number): string {
  if (status === 1) return 'status-open'
  if (status === 2) return 'status-temp'
  return 'status-permanent'
}

function weekLabel(day: number): string {
  return weekOptions.find(item => item.value === day)?.label || `周${day}`
}

function formatTime(value: string | null | undefined): string {
  return value ? value.slice(0, 5) : ''
}

function formatDateTime(value: string | null | undefined): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function toDateTimeLocal(value: string | null | undefined): string {
  return value ? value.slice(0, 16) : ''
}

function optionalText(value: string): string | undefined {
  const normalized = value.trim()
  return normalized || undefined
}

function optionalNumber(value: string): number | undefined {
  if (value === '' || value == null) return undefined
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

function numberOrZero(value: string): number {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function makeClientId(): string {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}
</script>

<style scoped>
.lm-page {
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

.overview-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(96px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.stat-card {
  padding: 14px 14px 13px;
  background: #fff;
  border: 1px solid #e7eaf0;
  border-radius: 8px;
}

.stat-card strong {
  display: block;
  margin-top: 7px;
  font-size: 24px;
  line-height: 1;
}

.stat-label {
  font-size: 12px;
  color: #697386;
}

.filter-panel,
.table-section {
  background: #fff;
  border: 1px solid #e6e9ef;
  border-radius: 8px;
}

.filter-panel {
  display: grid;
  grid-template-columns: minmax(220px, 1.5fr) repeat(5, minmax(120px, 1fr)) auto;
  gap: 10px;
  padding: 12px;
  margin-bottom: 14px;
}

.keyword-box {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid #d8dde7;
  border-radius: 7px;
  background: #fff;
  color: #7b8495;
}

.keyword-box input {
  width: 100%;
  height: 36px;
  border: 0;
  outline: 0;
  font-size: 13px;
  color: #1f2937;
  background: transparent;
}

.filter-select,
.form-input {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d8dde7;
  border-radius: 7px;
  background: #fff;
  color: #1f2937;
  font-size: 13px;
  font-family: inherit;
  outline: none;
}

.filter-select {
  height: 38px;
  padding: 0 10px;
}

.form-input {
  min-height: 38px;
  padding: 8px 10px;
}

.form-input:focus,
.filter-select:focus,
.keyword-box:focus-within {
  border-color: #2563eb;
}

.table-section {
  padding: 0;
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #eef1f5;
  color: #6b7280;
  font-size: 13px;
}

.table-wrap {
  overflow-x: auto;
}

.lab-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
}

.lab-table th,
.lab-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #eef1f5;
  text-align: left;
  white-space: nowrap;
  font-size: 13px;
}

.lab-table th {
  background: #fafbfc;
  color: #5e6778;
  font-weight: 600;
}

.strong-cell {
  color: #111827;
  font-weight: 600;
}

.type-pill {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 5px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 12px;
  font-weight: 600;
}

.status-pair {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 600;
}

.status-idle,
.status-open {
  background: #e8f7ef;
  color: #137a43;
}

.status-use {
  background: #e8f0ff;
  color: #1d4ed8;
}

.status-maintain,
.status-temp {
  background: #fff7df;
  color: #9a5b00;
}

.status-disabled,
.status-permanent {
  background: #f2f4f7;
  color: #667085;
}

.row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.primary-btn,
.ghost-btn,
.text-btn,
.link-btn,
.small-btn,
.icon-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 0;
  border-radius: 7px;
  font-family: inherit;
  cursor: pointer;
  transition: background .15s, border-color .15s, color .15s;
}

.primary-btn {
  height: 38px;
  padding: 0 15px;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.primary-btn:disabled,
.ghost-btn:disabled,
.text-btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.ghost-btn {
  height: 38px;
  padding: 0 13px;
  border: 1px solid #d8dde7;
  background: #fff;
  color: #42526b;
  font-size: 13px;
}

.text-btn {
  background: transparent;
  color: #2563eb;
  font-size: 13px;
}

.link-btn {
  background: transparent;
  color: #2563eb;
  font-size: 13px;
}

.danger {
  color: #dc2626;
}

.empty-cell {
  height: 120px;
  color: #9aa3b2;
  text-align: center !important;
}

.state-box {
  padding: 36px 16px;
  color: #7b8495;
  text-align: center;
}

.state-box.error {
  color: #b42318;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 14px;
}

.pagination button {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d8dde7;
  border-radius: 6px;
  background: #fff;
  color: #42526b;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: .45;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, .38);
}

.modal-panel {
  width: 100%;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 22px 70px rgba(15, 23, 42, .22);
}

.detail-panel {
  max-width: 760px;
}

.editor-panel {
  max-width: 1040px;
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid #eef1f5;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
}

.modal-header p {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: 7px;
  background: #f3f5f8;
  color: #667085;
  cursor: pointer;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 22px;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px 18px;
  border-top: 1px solid #eef1f5;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.detail-grid div,
.usage-card div {
  padding: 12px;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  background: #fafbfc;
}

.detail-grid span,
.usage-card span {
  display: block;
  margin-bottom: 5px;
  color: #697386;
  font-size: 12px;
}

.detail-grid strong,
.usage-card strong {
  font-size: 13px;
  color: #172033;
}

.detail-status {
  margin-top: 12px;
}

.info-section {
  margin-top: 18px;
}

.info-section h4,
.form-section h4 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
}

.info-section p {
  margin: 0;
  line-height: 1.7;
  color: #3f4a5f;
  font-size: 13px;
}

.usage-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.schedule-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.schedule-list span {
  padding: 6px 9px;
  border-radius: 6px;
  background: #f3f6fb;
  color: #3f4a5f;
  font-size: 12px;
}

.close-item {
  display: grid;
  grid-template-columns: 90px 1fr 180px;
  gap: 10px;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #eef1f5;
  font-size: 13px;
}

.close-item em {
  color: #697386;
  font-style: normal;
}

.form-section {
  padding: 16px;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  margin-bottom: 14px;
}

.compact-section {
  padding: 12px 16px;
}

.form-grid {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
}

.form-grid.two {
  grid-template-columns: repeat(2, 1fr);
}

.form-grid.three {
  grid-template-columns: repeat(3, 1fr);
}

.form-grid.four {
  grid-template-columns: repeat(4, 1fr);
}

label span,
.field-label {
  display: block;
  margin-bottom: 5px;
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
}

.block-label {
  display: block;
}

.check-line {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #334155;
  font-size: 13px;
}

.check-line span {
  margin: 0;
  font-size: 13px;
}

.person-picker,
.person-search {
  position: relative;
}

.selected-person {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 36px;
  padding: 0 10px;
  margin-bottom: 8px;
  border-radius: 7px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 13px;
  font-weight: 600;
}

.selected-person button {
  border: 0;
  background: transparent;
  color: #4f46e5;
  cursor: pointer;
}

.person-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 10;
  max-height: 220px;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid #e1e5ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, .14);
}

.person-dropdown button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.person-dropdown button:hover {
  background: #f3f6fb;
}

.person-dropdown span {
  margin: 0;
  color: #697386;
  font-size: 12px;
  font-weight: 400;
}

.dropdown-state,
.muted-text {
  color: #8a94a6;
  font-size: 13px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.small-btn {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #d8dde7;
  background: #fff;
  color: #2563eb;
  font-size: 12px;
}

.inline-row {
  display: grid;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.schedule-row {
  grid-template-columns: 130px 1fr 1fr 92px;
}

.close-editor-row {
  display: grid;
  grid-template-columns: 130px 1.1fr 1.1fr 1fr 92px;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

.icon-text {
  height: 36px;
  padding: 0 10px;
  border: 1px solid #fee2e2;
  background: #fff;
  color: #dc2626;
  font-size: 12px;
}

.current-status-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #4b5563;
  font-size: 13px;
}

.current-status-line span {
  margin: 0;
  padding: 4px 8px;
  border-radius: 5px;
  background: #f3f6fb;
}

.transition-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.transition-list button {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 12px;
  border: 1px solid #d8dde7;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.transition-list button:hover:not(:disabled) {
  border-color: #2563eb;
}

.transition-list strong {
  color: #172033;
  font-size: 13px;
}

.transition-list span {
  margin: 0;
  color: #697386;
  font-size: 12px;
  font-weight: 400;
}

.save-error {
  margin-right: auto;
  color: #b42318;
  font-size: 13px;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity .16s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1180px) {
  .overview-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .filter-panel {
    grid-template-columns: repeat(3, 1fr);
  }

  .keyword-box {
    grid-column: span 3;
  }
}

@media (max-width: 760px) {
  .lm-page {
    padding: 16px;
  }

  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .overview-grid,
  .filter-panel,
  .detail-grid,
  .usage-card,
  .form-grid.two,
  .form-grid.three,
  .form-grid.four,
  .transition-list {
    grid-template-columns: 1fr;
  }

  .keyword-box {
    grid-column: span 1;
  }

  .schedule-row,
  .close-editor-row,
  .close-item {
    grid-template-columns: 1fr;
  }

  .modal-overlay {
    padding: 12px;
  }
}
</style>
