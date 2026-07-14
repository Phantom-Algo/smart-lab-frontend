<template>
  <div class="grade-page">
    <div class="page-header">
      <h2>我的成绩</h2>
      <p>查看实验报告评分与课程成绩</p>
    </div>

    <!-- ===== 统计概览卡片 ===== -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(26,115,232,.1); color: #1a73e8;">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ overallAvg }}</span>
          <span class="stat-label">平均分</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(39,174,96,.1); color: #27ae60;">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ completedTasks }}</span>
          <span class="stat-label">已批改任务</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: rgba(230,126,34,.1); color: #e67e22;">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ highestScore }}</span>
          <span class="stat-label">最高分</span>
        </div>
      </div>
    </div>

    <!-- ===== 主体：左列表 + 右图表 ===== -->
    <div class="section-title-row">
      <h3>课程成绩明细</h3>
    </div>

    <div class="grade-body" v-if="courseGrades.length > 0">
      <!-- 左侧课程列表 -->
      <div class="course-list">
        <div
          v-for="course in courseGradesWithAvg"
          :key="course.id"
          class="course-group"
          :class="{ selected: selectedCourseId === course.id }"
        >
          <div class="course-group-header" @click="toggleCourse(course.id)">
            <div class="course-header-left">
              <div class="course-color-dot" :style="{ background: course.courseColor }"></div>
              <span class="course-header-name">{{ course.courseName }}</span>
              <span class="course-task-count">{{ course.tasks.length }} 项任务</span>
            </div>
            <div class="course-header-right">
              <div class="course-avg-badge" :class="scoreClass(course.average)">
                {{ course.average > 0 ? course.average.toFixed(1) : '--' }}
              </div>
              <svg
                class="collapse-arrow"
                :class="{ expanded: expandedCourses.has(course.id) }"
                viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"
              >
                <polyline points="6 9 12 15 18 9"/>
              </svg>
            </div>
          </div>

          <transition name="expand">
            <div v-if="expandedCourses.has(course.id)" class="course-task-list">
              <div v-for="task in course.tasks" :key="task.id" class="grade-row">
                <div class="grade-row-left">
                  <div class="grade-task-top">
                    <span class="grade-task-name">{{ task.taskName }}</span>
                    <span class="grade-task-weight">权重 {{ task.weight }}%</span>
                  </div>
                  <span v-if="task.feedback" class="grade-feedback-preview">{{ task.feedback }}</span>
                </div>
                <div class="grade-row-right">
                  <template v-if="task.status === 'graded'">
                    <span class="grade-score" :class="scoreClass(task.score!)">{{ task.score }}</span>
                    <span class="grade-max">/ {{ task.maxScore }}</span>
                  </template>
                  <span v-else class="grade-pending">待批改</span>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>

      <!-- 右侧成绩组成面板 -->
      <transition name="panel-fade">
        <div v-if="selectedCourse" class="detail-panel">
          <div class="panel-card">
            <h4 class="panel-card-title">成绩组成</h4>

            <!-- SVG 环形图 -->
            <div class="donut-wrapper">
              <svg viewBox="0 0 200 200" class="donut-svg">
                <circle cx="100" cy="100" r="78" fill="none" stroke="rgba(0,0,0,.04)" stroke-width="22"/>
                <circle
                  v-for="(seg, i) in donutSegments"
                  :key="i"
                  cx="100" cy="100" r="78" fill="none"
                  :stroke="seg.color"
                  stroke-width="22"
                  :stroke-dasharray="seg.dashArray"
                  :stroke-dashoffset="seg.dashOffset"
                  stroke-linecap="butt"
                  :transform="'rotate(-90 100 100)'"
                />
              </svg>
              <div class="donut-center">
                <span class="donut-score" :class="scoreClass(selectedCourse.average)">{{ weightedDisplay }}</span>
                <span class="donut-label">加权总分</span>
              </div>
            </div>

            <!-- 图例 -->
            <div class="donut-legend">
              <div v-for="seg in donutSegments" :key="seg.name" class="legend-item">
                <span class="legend-dot" :style="{ background: seg.color }"></span>
                <span class="legend-name">{{ seg.name }}</span>
                <span class="legend-weight">{{ seg.weight }}%</span>
              </div>
            </div>

            <!-- 加权公式卡片 -->
            <div class="formula-card">
              <div class="formula-title">加权计算</div>
              <div class="formula-body">
                <template v-for="(task, i) in selectedCourse.tasks" :key="task.id">
                  <span v-if="i > 0" class="formula-op">+</span>
                  <span class="formula-segment">
                    <span :class="task.status === 'graded' ? 'formula-score-graded' : 'formula-score-pending'">
                      {{ task.status === 'graded' ? task.score : '?' }}
                    </span>
                    <span class="formula-times">&times;</span>
                    <span class="formula-weight">{{ task.weight }}%</span>
                  </span>
                </template>
              </div>
              <div class="formula-result" v-if="weightedScore !== null">
                = <strong :class="scoreClass(weightedScore)">{{ weightedScore.toFixed(1) }}</strong>
                <span class="formula-note" v-if="hasPendingInCourse">（仅含已批改项）</span>
              </div>
              <div class="formula-result" v-else>
                <span class="formula-note">该课程暂无已批改任务</span>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>
      <p>暂无成绩记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMyGrades } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

