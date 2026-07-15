<template>
  <div class="app-container">
    <!-- 背景图层 -->
    <div class="bg-layer"></div>

    <!-- 毛玻璃内容面板 -->
    <div class="glass-panel">
      <!-- 上边栏（未登录 / 消息中心 / 用户详情页面隐藏） -->
      <HeaderBar v-if="isLoggedIn && currentPage !== 'messages' && currentPage !== 'user-detail'" :current-page="currentPage" @navigate="handleNavigate" />

      <!-- 主体内容区域 -->
      <div class="main-content">
        <!-- 登录页面（未登录时全屏展示） -->
        <LoginView v-if="!isLoggedIn && !isMockMode" />

        <!-- 已登录时按角色和页面渲染 -->
        <template v-else>
          <!-- 消息中心（全屏覆盖） -->
          <MessageCenter
            v-if="currentPage === 'messages'"
            @back="goHome"
          />

        <!-- 用户详情（全屏覆盖） -->
        <UserDetail
          v-else-if="currentPage === 'user-detail'"
          @back="goHome"
        />

        <!-- 学生首页仪表盘 -->
        <StudentDashboard
          v-else-if="currentPage === 'home' && isStudent"
          @navigate="handleNavigate"
        />

        <!-- 教师首页仪表盘 -->
        <TeacherDashboard
          v-else-if="currentPage === 'home' && isTeacher"
          @navigate="handleNavigate"
        />

        <!-- 管理员首页仪表盘 -->
        <AdminDashboard
          v-else-if="currentPage === 'home' && isAdmin"
          @navigate="handleNavigate"
        />

        <!-- 3D全景展示 -->
        <Lab3DViewer
          v-else-if="currentPage === '3d-show'"
        />

        <!-- 我的课程（学生） -->
        <CourseView
          v-else-if="currentPage === 'my-course' && isStudent"
        />

        <!-- 我的课程（教师） -->
        <TeacherCourseView
          v-else-if="currentPage === 'my-course' && isTeacher"
          @navigate="handleNavigate"
        />

        <!-- 实验任务（学生） -->
        <TaskView
          v-else-if="currentPage === 'my-task' && isStudent"
        />

        <!-- 实验任务（教师） -->
        <TeacherTaskView
          v-else-if="currentPage === 'my-task' && isTeacher"
          @navigate="handleNavigate"
        />

        <!-- 成绩评定（教师） -->
        <TeacherScoreView
          v-else-if="currentPage === 'my-score'"
        />

        <!-- 教学资源（教师） -->
        <TeacherResourceView
          v-else-if="currentPage === 'my-resource'"
        />

        <!-- 人员档案管理（管理员） -->
        <PersonnelManage
          v-else-if="currentPage === 'person-manage'"
        />

        <!-- 部门管理（管理员） -->
        <DepartmentManage
          v-else-if="currentPage === 'department-manage'"
        />

        <!-- 岗位管理（管理员） -->
        <PositionManage
          v-else-if="currentPage === 'position-manage'"
        />

        <!-- 账号管理（管理员） -->
        <AccountManage
          v-else-if="currentPage === 'account-manage'"
        />

        <!-- 设备台账（管理员） -->
        <DeviceLedger
          v-else-if="currentPage === 'device-manage'"
        />

        <!-- 借用审批（管理员） -->
        <DeviceApproval
          v-else-if="currentPage === 'device-approve'"
        />

        <!-- 维修管理（管理员） -->
        <DeviceRepair
          v-else-if="currentPage === 'device-repair'"
        />

        <!-- 统计报表（管理员） -->
        <DeviceStats
          v-else-if="currentPage === 'device-stats'"
        />

        <!-- 课程管理（管理员） -->
        <CourseManage
          v-else-if="currentPage === 'course-manage'"
        />

        <!-- 教学资源（管理员） -->
        <ResourceManage
          v-else-if="currentPage === 'resource-manage'"
        />

        <!-- 耗材库存（管理员） -->
        <ConsumableStock
          v-else-if="currentPage === 'consumable-stock'"
        />

        <!-- 领用审批（管理员） -->
        <ConsumablePick
          v-else-if="currentPage === 'consumable-pick'"
        />

        <!-- 库存预警（管理员） -->
        <ConsumableWarn
          v-else-if="currentPage === 'consumable-warn'"
        />

        <!-- 安全检查（管理员） -->
        <SafetyCheck
          v-else-if="currentPage === 'safety-check'"
        />

        <!-- 隐患管理（管理员） -->
        <SafetyHazard
          v-else-if="currentPage === 'safety-hazard'"
        />

        <!-- 报警记录（管理员） -->
        <SafetyAlarm
          v-else-if="currentPage === 'safety-alarm'"
        />

        <!-- 安全台账（管理员） -->
        <SafetyLedger
          v-else-if="currentPage === 'safety-ledger'"
        />

        <!-- 排班管理（管理员） -->
        <CleanSchedule
          v-else-if="currentPage === 'clean-schedule'"
        />

        <!-- 检查评价（管理员） -->
        <CleanReview
          v-else-if="currentPage === 'clean-review'"
        />

        <!-- 物联网环境（管理员） -->
        <IotMonitor
          v-else-if="currentPage === 'iot-monitor'"
        />

        <!-- 阈值配置（管理员） -->
        <IotThreshold
          v-else-if="currentPage === 'iot-threshold'"
        />

        <!-- 传感器设备（管理员） -->
        <SensorDeviceManage
          v-else-if="currentPage === 'sensor-device'"
        />

        <!-- 场景映射管理（管理员） -->
        <SceneManager
          v-else-if="currentPage === '3d-manager'"
          @navigate="handleNavigate"
        />

        <!-- 效能看板（管理员） -->
        <Efficiency
          v-else-if="currentPage === 'efficiency'"
        />

        <!-- 消息模板（管理员） -->
        <MsgTemplate
          v-else-if="currentPage === 'msg-template'"
        />

        <!-- 消息发布（管理员） -->
        <MsgPublish
          v-else-if="currentPage === 'msg-publish'"
        />

        <!-- 场地管理（管理员） -->
        <LabManage
          v-else-if="currentPage === 'lab-manage'"
        />

        <!-- 预约审核（管理员） -->
        <LabApproval
          v-else-if="currentPage === 'lab-approve'"
        />

        <!-- 使用统计（管理员） -->
        <LabStats
          v-else-if="currentPage === 'lab-stats'"
        />

        <!-- 我的成绩 -->
        <GradeView
          v-else-if="currentPage === 'my-grade'"
        />

        <!-- 设备查询 -->
        <DeviceSearch
          v-else-if="currentPage === 'device-search'"
        />

        <!-- 借用申请 -->
        <BorrowApply
          v-else-if="currentPage === 'device-borrow'"
          @navigate="handleNavigate"
        />

        <!-- 实验室查询 -->
        <LabSearch
          v-else-if="currentPage === 'lab-search'"
        />

        <!-- 场地预约（教师） -->
        <LabSearch
          v-else-if="currentPage === 'lab-reserve'"
        />

        <!-- 隐患上报 -->
        <HazardReport
          v-else-if="currentPage === 'hazard-report'"
        />

        <!-- 我的上报 -->
        <MyReport
          v-else-if="currentPage === 'my-report'"
          @navigate="handleNavigate"
        />

        <!-- 值日安排（学生） -->
        <CleanArrange
          v-else-if="currentPage === 'clean-arrange' && isStudent"
        />

        <!-- 值日安排（教师） -->
        <TeacherCleanArrange
          v-else-if="currentPage === 'clean-arrange' && isTeacher"
          @navigate="handleNavigate"
        />

        <!-- 检查评价（教师） -->
        <TeacherCleanEvaluate
          v-else-if="currentPage === 'clean-evaluate' && isTeacher"
          @navigate="handleNavigate"
        />

        <!-- 我的签到 -->
        <CleanSign
          v-else-if="currentPage === 'clean-sign'"
        />

        <!-- 耗材查询（教师） -->
        <ConsumableSearch
          v-else-if="currentPage === 'consumable-search'"
          @navigate="handleNavigate"
        />

        <!-- 耗材领用申请（教师） -->
        <ConsumableApply
          v-else-if="currentPage === 'consumable-apply'"
          @navigate="handleNavigate"
        />

        <!-- 默认占位页面 -->
        <div v-else class="content-placeholder">
          <span class="placeholder-icon">--</span>
          <h2 class="placeholder-title">这里是「{{ currentPageName }}」的界面展示</h2>
          <p class="placeholder-sub">该模块正在开发中，敬请期待...</p>
        </div>
        </template>
      </div>

      <transition-group name="realtime-toast" tag="div" class="realtime-toast-stack">
        <div
          v-for="notice in realtimeNotices"
          :key="notice.id"
          class="realtime-toast"
          :class="notice.kind"
        >
          <div class="realtime-toast-head">
            <div>
              <p class="realtime-toast-label">实时动态</p>
              <h4 class="realtime-toast-title">{{ notice.title }}</h4>
            </div>
            <span class="realtime-toast-time">{{ notice.time }}</span>
          </div>
          <p class="realtime-toast-detail">{{ notice.detail }}</p>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { loadRole, isStudent, isTeacher, isAdmin, isLoggedIn, isMockMode } from './stores/authStore'
