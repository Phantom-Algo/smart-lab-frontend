<template>
  <div class="la-page">
    <header class="page-header">
      <div>
        <h2>预约审核</h2>
        <p>审批教师与学生提交的实验室预约</p>
      </div>
      <button class="text-btn header-action" type="button" :disabled="loading" @click="loadData">
        <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 1 1-2.64-6.36" />
          <path d="M21 3v6h-6" />
        </svg>
        刷新
      </button>
    </header>

    <section class="overview-grid">
      <article class="stat-card">
        <span class="stat-label">待审核</span>
        <strong class="stat-blue">{{ pendingCount }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">今日通过</span>
        <strong class="stat-green">{{ todayApproved }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">本月预约</span>
        <strong>{{ monthReservations }}</strong>
      </article>
      <article class="stat-card">
        <span class="stat-label">通过率</span>
        <strong :class="approvalRate >= 80 ? 'stat-green' : 'stat-amber'">{{ approvalRate }}%</strong>
      </article>
    </section>

    <section class="filter-panel">
      <div class="filter-tabs">
        <button
          v-for="t in statusTabs"
          :key="t.key"
          type="button"
          class="filter-tab"
          :class="{ active: activeStatus === t.key }"
          @click="changeStatus(t.key)"
        >
          {{ t.label }}
          <span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
        </button>
      </div>
      <div class="filter-actions">
        <div class="keyword-box">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <input v-model="searchNo" placeholder="搜索预约单号" @keyup.enter="submitSearch" />
        </div>
        <button class="ghost-btn" type="button" :disabled="loading" @click="submitSearch">查询</button>
      </div>
    </section>

    <section class="table-section">
      <div class="table-toolbar">
        <span>共 {{ totalRecordCount }} 条预约</span>
        <span v-if="loading" class="loading-text">加载中...</span>
      </div>

      <div v-if="loadError" class="state-box error">{{ loadError }}</div>
      <div v-else class="table-wrap">
        <table class="approval-table">
          <thead>
            <tr>
              <th>状态</th>
              <th>预约单号</th>
              <th>实验室</th>
              <th>申请人</th>
              <th>预约时间</th>
              <th>人数</th>
              <th>用途</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="empty-cell">加载中...</td>
            </tr>
            <tr v-else-if="pagedList.length === 0">
              <td colspan="8" class="empty-cell">{{ emptyText }}</td>
            </tr>
            <template v-else>
              <tr v-for="r in pagedList" :key="r.id">
                <td>
                  <span class="status-badge" :class="'badge-' + r.status">{{ r.statusName }}</span>
                </td>
                <td>
                  <div class="stack-cell">
                    <strong>{{ r.applyNo }}</strong>
                    <span>{{ r.createdAt }}</span>
                  </div>
                </td>
                <td>
                  <div class="stack-cell lab-cell">
                    <strong>{{ r.labName }}</strong>
                    <span>{{ r.labCode || '-' }}</span>
                  </div>
                </td>
                <td>
                  <div class="applicant-cell">
                    <span>{{ r.applicantName }}</span>
                    <span class="person-type" :class="r.applicantType === 1 ? 'type-stu' : 'type-tea'">{{ r.applicantTypeName }}</span>
                  </div>
                </td>
                <td>
                  <div class="stack-cell">
                    <strong>{{ r.reserveDate }}</strong>
                    <span>{{ r.timeSlot }}</span>
                  </div>
                </td>
                <td>{{ r.peopleCount }} 人</td>
                <td>
                  <div class="purpose-cell">
                    <span class="purpose-tag" :class="'purpose-' + r.purposeTag">{{ r.purposeTagName }}</span>
                    <span class="purpose-text">{{ r.purpose || '-' }}</span>
                    <span v-if="r.status === 'rejected' && r.rejectReason" class="reject-reason">驳回原因：{{ r.rejectReason }}</span>
                  </div>
                </td>
                <td>
                  <div class="row-actions">
                    <template v-if="r.status === 'pending'">
                      <button class="link-btn danger" type="button" :disabled="actionLoading" @click="openReject(r)">驳回</button>
                      <button class="link-btn" type="button" :disabled="actionLoading" @click="openApprove(r)">通过</button>
                    </template>
                    <template v-else-if="r.status === 'approved' && isMockMode">
                      <button class="link-btn danger" type="button" @click="revokeApproval(r)">撤销</button>
                    </template>
                    <span v-else class="final-text">{{ r.statusName }}</span>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </section>

    <div class="pagination" v-if="totalRecordCount > pageSize">
      <button :disabled="pageNum <= 1 || loading" @click="goPage(pageNum - 1)">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages || loading" @click="goPage(pageNum + 1)">下一页</button>
    </div>

    <!-- 审核通过弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showApprove" class="modal-overlay" @click.self="showApprove = false">
          <div class="modal-panel">
            <div class="modal-header">
              <div>
                <h3>确认审核通过</h3>
                <p>通过后申请人可按预约时段使用实验室</p>
              </div>
              <button class="modal-close" type="button" @click="showApprove = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="confirm-row"><span>实验室</span><strong>{{ currentReservation?.labName }}</strong></div>
                <div class="confirm-row"><span>申请人</span><strong>{{ currentReservation?.applicantName }}（{{ currentReservation?.applicantTypeName }}）</strong></div>
                <div class="confirm-row"><span>日期</span><strong>{{ currentReservation?.reserveDate }}</strong></div>
                <div class="confirm-row"><span>时段</span><strong>{{ currentReservation?.timeSlot }}</strong></div>
                <div class="confirm-row"><span>人数</span><strong>{{ currentReservation?.peopleCount }} 人</strong></div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" type="button" @click="showApprove = false">取消</button>
              <button class="modal-btn primary" type="button" @click="doApprove" :disabled="actionLoading">确认通过</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 驳回弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showReject" class="modal-overlay" @click.self="showReject = false">
          <div class="modal-panel">
            <div class="modal-header">
              <div>
                <h3>驳回预约</h3>
                <p>驳回原因会同步给申请人</p>
              </div>
              <button class="modal-close" type="button" @click="showReject = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>驳回原因 <span class="required">*</span></label>
                <textarea v-model="rejectForm.reason" class="form-input form-textarea" rows="3" placeholder="请填写驳回原因，系统将通知申请人..." maxlength="300" />
              </div>
              <div class="quick-reasons">
                <span class="qr-label">快捷填写：</span>
                <button v-for="r in quickReasons" :key="r" class="qr-btn" type="button" @click="rejectForm.reason = r">{{ r }}</button>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" type="button" @click="showReject = false">取消</button>
              <button class="modal-btn danger" type="button" @click="doReject" :disabled="!rejectForm.reason.trim() || actionLoading">确认驳回</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onBeforeUnmount, onMounted } from 'vue'
import { isMockMode } from '../../stores/authStore'
import {
  approveReservation,
  getReservationPage,
  getReservationStats,
  type ReservationItem,
  type ReservationStats,
  type ReservationStatus,
} from '../../utils/labApi'

type StatusTabKey = ReservationStatus | 'all'

const MOCK_RESERVATIONS: ReservationItem[] = [
  { id:1, applyNo:'LAB-RSV-001', labId:1, labName:'B103 物理实验室', labCode:'LAB-2024-B103',
    applicantId:201, applicantName:'李老师', applicantType:2, applicantTypeName:'教师',
    reserveDate:'2026-07-12', timeSlot:'09:00~12:00', peopleCount:15,
    purpose:'物理实验课程教学', purposeTag:'teaching', purposeTagName:'教学',
    status:'pending', statusName:'待审核', createdAt:'2026-07-08 14:30' },
  { id:2, applyNo:'LAB-RSV-002', labId:2, labName:'C202 计算机实验室', labCode:'LAB-2024-C202',
    applicantId:202, applicantName:'王同学', applicantType:1, applicantTypeName:'学生',
    reserveDate:'2026-07-13', timeSlot:'14:00~17:00', peopleCount:30,
    purpose:'算法竞赛集训', purposeTag:'exam', purposeTagName:'考试',
    status:'pending', statusName:'待审核', createdAt:'2026-07-08 16:00' },
  { id:3, applyNo:'LAB-RSV-003', labId:3, labName:'A305 分析化学实验室', labCode:'LAB-2023-A305',
    applicantId:203, applicantName:'张同学', applicantType:1, applicantTypeName:'学生',
    reserveDate:'2026-07-11', timeSlot:'13:00~16:00', peopleCount:10,
    purpose:'课题组实验', purposeTag:'research', purposeTagName:'科研',
    status:'rejected', statusName:'已驳回', createdAt:'2026-07-06 16:00',
    approvedAt:'2026-07-07 10:00', rejectReason:'该时段实验室已被其他班级预约占用，请调整时间后重新提交。' },
  { id:4, applyNo:'LAB-RSV-004', labId:4, labName:'D108 生物培养实验室', labCode:'LAB-2024-D108',
    applicantId:204, applicantName:'周老师', applicantType:2, applicantTypeName:'教师',
    reserveDate:'2026-07-15', timeSlot:'08:00~11:00', peopleCount:20,
    purpose:'生物实验课程', purposeTag:'teaching', purposeTagName:'教学',
    status:'pending', statusName:'待审核', createdAt:'2026-07-08 11:00' },
  { id:5, applyNo:'LAB-RSV-005', labId:5, labName:'G102 环境监测实验室', labCode:'LAB-2024-G102',
    applicantId:205, applicantName:'孙老师', applicantType:2, applicantTypeName:'教师',
    reserveDate:'2026-07-18', timeSlot:'08:00~12:00', peopleCount:8,
    purpose:'环境采样分析', purposeTag:'research', purposeTagName:'科研',
    status:'approved', statusName:'已通过', createdAt:'2026-07-07 08:00',
    approvedAt:'2026-07-07 16:30' },
  { id:6, applyNo:'LAB-RSV-006', labId:2, labName:'C202 计算机实验室', labCode:'LAB-2024-C202',
    applicantId:206, applicantName:'赵同学', applicantType:1, applicantTypeName:'学生',
    reserveDate:'2026-07-10', timeSlot:'09:00~11:00', peopleCount:25,
    purpose:'毕业设计答辩', purposeTag:'other', purposeTagName:'其他',
    status:'cancelled', statusName:'已取消', createdAt:'2026-07-05 09:00' },
]

const reservations = ref<ReservationItem[]>([])
const apiStats = ref<ReservationStats | null>(null)
const apiTotal = ref(0)
const apiPages = ref(1)
const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')

async function loadData() {
  loadError.value = ''
  try {
    if (isMockMode.value) {
      reservations.value = [...MOCK_RESERVATIONS]
      apiStats.value = null
      apiTotal.value = reservations.value.length
      apiPages.value = Math.ceil(reservations.value.length / pageSize) || 1
      return
    }

    loading.value = true
    const [page, stats] = await Promise.all([
      getReservationPage({
        page: pageNum.value,
        pageSize,
        status: activeStatus.value === 'all' ? undefined : activeStatus.value,
        reservationNo: searchNo.value.trim() || undefined,
      }),
      getReservationStats(),
    ])
    reservations.value = page.records
    apiTotal.value = page.total
    apiPages.value = page.pages || 1
    apiStats.value = stats
    if (pageNum.value > apiPages.value) {
      pageNum.value = apiPages.value
      await loadData()
    }
  } catch (e: any) {
    console.error('LabApproval loadData error:', e)
    loadError.value = e.message || '加载预约记录失败'
  } finally {
    loading.value = false
  }
}

function handleRefreshRequest(): void {
  void loadData()
}

onMounted(() => {
  void loadData()
  window.addEventListener('refresh-reservation-list', handleRefreshRequest)
  window.addEventListener('unity-biz-event', handleRefreshRequest)
})

onBeforeUnmount(() => {
  window.removeEventListener('refresh-reservation-list', handleRefreshRequest)
  window.removeEventListener('unity-biz-event', handleRefreshRequest)
})

const activeStatus = ref<StatusTabKey>('all')
const searchNo = ref('')
const pageNum = ref(1)
const pageSize = 6

const statusConfig: Array<{ key: StatusTabKey; label: string }> = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待审核' },
  { key: 'approved', label: '已通过' },
  { key: 'rejected', label: '已驳回' },
  { key: 'cancelled', label: '已取消' },
  { key: 'checkedIn', label: '已签到' },
  { key: 'absent', label: '已爽约' },
]

