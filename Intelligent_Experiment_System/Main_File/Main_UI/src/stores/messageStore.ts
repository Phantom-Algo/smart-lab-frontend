import { reactive, computed } from 'vue'
import { isMockMode, userRole } from './authStore'
import {
  getInboxMessages,
  deleteInboxMessage,
  batchDeleteInboxMessages,
  getNewMessageIds,
  markInboxViewed,
  type InboxMessageListItem,
  type InboxPageQuery,
} from '../utils/inboxApi'

interface Message {
  id: number; title: string; content: string; time: string; read: boolean; level?: string
}

// ========== Mock 数据工厂：按角色生成独立的消息副本 ==========
// 管理员（role=3）：只有系统通知，无预约/借用消息 → 总未读 5
// 教师（role=2）  ：通知+预约+借用 → 总未读 6
// 学生（role=1）  ：通知+预约+借用 → 总未读 6
function createRoleMockData(role: number) {
  const isAdmin = role === 3
  return {
    noticeList: [
      { id: 1, title: '系统维护通知', content: '平台将于2026年7月10日凌晨2:00-4:00进行系统维护升级，期间部分功能可能暂时不可用，请提前做好相关安排。', time: '2026-07-08 09:30', read: false, level: 'system' },
      { id: 2, title: '安全培训提醒', content: '您尚未完成本季度的实验室安全培训课程，请在7月15日前完成线上学习和考核，以免影响实验室使用权限。', time: '2026-07-07 14:20', read: false, level: 'warning' },
      { id: 3, title: '新功能上线', content: '3D全景展示子系统已上线，您可以在实验室详情页点击「3D漫游」按钮体验虚拟实验室漫游功能。', time: '2026-07-06 10:00', read: false, level: 'info' },
      // 管理员额外通知（教师和学生看不到）
      ...(isAdmin ? [
        { id: 10, title: '耗材预警：无水乙醇库存不足', content: '耗材「无水乙醇」当前库存45瓶，安全库存50瓶，请及时补充。', time: '2026-07-09 08:00', read: false, level: 'warning' },
        { id: 11, title: '设备维修完成通知', content: '恒温培养箱（EQ-2023-015）维修已完成，请前往实验室验收。', time: '2026-07-08 16:30', read: false, level: 'info' },
      ] : []),
      { id: 4, title: '卫生值日安排', content: '下周（7月13日-7月19日）的值日排班表已发布，请登录系统查看您的值日安排。', time: '2026-07-05 16:45', read: true, level: 'info' },
    ] as Message[],
    // 预约消息：仅教师和学生有，管理员无
    bookingList: isAdmin ? [] : [
      { id: 5, title: '预约审批通过', content: '您申请的A201有机化学实验室（7月12日 14:00-16:00）预约已通过审批，请按时使用。', time: '2026-07-08 08:15', read: false },
      { id: 6, title: '预约审批驳回', content: '您申请的B103物理实验室（7月11日 10:00-12:00）预约已被驳回，原因：该时段已有其他课程安排。', time: '2026-07-07 11:30', read: false },
      { id: 7, title: '预约即将到期提醒', content: '您预约的C202计算机实验室（7月9日 08:00-10:00）将于明天开始，请提前做好准备。', time: '2026-07-06 20:00', read: true },
    ] as Message[],
    // 借用通知：仅教师和学生有，管理员无
    borrowList: isAdmin ? [] : [
      { id: 8, title: '借用审批通过', content: '您申请的旋转蒸发仪（编号EQU-2024-0001）借用已通过审批，请于7月10日前至A201实验室领取。', time: '2026-07-08 10:00', read: false },
      { id: 9, title: '归还提醒', content: '您借用的数字示波器（编号EQU-2024-0015）应于7月9日前归还，逾期将产生违约记录，请及时归还。', time: '2026-07-07 09:00', read: true },
    ] as Message[],
    // 审批消息：仅管理员有
    approvalList: isAdmin ? [
      { id: 101, title: '设备借用待审批', content: '张同学申请借用旋转蒸发仪（EQU-2024-0001），借用时间 7月10日-7月12日，请及时审批。', time: '2026-07-08 10:30', read: false, level: 'warning' },
      { id: 102, title: '实验室预约待审核', content: '李老师申请预约 A201 有机化学实验室，时间 7月12日 14:00-16:00，请审核。', time: '2026-07-08 09:15', read: false, level: 'warning' },
      { id: 103, title: '耗材领用待审批', content: '张同学申请领用无水乙醇 500ml×2瓶，请审批。', time: '2026-07-08 08:00', read: true, level: 'info' },
    ] : [] as Message[],
  }
}

