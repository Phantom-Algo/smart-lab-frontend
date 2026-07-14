<template>
  <div class="msg-layout">
    <!-- 左侧边栏 -->
    <aside class="msg-sidebar">
      <div class="sidebar-header"><h2 class="sidebar-title">消息中心</h2></div>
      <nav class="sidebar-tabs">
        <div v-for="tab in visibleTabs" :key="tab.key" class="sidebar-tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
          <span class="tab-label">{{ tab.label }}</span>
          <span class="tab-badge" v-if="tab.count > 0">{{ tab.count > 99 ? '99+' : tab.count }}</span>
        </div>
      </nav>
      <div class="sidebar-footer">
        <button class="back-btn" @click="$emit('back')">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
          回到首页
        </button>
      </div>
    </aside>

    <!-- 右侧内容区 -->
    <main class="msg-content">
      <!-- ===== API 模式：统一消息列表（非 mock 时显示） ===== -->
      <template v-if="!mockMode">
        <div class="api-toolbar">
          <div class="api-filters">
            <select v-model="apiFilter.priority" class="api-select" @change="loadApiMessages">
              <option :value="undefined">全部优先级</option>
              <option :value="1">预警通知</option>
              <option :value="2">系统通知</option>
              <option :value="3">普通通知</option>
            </select>
            <select v-model="apiFilter.isRead" class="api-select" @change="loadApiMessages">
              <option :value="undefined">全部状态</option>
              <option :value="0">未读</option>
              <option :value="1">已读</option>
            </select>
            <span class="api-total">共 {{ apiDisplayMessages.length }} 条</span>
          </div>
          <div class="api-actions">
            <button v-if="selectedIds.length > 0" class="batch-delete-btn" @click="batchDelete">删除选中 ({{ selectedIds.length }})</button>
            <button class="refresh-btn" @click="loadApiMessages" :disabled="apiLoading">刷新</button>
          </div>
        </div>

        <div class="msg-empty" v-if="apiDisplayMessages.length === 0 && !apiLoading">暂无消息</div>
        <div v-for="msg in apiDisplayMessages" :key="msg.inboxMessageId" class="msg-card" :class="{ read: msg.read }">
          <div class="msg-card-check" @click.stop>
            <input type="checkbox" :value="msg.inboxMessageId" v-model="selectedIds" />
          </div>
          <div class="msg-card-left">
            <span class="msg-type-tag" :class="'tag-' + prioClass(msg.priority)">{{ msg.priorityDescription }}</span>
          </div>
          <div class="msg-card-body" @click="openApiDetail(msg)">
            <div class="msg-card-title">{{ msg.title }}</div>
            <div class="msg-card-time">{{ msg.receivedAt }}</div>
          </div>
          <div class="msg-card-right">
            <span v-if="!msg.read" class="msg-unread-dot"></span>
            <button class="msg-delete-btn" @click.stop="deleteOne(msg.inboxMessageId)" title="删除">×</button>
          </div>
        </div>
        <!-- 分页 -->
        <div class="pagination" v-if="apiTotal > pageSize">
          <button :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button>
          <span>{{ pageNum }} / {{ Math.ceil(apiTotal / pageSize) }}</span>
          <button :disabled="pageNum >= Math.ceil(apiTotal / pageSize)" @click="goPage(pageNum + 1)">下一页</button>
        </div>
      </template>

      <!-- ===== Mock 模式：原有 Tab 消息（mock 时显示） ===== -->
      <template v-else>
        <!-- 系统通知 -->
        <div v-if="activeTab === 'notice'" class="msg-list">
          <div class="msg-list-header"><h3>系统通知</h3><span class="msg-count">共 {{ noticeList.length }} 条，{{ noticeUnread }} 条未读</span></div>
          <div class="msg-empty" v-if="noticeList.length === 0">暂无通知</div>
          <div v-for="msg in noticeList" :key="msg.id" class="msg-card" :class="{ read: msg.read }" @click="openDetail(msg)">
            <div class="msg-card-left"><span class="msg-type-tag" :class="'tag-' + msg.level">{{ levelLabel(msg.level) }}</span></div>
            <div class="msg-card-body"><div class="msg-card-title">{{ msg.title }}</div><div class="msg-card-desc">{{ msg.content }}</div><div class="msg-card-time">{{ msg.time }}</div></div>
            <span v-if="!msg.read" class="msg-unread-dot"></span>
          </div>
        </div>

        <!-- 预约消息 -->
        <div v-if="activeTab === 'booking'" class="msg-list">
          <div class="msg-list-header"><h3>预约消息</h3><span class="msg-count">共 {{ bookingList.length }} 条，{{ bookingUnread }} 条未读</span></div>
          <div class="msg-empty" v-if="bookingList.length === 0">暂无预约消息</div>
          <div v-for="msg in bookingList" :key="msg.id" class="msg-card" :class="{ read: msg.read }" @click="openDetail(msg)">
            <div class="msg-card-left"><span class="msg-type-tag tag-info">预约</span></div>
            <div class="msg-card-body"><div class="msg-card-title">{{ msg.title }}</div><div class="msg-card-desc">{{ msg.content }}</div><div class="msg-card-time">{{ msg.time }}</div></div>
            <span v-if="!msg.read" class="msg-unread-dot"></span>
          </div>
        </div>

        <!-- 借用消息 -->
        <div v-if="activeTab === 'borrow'" class="msg-list">
          <div class="msg-list-header"><h3>借用器材通知</h3><span class="msg-count">共 {{ borrowList.length }} 条，{{ borrowUnread }} 条未读</span></div>
          <div class="msg-empty" v-if="borrowList.length === 0">暂无借用消息</div>
          <div v-for="msg in borrowList" :key="msg.id" class="msg-card" :class="{ read: msg.read }" @click="openDetail(msg)">
            <div class="msg-card-left"><span class="msg-type-tag tag-warning">器材</span></div>
            <div class="msg-card-body"><div class="msg-card-title">{{ msg.title }}</div><div class="msg-card-desc">{{ msg.content }}</div><div class="msg-card-time">{{ msg.time }}</div></div>
            <span v-if="!msg.read" class="msg-unread-dot"></span>
          </div>
        </div>

        <!-- 管理员：审批消息 -->
        <div v-if="activeTab === 'approval'" class="msg-list">
          <div class="msg-list-header"><h3>待审批消息</h3><span class="msg-count">共 {{ approvalList.length }} 条，{{ approvalUnread }} 条未读</span></div>
          <div class="msg-empty" v-if="approvalList.length === 0">暂无待审批消息</div>
          <div v-for="msg in approvalList" :key="msg.id" class="msg-card" :class="{ read: msg.read }" @click="openDetail(msg)">
            <div class="msg-card-left"><span class="msg-type-tag" :class="'tag-' + (msg.level || 'warning')">{{ levelLabel(msg.level || 'warning') }}</span></div>
            <div class="msg-card-body"><div class="msg-card-title">{{ msg.title }}</div><div class="msg-card-desc">{{ msg.content }}</div><div class="msg-card-time">{{ msg.time }}</div></div>
            <span v-if="!msg.read" class="msg-unread-dot"></span>
          </div>
        </div>


      </template>
    </main>

    <!-- 消息详情弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="detailVisible" class="popup-overlay" @click.self="closeDetail">
          <div class="popup-panel">
            <div class="popup-header">
              <span class="popup-tag" :class="'tag-' + (detailTag || 'info')">{{ detailTagText }}</span>
              <button class="popup-close" @click="closeDetail"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="popup-body">
              <h3 class="popup-title">{{ detailTitle }}</h3>
              <div class="popup-meta"><span>{{ detailTime }}</span></div>
              <div class="popup-content" v-html="detailContent"></div>
              <div v-if="detailActionUrl" class="popup-action">
                <a :href="detailActionUrl" target="_blank">查看详情 →</a>
              </div>
            </div>
            <div class="popup-footer">
              <button class="popup-btn" @click="closeDetail">我知道了</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useMessageStore } from '../stores/messageStore'