const statusTabs = computed(() => statusConfig.map(item => ({
  ...item,
  count: item.key === 'all' ? totalReservationCount.value : statusCount(item.key),
})))

const filteredList = computed(() => {
  if (!isMockMode.value) return reservations.value
  let list = reservations.value
  if (activeStatus.value !== 'all') list = list.filter(r => r.status === activeStatus.value)
  if (searchNo.value) list = list.filter(r => r.applyNo.toLowerCase().includes(searchNo.value.toLowerCase()))
  return list
})
const totalPages = computed(() => isMockMode.value ? Math.ceil(filteredList.value.length / pageSize) || 1 : apiPages.value)
const pagedList = computed(() => isMockMode.value ? filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize) : reservations.value)
const totalRecordCount = computed(() => isMockMode.value ? filteredList.value.length : apiTotal.value)
const totalReservationCount = computed(() => {
  if (isMockMode.value) return reservations.value.length
  const counts = apiStats.value?.statusCounts ?? {}
  return Object.values(counts).reduce((sum, value) => sum + (value ?? 0), 0)
})

const pendingCount = computed(() => isMockMode.value ? reservations.value.filter(r => r.status === 'pending').length : (apiStats.value?.pendingCount ?? 0))
const todayApproved = computed(() => {
  if (!isMockMode.value) return apiStats.value?.todayApproved ?? 0
  const today = new Date().toISOString().slice(0, 10)
  return reservations.value.filter(r => r.status === 'approved' && r.approvedAt?.startsWith(today)).length
})
const monthReservations = computed(() => isMockMode.value ? reservations.value.length : (apiStats.value?.monthReservations ?? 0))
const approvalRate = computed(() => {
  if (!isMockMode.value) return apiStats.value?.approvalRate ?? 0
  const approvedStatuses: ReservationStatus[] = ['approved', 'checkedIn', 'absent']
  const approved = reservations.value.filter(r => approvedStatuses.includes(r.status)).length
  const rejected = reservations.value.filter(r => r.status === 'rejected').length
  return approved + rejected === 0 ? 0 : Math.round(approved / (approved + rejected) * 100)
})
const emptyText = computed(() => loading.value ? '预约记录加载中...' : (loadError.value || '暂无预约记录'))

