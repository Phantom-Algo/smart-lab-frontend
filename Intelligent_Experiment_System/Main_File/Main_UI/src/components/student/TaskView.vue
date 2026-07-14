<template>
  <div class="task-page">
    <div class="page-header">
      <h2>实验任务</h2>
      <p>查看并完成实验课程任务</p>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-left">
        <div
          v-for="f in statusFilters"
          :key="f.key"
          class="filter-tab"
          :class="{ active: activeFilter === f.key }"
          @click="activeFilter = f.key"
        >
          {{ f.label }}
          <span v-if="f.count !== undefined" class="filter-count">{{ f.count }}</span>
        </div>
      </div>
      <div class="filter-right">
        <select v-model="selectedCourse" class="course-select">
          <option value="">全部课程</option>
          <option v-for="c in courseOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
    </div>

    <!-- 任务卡片网格 -->
    <div class="task-grid" v-if="filteredTasks.length > 0">
      <div
        v-for="task in filteredTasks"
        :key="task.id"
        class="task-card"
        @click="openTask(task)"
      >
        <div class="task-card-top">
          <div class="task-course-tag" :style="{ background: task.courseColor }">
            {{ task.courseName }}
          </div>
          <span class="task-status-tag" :class="'status-' + task.status">
            {{ statusLabel(task.status) }}
          </span>
        </div>
        <h4 class="task-name">{{ task.name }}</h4>
        <div class="task-meta">
          <div class="task-meta-row">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            <span>截止 {{ task.deadline }}</span>
          </div>
          <div class="task-meta-row" v-if="task.score !== undefined">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>
            <span :class="task.score >= 80 ? 'score-high' : task.score >= 60 ? 'score-mid' : 'score-low'">{{ task.score }} 分</span>
          </div>
        </div>
        <div class="task-card-footer">
          <div class="task-deadline-bar">
            <div class="deadline-fill" :style="{ width: deadlinePercent(task.deadline) + '%', background: task.courseColor }"></div>
          </div>
          <span class="task-action-text">{{ actionText(task.status) }}</span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
      <p>暂无符合条件的实验任务</p>
    </div>

    <!-- ===== 右侧任务详情抽屉 ===== -->
    <teleport to="body">
      <transition name="drawer-fade">
        <div v-if="selectedTask" class="drawer-overlay" @click.self="closeTask">
          <transition name="drawer-slide">
            <div v-if="selectedTask" class="drawer-panel">
              <!-- 抽屉头部 -->
              <div class="drawer-header">
                <div>
                  <div class="drawer-course" :style="{ color: selectedTask.courseColor }">{{ selectedTask.courseName }}</div>
                  <h3 class="drawer-title">{{ selectedTask.name }}</h3>
                </div>
                <button class="drawer-close" @click="closeTask">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                </button>
              </div>

              <!-- 抽屉内容 -->
              <div class="drawer-body">
                <!-- 基本信息 -->
                <div class="drawer-section">
                  <h4 class="drawer-section-title">任务信息</h4>
                  <div class="drawer-info-grid">
                    <div class="drawer-info-item">
                      <span class="di-label">截止时间</span>
                      <span class="di-value" :class="{ 'text-danger': isOverdue(selectedTask.deadline) && selectedTask.status === 'pending' }">{{ selectedTask.deadline }}</span>
                    </div>
                    <div class="drawer-info-item">
                      <span class="di-label">当前状态</span>
                      <span class="di-value status-tag-drawer" :class="'drawer-status-' + selectedTask.status">{{ statusLabel(selectedTask.status) }}</span>
                    </div>
                    <div class="drawer-info-item" v-if="selectedTask.score !== undefined">
                      <span class="di-label">成绩</span>
                      <span class="di-value fw700" :class="selectedTask.score >= 80 ? 'score-high' : selectedTask.score >= 60 ? 'score-mid' : 'score-low'">{{ selectedTask.score }} 分</span>
                    </div>
                    <div class="drawer-info-item" v-if="selectedTask.submittedAt">
                      <span class="di-label">提交时间</span>
                      <span class="di-value">{{ selectedTask.submittedAt }}</span>
                    </div>
                  </div>
                </div>

                <!-- 任务描述 -->
                <div class="drawer-section">
                  <h4 class="drawer-section-title">任务描述</h4>
                  <p class="drawer-desc">{{ selectedTask.description }}</p>
                </div>

                <!-- 附件资料 -->
                <div class="drawer-section" v-if="selectedTask.attachments.length > 0">
                  <h4 class="drawer-section-title">附件资料</h4>
                  <div class="attachment-list">
                    <div v-for="att in selectedTask.attachments" :key="att.name" class="attachment-item">
                      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                      <span class="att-name">{{ att.name }}</span>
                      <span class="att-type">{{ att.type }}</span>
                    </div>
                  </div>
                </div>

                <!-- 提交区域（仅待完成状态） -->
                <div class="drawer-section" v-if="selectedTask.status === 'pending'">
                  <h4 class="drawer-section-title">提交作业</h4>
                  <textarea
                    v-model="submitText"
                    class="submit-textarea"
                    placeholder="请输入你的实验结论或作答内容..."
                    rows="5"
                  ></textarea>
                  <div class="submit-file-area">
                    <div class="file-upload-zone">
                      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                      <span>点击或拖拽上传实验报告</span>
                    </div>
                  </div>
                  <button class="submit-btn" @click.stop="submitTask">提交任务</button>
                </div>

                <!-- 已提交/已批改的回显 -->
                <div class="drawer-section" v-if="selectedTask.status !== 'pending' && selectedTask.submitContent">
                  <h4 class="drawer-section-title">我的提交</h4>
                  <div class="submitted-content">{{ selectedTask.submitContent }}</div>
                </div>

                <!-- 教师反馈 -->
                <div class="drawer-section" v-if="selectedTask.feedback">
                  <h4 class="drawer-section-title">教师反馈</h4>
                  <div class="feedback-box">
                    <div class="feedback-header">
                      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                      <span>{{ selectedTask.courseName }} 教师</span>
                    </div>
                    <p class="feedback-text">{{ selectedTask.feedback }}</p>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTaskPage, getMySubmission, submitTask as submitTaskApi, type TaskListItem } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