import { userRole, isMockMode } from '../stores/authStore'
import { getInboxMessageDetail, deleteInboxMessage, batchDeleteInboxMessages, type InboxMessageListItem, type InboxMessageDetail } from '../utils/inboxApi'
import { REALTIME_MESSAGE_NEW_EVENT } from '../utils/wsClient'

defineEmits(['back'])

const {
  noticeList, bookingList, borrowList, approvalList, noticeUnread, bookingUnread, borrowUnread, approvalUnread,
  markAsRead, apiMessages, apiTotal, apiLoading,
  fetchApiMessages, removeApiMessage, batchRemoveApiMessages, fetchNewIds
} = useMessageStore()

const activeTab = ref('notice')
const mockMode = computed(() => isMockMode.value)

// ===== Mock 模式数据 =====
const detailMsg = ref<{ id: number; title: string; content: string; time: string; read: boolean; level?: string } | null>(null)

const templateList = ref([
  { id: 1, templateCode: 'equipment_loan_approved', templateName: '设备借用审批通过', messageType: 'equipment.loan.approved', titleTemplate: '您的设备借用申请已通过审批', status: 1 },
  { id: 2, templateCode: 'equipment_loan_rejected', templateName: '设备借用审批驳回', messageType: 'equipment.loan.rejected', titleTemplate: '您的设备借用申请已被驳回', status: 1 },
  { id: 3, templateCode: 'lab_reserve_approved', templateName: '实验室预约通过', messageType: 'lab.reserve.approved', titleTemplate: '您的实验室预约已通过审批', status: 1 },
  { id: 4, templateCode: 'safety_alarm', templateName: '安全告警通知', messageType: 'safety.alarm', titleTemplate: '实验室安全告警通知', status: 1 },
  { id: 5, templateCode: 'clean_remind', templateName: '值日提醒', messageType: 'clean.remind', titleTemplate: '值日提醒通知', status: 0 },
])