const showApprove = ref(false)
const showReject = ref(false)
const currentReservation = ref<ReservationItem | null>(null)
const rejectForm = reactive({ reason: '' })
const quickReasons = ['该时段已被占用', '人数超出实验室容量', '实验室正在维护中', '预约用途不符合规定']

function statusCount(status: StatusTabKey): number {
  if (status === 'all') return totalReservationCount.value
  if (isMockMode.value) return reservations.value.filter(r => r.status === status).length
  return apiStats.value?.statusCounts[status] ?? 0
}

function changeStatus(status: StatusTabKey): void {
  activeStatus.value = status
  pageNum.value = 1
  void loadData()
}

function submitSearch(): void {
  pageNum.value = 1
  void loadData()
}

function goPage(page: number): void {
  const nextPage = Math.min(Math.max(page, 1), totalPages.value)
  if (nextPage === pageNum.value) return
  pageNum.value = nextPage
  void loadData()
}

function refreshAfterMutation(): void {
  void loadData()
}

function openApprove(r: ReservationItem) { currentReservation.value = r; showApprove.value = true }
function openReject(r: ReservationItem) { currentReservation.value = r; rejectForm.reason = ''; showReject.value = true }
async function doApprove() {
  if (!currentReservation.value) return
  if (isMockMode.value) {
    const r = reservations.value.find(x => x.id === currentReservation.value!.id)
    if (r) { r.status = 'approved'; r.statusName = '已通过'; r.approvedAt = new Date().toISOString().slice(0, 16) }
    showApprove.value = false; return
  }
  try {
    actionLoading.value = true
    await approveReservation({ reservationNo: currentReservation.value.applyNo, action: 'approve' })
    showApprove.value = false
    refreshAfterMutation()
  } catch (e: any) { alert(e.message || '操作失败') }
  finally { actionLoading.value = false }
}
async function doReject() {
  if (!currentReservation.value || !rejectForm.reason.trim()) return
  if (isMockMode.value) {
    const r = reservations.value.find(x => x.id === currentReservation.value!.id)
    if (r) { r.status = 'rejected'; r.statusName = '已驳回'; r.rejectReason = rejectForm.reason.trim() }
    showReject.value = false; return
  }
  try {
    actionLoading.value = true
    await approveReservation({
      reservationNo: currentReservation.value.applyNo,
      action: 'reject',
      rejectReason: rejectForm.reason.trim(),
    })
    showReject.value = false
    refreshAfterMutation()
  } catch (e: any) { alert(e.message || '操作失败') }
  finally { actionLoading.value = false }
}
function revokeApproval(r: ReservationItem) {
  if (confirm(`确定撤销对「${r.labName}」的审批通过吗？`)) { r.status = 'pending'; r.statusName = '待审核'; r.approvedAt = undefined }
}
</script>