interface Attachment { name: string; type: string }

interface Task {
  id: number; name: string; courseName: string; courseColor: string
  deadline: string; status: 'pending' | 'submitted' | 'graded'
  description: string; attachments: Attachment[]
  score?: number; feedback?: string; submittedAt?: string; submitContent?: string
}

const activeFilter = ref<string>('all')
const selectedCourse = ref<string>('')
const selectedTask = ref<Task | null>(null)
const submitText = ref('')
const tasks = ref<Task[]>([])

const statusFilters = computed(() => [
  { key: 'all', label: '全部', count: tasks.value.length },
  { key: 'pending', label: '待完成', count: tasks.value.filter(t => t.status === 'pending').length },
  { key: 'submitted', label: '已提交', count: tasks.value.filter(t => t.status === 'submitted').length },
  { key: 'graded', label: '已批改', count: tasks.value.filter(t => t.status === 'graded').length },
])

const MOCK_TASKS: Task[] = [
  {
    id: 1, name: '实验一：有机化合物的分离与纯化', courseName: '有机化学实验', courseColor: '#4A90D9',
    deadline: '2026-07-12 18:00', status: 'pending',
    description: '本实验要求掌握有机化合物的分离与纯化基本操作，包括蒸馏、萃取、重结晶等方法。请按照实验指导书完成实验操作，记录实验数据，撰写实验报告。实验报告需包含：实验目的、实验原理、实验步骤、实验数据记录与处理、结果分析与讨论。',
    attachments: [
      { name: '有机化学实验指导书-第三章', type: 'PDF' },
      { name: '实验一操作演示视频', type: 'MP4' },
      { name: '实验报告模板', type: 'DOCX' },
    ],
  },
  {
    id: 2, name: '实验二：红外光谱分析', courseName: '有机化学实验', courseColor: '#4A90D9',
    deadline: '2026-07-20 18:00', status: 'pending',
    description: '学习红外光谱仪的基本原理与操作方法，通过测定已知和未知有机化合物的红外光谱，掌握官能团的特征吸收峰识别方法。要求：1）测定至少3个标准样品；2）解析未知样品的红外光谱图；3）完成实验报告。',
    attachments: [
      { name: '红外光谱分析原理', type: 'PDF' },
      { name: '标准谱图库', type: 'ZIP' },
    ],
  },
  {
    id: 3, name: '实验一：力学基本测量', courseName: '大学物理实验', courseColor: '#27AE60',
    deadline: '2026-07-14 18:00', status: 'submitted', submittedAt: '2026-07-10 15:30',
    submitContent: '实验数据记录：\n1. 金属棒长度 L = 50.00 ± 0.02 cm\n2. 直径 d = 2.508 ± 0.002 mm\n3. 质量 m = 24.36 ± 0.01 g\n\n计算得到密度 ρ = 9.86 g/cm³\n\n误差分析：相对误差约 0.5%，主要来源于千分尺读数误差。',
    description: '掌握游标卡尺、螺旋测微计等基本测量工具的使用方法，学习有效数字与误差分析的基本概念。测量金属棒的长度、直径和质量，计算密度并进行误差分析。',
    attachments: [
      { name: '大学物理实验教程-第一章', type: 'PDF' },
      { name: '力学实验数据记录表', type: 'XLSX' },
    ],
  },
  {
    id: 4, name: '实验一：Python 数据分析基础', courseName: '计算机实验', courseColor: '#E67E22',
    deadline: '2026-07-18 18:00', status: 'graded', score: 95, submittedAt: '2026-07-12 10:20',
    submitContent: '完成了 NumPy 数组操作、Pandas 数据清洗、Matplotlib 可视化的三个练习模块，代码已上传至附件。',
    feedback: '完成度很高，代码规范，数据分析思路清晰。建议在数据可视化部分可以尝试更多图表类型。继续保持！',
    description: '学习使用 Python 进行数据分析的基本流程：数据读取、数据清洗、数据分析和可视化展示。使用 NumPy、Pandas、Matplotlib 库完成一个完整的数据分析项目。',
    attachments: [
      { name: 'Python 数据分析入门', type: 'PDF' },
      { name: '实验数据集 weather.csv', type: 'CSV' },
      { name: 'Jupyter Notebook 模板', type: 'IPYNB' },
    ],
  },
  {
    id: 5, name: '实验安全预习报告', courseName: '有机化学实验', courseColor: '#4A90D9',
    deadline: '2026-07-08 12:00', status: 'graded', score: 88, submittedAt: '2026-07-07 22:00',
    submitContent: '已阅读实验室安全规范，了解有机化学实验室的基本安全要求：防护用品使用、化学品安全数据查阅、紧急事故处理流程。',
    feedback: '预习报告内容完整，对安全规范理解到位。建议补充实验废弃物的分类处理方法。',
    description: '在进行有机化学实验之前，需要完成实验室安全规范的预习。查阅化学品安全技术说明书（MSDS），了解实验室紧急事故处理流程，完成预习报告。',
    attachments: [
      { name: '实验室安全规范手册', type: 'PDF' },
    ],
  },
  {
    id: 6, name: '实验二：电磁学实验', courseName: '大学物理实验', courseColor: '#27AE60',
    deadline: '2026-07-22 18:00', status: 'pending',
    description: '掌握示波器的使用方法，测量交流信号的电压、频率和相位。学习用惠斯通电桥测量电阻。实验完成后需提交完整实验报告，包括电路图、数据表格和误差分析。',
    attachments: [
      { name: '大学物理实验教程-第三章', type: 'PDF' },
      { name: '示波器使用指南', type: 'PDF' },
    ],
  },
]