// 发布消息表单
const publishForm = reactive({ targetType: 'all', title: '', content: '', priority: 3 })
const publishing = ref(false)
const publishMsg = ref('')
const publishOk = ref(false)

async function handlePublish(): Promise<void> {
  if (!publishForm.title || !publishForm.content) { publishMsg.value = '请填写标题和内容'; publishOk.value = false; return }
  publishing.value = true; publishMsg.value = ''
  try {
    const { publishMessage } = await import('../utils/inboxApi')
    await publishMessage({
      messageType: 'system.normal.manual',
      targetType: publishForm.targetType,
      title: publishForm.title,
      content: publishForm.content,
    })
    publishMsg.value = '消息发送成功'
    publishOk.value = true
    publishForm.title = ''; publishForm.content = ''
  } catch { publishMsg.value = '发送失败，请重试'; publishOk.value = false }
  finally { publishing.value = false }
}

const visibleTabs = computed(() => {
  // API 模式：从真实后端数据统计未读数
  if (!mockMode.value) {
    const noticeCount = countApiUnreadByTab('notice')
    const bookingCount = countApiUnreadByTab('booking')
    const borrowCount = countApiUnreadByTab('borrow')
    const approvalCount = countApiUnreadByTab('approval')
    return [
      { key: 'notice', label: '通知', count: noticeCount },
      { key: 'booking', label: '预约消息', count: bookingCount },
      { key: 'borrow', label: '借用通知', count: borrowCount },
      { key: 'approval', label: '审批消息', count: approvalCount },
    ]
  }
  // Mock 模式：从 mock 数据统计未读数
  if (userRole.value === 3) {
    return [
      { key: 'notice', label: '通知', count: noticeUnread.value },
      { key: 'approval', label: '审批消息', count: approvalUnread.value },
    ]
  }
  return [
    { key: 'notice', label: '通知', count: noticeUnread.value },
    { key: 'booking', label: '预约消息', count: bookingUnread.value },
    { key: 'borrow', label: '借用通知', count: borrowUnread.value },
  ]
})

const apiDisplayMessages = computed(() =>
  apiMessages.value.filter((msg) => apiTabOfMessage(msg) === activeTab.value)
)