<style scoped>
.la-page {
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

.page-header p,
.modal-header p {
  margin: 5px 0 0;
  font-size: 13px;
  color: #6b7280;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
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
  color: #111827;
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

.filter-panel,
.table-section {
  background: #fff;
  border: 1px solid #e6e9ef;
  border-radius: 8px;
}

.filter-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  margin-bottom: 14px;
}

.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: #536072;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
}

.filter-tab:hover {
  background: #f6f8fb;
  color: #1f2937;
}

.filter-tab.active {
  border-color: #c7d7fe;
  background: #eef4ff;
  color: #1d4ed8;
  font-weight: 600;
}

.tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: #eef1f5;
  color: #697386;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.filter-tab.active .tab-count {
  background: #dbeafe;
  color: #1d4ed8;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.keyword-box {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 240px;
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

.keyword-box:focus-within,
.form-input:focus {
  border-color: #2563eb;
}

.table-section {
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

.loading-text {
  color: #2563eb;
}

.table-wrap {
  overflow-x: auto;
}

.approval-table {
  width: 100%;
  min-width: 1040px;
  border-collapse: collapse;
}

.approval-table th,
.approval-table td {
  padding: 13px 14px;
  border-bottom: 1px solid #eef1f5;
  text-align: left;
  vertical-align: top;
  font-size: 13px;
}

.approval-table th {
  background: #fafbfc;
  color: #5e6778;
  font-weight: 600;
  white-space: nowrap;
}

.stack-cell,
.purpose-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.stack-cell strong,
.lab-cell strong {
  color: #111827;
  font-weight: 600;
}

.stack-cell span,
.purpose-text {
  color: #7b8495;
  font-size: 12px;
}

.applicant-cell,
.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.status-badge,
.person-type,
.purpose-tag {
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

.badge-approved,
.badge-checkedIn {
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

.type-stu {
  background: #e8f7ef;
  color: #137a43;
}

.type-tea {
  background: #eef2ff;
  color: #3730a3;
}

.purpose-teaching {
  background: #e8f7ef;
  color: #137a43;
}

.purpose-research {
  background: #eef2ff;
  color: #3730a3;
}

.purpose-exam {
  background: #fff7df;
  color: #9a5b00;
}

.purpose-other {
  background: #f2f4f7;
  color: #667085;
}

.reject-reason {
  color: #b42318;
  font-size: 12px;
  line-height: 1.5;
}

.primary-btn,
.ghost-btn,
.text-btn,
.link-btn,
.modal-btn,
.modal-close,
.qr-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 7px;
  font-family: inherit;
  cursor: pointer;
  transition: background .15s, border-color .15s, color .15s;
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
  border: 0;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
}

.header-action {
  height: 34px;
}

.link-btn {
  border: 0;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
}

.danger {
  color: #dc2626;
}

.ghost-btn:hover:not(:disabled) {
  border-color: #2563eb;
  color: #2563eb;
}

.text-btn:hover:not(:disabled),
.link-btn:hover:not(:disabled) {
  color: #1d4ed8;
}

.danger:hover:not(:disabled) {
  color: #b42318;
}

.ghost-btn:disabled,
.text-btn:disabled,
.link-btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.final-text {
  color: #8a94a6;
  font-size: 12px;
}

.empty-cell {
  height: 128px;
  color: #9aa3b2;
  text-align: center !important;
  vertical-align: middle !important;
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
  gap: 10px;
  margin-top: 14px;
  color: #6b7280;
  font-size: 13px;
}

.pagination button {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #d8dde7;
  border-radius: 7px;
  background: #fff;
  color: #42526b;
  font-size: 13px;
  font-family: inherit;
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
  padding: 20px;
  background: rgba(15, 23, 42, .38);
}

.modal-panel {
  display: flex;
  flex-direction: column;
  width: min(480px, 96vw);
  max-height: 85vh;
  overflow: hidden;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 22px 70px rgba(15, 23, 42, .22);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid #eef1f5;
}

.modal-header h3 {
  margin: 0;
  color: #111827;
  font-size: 17px;
  font-weight: 700;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #7b8495;
}

.modal-close:hover {
  background: #f3f5f8;
  color: #111827;
}

.modal-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  padding: 18px 22px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px 18px;
  border-top: 1px solid #eef1f5;
}

.confirm-info {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  background: #fafbfc;
}

.confirm-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #697386;
  font-size: 13px;
}