async function loadTasks() {
  if (isMockMode.value) { tasks.value = [...MOCK_TASKS]; return }
  try {
    const res = await getTaskPage({ pageNum: 1, pageSize: 100 })
    if (res.records.length > 0) {
      const colors = ['#4A90D9', '#27AE60', '#E67E22']
      tasks.value = res.records.map((t, i) => ({
        id: t.id, name: t.taskName, courseName: t.courseName, courseColor: colors[i % colors.length],
        deadline: t.endTime || '', status: 'pending' as const, description: t.description || '',
        attachments: [], score: undefined, feedback: undefined, submittedAt: undefined
      }))
    } else tasks.value = [...MOCK_TASKS]
  } catch { /* API 不可用，请检查后端服务 */ }
}

onMounted(() => loadTasks())

const courseOptions = computed(() => {
  const seen = new Set<string>()
  return tasks.value.filter(t => { if (seen.has(t.courseName)) return false; seen.add(t.courseName); return true }).map(t => ({ id: t.courseName, name: t.courseName }))
})

const filteredTasks = computed(() => {
  let list = tasks.value
  if (activeFilter.value !== 'all') list = list.filter(t => t.status === activeFilter.value)
  if (selectedCourse.value) list = list.filter(t => t.courseName === selectedCourse.value)
  return list
})

function statusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待完成', submitted: '已提交', graded: '已批改' }
  return m[s] || s
}

function actionText(s: string): string {
  const m: Record<string, string> = { pending: '点击查看并提交', submitted: '等待批改中', graded: '查看详情与反馈' }
  return m[s] || ''
}

function deadlinePercent(deadline: string): number {
  // 模拟：如果截止日期在未来则按已过去的时间比例显示
  const dl = new Date(deadline)
  const now = new Date()
  // 假设任务从7天前发布
  const start = new Date(dl.getTime() - 7 * 24 * 3600 * 1000)
  const total = dl.getTime() - start.getTime()
  const elapsed = now.getTime() - start.getTime()
  const pct = Math.min(100, Math.max(0, (elapsed / total) * 100))
  return Math.round(pct)
}

function isOverdue(deadline: string): boolean {
  return new Date(deadline) < new Date()
}

function openTask(task: Task): void {
  selectedTask.value = task
  submitText.value = ''
}

function closeTask(): void {
  selectedTask.value = null
  submitText.value = ''
}

async function submitTask(): Promise<void> {
  if (!selectedTask.value) return
  if (isMockMode.value) {
    const idx = tasks.value.findIndex(t => t.id === selectedTask.value!.id)
    if (idx !== -1) {
      tasks.value[idx].status = 'submitted'
      tasks.value[idx].submittedAt = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
      tasks.value[idx].submitContent = submitText.value || '（仅上传文件，未填写文字内容）'
      selectedTask.value = { ...tasks.value[idx] }
    }
    return
  }
  try {
    await submitTaskApi(selectedTask.value.id, { content: submitText.value || undefined })
    await loadTasks()
    closeTask()
  } catch (e: any) { alert(e.message || '提交失败') }
}
</script>