interface TaskGrade { id: number; taskName: string; score?: number; maxScore: number; weight: number; status: 'graded' | 'pending'; feedback?: string }
interface CourseGrade { id: number; courseName: string; courseColor: string; tasks: TaskGrade[] }
interface DonutSegment { name: string; weight: number; color: string; dashArray: string; dashOffset: string }

const expandedCourses = ref(new Set<number>())
const selectedCourseId = ref<number | null>(null)
const courseGrades = ref<CourseGrade[]>([])

const MOCK_GRADES: CourseGrade[] = [
  {
    id: 1, courseName: '有机化学实验', courseColor: '#4A90D9',
    tasks: [
      { id: 1, taskName: '实验一：分离与纯化', score: undefined, maxScore: 100, weight: 40, status: 'pending' },
      { id: 2, taskName: '实验二：红外光谱分析', score: undefined, maxScore: 100, weight: 30, status: 'pending' },
      { id: 3, taskName: '实验安全预习报告', score: 92, maxScore: 100, weight: 30, status: 'graded', feedback: '预习报告内容完整，对安全规范理解到位。' },
    ],
  },
  {
    id: 2, courseName: '大学物理实验', courseColor: '#27AE60',
    tasks: [
      { id: 4, taskName: '实验一：力学基本测量', score: 75, maxScore: 100, weight: 50, status: 'graded', feedback: '实验数据记录规范，误差分析基本正确。' },
      { id: 5, taskName: '实验二：电磁学实验', score: undefined, maxScore: 100, weight: 50, status: 'pending' },
    ],
  },
  {
    id: 3, courseName: '计算机实验', courseColor: '#E67E22',
    tasks: [
      { id: 6, taskName: 'Python 数据分析基础', score: 95, maxScore: 100, weight: 50, status: 'graded', feedback: '完成度很高，代码规范，数据分析思路清晰。' },
      { id: 7, taskName: '数据库设计与查询', score: 90, maxScore: 100, weight: 50, status: 'graded', feedback: 'SQL 语句编写规范，索引设计合理。' },
    ],
  },
  {
    id: 4, courseName: '物理实验', courseColor: '#8E44AD',
    tasks: [
      { id: 8, taskName: '实验一：光学基础实验', score: undefined, maxScore: 100, weight: 100, status: 'pending' },
    ],
  },
]

async function loadGrades() {
  if (isMockMode.value) { courseGrades.value = [...MOCK_GRADES]; initExpand(); return }
  try {
    const res = await getMyGrades()
    if (res.length > 0) {
      const colorMap: Record<string, string> = {}
      const colors = ['#4A90D9', '#27AE60', '#E67E22', '#8E44AD']
      let ci = 0
      const grouped: Record<string, { tasks: TaskGrade[]; color: string }> = {}
      for (const g of res) {
        if (!grouped[g.courseName]) { grouped[g.courseName] = { tasks: [], color: colors[ci++ % colors.length] } }
        grouped[g.courseName].tasks.push({ id: Date.now() + Math.random(), taskName: g.taskName, score: g.score, maxScore: 100, weight: 50, status: g.score != null ? 'graded' as const : 'pending' as const, feedback: g.comment || undefined })
      }
      courseGrades.value = Object.entries(grouped).map(([name, v], i) => ({ id: i + 1, courseName: name, courseColor: v.color, tasks: v.tasks }))
    } else courseGrades.value = [...MOCK_GRADES]
  } catch { /* API 不可用，请检查后端服务 */ }
  initExpand()
}

function initExpand() { if (courseGrades.value.length > 0) { expandedCourses.value.add(courseGrades.value[0].id); selectedCourseId.value = courseGrades.value[0].id } }