function countApiUnreadByTab(tab: string): number {
  return apiMessages.value.filter((msg) => apiTabOfMessage(msg) === tab && !msg.read).length
}

function apiTabOfMessage(msg: InboxMessageListItem): string {
  const type = msg.messageType || ''
  if (type.includes('reservation') || type.includes('reserve')) return 'booking'
  if (type.includes('pending_review') || type.includes('submitted')) return 'approval'
  if (type.includes('borrow') || type.includes('return') || type.includes('material') || type.includes('receive')) return 'borrow'
  return 'notice'
}

function openDetail(msg: { id: number; title: string; content: string; time: string; read: boolean; level?: string }): void {
  detailMsg.value = msg
  markAsRead(msg.id)
  const found = approvalList.value.find(m => m.id === msg.id)
  if (found) found.read = true
  showMockDetail()
}

// ===== API 模式数据 =====
const pageNum = ref(1)
const pageSize = 20
const selectedIds = ref<number[]>([])
const apiFilter = reactive({ priority: undefined as number | undefined, isRead: undefined as number | undefined })
const apiDetail = ref<InboxMessageDetail | null>(null)
let realtimeRefreshTimer: ReturnType<typeof setTimeout> | null = null

// 统一详情弹窗状态
const detailVisible = ref(false)
const detailTitle = ref('')
const detailContent = ref('')
const detailTime = ref('')
const detailTag = ref('')
const detailTagText = ref('')
const detailActionUrl = ref('')

onMounted(() => {
  if (!mockMode.value) loadApiMessages()
  window.addEventListener(REALTIME_MESSAGE_NEW_EVENT, handleRealtimeMessageNew)
})

onUnmounted(() => {
  window.removeEventListener(REALTIME_MESSAGE_NEW_EVENT, handleRealtimeMessageNew)
  if (realtimeRefreshTimer) clearTimeout(realtimeRefreshTimer)
})

async function loadApiMessages(): Promise<void> {
  await fetchApiMessages({ pageNum: pageNum.value, pageSize, ...apiFilter })
  await fetchNewIds()
}

watch(visibleTabs, (tabs) => {
  if (!tabs.some(tab => tab.key === activeTab.value)) {
    activeTab.value = tabs[0]?.key || 'notice'
  }
}, { immediate: true })

function handleRealtimeMessageNew(): void {
  if (mockMode.value || realtimeRefreshTimer) return
  realtimeRefreshTimer = setTimeout(() => {
    realtimeRefreshTimer = null
    void loadApiMessages()
  }, 200)
}

function goPage(p: number): void {
  pageNum.value = p
  loadApiMessages()
}

async function openApiDetail(msg: InboxMessageListItem): Promise<void> {
  selectedIds.value = []
  try {
    const detail = await getInboxMessageDetail(msg.inboxMessageId)
    apiDetail.value = detail
    detailTitle.value = detail.title
    detailContent.value = detail.content.replace(/\n/g, '<br>')
    detailTime.value = detail.receivedAt || ''
    detailTag.value = prioClass(detail.priority)
    detailTagText.value = detail.priorityDescription
    detailActionUrl.value = detail.actionUrl || ''
    detailVisible.value = true
    // 刷新列表（标记已读后状态会变）
    await loadApiMessages()
  } catch { /* ignore */ }
}

async function deleteOne(id: number): Promise<void> {
  if (!confirm('确认删除此消息？')) return
  await removeApiMessage(id)
  await fetchNewIds()
  selectedIds.value = selectedIds.value.filter(x => x !== id)
}

async function batchDelete(): Promise<void> {
  if (!confirm(`确认删除选中的 ${selectedIds.value.length} 条消息？`)) return
  await batchRemoveApiMessages(selectedIds.value)
  await fetchNewIds()
  selectedIds.value = []
}

function showMockDetail(): void {
  if (!detailMsg.value) return
  detailTitle.value = detailMsg.value.title
  detailContent.value = detailMsg.value.content.replace(/\n/g, '<br>')
  detailTime.value = detailMsg.value.time
  detailTag.value = detailMsg.value.level || 'info'
  detailTagText.value = levelLabel(detailMsg.value.level || 'info')
  detailActionUrl.value = ''
  detailVisible.value = true
}