<style scoped>
.task-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 筛选栏 ===== */
.filter-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.filter-left { display: flex; gap: 4px; background: rgba(255,255,255,.4); border-radius: 10px; padding: 4px; }
.filter-tab { padding: 8px 16px; border-radius: 8px; font-size: 13px; color: #666; cursor: pointer; transition: all .15s; display: flex; align-items: center; gap: 6px; user-select: none; }
.filter-tab:hover { color: #333; background: rgba(0,0,0,.03); }
.filter-tab.active { background: #fff; color: #1a73e8; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.filter-count { font-size: 11px; background: rgba(0,0,0,.06); padding: 1px 7px; border-radius: 10px; color: #888; }
.filter-tab.active .filter-count { background: rgba(26,115,232,.1); color: #1a73e8; }
.filter-right { }
.course-select { padding: 8px 14px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; font-size: 13px; color: #555; outline: none; background: rgba(255,255,255,.5); cursor: pointer; }
.course-select:focus { border-color: #1a73e8; }

/* ===== 卡片网格 ===== */
.task-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }

.task-card { background: rgba(255,255,255,.5); border-radius: 14px; padding: 20px 22px; border: 1px solid rgba(0,0,0,.04); cursor: pointer; transition: all .18s; }
.task-card:hover { background: rgba(255,255,255,.8); box-shadow: 0 4px 16px rgba(0,0,0,.06); }

.task-card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.task-course-tag { font-size: 11px; font-weight: 600; color: #fff; padding: 3px 10px; border-radius: 4px; }
.task-status-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 4px; }
.status-pending { background: rgba(243,156,18,.1); color: #e67e22; }
.status-submitted { background: rgba(26,115,232,.1); color: #1a73e8; }
.status-graded { background: rgba(39,174,96,.1); color: #27ae60; }

.task-name { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0 0 12px; line-height: 1.4; }

.task-meta { display: flex; flex-direction: column; gap: 5px; margin-bottom: 14px; }
.task-meta-row { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #999; }
.score-high { color: #27ae60; font-weight: 600; }
.score-mid { color: #e67e22; font-weight: 600; }
.score-low { color: #e74c3c; font-weight: 600; }

.task-card-footer { display: flex; align-items: center; gap: 10px; }
.task-deadline-bar { flex: 1; height: 4px; background: rgba(0,0,0,.06); border-radius: 2px; overflow: hidden; }
.deadline-fill { height: 100%; border-radius: 2px; transition: width .3s; }
.task-action-text { font-size: 12px; color: #1a73e8; white-space: nowrap; }

/* 空状态 */
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin-top: 12px; }

/* ===== 右侧抽屉 ===== */
.drawer-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,.25); backdrop-filter: blur(3px); -webkit-backdrop-filter: blur(3px); display: flex; justify-content: flex-end; }

.drawer-panel { width: 560px; max-width: 92vw; height: 100%; background: #fff; box-shadow: -8px 0 40px rgba(0,0,0,.12); display: flex; flex-direction: column; overflow: hidden; }

.drawer-header { display: flex; align-items: flex-start; justify-content: space-between; padding: 28px 32px 0; flex-shrink: 0; }
.drawer-course { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.drawer-title { font-size: 19px; font-weight: 700; color: #1a1a2e; margin: 0; line-height: 1.4; }
.drawer-close { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border: none; background: none; border-radius: 8px; cursor: pointer; color: #999; flex-shrink: 0; margin-left: 12px; }
.drawer-close:hover { background: #f5f5f5; color: #333; }

.drawer-body { flex: 1; overflow-y: auto; padding: 20px 32px 40px; }

.drawer-section { margin-bottom: 24px; }
.drawer-section-title { font-size: 13px; font-weight: 600; color: #aaa; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 12px; padding-bottom: 6px; border-bottom: 1px solid rgba(0,0,0,.05); }

.drawer-info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 20px; }
.drawer-info-item { display: flex; flex-direction: column; gap: 2px; }
.di-label { font-size: 12px; color: #aaa; }
.di-value { font-size: 14px; color: #333; font-weight: 500; }
.text-danger { color: #e74c3c; }
.fw700 { font-weight: 700; }
.status-tag-drawer { display: inline-block; }
.drawer-status-pending { color: #e67e22; }
.drawer-status-submitted { color: #1a73e8; }
.drawer-status-graded { color: #27ae60; }

.drawer-desc { font-size: 14px; color: #555; line-height: 1.8; margin: 0; }

/* 附件 */
.attachment-list { display: flex; flex-direction: column; gap: 6px; }
.attachment-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: rgba(0,0,0,.02); border-radius: 8px; cursor: pointer; transition: background .15s; color: #555; }
.attachment-item:hover { background: rgba(0,0,0,.05); }
.att-name { flex: 1; font-size: 13px; color: #333; }
.att-type { font-size: 11px; color: #fff; background: #888; padding: 1px 6px; border-radius: 3px; font-weight: 600; }

/* 提交区域 */
.submit-textarea { width: 100%; padding: 12px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; resize: vertical; font-family: inherit; transition: border .15s; box-sizing: border-box; }
.submit-textarea:focus { border-color: #1a73e8; }
.submit-textarea::placeholder { color: #ccc; }

.submit-file-area { margin-top: 12px; }
.file-upload-zone { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 24px; border: 2px dashed rgba(0,0,0,.1); border-radius: 10px; cursor: pointer; transition: all .15s; color: #aaa; font-size: 13px; }
.file-upload-zone:hover { border-color: #1a73e8; color: #1a73e8; background: rgba(26,115,232,.03); }

.submit-btn { margin-top: 16px; width: 100%; padding: 12px 0; border: none; border-radius: 10px; background: linear-gradient(135deg, #1a73e8, #4a90d9); color: #fff; font-size: 15px; font-weight: 600; cursor: pointer; transition: all .15s; }
.submit-btn:hover { box-shadow: 0 4px 14px rgba(26,115,232,.35); }

/* 已提交内容 */
.submitted-content { padding: 14px 16px; background: rgba(0,0,0,.02); border-radius: 8px; font-size: 13px; color: #555; line-height: 1.7; white-space: pre-wrap; }

/* 教师反馈 */
.feedback-box { padding: 14px 16px; background: rgba(26,115,232,.04); border-radius: 10px; border-left: 3px solid #1a73e8; }
.feedback-header { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #1a73e8; font-weight: 600; margin-bottom: 8px; }
.feedback-text { font-size: 14px; color: #444; line-height: 1.7; margin: 0; }
</style>

<style>
.drawer-fade-enter-active, .drawer-fade-leave-active { transition: all .25s ease; }
.drawer-fade-enter-from, .drawer-fade-leave-to { opacity: 0; }

.drawer-slide-enter-active { transition: all .3s cubic-bezier(.22, .61, .36, 1); }
.drawer-slide-leave-active { transition: all .2s ease-in; }
.drawer-slide-enter-from { transform: translateX(100%); }
.drawer-slide-leave-to { transform: translateX(100%); }
</style>