import { useMessageStore } from './stores/messageStore'
import { connectRealtime, disconnectRealtime, REALTIME_EVENT_NAME, REALTIME_MESSAGE_NEW_EVENT, type RealtimeEnvelope } from './utils/wsClient'
import HeaderBar from './components/HeaderBar.vue'
import MessageCenter from './components/MessageCenter.vue'
import UserDetail from './components/UserDetail.vue'
import StudentDashboard from './components/student/StudentDashboard.vue'
import TeacherDashboard from './components/teacher/TeacherDashboard.vue'
import AdminDashboard from './components/admin/AdminDashboard.vue'
import Lab3DViewer from './components/Lab3DViewer.vue'
import CourseView from './components/student/CourseView.vue'
import LoginView from './components/LoginView.vue'
import TaskView from './components/student/TaskView.vue'
import TeacherCourseView from './components/teacher/TeacherCourseView.vue'
import TeacherTaskView from './components/teacher/TeacherTaskView.vue'
import TeacherScoreView from './components/teacher/TeacherScoreView.vue'
import TeacherResourceView from './components/teacher/TeacherResourceView.vue'
import PersonnelManage from './components/admin/PersonnelManage.vue'
import DepartmentManage from './components/admin/DepartmentManage.vue'
import PositionManage from './components/admin/PositionManage.vue'
import AccountManage from './components/admin/AccountManage.vue'
import DeviceLedger from './components/admin/DeviceLedger.vue'
import DeviceApproval from './components/admin/DeviceApproval.vue'
import DeviceRepair from './components/admin/DeviceRepair.vue'
import DeviceStats from './components/admin/DeviceStats.vue'
import CourseManage from './components/admin/CourseManage.vue'
import ResourceManage from './components/admin/ResourceManage.vue'
import ConsumableStock from './components/admin/ConsumableStock.vue'
import ConsumablePick from './components/admin/ConsumablePick.vue'
import ConsumableWarn from './components/admin/ConsumableWarn.vue'
import SafetyCheck from './components/admin/SafetyCheck.vue'
import SafetyHazard from './components/admin/SafetyHazard.vue'
import SafetyAlarm from './components/admin/SafetyAlarm.vue'
import SafetyLedger from './components/admin/SafetyLedger.vue'
import CleanSchedule from './components/admin/CleanSchedule.vue'
import CleanReview from './components/admin/CleanReview.vue'
import IotMonitor from './components/admin/IotMonitor.vue'
import IotThreshold from './components/admin/IotThreshold.vue'
import SensorDeviceManage from './components/admin/SensorDeviceManage.vue'
import SceneManager from './components/admin/SceneManager.vue'
import Efficiency from './components/admin/Efficiency.vue'
import MsgTemplate from './components/admin/MsgTemplate.vue'
import MsgPublish from './components/admin/MsgPublish.vue'
import LabManage from './components/admin/LabManage.vue'
import LabApproval from './components/admin/LabApproval.vue'
import LabStats from './components/admin/LabStats.vue'
import GradeView from './components/student/GradeView.vue'
import DeviceSearch from './components/student/DeviceSearch.vue'
import BorrowApply from './components/student/BorrowApply.vue'
import LabSearch from './components/student/LabSearch.vue'
import HazardReport from './components/student/HazardReport.vue'
import MyReport from './components/student/MyReport.vue'
import CleanArrange from './components/student/CleanArrange.vue'
import CleanSign from './components/student/CleanSign.vue'
import ConsumableSearch from './components/teacher/ConsumableSearch.vue'
import ConsumableApply from './components/teacher/ConsumableApply.vue'
import TeacherCleanArrange from './components/teacher/TeacherCleanArrange.vue'
import TeacherCleanEvaluate from './components/teacher/TeacherCleanEvaluate.vue'

