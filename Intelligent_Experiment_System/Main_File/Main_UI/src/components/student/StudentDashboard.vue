<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="welcome-bar">
      <div class="welcome-text">
        <h2 class="welcome-greeting">你好，{{ userName }}</h2>
        <p class="welcome-date">{{ todayStr }}</p>
      </div>
      <div class="welcome-stats">
        <div class="stat-item">
          <span class="stat-num">{{ borrowCount }}</span>
          <span class="stat-label">借用中</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ pendingTaskCount }}</span>
          <span class="stat-label">待完成</span>
        </div>
        <div class="stat-item">
          <span class="stat-num">{{ unreadMsgCount }}</span>
          <span class="stat-label">未读消息</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <div
        v-for="action in quickActions"
        :key="action.key"
        class="action-card"
        @click="$emit('navigate', { key: action.key, name: action.label })"
      >
        <div class="action-icon" :style="{ background: action.color }">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8">
            <path :d="action.iconPath"/>
          </svg>
        </div>
        <span class="action-label">{{ action.label }}</span>
      </div>
    </div>

    <!-- 双栏：待办 + 动态 -->
    <div class="dashboard-row">
      <!-- 待办提醒 -->
      <div class="dash-card todo-card">
        <div class="card-header clickable" @click="$emit('navigate', { key: 'my-task', name: '实验任务' })">
          <h3>待办提醒</h3>
          <span class="card-badge">{{ todoList.length }}</span>
        </div>
        <div class="todo-list" v-if="todoList.length > 0">
          <div v-for="item in todoList" :key="item.id" class="todo-item">
            <span class="todo-dot" :class="item.urgent ? 'urgent' : 'normal'"></span>
            <div class="todo-body">
              <span class="todo-title">{{ item.title }}</span>
              <span class="todo-meta">{{ item.deadline }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无待办事项</div>
      </div>

      <!-- 借用动态 -->
      <div class="dash-card borrow-card">
        <div class="card-header clickable" @click="$emit('navigate', { key: 'device-borrow', name: '设备借用' })">
          <h3>设备借用</h3>
        </div>
        <div class="borrow-list" v-if="borrowItems.length > 0">
          <div v-for="item in borrowItems" :key="item.id" class="borrow-item">
            <span class="borrow-status" :class="item.status">
              {{ item.status === 'using' ? '借用中' : item.status === 'pending' ? '审批中' : '已逾期' }}
            </span>
            <span class="borrow-name">{{ item.name }}</span>
            <span class="borrow-date">{{ item.deadline }}</span>
          </div>
        </div>
        <div v-else class="empty-hint">暂无借用记录</div>
      </div>
    </div>

    <!-- 今日课程 -->
    <div class="dash-card course-card">
      <div class="card-header clickable" @click="$emit('navigate', { key: 'my-course', name: '我的课程' })">
        <h3>今日课程</h3>
        <span class="card-subtitle">{{ weekDay }}</span>
      </div>
      <div class="course-list" v-if="todayCourses.length > 0">
        <div v-for="course in todayCourses" :key="course.id" class="course-item">
          <div class="course-time">{{ course.time }}</div>
          <div class="course-divider"></div>
          <div class="course-info">
            <span class="course-name">{{ course.name }}</span>
            <span class="course-room">地点: {{ course.room }}</span>
          </div>
          <span class="course-status" :class="course.status">
            {{ course.status === 'upcoming' ? '即将开始' : course.status === 'ongoing' ? '进行中' : '已结束' }}
          </span>
        </div>
      </div>
      <div v-else class="empty-hint">今日暂无课程安排</div>
    </div>

    <!-- 3D实验室漫游 -->
    <div class="dash-card lab3d-card">
      <div class="card-header">
        <h3>3D 实验室漫游</h3>
        <span class="card-subtitle">点击进入虚拟实验室</span>
      </div>
      <div class="lab3d-grid">
        <div
          v-for="lab in lab3dList"
          :key="lab.key"
          class="lab3d-item"
          :style="{ '--lab-color': lab.color, '--lab-color-light': lab.colorLight }"
          @click="$emit('navigate', { key: '3d-show', name: lab.name })"
        >
          <div class="lab3d-preview">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" :stroke="lab.color" stroke-width="1.5">
              <path :d="lab.iconPath"/>
            </svg>
          </div>
          <span class="lab3d-name">{{ lab.name }}</span>
          <span class="lab3d-desc">{{ lab.desc }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { userName, userRole } from '../../stores/authStore'
import { useMessageStore } from '../../stores/messageStore'

defineEmits(['navigate'])

const { totalUnread } = useMessageStore()
const unreadMsgCount = computed(() => totalUnread.value)

// 日期
const now = new Date()
const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const todayStr = computed(() => `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日`)
const weekDay = computed(() => weekDays[now.getDay()])

// 快捷入口
const quickActions = [
  { key: 'my-course', label: '我的课程', color: '#4A90D9', iconPath: 'M4 6h16M4 10h16M4 14h16M4 18h16' },
  { key: 'device-borrow', label: '设备借用', color: '#27AE60', iconPath: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z' },
  { key: 'lab-search', label: '实验室查询', color: '#E67E22', iconPath: 'M3 21h18M9 21V3l-6 6M15 21V9l6 6' },
  { key: 'clean-arrange', label: '值日安排', color: '#8E44AD', iconPath: 'M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14l-5-4.87 6.91-1.01z' },
]

// 借用统计
const borrowCount = 1

// 待办列表
const todoList = [
  { id: 1, title: '有机化学实验报告提交', deadline: '截止: 7月12日 23:59', urgent: true },
  { id: 2, title: '实验室安全培训考核', deadline: '截止: 7月15日 17:00', urgent: true },
  { id: 3, title: '卫生值日 - A201实验室', deadline: '7月10日 08:00', urgent: false },
]
const pendingTaskCount = todoList.length

// 借用列表
const borrowItems = [
  { id: 1, name: '旋转蒸发仪 (EQU-2024-0001)', status: 'using', deadline: '应于7月10日归还' },
  { id: 2, name: '数字示波器 (EQU-2024-0015)', status: 'pending', deadline: '等待审批' },
]

// 今日课程
const todayCourses = [
  { id: 1, time: '08:00-10:00', name: '有机化学实验', room: 'A201有机化学实验室', status: 'ongoing' },
  { id: 2, time: '14:00-16:00', name: '大学物理实验', room: 'B103物理实验室', status: 'upcoming' },
]

// 3D实验室列表（对应Unity场景）
const lab3dList = [
  { key: '3d-class', name: '教学实验室', desc: '标准教学实验环境', color: '#4A90D9', colorLight: 'rgba(74,144,217,.1)', iconPath: 'M4 6h16M4 10h16M4 14h16M4 18h16M9 3v18' },
  { key: '3d-common', name: '通用实验室', desc: '多功能综合实验室', color: '#27AE60', colorLight: 'rgba(39,174,96,.1)', iconPath: 'M9 3H5a2 2 0 00-2 2v4m6-6h10a2 2 0 012 2v4M9 3v18m0 0h10a2 2 0 002-2V9M9 21H5a2 2 0 01-2-2V9m0 0h18' },
  { key: '3d-computer', name: '计算机实验室', desc: '计算机与网络实验', color: '#E67E22', colorLight: 'rgba(230,126,34,.1)', iconPath: 'M20 18V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12m16 0a2 2 0 01-2 2H6a2 2 0 01-2-2m16 0v-4M4 14v4m0-4h16' },
  { key: '3d-group', name: '小组研讨室', desc: '小组讨论与协作空间', color: '#8E44AD', colorLight: 'rgba(142,68,173,.1)', iconPath: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M23 21v-2a4 4 0 00-4-4h-1M7 7a4 4 0 108 0 4 4 0 00-8 0zm10-2a4 4 0 110 8' },
  { key: '3d-reagent', name: '器材借用室', desc: '试剂与器材领用管理', color: '#E74C3C', colorLight: 'rgba(231,76,60,.1)', iconPath: 'M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16zM3.27 6.96L12 12.01l8.73-5.05M12 22.08V12' },
]
</script>

<style scoped>
.dashboard {
  height: 100%;
  overflow-y: auto;
  padding: 28px 36px;
}

/* ===== 欢迎区 ===== */
.welcome-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.welcome-greeting {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.welcome-date {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.welcome-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: #1a73e8;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

/* ===== 快捷入口 ===== */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 22px 16px;
  background: rgba(255,255,255,.55);
  border-radius: 12px;
  cursor: pointer;
  transition: all .18s;
  border: 1px solid rgba(0,0,0,.04);
}
.action-card:hover {
  background: rgba(255,255,255,.85);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,.08);
}

.action-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-label {
  font-size: 13px;
  font-weight: 500;
  color: #444;
}

/* ===== 双栏卡片 ===== */
.dashboard-row {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.dash-card {
  flex: 1;
  background: rgba(255,255,255,.5);
  border-radius: 14px;
  padding: 20px 22px;
  border: 1px solid rgba(0,0,0,.04);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.card-header.clickable {
  cursor: pointer;
  user-select: none;
}
.card-header.clickable:hover h3 {
  color: #1a73e8;
}

.card-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.card-badge {
  min-width: 22px;
  height: 22px;
  border-radius: 11px;
  background: #e74c3c;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.card-subtitle {
  font-size: 12px;
  color: #999;
}

.empty-hint {
  text-align: center;
  padding: 28px 0;
  color: #bbb;
  font-size: 13px;
}

/* 待办 */
.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.todo-dot.urgent { background: #e74c3c; }
.todo-dot.normal { background: #f39c12; }

.todo-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.todo-title {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.todo-meta {
  font-size: 11px;
  color: #bbb;
}

/* 借用 */
.borrow-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.borrow-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.borrow-status {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  white-space: nowrap;
}
.borrow-status.using { background: rgba(26,115,232,.1); color: #1a73e8; }
.borrow-status.pending { background: rgba(243,156,18,.1); color: #e67e22; }
.borrow-status.overdue { background: rgba(231,76,60,.1); color: #e74c3c; }

.borrow-name {
  flex: 1;
  font-size: 13px;
  color: #333;
}

.borrow-date {
  font-size: 11px;
  color: #bbb;
  white-space: nowrap;
}

/* 课程 */
.course-card {
  margin-bottom: 0;
}

.course-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.course-time {
  font-size: 14px;
  font-weight: 600;
  color: #1a73e8;
  white-space: nowrap;
}

.course-divider {
  width: 3px;
  height: 28px;
  border-radius: 2px;
  background: #e0e0e0;
}

.course-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.course-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.course-room {
  font-size: 12px;
  color: #999;
}

.course-status {
  font-size: 11px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 4px;
  white-space: nowrap;
}
.course-status.ongoing { background: rgba(39,174,96,.1); color: #27ae60; }
.course-status.upcoming { background: rgba(26,115,232,.08); color: #1a73e8; }
.course-status.done { color: #bbb; }

/* ===== 3D实验室漫游 ===== */
.lab3d-card {
  margin-top: 24px;
}

.lab3d-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}

.lab3d-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 24px 12px 18px;
  border-radius: 14px;
  background: var(--lab-color-light, rgba(26,115,232,.06));
  border: 1px solid rgba(0,0,0,.04);
  cursor: pointer;
  transition: all .2s;
}
.lab3d-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 24px rgba(0,0,0,.08);
  border-color: var(--lab-color, #1a73e8);
}

.lab3d-preview {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255,255,255,.7);
  display: flex;
  align-items: center;
  justify-content: center;
}

.lab3d-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-top: 2px;
}

.lab3d-desc {
  font-size: 11px;
  color: #999;
  text-align: center;
}
</style>