.confirm-row strong {
  color: #1f2937;
  font-weight: 600;
  text-align: right;
}

.form-g {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.form-g label {
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
}

.required {
  color: #dc2626;
}

.form-input {
  width: 100%;
  box-sizing: border-box;
  min-height: 38px;
  padding: 9px 10px;
  border: 1px solid #d8dde7;
  border-radius: 7px;
  outline: 0;
  background: #fff;
  color: #1f2937;
  font-size: 13px;
  font-family: inherit;
}

.form-textarea {
  min-height: 92px;
  resize: vertical;
}

.quick-reasons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.qr-label {
  color: #7b8495;
  font-size: 12px;
}

.qr-btn {
  height: 28px;
  padding: 0 9px;
  border: 1px solid #d8dde7;
  background: #fff;
  color: #42526b;
  font-size: 12px;
}

.qr-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.modal-btn {
  height: 36px;
  padding: 0 16px;
  border: 0;
  font-size: 13px;
  font-weight: 600;
}

.modal-btn.cancel {
  border: 1px solid #d8dde7;
  background: #fff;
  color: #42526b;
}

.modal-btn.primary {
  background: #137a43;
  color: #fff;
}

.modal-btn.danger {
  background: #dc2626;
  color: #fff;
}

.modal-btn:disabled {
  opacity: .5;
  cursor: not-allowed;
}

.modal-fade-enter-active {
  transition: all .2s ease;
}

.modal-fade-leave-active {
  transition: all .15s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-enter-from .modal-panel {
  transform: scale(.96) translateY(10px);
}

@media (max-width: 1180px) {
  .filter-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-actions {
    width: 100%;
  }

  .keyword-box {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .la-page {
    padding: 16px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