interface RealtimeNotice {
  id: number
  title: string
  detail: string
  time: string
  kind: 'lab' | 'equipment' | 'material' | 'message' | 'system'
}

const { fetchNewIds } = useMessageStore()

const currentPage = ref('home')
const currentPageName = ref('首页')
const realtimeNotices = ref<RealtimeNotice[]>([])

let unreadRefreshTimer: ReturnType<typeof setTimeout> | null = null
let realtimeNoticeSeq = 0
const realtimeNoticeTimers = new Map<number, ReturnType<typeof setTimeout>>()

// 防抖：避免多个并发 401 请求重复触发 loadRole
let logoutDebounceTimer: ReturnType<typeof setTimeout> | null = null
function handleAuthLogout(): void {
  if (logoutDebounceTimer) return  // 已经在等待处理
  logoutDebounceTimer = setTimeout(() => {
    logoutDebounceTimer = null
    loadRole()
  }, 100)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function getStringField(record: Record<string, unknown>, keys: string[]): string | null {
  for (const key of keys) {
    const value = record[key]
    if (typeof value === 'string' && value.trim()) {
      return value.trim()
    }
  }
  return null
}

function getNumberField(record: Record<string, unknown>, key: string): number | null {
  const value = record[key]
  return typeof value === 'number' ? value : null
}

function formatRealtimeTime(occurredAt: string): string {
  if (!occurredAt) return '刚刚'
  return occurredAt.replace('T', ' ').slice(11, 16) || '刚刚'
}

function realtimeTitle(eventName: string): string {
  const titleMap: Record<string, string> = {
    'message.new': '收到新消息',
    'lab.status_changed': '实验室状态更新',
    'lab.created': '实验室已创建',
    'lab.info_updated': '实验室信息更新',
    'lab.deleted': '实验室已删除',
    'reservation.created': '新预约已提交',
    'reservation.approved': '预约已通过',
    'reservation.rejected': '预约已驳回',
    'reservation.cancelled': '预约已取消',
    'reservation.checked_in': '预约已签到',
    'reservation.checked_out': '预约已签退',
    'reservation.absent': '预约已记缺席',
    'equipment.stock_inbound': '设备台账新增',
    'equipment.info_updated': '设备信息更新',
    'equipment.deleted': '设备已删除',
    'equipment.borrow_submitted': '设备借用申请提交',
    'equipment.borrow_cancelled': '设备借用申请取消',
    'equipment.borrow_approved': '设备借用申请通过',
    'equipment.borrow_rejected': '设备借用申请驳回',
    'equipment.borrow_checked_out': '设备已领用',
    'equipment.borrow_returned': '设备已归还',
    'equipment.borrow_overdue': '设备借用已逾期',
    'equipment.repair_reported': '设备故障已上报',
    'equipment.repair_started': '设备开始维修',
    'equipment.repair_completed': '设备维修完成',
    'equipment.repair_accepted': '设备维修已验收',
    'equipment.repair_cost_recorded': '维修费用已登记',
    'equipment.status_changed': '设备状态更新',
    'material.created': '物资已创建',
    'material.info_updated': '物资信息更新',
    'material.deleted': '物资已删除',
    'material.stock_inbound': '物资已入库',
    'material.stock_changed': '物资库存变化',
    'material.receive_submitted': '领用申请已提交',
    'material.receive_cancelled': '领用申请已取消',
    'material.receive_approved': '领用申请已通过',
    'material.receive_rejected': '领用申请已驳回',
    'material.batch_scrapped': '批次已报废',
  }
  return titleMap[eventName] || '业务数据已更新'
}

function realtimeKind(eventName: string): RealtimeNotice['kind'] {
  if (eventName.startsWith('lab.') || eventName.startsWith('reservation.')) return 'lab'
  if (eventName.startsWith('equipment.')) return 'equipment'
  if (eventName.startsWith('material.')) return 'material'
  if (eventName.startsWith('message.')) return 'message'
  return 'system'
}

function realtimeDetail(envelope: RealtimeEnvelope): string {
  const data = isRecord(envelope.data) ? envelope.data : {}
  const entityName = getStringField(data, ['labName', 'deviceName', 'materialName'])
  const applyNo = getStringField(data, ['applyNo', 'repairNo'])
  const fromStatusDesc = getStringField(data, ['fromStatusDesc'])
  const statusDesc = getStringField(data, ['statusDesc', 'applyStatusDesc', 'repairStatusDesc'])
  const quantity = getNumberField(data, 'quantity') ?? getNumberField(data, 'expectedQuantity')

  if (envelope.event === 'message.new') {
    return '消息中心和头部未读角标已刷新'
  }
  if (entityName && fromStatusDesc && statusDesc) {
    return `${entityName}：${fromStatusDesc} -> ${statusDesc}`
  }
  if (entityName && statusDesc) {
    return `${entityName}：${statusDesc}`
  }
  if (entityName && applyNo) {
    return `${entityName}，单号 ${applyNo}`
  }
  if (entityName && quantity !== null) {
    return `${entityName}，数量 ${quantity}`
  }
  if (entityName) {
    return entityName
  }
  if (applyNo) {
    return `单号 ${applyNo}`
  }
  return '前端已收到实时业务推送'
}

function clearRealtimeNoticeTimer(id: number): void {
  const timer = realtimeNoticeTimers.get(id)
  if (timer) {
    clearTimeout(timer)
    realtimeNoticeTimers.delete(id)
  }
}

function removeRealtimeNotice(id: number): void {
  clearRealtimeNoticeTimer(id)
  realtimeNotices.value = realtimeNotices.value.filter((notice) => notice.id !== id)
}

function pushRealtimeNotice(envelope: RealtimeEnvelope): void {
  const id = ++realtimeNoticeSeq
  if (realtimeNotices.value.length >= 4) {
    const staleNotice = realtimeNotices.value[realtimeNotices.value.length - 1]
    if (staleNotice) removeRealtimeNotice(staleNotice.id)
  }
  realtimeNotices.value = [
    {
      id,
      title: realtimeTitle(envelope.event),
      detail: realtimeDetail(envelope),
      time: formatRealtimeTime(envelope.occurredAt),
      kind: realtimeKind(envelope.event),
    },
    ...realtimeNotices.value,
  ]
  const timer = setTimeout(() => removeRealtimeNotice(id), 6000)
  realtimeNoticeTimers.set(id, timer)
}

function clearRealtimeNotices(): void {
  realtimeNoticeTimers.forEach((timer) => clearTimeout(timer))
  realtimeNoticeTimers.clear()
  realtimeNotices.value = []
}

function scheduleUnreadRefresh(): void {
  if (unreadRefreshTimer) return
  unreadRefreshTimer = setTimeout(() => {
    unreadRefreshTimer = null
    void fetchNewIds()
  }, 150)
}

function handleRealtimeEvent(event: Event): void {
  const realtimeEvent = event as CustomEvent<RealtimeEnvelope>
  if (!realtimeEvent.detail) return
  pushRealtimeNotice(realtimeEvent.detail)
}

function handleRealtimeMessageNew(_event: Event): void {
  scheduleUnreadRefresh()
}

watch([isLoggedIn, isMockMode], ([loggedIn, mockMode]) => {
  if (loggedIn && !mockMode) {
    void fetchNewIds()
    connectRealtime()
    return
  }
  disconnectRealtime()
  clearRealtimeNotices()
}, { immediate: true })

onMounted(() => {
  loadRole()
  window.addEventListener('auth:logout', handleAuthLogout)
  window.addEventListener(REALTIME_EVENT_NAME, handleRealtimeEvent as EventListener)
  window.addEventListener(REALTIME_MESSAGE_NEW_EVENT, handleRealtimeMessageNew)
})

onUnmounted(() => {
  window.removeEventListener('auth:logout', handleAuthLogout)
  window.removeEventListener(REALTIME_EVENT_NAME, handleRealtimeEvent as EventListener)
  window.removeEventListener(REALTIME_MESSAGE_NEW_EVENT, handleRealtimeMessageNew)
  if (logoutDebounceTimer) clearTimeout(logoutDebounceTimer)
  if (unreadRefreshTimer) clearTimeout(unreadRefreshTimer)
  disconnectRealtime()
  clearRealtimeNotices()
})

function handleNavigate({ key, name }: { key: string; name: string }): void {
  if (key === 'home') {
    currentPage.value = 'home'
    currentPageName.value = '首页'
  } else if (key === 'msg-inbox') {
    currentPage.value = 'messages'
    currentPageName.value = name
  } else if (key === 'user-detail') {
    currentPage.value = 'user-detail'
    currentPageName.value = name
  } else if (key === '3d-show') {
    currentPage.value = '3d-show'
    currentPageName.value = name
  } else if (key === 'my-course') {
    currentPage.value = 'my-course'
    currentPageName.value = name
  } else if (key === 'my-task') {
    currentPage.value = 'my-task'
    currentPageName.value = name
  } else if (key === 'my-grade') {
    currentPage.value = 'my-grade'
    currentPageName.value = name
  } else if (key === 'device-search') {
    currentPage.value = 'device-search'
    currentPageName.value = name
  } else if (key === 'my-borrow' || key === 'device-borrow') {
    currentPage.value = 'device-borrow'
    currentPageName.value = name
  } else if (key === 'my-reserve' || key === 'lab-reserve') {
    currentPage.value = 'lab-reserve'
    currentPageName.value = name
  } else if (key === 'lab-search') {
    currentPage.value = 'lab-search'
    currentPageName.value = name
  } else if (key === 'hazard-report') {
    currentPage.value = 'hazard-report'
    currentPageName.value = name
  } else if (key === 'my-report') {
    currentPage.value = 'my-report'
    currentPageName.value = name
  } else if (key === 'clean-arrange') {
    currentPage.value = 'clean-arrange'
    currentPageName.value = name
  } else if (key === 'clean-sign') {
    currentPage.value = 'clean-sign'
    currentPageName.value = name
  } else if (key === 'my-resource') {
    currentPage.value = 'my-resource'
    currentPageName.value = name
  } else if (key === 'my-score') {
    currentPage.value = 'my-score'
    currentPageName.value = name
  } else if (key === 'consumable-search') {
    currentPage.value = 'consumable-search'
    currentPageName.value = name
  } else if (key === 'consumable-apply') {
    currentPage.value = 'consumable-apply'
    currentPageName.value = name
  } else if (key === 'clean-evaluate') {
    currentPage.value = 'clean-evaluate'
    currentPageName.value = name
  } else if (key === 'person-manage') {
    currentPage.value = 'person-manage'
    currentPageName.value = name
  } else if (key === 'department-manage') {
    currentPage.value = 'department-manage'
    currentPageName.value = name
  } else if (key === 'position-manage') {
    currentPage.value = 'position-manage'
    currentPageName.value = name
  } else if (key === 'account-manage') {
    currentPage.value = 'account-manage'
    currentPageName.value = name
  } else if (key === 'device-manage') {
    currentPage.value = 'device-manage'
    currentPageName.value = name
  } else if (key === 'device-approve') {
    currentPage.value = 'device-approve'
    currentPageName.value = name
  } else if (key === 'device-repair') {
    currentPage.value = 'device-repair'
    currentPageName.value = name
  } else if (key === 'device-stats') {
    currentPage.value = 'device-stats'
    currentPageName.value = name
  } else if (key === 'lab-manage') {
    currentPage.value = 'lab-manage'
    currentPageName.value = name
  } else if (key === 'lab-approve') {
    currentPage.value = 'lab-approve'
    currentPageName.value = name
  } else if (key === 'lab-stats') {
    currentPage.value = 'lab-stats'
    currentPageName.value = name
  } else if (key === 'course-manage') {
    currentPage.value = 'course-manage'
    currentPageName.value = name
  } else if (key === 'resource-manage') {
    currentPage.value = 'resource-manage'
    currentPageName.value = name
  } else if (key === 'consumable-stock') {
    currentPage.value = 'consumable-stock'
    currentPageName.value = name
  } else if (key === 'consumable-pick') {
    currentPage.value = 'consumable-pick'
    currentPageName.value = name
  } else if (key === 'consumable-warn') {
    currentPage.value = 'consumable-warn'
    currentPageName.value = name
  } else if (key === 'safety-check') {
    currentPage.value = 'safety-check'
    currentPageName.value = name
  } else if (key === 'safety-hazard') {
    currentPage.value = 'safety-hazard'
    currentPageName.value = name
  } else if (key === 'safety-alarm') {
    currentPage.value = 'safety-alarm'
    currentPageName.value = name
  } else if (key === 'safety-ledger') {
    currentPage.value = 'safety-ledger'
    currentPageName.value = name
  } else if (key === 'clean-schedule') {
    currentPage.value = 'clean-schedule'
    currentPageName.value = name
  } else if (key === 'clean-review') {
    currentPage.value = 'clean-review'
    currentPageName.value = name
  } else if (key === 'iot-monitor') {
    currentPage.value = 'iot-monitor'
    currentPageName.value = name
  } else if (key === 'iot-threshold') {
    currentPage.value = 'iot-threshold'
    currentPageName.value = name
  } else if (key === 'sensor-device') {
    currentPage.value = 'sensor-device'
    currentPageName.value = name
  } else if (key === '3d-manager') {
    currentPage.value = '3d-manager'
    currentPageName.value = name
  } else if (key === 'efficiency') {
    currentPage.value = 'efficiency'
    currentPageName.value = name
  } else if (key === 'msg-template') {
    currentPage.value = 'msg-template'
    currentPageName.value = name
  } else if (key === 'msg-publish') {
    currentPage.value = 'msg-publish'
    currentPageName.value = name
  } else {
    currentPage.value = 'page'
    currentPageName.value = name
  }
}

function goHome(): void {
  currentPage.value = 'home'
  currentPageName.value = '首页'
}
</script>

<style scoped>
.app-container {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;
}

/* ===== 背景图层 ===== */
.bg-layer {
  position: absolute;
  inset: 0;
  background-image: url('./assets/background.webp');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.18;
}

/* ===== 毛玻璃内容面板（全屏覆盖） ===== */
.glass-panel {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;

  /* 轻透玻璃：极淡白底 + 极微模糊，不遮挡背景 */
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

/* ===== 主体内容区 ===== */
.main-content {
  flex: 1;
  overflow: hidden;
}

.content-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #555;
}

.placeholder-icon {
  font-size: 64px;
  display: block;
  margin-bottom: 16px;
  opacity: 0.7;
}

.placeholder-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.placeholder-sub {
  font-size: 14px;
  color: #999;
}

.realtime-toast-stack {
  position: absolute;
  right: 24px;
  bottom: 24px;
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: min(360px, calc(100% - 32px));
  pointer-events: none;
}

.realtime-toast {
  border-radius: 18px;
  padding: 14px 16px;
  color: #f4f7fb;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(7, 22, 41, 0.82);
  box-shadow: 0 18px 36px rgba(10, 28, 45, 0.22);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.realtime-toast.lab {
  border-left: 4px solid #2c9f77;
}

.realtime-toast.equipment {
  border-left: 4px solid #2276d2;
}

.realtime-toast.material {
  border-left: 4px solid #c7851b;
}

.realtime-toast.message {
  border-left: 4px solid #9b59b6;
}

.realtime-toast.system {
  border-left: 4px solid #607d8b;
}

.realtime-toast-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.realtime-toast-label {
  margin: 0 0 4px;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.72;
}

.realtime-toast-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.realtime-toast-time {
  font-size: 12px;
  color: rgba(244, 247, 251, 0.72);
  white-space: nowrap;
}

.realtime-toast-detail {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: rgba(244, 247, 251, 0.88);
}

.realtime-toast-enter-active,
.realtime-toast-leave-active {
  transition: all 0.28s ease;
}

.realtime-toast-enter-from,
.realtime-toast-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}

@media (max-width: 768px) {
  .realtime-toast-stack {
    right: 12px;
    left: 12px;
    bottom: 12px;
    width: auto;
  }
}
</style>
