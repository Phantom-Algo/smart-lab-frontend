<template>
  <div class="dashboard">
    <!-- 欢迎区 -->
    <div class="welcome-bar">
      <div class="welcome-text">
        <h2 class="welcome-greeting">你好，{{ userName }}</h2>
        <p class="welcome-date">{{ todayStr }}</p>
      </div>
      <div class="welcome-stats">
        <div class="stat-item" @click="$emit('navigate', { key: 'my-course', name: '我的课程' })">
          <span class="stat-num">{{ todayCourseCount }}</span>
          <span class="stat-label">今日课程</span>
        </div>
        <div class="stat-item highlight" @click="$emit('navigate', { key: 'my-score', name: '成绩评定' })">
          <span class="stat-num orange">{{ pendingGradeCount }}</span>
          <span class="stat-label">待批改</span>
        </div>
        <div class="stat-item" @click="$emit('navigate', { key: 'my-borrow', name: '我的借用' })">
          <span class="stat-num">{{ borrowingCount }}</span>
          <span class="stat-label">借用中</span>
        </div>
        <div class="stat-item highlight" @click="$emit('navigate', { key: 'my-reserve', name: '我的预约' })">
          <span class="stat-num blue">{{ pendingReserveCount }}</span>
          <span class="stat-label">预约待审</span>
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

    <!-- 双栏布局 -->
    <div class="dashboard-row">
      <!-- 左栏：待批改任务 -->
      <div class="dash-card grade-card">
        <div class="card-header clickable" @click="$emit('navigate', { key: 'my-score', name: '成绩评定' })">
          <h3>待批改任务</h3>
          <span class="card-badge orange-bg">{{ pendingGradeCount }}</span>
        </div>
        <div class="grade-list" v-if="pendingGradeItems.length > 0">
          <div v-for="item in pendingGradeItems" :key="item.id" class="grade-item">
            <div class="grade-item-top">
              <span class="grade-course">{{ item.courseName }}</span>
              <span class="grade-count">{{ item.unchecked }} 份未批</span>
            </div>
            <div class="grade-item-bottom">
              <span class="grade-task">{{ item.taskName }}</span>
              <span class="grade-deadline" :class="{ overdue: item.overdue }">
                {{ item.overdue ? '已截止' : '截止 ' + item.deadline }}
              </span>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无待批改任务</div>
      </div>

      <!-- 右栏：我的借用 & 预约 -->
      <div class="dash-card resource-card">
        <div class="card-header clickable" @click="$emit('navigate', { key: 'my-resource', name: '教学资源' })">
          <h3>我的借用 & 预约</h3>
        </div>
        <!-- 设备借用 -->
        <div class="resource-section">
          <div class="section-label">🔬 设备借用</div>
          <div class="resource-list" v-if="borrowItems.length > 0">
            <div v-for="item in borrowItems" :key="'b'+item.id" class="resource-item">
              <span class="res-status" :class="item.status">
                {{ item.status === 'using' ? '借用中' : item.status === 'pending' ? '审批中' : '已逾期' }}
              </span>
              <span class="res-name">{{ item.name }}</span>
              <span class="res-date">{{ item.deadline }}</span>
            </div>
          </div>
          <div v-else class="empty-mini">暂无借用</div>
        </div>
        <!-- 实验室预约 -->
        <div class="resource-section">
          <div class="section-label">🏫 实验室预约</div>
          <div class="resource-list" v-if="reserveItems.length > 0">
            <div v-for="item in reserveItems" :key="'r'+item.id" class="resource-item">
              <span class="res-status" :class="item.status">
                {{ item.status === 'approved' ? '已通过' : item.status === 'pending' ? '审批中' : '已驳回' }}
              </span>
              <span class="res-name">{{ item.labName }}</span>
              <span class="res-date">{{ item.date }} {{ item.time }}</span>
            </div>
          </div>
          <div v-else class="empty-mini">暂无预约</div>
        </div>
      </div>
    </div>

    <!-- 今日课程 -->
    <div class="dash-card course-card">
      <div class="card-header clickable" @click="$emit('navigate', { key: 'my-course', name: '我的课程' })">
        <h3>今日课程</h3>
        <span class="card-badge">{{ todayCourseCount }}</span>
      </div>
      <div class="course-list" v-if="todayCourses.length > 0">
        <div v-for="c in todayCourses" :key="c.id" class="course-item">
          <div class="course-time">
            <span class="course-time-range">{{ c.time }}</span>
          </div>
          <div class="course-divider">
            <div class="course-dot"></div>
            <div class="course-line" v-if="c.id < todayCourses.length"></div>
          </div>
          <div class="course-info">
            <div class="course-name">{{ c.name }}</div>
            <div class="course-meta">{{ c.lab }} · {{ c.className }} · {{ c.studentCount }}人</div>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint">今日无课程安排</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { userName } from '../../stores/authStore'

defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

const todayStr = computed(() => {
  const d = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
})

// 统计
const todayCourseCount = 2
const pendingGradeCount = 12
const borrowingCount = 3
const pendingReserveCount = 1

// 快捷操作
const quickActions = [
  { key: 'my-task', label: '发布任务', color: 'linear-gradient(135deg, #1a73e8, #4a90d9)',
    iconPath: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M16 13H8 M16 17H8 M10 9H8' },
  { key: 'my-score', label: '成绩评定', color: 'linear-gradient(135deg, #e67e22, #f39c12)',
    iconPath: 'M9 11l3 3L22 4 M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11' },
  { key: 'lab-reserve', label: '场地预约', color: 'linear-gradient(135deg, #27ae60, #2ecc71)',
    iconPath: 'M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10' },
  { key: 'device-search', label: '设备借用', color: 'linear-gradient(135deg, #8e44ad, #9b59b6)',
    iconPath: 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4' },
]

// 待批改
interface GradeItem {
  id: number; courseName: string; taskName: string; unchecked: number; deadline: string; overdue: boolean
}
const pendingGradeItems: GradeItem[] = [
  { id: 1, courseName: '有机化学实验', taskName: '酯化反应实验报告', unchecked: 8, deadline: '7/9', overdue: false },
  { id: 2, courseName: '大学物理实验', taskName: '迈克尔逊干涉实验数据', unchecked: 4, deadline: '7/8', overdue: true },
  { id: 3, courseName: '程序设计实验', taskName: '数据结构综合实验报告', unchecked: 0, deadline: '7/12', overdue: false },
]

// 借用
interface BorrowItem {
  id: number; name: string; status: 'using' | 'pending' | 'overdue'; deadline: string
}
const borrowItems: BorrowItem[] = [
  { id: 1, name: '紫外分光光度计', status: 'using', deadline: '应还 7/10' },
  { id: 2, name: '高速离心机', status: 'pending', deadline: '提交 7/7' },
  { id: 3, name: '电子天平 (0.1mg)', status: 'using', deadline: '应还 7/15' },
]

// 预约
interface ReserveItem {
  id: number; labName: string; status: 'approved' | 'pending' | 'rejected'; date: string; time: string
}
const reserveItems: ReserveItem[] = [
  { id: 1, labName: 'A201 化学实验室', status: 'approved', date: '7/9', time: '14:00-17:00' },
  { id: 2, labName: 'B103 物理实验室', status: 'pending', date: '7/12', time: '08:00-12:00' },
]

// 今日课程
interface TodayCourse {
  id: number; time: string; name: string; lab: string; className: string; studentCount: number
}
const todayCourses: TodayCourse[] = [
  { id: 1, time: '08:00-10:00', name: '有机化学实验', lab: 'A201', className: '化工2401', studentCount: 32 },
  { id: 2, time: '14:00-17:00', name: '大学物理实验', lab: 'B103', className: '物理2401', studentCount: 28 },
]
</script>

<style scoped>
.dashboard { height: 100%; overflow-y: auto; padding: 28px 36px; }

/* ===== 欢迎区 ===== */
.welcome-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.welcome-text { }
.welcome-greeting { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 2px; }
.welcome-date { font-size: 13px; color: #999; margin: 0; }

.welcome-stats { display: flex; gap: 12px; }
.stat-item { background: rgba(255,255,255,.5); border-radius: 12px; padding: 12px 20px; text-align: center; cursor: pointer; border: 1px solid rgba(0,0,0,.04); transition: all .15s; min-width: 80px; }
.stat-item:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.06); }
.stat-item.highlight { border-color: rgba(0,0,0,.06); }
.stat-num { font-size: 24px; font-weight: 700; color: #1a1a2e; display: block; line-height: 1.2; }
.stat-num.blue { color: #1a73e8; }
.stat-num.orange { color: #e67e22; }
.stat-label { font-size: 11px; color: #999; }

/* ===== 快捷操作 ===== */
.quick-actions { display: flex; gap: 12px; margin-bottom: 20px; }
.action-card { flex: 1; display: flex; align-items: center; gap: 10px; padding: 14px 18px; background: rgba(255,255,255,.5); border-radius: 12px; border: 1px solid rgba(0,0,0,.04); cursor: pointer; transition: all .15s; }
.action-card:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.06); }
.action-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.action-label { font-size: 14px; font-weight: 600; color: #333; }

/* ===== 双栏 ===== */
.dashboard-row { display: flex; gap: 16px; margin-bottom: 20px; }

.dash-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 18px 20px; }
.grade-card { flex: 1; }
.resource-card { flex: 1; display: flex; flex-direction: column; gap: 14px; }

.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-header.clickable { cursor: pointer; user-select: none; }
.card-header.clickable:hover h3 { color: #1a73e8; }
.card-header h3 { font-size: 14px; font-weight: 600; color: #1a1a2e; margin: 0; }
.card-badge { font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: 10px; background: rgba(0,0,0,.05); color: #888; }
.card-badge.orange-bg { background: rgba(230,126,34,.12); color: #e67e22; }

/* 待批改列表 */
.grade-list { display: flex; flex-direction: column; gap: 10px; }
.grade-item { padding: 10px 12px; background: rgba(0,0,0,.015); border-radius: 8px; }
.grade-item-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 3px; }
.grade-course { font-size: 13px; font-weight: 600; color: #333; }
.grade-count { font-size: 12px; font-weight: 600; color: #e67e22; }
.grade-item-bottom { display: flex; align-items: center; justify-content: space-between; }
.grade-task { font-size: 12px; color: #888; }
.grade-deadline { font-size: 11px; color: #aaa; }
.grade-deadline.overdue { color: #e74c3c; font-weight: 600; }

/* 借用 & 预约 */
.resource-section { }
.section-label { font-size: 12px; font-weight: 600; color: #888; margin-bottom: 8px; }
.resource-list { display: flex; flex-direction: column; gap: 6px; }
.resource-item { display: flex; align-items: center; gap: 10px; padding: 8px 10px; background: rgba(0,0,0,.015); border-radius: 8px; }
.res-status { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 6px; white-space: nowrap; }
.res-status.using, .res-status.approved { background: rgba(39,174,96,.1); color: #27ae60; }
.res-status.pending { background: rgba(26,115,232,.1); color: #1a73e8; }
.res-status.overdue, .res-status.rejected { background: rgba(231,76,60,.1); color: #e74c3c; }
.res-name { font-size: 13px; color: #333; flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.res-date { font-size: 11px; color: #aaa; white-space: nowrap; }

.empty-mini { font-size: 12px; color: #ccc; text-align: center; padding: 10px 0; }

/* ===== 今日课程 ===== */
.course-card { }
.course-list { display: flex; flex-direction: column; }
.course-item { display: flex; gap: 14px; }
.course-time { width: 100px; flex-shrink: 0; text-align: right; padding-top: 2px; }
.course-time-range { font-size: 13px; font-weight: 600; color: #1a73e8; }
.course-divider { display: flex; flex-direction: column; align-items: center; width: 12px; flex-shrink: 0; }
.course-dot { width: 8px; height: 8px; border-radius: 50%; background: #1a73e8; margin-top: 6px; flex-shrink: 0; }
.course-line { width: 2px; flex: 1; background: rgba(26,115,232,.2); min-height: 20px; }
.course-info { flex: 1; padding-bottom: 16px; }
.course-name { font-size: 14px; font-weight: 600; color: #1a1a2e; margin-bottom: 2px; }
.course-meta { font-size: 12px; color: #999; }

.empty-hint { text-align: center; padding: 24px 0; font-size: 13px; color: #ccc; }
</style>