onMounted(() => loadGrades())

const allGradedTasks = computed(() =>
  courseGrades.value.flatMap(c => c.tasks.filter(t => t.status === 'graded' && t.score !== undefined))
)

const overallAvg = computed(() => {
  const graded = allGradedTasks.value
  if (graded.length === 0) return '--'
  const sum = graded.reduce((acc, t) => acc + (t.score || 0), 0)
  return (sum / graded.length).toFixed(1)
})

const completedTasks = computed(() => allGradedTasks.value.length)

const highestScore = computed(() => {
  const graded = allGradedTasks.value
  if (graded.length === 0) return '--'
  return Math.max(...graded.map(t => t.score || 0))
})

const courseGradesWithAvg = computed(() =>
  courseGrades.value.map(c => {
    const graded = c.tasks.filter(t => t.status === 'graded' && t.score !== undefined)
    const avg = graded.length > 0 ? graded.reduce((acc, t) => acc + (t.score || 0), 0) / graded.length : 0
    return { ...c, average: avg }
  })
)

// ===== 当前选中课程 =====
const selectedCourse = computed(() => selectedCourseId.value === null ? null : courseGradesWithAvg.value.find(c => c.id === selectedCourseId.value) || null)

const hasPendingInCourse = computed(() =>
  selectedCourse.value ? selectedCourse.value.tasks.some(t => t.status === 'pending') : false
)

const weightedScore = computed(() => {
  const course = selectedCourse.value
  if (!course) return null
  const graded = course.tasks.filter(t => t.status === 'graded' && t.score !== undefined)
  if (graded.length === 0) return null
  const totalWeight = graded.reduce((acc, t) => acc + t.weight, 0)
  if (totalWeight === 0) return null
  const weighted = graded.reduce((acc, t) => acc + (t.score || 0) * t.weight, 0) / totalWeight
  return Math.round(weighted * 10) / 10
})

const weightedDisplay = computed(() => {
  if (weightedScore.value === null) return '--'
  return weightedScore.value.toFixed(1)
})

// ===== 环形图数据 =====
const donutSegments = computed((): DonutSegment[] => {
  const course = selectedCourse.value
  if (!course) return []

  const circumference = 2 * Math.PI * 78
  const gradedColor = course.courseColor
  const pendingColor = '#e8e8e8'

  let offset = 0
  return course.tasks.map((task, i) => {
    const pct = task.weight / 100
    const dashLen = circumference * pct
    const gap = course.tasks.length > 1 ? 1.5 : 0
    const seg: DonutSegment = {
      name: task.taskName.length > 8 ? task.taskName.substring(0, 8) + '...' : task.taskName,
      weight: task.weight,
      color: task.status === 'graded' ? gradedColor : pendingColor,
      dashArray: `${dashLen - gap} ${circumference - dashLen + gap}`,
      dashOffset: String(-offset),
    }
    offset += dashLen
    return seg
  })
})

// ===== 交互 =====
function toggleCourse(id: number): void {
  if (expandedCourses.value.has(id)) {
    expandedCourses.value.delete(id)
    if (selectedCourseId.value === id) {
      selectedCourseId.value = null
    }
  } else {
    expandedCourses.value.add(id)
    selectedCourseId.value = id
  }
}

function scoreClass(score: number | undefined): string {
  if (score === undefined || score === 0) return 'score-none'
  if (score >= 80) return 'score-high'
  if (score >= 60) return 'score-mid'
  return 'score-low'
}


</script>

<style scoped>
.grade-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 统计概览 ===== */
.stat-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 28px; }
.stat-card { background: rgba(255,255,255,.5); border-radius: 14px; padding: 20px 24px; border: 1px solid rgba(0,0,0,.04); display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-value { font-size: 24px; font-weight: 700; color: #1a1a2e; line-height: 1; }
.stat-label { font-size: 13px; color: #999; }

.section-title-row { margin-bottom: 14px; }
.section-title-row h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }

/* ===== 两栏布局 ===== */
.grade-body { display: flex; gap: 24px; align-items: flex-start; }

/* ===== 左侧课程列表 ===== */
.course-list { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 10px; }

.course-group { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); overflow: hidden; transition: border-color .2s; }
.course-group.selected { border-color: rgba(26,115,232,.2); }

.course-group-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; cursor: pointer; transition: background .15s; user-select: none; }
.course-group-header:hover { background: rgba(0,0,0,.015); }

