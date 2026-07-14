<template>
  <div class="la-page">
    <div class="page-header">
      <h2>预约审核</h2>
      <p>审批教师/学生的实验室预约</p>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num highlight">{{ pendingCount }}</div>
        <div class="stat-label">待审核</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ todayApproved }}</div>
        <div class="stat-label">今日通过</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ monthReservations }}</div>
        <div class="stat-label">本月预约</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" :class="approvalRate >= 80 ? 'green' : 'orange'">{{ approvalRate }}%</div>
        <div class="stat-label">通过率</div>
      </div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="changeStatus(t.key)">
          {{ t.label }}<span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <input v-model="searchNo" placeholder="搜索预约单号..." class="search-input" @keyup.enter="submitSearch" />
    </div>

    <div class="card-list" v-if="pagedList.length > 0">
      <div v-for="r in pagedList" :key="r.id" class="reserve-card" :class="'card-' + r.status">
        <div class="card-left">
          <div class="card-header">
            <span class="status-badge" :class="'badge-' + r.status">{{ r.statusName }}</span>
            <span class="apply-no">{{ r.applyNo }}</span>
            <span class="apply-time">{{ r.createdAt }}</span>
          </div>
          <div class="card-body">
            <div class="card-row">
              <span class="ci-label">实验室</span>
              <span class="ci-value lab-link">{{ r.labName }}<template v-if="r.labCode">（{{ r.labCode }}）</template></span>
            </div>
            <div class="card-row">
              <span class="ci-label">申请人</span>
              <span class="ci-value">
                {{ r.applicantName }}
                <span class="person-type" :class="r.applicantType === 1 ? 'type-stu' : 'type-tea'">{{ r.applicantTypeName }}</span>
              </span>
              <span class="ci-sep">|</span>
              <span class="ci-label">日期</span>
              <span class="ci-value">{{ r.reserveDate }}</span>
              <span class="ci-sep">|</span>
              <span class="ci-value">{{ r.timeSlot }}</span>
            </div>
            <div class="card-row">
              <span class="ci-label">人数</span>
              <span class="ci-value">{{ r.peopleCount }}人</span>
              <span class="ci-sep">|</span>
              <span class="ci-label">用途</span>
              <span class="purpose-tag" :class="'purpose-' + r.purposeTag">{{ r.purposeTagName }}</span>
            </div>
            <div v-if="r.status === 'rejected' && r.rejectReason" class="reject-reason">
              <span class="reason-label">驳回原因：</span>{{ r.rejectReason }}
            </div>
          </div>
        </div>
        <div class="card-right">
          <template v-if="r.status === 'pending'">
            <button class="btn-reject" @click="openReject(r)" :disabled="actionLoading">驳回</button>
            <button class="btn-approve" @click="openApprove(r)" :disabled="actionLoading">审核通过</button>
          </template>
          <template v-else-if="r.status === 'approved' && isMockMode">
            <button class="btn-revoke" @click="revokeApproval(r)">撤销审核</button>
          </template>
          <span v-else class="final-text">{{ r.statusName }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
      <p>{{ emptyText }}</p>
    </div>

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
              <h3>确认审核通过</h3>
              <button class="modal-close" @click="showApprove = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>实验室</span><span>{{ currentReservation?.labName }}</span></div>
                <div class="ci-row"><span>申请人</span><span>{{ currentReservation?.applicantName }}（{{ currentReservation?.applicantTypeName }}）</span></div>
                <div class="ci-row"><span>日期</span><span>{{ currentReservation?.reserveDate }}</span></div>
                <div class="ci-row"><span>时段</span><span>{{ currentReservation?.timeSlot }}</span></div>
                <div class="ci-row"><span>人数</span><span>{{ currentReservation?.peopleCount }}人</span></div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showApprove = false">取消</button>
              <button class="modal-btn primary" @click="doApprove" :disabled="actionLoading">确认通过</button>
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
              <h3>驳回预约</h3>
              <button class="modal-close" @click="showReject = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>驳回原因 <span class="required">*</span></label>
                <textarea v-model="rejectForm.reason" class="form-input form-textarea" rows="3" placeholder="请填写驳回原因，系统将通知申请人..." maxlength="300" />
              </div>
              <div class="quick-reasons">
                <span class="qr-label">快捷填写：</span>
                <button v-for="r in quickReasons" :key="r" class="qr-btn" @click="rejectForm.reason = r">{{ r }}</button>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showReject = false">取消</button>
              <button class="modal-btn danger" @click="doReject" :disabled="!rejectForm.reason.trim() || actionLoading">确认驳回</button>
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
.la-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.highlight { color:#1a73e8 }
.green { color:#27ae60 }
.orange { color:#f39c12 }

.filter-bar { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; gap:12px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:180px; background:rgba(255,255,255,.7) }

.card-list { display:flex; flex-direction:column; gap:12px }
.reserve-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:stretch; overflow:hidden }
.reserve-card.card-pending { border-left:3px solid #1a73e8 }
.reserve-card.card-approved { border-left:3px solid #27ae60 }
.reserve-card.card-rejected { border-left:3px solid #e74c3c }
.reserve-card.card-cancelled { border-left:3px solid #ccc; opacity:.65 }
.reserve-card.card-checkedIn { border-left:3px solid #8e44ad }
.reserve-card.card-absent { border-left:3px solid #e67e22 }

.card-left { flex:1; padding:16px 20px; min-width:0 }
.card-right { display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:8px; padding:16px 20px; flex-shrink:0; min-width:130px; border-left:1px solid rgba(0,0,0,.04) }

.card-header { display:flex; align-items:center; gap:10px; margin-bottom:10px }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.badge-pending { background:rgba(26,115,232,.1); color:#1a73e8 }
.badge-approved { background:rgba(39,174,96,.1); color:#27ae60 }
.badge-rejected { background:rgba(231,76,60,.1); color:#e74c3c }
.badge-cancelled { background:rgba(0,0,0,.05); color:#999 }
.badge-checkedIn { background:rgba(142,68,173,.1); color:#8e44ad }
.badge-absent { background:rgba(230,126,34,.1); color:#e67e22 }
.apply-no { font-size:13px; font-weight:600; color:#1a1a2e }
.apply-time { font-size:12px; color:#999; margin-left:auto }

.card-body { display:flex; flex-direction:column; gap:5px }
.card-row { display:flex; align-items:baseline; gap:6px; font-size:13px; flex-wrap:wrap }
.ci-label { color:#999; min-width:40px; flex-shrink:0 }
.ci-value { color:#333 }
.lab-link { color:#1a73e8; font-weight:500 }
.person-type { padding:1px 6px; border-radius:3px; font-size:11px; margin-left:4px }
.type-stu { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-tea { background:rgba(26,115,232,.1); color:#1557b0 }
.ci-sep { color:#ddd }

.purpose-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.purpose-teaching { background:rgba(76,175,80,.1); color:#2e7d32 }
.purpose-research { background:rgba(26,115,232,.1); color:#1557b0 }
.purpose-exam { background:rgba(243,156,18,.1); color:#e67e22 }
.purpose-other { background:rgba(0,0,0,.05); color:#888 }

.reject-reason { margin-top:6px; padding:8px 12px; background:rgba(231,76,60,.04); border-radius:8px; font-size:12px; color:#c0392b; line-height:1.5 }
.reason-label { font-weight:600 }

.btn-approve { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#27ae60,#2ecc71); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }
.btn-approve:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.btn-reject { padding:9px 20px; border:1px solid rgba(231,76,60,.3); border-radius:10px; background:#fff; color:#e74c3c; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-reject:hover { background:rgba(231,76,60,.05); border-color:#e74c3c }
.btn-revoke { padding:8px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; color:#888; font-size:12px; cursor:pointer }
.btn-revoke:hover { border-color:#e74c3c; color:#e74c3c }
.btn-approve:disabled,.btn-reject:disabled,.btn-revoke:disabled { opacity:.55; cursor:not-allowed; box-shadow:none }
.final-text { font-size:12px; color:#bbb }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#27ae60,#2ecc71); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.modal-btn.danger { background:linear-gradient(135deg,#e74c3c,#ec7063); color:#fff }
.modal-btn.danger:hover:not(:disabled) { box-shadow:0 4px 12px rgba(231,76,60,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; outline:none; background:rgba(255,255,255,.7); font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { resize:vertical; min-height:60px }
.quick-reasons { display:flex; align-items:center; gap:6px; flex-wrap:wrap }
.qr-label { font-size:11px; color:#999; white-space:nowrap }
.qr-btn { padding:5px 10px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#666; cursor:pointer; white-space:nowrap }
.qr-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