function closeDetail(): void {
  detailVisible.value = false
  detailMsg.value = null
  apiDetail.value = null
}

function prioClass(p: number): string {
  return p === 1 ? 'warning' : p === 2 ? 'system' : 'info'
}

function levelLabel(level?: string): string {
  if (!level) return ''
  const map: Record<string, string> = { system: '系统', warning: '提醒', info: '通知', alarm: '告警' }
  return map[level] || level
}
</script>

<style scoped>
.msg-layout{ display:flex; height:100% }
.msg-sidebar{ width:240px; flex-shrink:0; display:flex; flex-direction:column; border-right:1px solid rgba(0,0,0,.06); background:rgba(255,255,255,.1) }
.sidebar-header{ padding:24px 20px 16px }
.sidebar-title{ font-size:17px; font-weight:600; color:#1a1a2e; margin:0 }
.sidebar-tabs{ flex:1; padding:4px 12px; display:flex; flex-direction:column; gap:2px }
.sidebar-tab{ display:flex; align-items:center; gap:10px; padding:10px 12px; border-radius:8px; cursor:pointer; transition:all .15s; color:#555; font-size:14px; user-select:none }
.sidebar-tab:hover{ background:rgba(26,115,232,.06); color:#1a73e8 }
.sidebar-tab.active{ background:rgba(26,115,232,.1); color:#1a73e8; font-weight:500 }
.tab-label{ flex:1 }
.tab-badge{ min-width:20px; height:20px; padding:0 6px; font-size:11px; font-weight:600; color:#fff; background:#e74c3c; border-radius:10px; display:flex; align-items:center; justify-content:center }
.sidebar-footer{ padding:16px 12px; border-top:1px solid rgba(0,0,0,.06) }
.back-btn{ display:flex; align-items:center; justify-content:center; gap:6px; width:100%; padding:10px 0; border:none; border-radius:8px; background:rgba(0,0,0,.04); color:#666; font-size:13px; cursor:pointer; transition:all .15s }
.back-btn:hover{ background:rgba(26,115,232,.08); color:#1a73e8 }
.msg-content{ flex:1; overflow-y:auto; padding:24px 32px }

/* ===== API 工具栏 ===== */
.api-toolbar{ display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.api-filters{ display:flex; align-items:center; gap:10px }
.api-select{ padding:7px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; color:#555; outline:none; background:rgba(255,255,255,.6) }
.api-total{ font-size:13px; color:#999 }
.api-actions{ display:flex; gap:8px }
.batch-delete-btn{ padding:6px 14px; border:1px solid #e74c3c; border-radius:6px; background:rgba(231,76,60,.05); color:#e74c3c; font-size:12px; cursor:pointer }
.batch-delete-btn:hover{ background:rgba(231,76,60,.1) }
.refresh-btn{ padding:6px 14px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:12px; color:#888; cursor:pointer }
.refresh-btn:hover{ border-color:#1a73e8; color:#1a73e8 }

/* ===== Mock 列表 ===== */
.msg-list-header{ display:flex; align-items:baseline; gap:12px; margin-bottom:20px }
.msg-list-header h3{ font-size:16px; font-weight:600; color:#1a1a2e; margin:0 }
.msg-count{ font-size:13px; color:#999 }
.msg-empty{ padding:60px 0; text-align:center; color:#bbb; font-size:14px }

.msg-card{ display:flex; align-items:flex-start; gap:12px; padding:14px 16px; border-radius:10px; cursor:pointer; transition:all .15s; margin-bottom:8px; background:rgba(255,255,255,.6); position:relative }
.msg-card:hover{ background:rgba(26,115,232,.04) }
.msg-card.read{ opacity:.65 }

.msg-card-check{ flex-shrink:0; padding-top:3px }
.msg-card-check input{ cursor:pointer }

.msg-card-left{ flex-shrink:0; padding-top:2px }
.msg-type-tag{ display:inline-block; padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.tag-system{ background:rgba(26,115,232,.1); color:#1a73e8 }
.tag-warning,.tag-alarm{ background:rgba(230,126,34,.1); color:#e67e22 }
.tag-info{ background:rgba(46,204,113,.1); color:#27ae60 }

.msg-card-body{ flex:1; min-width:0 }
.msg-card-title{ font-size:14px; font-weight:600; color:#222; margin-bottom:4px }
.msg-card-desc{ font-size:13px; color:#888; line-height:1.5; display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden }
.msg-card-time{ font-size:12px; color:#bbb; margin-top:6px }

.msg-card-right{ display:flex; align-items:center; gap:8px; flex-shrink:0 }
.msg-unread-dot{ width:8px; height:8px; border-radius:50%; background:#e74c3c; flex-shrink:0 }
.msg-delete-btn{ width:24px; height:24px; display:flex; align-items:center; justify-content:center; border:none; background:none; color:#ccc; font-size:18px; cursor:pointer; border-radius:4px }
.msg-delete-btn:hover{ color:#e74c3c; background:rgba(231,76,60,.06) }

/* ===== 分页 ===== */
.pagination{ display:flex; align-items:center; justify-content:center; gap:14px; padding:20px 0; font-size:13px; color:#888 }
.pagination button{ padding:6px 16px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; color:#555; cursor:pointer }
.pagination button:hover:not(:disabled){ border-color:#1a73e8; color:#1a73e8 }
.pagination button:disabled{ opacity:.4; cursor:not-allowed }

/* ===== 发布消息表单 ===== */
.tab-panel{ padding:0 }
.panel-title{ font-size:16px; font-weight:600; color:#1a1a2e; margin:0 0 20px }
.publish-form{ max-width:560px; display:flex; flex-direction:column; gap:16px }
.form-group{ display:flex; flex-direction:column; gap:6px }
.form-group label{ font-size:13px; font-weight:500; color:#555 }
.form-input,.form-select,.form-textarea{ padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:border .15s; font-family:inherit }
.form-input:focus,.form-select:focus,.form-textarea:focus{ border-color:#1a73e8 }
.form-textarea{ resize:vertical }
.publish-btn{ align-self:flex-start; padding:10px 28px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; border:none; border-radius:8px; font-size:14px; font-weight:500; cursor:pointer; transition:all .15s }
.publish-btn:hover{ box-shadow:0 4px 12px rgba(26,115,232,.3); transform:translateY(-1px) }

/* ===== 弹窗 ===== */
.popup-overlay{ position:fixed; inset:0; background:rgba(0,0,0,.3); display:flex; align-items:center; justify-content:center; z-index:1000 }
.popup-panel{ background:#fff; border-radius:16px; width:480px; max-height:80vh; overflow-y:auto; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.popup-header{ display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.popup-tag{ padding:3px 10px; border-radius:4px; font-size:12px; font-weight:600 }
.popup-close{ background:none; border:none; cursor:pointer; color:#999; padding:4px; border-radius:6px }
.popup-close:hover{ background:rgba(0,0,0,.05); color:#333 }
.popup-body{ padding:16px 24px 24px }
.popup-title{ font-size:18px; font-weight:600; color:#1a1a2e; margin:0 0 8px }
.popup-meta{ font-size:13px; color:#bbb; margin-bottom:16px }
.popup-content{ font-size:14px; color:#555; line-height:1.8 }
.popup-action{ margin-top:16px }
.popup-action a{ color:#1a73e8; font-size:14px; text-decoration:none }
.popup-action a:hover{ text-decoration:underline }
.popup-footer{ padding:0 24px 20px }
.popup-btn{ padding:10px 28px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; border:none; border-radius:8px; font-size:14px; cursor:pointer }
.popup-btn:hover{ box-shadow:0 4px 12px rgba(26,115,232,.3) }
.popup-fade-enter-active,.popup-fade-leave-active{ transition:all .2s ease }
.popup-fade-enter-from,.popup-fade-leave-to{ opacity:0 }
</style>