.course-header-left { display: flex; align-items: center; gap: 10px; }
.course-color-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.course-header-name { font-size: 15px; font-weight: 600; color: #1a1a2e; }
.course-task-count { font-size: 12px; color: #aaa; }

.course-header-right { display: flex; align-items: center; gap: 10px; }
.course-avg-badge { font-size: 18px; font-weight: 700; min-width: 42px; text-align: right; }
.course-avg-badge.score-high { color: #27ae60; }
.course-avg-badge.score-mid { color: #e67e22; }
.course-avg-badge.score-low { color: #e74c3c; }
.course-avg-badge.score-none { color: #ccc; font-size: 14px; font-weight: 500; }

.collapse-arrow { color: #bbb; transition: transform .25s ease; flex-shrink: 0; }
.collapse-arrow.expanded { transform: rotate(180deg); }

/* 成绩行 */
.course-task-list { border-top: 1px solid rgba(0,0,0,.04); }
.grade-row { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; transition: background .1s; }
.grade-row + .grade-row { border-top: 1px solid rgba(0,0,0,.03); }
.grade-row:hover { background: rgba(0,0,0,.01); }

.grade-row-left { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }
.grade-task-top { display: flex; align-items: center; gap: 8px; }
.grade-task-name { font-size: 13px; color: #333; }
.grade-task-weight { font-size: 11px; color: #aaa; background: rgba(0,0,0,.04); padding: 1px 6px; border-radius: 3px; }
.grade-feedback-preview { font-size: 12px; color: #aaa; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 360px; }

.grade-row-right { display: flex; align-items: baseline; gap: 2px; flex-shrink: 0; margin-left: 12px; }
.grade-score { font-size: 17px; font-weight: 700; }
.grade-score.score-high { color: #27ae60; }
.grade-score.score-mid { color: #e67e22; }
.grade-score.score-low { color: #e74c3c; }
.grade-max { font-size: 12px; color: #bbb; }
.grade-pending { font-size: 13px; color: #ccc; font-weight: 500; }

/* ===== 右侧面板 ===== */
.detail-panel { width: 320px; flex-shrink: 0; position: sticky; top: 0; }

.panel-card { background: rgba(255,255,255,.55); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 24px 22px; }
.panel-card-title { font-size: 14px; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 20px; text-align: center; }

/* 环形图 */
.donut-wrapper { position: relative; width: 180px; height: 180px; margin: 0 auto 16px; }
.donut-svg { width: 100%; height: 100%; display: block; }
.donut-center { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.donut-score { font-size: 28px; font-weight: 700; line-height: 1; }
.donut-score.score-high { color: #27ae60; }
.donut-score.score-mid { color: #e67e22; }
.donut-score.score-low { color: #e74c3c; }
.donut-score.score-none { color: #ccc; }
.donut-label { font-size: 11px; color: #aaa; margin-top: 3px; }

/* 图例 */
.donut-legend { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.legend-item { display: flex; align-items: center; gap: 8px; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-name { flex: 1; font-size: 12px; color: #555; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.legend-weight { font-size: 12px; color: #aaa; }

/* 加权公式 */
.formula-card { background: rgba(0,0,0,.02); border-radius: 10px; padding: 14px 16px; }
.formula-title { font-size: 12px; font-weight: 600; color: #aaa; margin-bottom: 10px; }
.formula-body { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 2px; font-size: 13px; margin-bottom: 8px; }
.formula-op { color: #ccc; margin: 0 1px; }
.formula-segment { display: flex; align-items: center; gap: 1px; }
.formula-score-graded { color: #333; font-weight: 600; }
.formula-score-pending { color: #ccc; }
.formula-times { color: #bbb; font-size: 11px; }
.formula-weight { color: #999; font-size: 11px; }
.formula-result { font-size: 14px; color: #555; padding-top: 8px; border-top: 1px solid rgba(0,0,0,.05); }
.formula-result strong { margin-left: 4px; }
.formula-note { font-size: 11px; color: #bbb; margin-left: 4px; }

/* 空状态 */
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin-top: 12px; }

/* 动画 */
.expand-enter-active, .expand-leave-active { transition: all .25s ease; overflow: hidden; }
.expand-enter-from, .expand-leave-to { opacity: 0; max-height: 0; }
.expand-enter-to, .expand-leave-from { opacity: 1; max-height: 600px; }
</style>

<style>
.panel-fade-enter-active, .panel-fade-leave-active { transition: all .25s ease; }
.panel-fade-enter-from, .panel-fade-leave-to { opacity: 0; transform: translateX(12px); }
</style>