// 每个角色独立的 mock 数据（延迟初始化）
const roleMocks: Record<number, ReturnType<typeof createRoleMockData>> = {}

function getRoleMock() {
  const role = userRole.value
  if (!roleMocks[role]) {
    roleMocks[role] = reactive(createRoleMockData(role))
  }
  return roleMocks[role]
}

// 获取当前角色的 mock 消息列表
function currentNoticeList() { return getRoleMock().noticeList }
function currentBookingList() { return getRoleMock().bookingList }
function currentBorrowList() { return getRoleMock().borrowList }
function currentApprovalList() { return getRoleMock().approvalList }

// ========== 共享状态（API 数据缓存） ==========
const state = reactive({
  apiMessages: [] as InboxMessageListItem[],
  apiTotal: 0,
  apiLoading: false,
  apiNewIds: [] as number[],
})

// ========== Mock 数据 computed（使用当前角色的数据） ==========
const sortedNotice = computed(() =>
  [...currentNoticeList()].sort((a, b) => (a.read ? 1 : 0) - (b.read ? 1 : 0))
)
const sortedBooking = computed(() =>
  [...currentBookingList()].sort((a, b) => (a.read ? 1 : 0) - (b.read ? 1 : 0))
)
const sortedBorrow = computed(() =>
  [...currentBorrowList()].sort((a, b) => (a.read ? 1 : 0) - (b.read ? 1 : 0))
)

const noticeUnread = computed(() => currentNoticeList().filter(m => !m.read).length)
const bookingUnread = computed(() => currentBookingList().filter(m => !m.read).length)
const borrowUnread = computed(() => currentBorrowList().filter(m => !m.read).length)
const approvalUnread = computed(() => currentApprovalList().filter(m => !m.read).length)
const totalUnread = computed(() => noticeUnread.value + bookingUnread.value + borrowUnread.value + approvalUnread.value)

/** 将当前角色的某条消息标记为已读 */
function markAsRead(msgId: number): boolean {
  for (const list of [currentNoticeList(), currentBookingList(), currentBorrowList(), currentApprovalList()]) {
    const msg = list.find(m => m.id === msgId)
    if (msg && !msg.read) { msg.read = true; return true }
  }
  return false
}

// ========== API 方法（真实后端模式） ==========

/** 分页获取收件箱消息 */
async function fetchApiMessages(query: InboxPageQuery = {}): Promise<void> {
  if (isMockMode.value) return
  state.apiLoading = true
  try {
    const result = await getInboxMessages({ pageNum: 1, pageSize: 20, ...query })
    state.apiMessages = result.records
    state.apiTotal = result.total
  } catch {
    // API 不可用时静默失败，回退 mock 数据
  } finally {
    state.apiLoading = false
  }
}

/** 通过 API 删除消息 */
async function removeApiMessage(inboxMessageId: number): Promise<void> {
  await deleteInboxMessage(inboxMessageId)
  state.apiMessages = state.apiMessages.filter(m => m.inboxMessageId !== inboxMessageId)
  state.apiTotal--
}

/** 通过 API 批量删除 */
async function batchRemoveApiMessages(ids: number[]): Promise<void> {
  await batchDeleteInboxMessages(ids)
  state.apiMessages = state.apiMessages.filter(m => !ids.includes(m.inboxMessageId))
  state.apiTotal -= ids.length
}

/** 获取未读消息 ID 列表（用于顶部角标） */
async function fetchNewIds(): Promise<void> {
  if (isMockMode.value) return
  try {
    const result = await getInboxMessages({ pageNum: 1, pageSize: 500, isRead: 0 })
    state.apiNewIds = result.records.map(m => m.inboxMessageId)
  } catch {
    try {
      state.apiNewIds = await getNewMessageIds()
    } catch { /* ignore */ }
  }
}

/** 标记收件箱已查看 */
async function markViewed(): Promise<void> {
  if (isMockMode.value) return
  try { await markInboxViewed() } catch { /* ignore */ }
}

export function useMessageStore() {
  return {
    // Mock 数据（保留向后兼容）
    noticeList: sortedNotice,
    bookingList: sortedBooking,
    borrowList: sortedBorrow,
    approvalList: computed(() => currentApprovalList()),
    noticeUnread,
    bookingUnread,
    borrowUnread,
    approvalUnread,
    totalUnread,
    markAsRead,
    // API 数据
    apiMessages: computed(() => state.apiMessages),
    apiTotal: computed(() => state.apiTotal),
    apiLoading: computed(() => state.apiLoading),
    apiNewIdCount: computed(() => state.apiNewIds.length),
    fetchApiMessages,
    removeApiMessage,
    batchRemoveApiMessages,
    fetchNewIds,
    markViewed,
  }
}
