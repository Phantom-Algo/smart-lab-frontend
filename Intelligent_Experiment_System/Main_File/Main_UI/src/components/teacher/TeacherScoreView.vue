<template>
  <div class="score-page">
    <div class="page-header">
      <h2>成绩评定</h2>
      <p>批改实验报告、录入学生成绩</p>
    </div>

    <!-- 无选中任务时的任务列表 -->
    <template v-if="!selectedTask">
      <!-- 筛选 -->
      <div class="filter-tabs">
        <button v-for="t in tabs" :key="t.key" class="filter-btn" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
          {{ t.label }}
          <span class="tab-count" v-if="t.count">{{ t.count }}</span>
        </button>
      </div>

      <!-- 任务列表 -->
      <div class="task-grid">
        <div v-for="task in filteredTasks" :key="task.id" class="task-card" @click="selectedTask = task">
          <div class="card-course">{{ task.courseName }}</div>
          <div class="card-title">{{ task.title }}</div>
          <div class="card-meta">
            <span>{{ task.className }}</span>
            <span class="meta-sep">·</span>
            <span>截止 {{ task.deadline }}</span>
          </div>
          <div class="card-bottom">
            <div class="card-progress">
              <div class="prog-bar">
                <div class="prog-fill" :style="{ width: (task.gradedCount/task.submitCount*100) + '%' }"></div>
              </div>
              <span class="prog-text">{{ task.gradedCount }}/{{ task.submitCount }} 已批改</span>
            </div>
            <span class="card-status" :class="task.gradedCount >= task.submitCount ? 'done' : 'pending'">
              {{ task.gradedCount >= task.submitCount ? '已完成' : '待批改' }}
            </span>
          </div>
        </div>
        <div v-if="filteredTasks.length === 0" class="empty-state">暂无需要评定的任务</div>
      </div>
    </template>

    <!-- 选中任务后的批改详情 -->
    <template v-else>
      <div class="back-row">
        <button class="back-btn" @click="selectedTask = null">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          返回任务列表
        </button>
        <div class="task-info-bar">
          <span class="task-info-name">{{ selectedTask.title }}</span>
          <span class="task-info-meta">{{ selectedTask.courseName }} · {{ selectedTask.className }}</span>
        </div>
      </div>

      <!-- 学生提交列表 -->
      <div class="student-list">
        <div v-for="s in students" :key="s.id" class="student-card" :class="{ graded: s.graded }">
          <div class="stu-left">
            <div class="stu-avatar">{{ s.name.charAt(0) }}</div>
            <div class="stu-info">
              <div class="stu-name">{{ s.name }}</div>
              <div class="stu-no">{{ s.studentNo }}</div>
            </div>
          </div>
          <div class="stu-mid">
            <div class="stu-submit" v-if="s.submitted">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              <span>{{ s.fileName }}</span>
            </div>
            <span v-else class="stu-nosubmit">未提交</span>
            <span class="stu-time" v-if="s.submitted">{{ s.submitTime }}</span>
          </div>
          <div class="stu-right">
            <template v-if="s.submitted">
              <div class="score-input-row" v-if="!s.graded">
                <input class="score-input" v-model="s.tempScore" placeholder="分数" type="number" min="0" max="100" />
                <span class="score-unit">分</span>
              </div>
              <div class="score-display" v-else>
                <span class="score-val" :class="scoreClass(s.score || 0)">{{ s.score }} 分</span>
              </div>
              <input v-if="!s.graded" class="comment-input" v-model="s.tempComment" placeholder="评语（可选）" />
              <span v-else class="stu-comment" v-if="s.comment">{{ s.comment }}</span>
              <button v-if="!s.graded" class="submit-score-btn" @click="submitScore(s)">提交</button>
              <button v-else class="edit-score-btn" @click="s.graded = false">修改</button>
            </template>
            <span v-else class="stu-waiting">等待提交</span>
          </div>
        </div>
      </div>

      <!-- 批量操作 -->
      <div class="batch-bar" v-if="pendingCount > 0">
        <span class="batch-hint">还有 {{ pendingCount }} 位学生待批改</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { getTaskPage, getTaskSubmissions, submitGrades, type TaskListItem, type GradeItem } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

const activeTab = ref('grading')
const selectedTask = ref<Task | null>(null)

interface Task {
  id: number; title: string; courseName: string; className: string
  deadline: string; studentCount: number; submitCount: number; gradedCount: number
  status: 'grading' | 'done'
}

const MOCK_TASKS: Task[] = [
  { id: 1, title: '酯化反应实验报告', courseName: '有机化学实验', className: '化工2401班', deadline: '7/9 23:59', studentCount: 32, submitCount: 24, gradedCount: 9, status: 'grading' },
  { id: 2, title: '蒸馏与沸点测定', courseName: '有机化学实验', className: '化工2402班', deadline: '7/10 23:59', studentCount: 30, submitCount: 22, gradedCount: 8, status: 'grading' },
  { id: 3, title: '红外光谱分析实验', courseName: '有机化学实验', className: '应化2401班', deadline: '7/14 23:59', studentCount: 28, submitCount: 10, gradedCount: 10, status: 'grading' },
  { id: 4, title: '分光光度法测定铁含量', courseName: '有机化学实验', className: '化工2401班', deadline: '7/15 23:59', studentCount: 32, submitCount: 16, gradedCount: 16, status: 'done' },
  { id: 5, title: '薄层色谱分离', courseName: '有机化学实验', className: '化工2402班', deadline: '7/18 23:59', studentCount: 30, submitCount: 5, gradedCount: 5, status: 'done' },
  { id: 6, title: '熔点测定实验', courseName: '有机化学实验', className: '应化2401班', deadline: '7/20 23:59', studentCount: 28, submitCount: 0, gradedCount: 0, status: 'done' },
]

const tasks = ref<Task[]>([])

async function loadTasks() {
  if (isMockMode.value) { tasks.value = [...MOCK_TASKS]; return }
  try {
    const res = await getTaskPage({ pageNum: 1, pageSize: 100 })
    if (res.records.length > 0) {
      tasks.value = res.records.map(t => ({ id: t.id, title: t.taskName, courseName: t.courseName, className: '', deadline: t.endTime?.slice(0, 10) || '', studentCount: 30, submitCount: 15, gradedCount: 10, status: t.gradedCount && t.gradedCount >= (t.submitCount || 15) ? 'done' as const : 'grading' as const }))
    } else tasks.value = [...MOCK_TASKS]
  } catch { /* API 不可用，请检查后端服务 */ }
}

const tabs = computed(() => [
  { key: 'all', label: '全部', count: tasks.value.length },
  { key: 'grading', label: '待批改', count: tasks.value.filter(t => t.gradedCount < t.submitCount).length },
  { key: 'done', label: '已完成', count: tasks.value.filter(t => t.gradedCount >= t.submitCount).length },
])

onMounted(() => loadTasks())

const filteredTasks = computed(() => {
  if (activeTab.value === 'all') return tasks.value
  if (activeTab.value === 'grading') return tasks.value.filter(t => t.gradedCount < t.submitCount)
  return tasks.value.filter(t => t.gradedCount >= t.submitCount)
})

// 学生列表（模拟选中任务1的提交情况）
interface Student {
  id: number; name: string; studentNo: string; submitted: boolean
  fileName?: string; submitTime?: string
  graded: boolean; score?: number; comment?: string
  tempScore?: string; tempComment?: string
}

const students = reactive<Student[]>([
  { id: 1, name: '张明', studentNo: 'S20240001', submitted: true, fileName: '酯化反应实验报告_张明.pdf', submitTime: '7/8 18:30', graded: true, score: 92, comment: '实验数据完整，分析透彻' },
  { id: 2, name: '李华', studentNo: 'S20240002', submitted: true, fileName: '酯化反应实验报告_李华.docx', submitTime: '7/8 20:15', graded: true, score: 85, comment: '操作步骤正确，产率计算有误' },
  { id: 3, name: '王芳', studentNo: 'S20240003', submitted: true, fileName: '酯化反应实验报告_王芳.pdf', submitTime: '7/9 10:00', graded: false, tempScore: '', tempComment: '' },
  { id: 4, name: '赵磊', studentNo: 'S20240004', submitted: true, fileName: '酯化反应实验报告_赵磊.docx', submitTime: '7/9 14:20', graded: false, tempScore: '', tempComment: '' },
  { id: 5, name: '陈静', studentNo: 'S20240005', submitted: false, graded: false },
  { id: 6, name: '刘洋', studentNo: 'S20240006', submitted: true, fileName: '酯化反应实验报告_刘洋.pdf', submitTime: '7/8 22:45', graded: true, score: 78, comment: '数据处理需加强' },
])

const pendingCount = computed(() => students.filter(s => s.submitted && !s.graded).length)

function submitScore(s: Student): void {
  const score = parseInt(s.tempScore || '0')
  if (score < 0 || score > 100) return
  s.score = score
  s.comment = s.tempComment || undefined
  s.graded = true
  // update task count
  if (selectedTask.value) {
    selectedTask.value.gradedCount++
    if (selectedTask.value.gradedCount >= selectedTask.value.submitCount) {
      selectedTask.value.status = 'done'
    }
  }
}

function scoreClass(s: number): string {
  return s >= 90 ? 'green' : s >= 75 ? 'orange' : 'red'
}
</script>

<style scoped>
.score-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 筛选 ===== */
.filter-tabs { display: flex; gap: 4px; margin-bottom: 18px; }
.filter-btn { padding: 7px 16px; border: 1px solid rgba(0,0,0,.06); border-radius: 8px; background: rgba(255,255,255,.4); font-size: 13px; color: #888; cursor: pointer; transition: all .15s; display: flex; align-items: center; gap: 6px; }
.filter-btn:hover { border-color: #1a73e8; color: #1a73e8; }
.filter-btn.active { background: rgba(26,115,232,.1); border-color: #1a73e8; color: #1a73e8; font-weight: 600; }
.tab-count { font-size: 11px; padding: 1px 7px; border-radius: 8px; background: rgba(0,0,0,.05); }
.filter-btn.active .tab-count { background: rgba(26,115,232,.15); }

/* ===== 任务卡片网格 ===== */
.task-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.task-card { background: rgba(255,255,255,.5); border-radius: 12px; border: 1px solid rgba(0,0,0,.04); padding: 18px 20px; cursor: pointer; transition: all .15s; }
.task-card:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); box-shadow: 0 4px 14px rgba(0,0,0,.06); }

.card-course { font-size: 11px; color: #1a73e8; font-weight: 500; margin-bottom: 6px; }
.card-title { font-size: 15px; font-weight: 700; color: #1a1a2e; margin-bottom: 4px; line-height: 1.3; }
.card-meta { font-size: 12px; color: #999; margin-bottom: 14px; display: flex; align-items: center; gap: 4px; }
.meta-sep { color: #ddd; }

.card-bottom { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.card-progress { flex: 1; min-width: 0; }
.prog-bar { height: 5px; background: rgba(0,0,0,.06); border-radius: 3px; overflow: hidden; margin-bottom: 4px; }
.prog-fill { height: 100%; background: #1a73e8; border-radius: 3px; transition: width .3s; }
.prog-text { font-size: 11px; color: #aaa; }
.card-status { font-size: 11px; font-weight: 600; padding: 2px 10px; border-radius: 10px; }
.card-status.pending { background: rgba(230,126,34,.1); color: #e67e22; }
.card-status.done { background: rgba(39,174,96,.1); color: #27ae60; }

.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: #ccc; font-size: 14px; }

/* ===== 批改详情 ===== */
.back-row { display: flex; align-items: center; gap: 20px; margin-bottom: 20px; }
.back-btn { display: flex; align-items: center; gap: 4px; padding: 6px 14px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; background: #fff; font-size: 13px; color: #666; cursor: pointer; transition: all .15s; }
.back-btn:hover { border-color: #1a73e8; color: #1a73e8; }
.task-info-bar { }
.task-info-name { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.task-info-meta { font-size: 12px; color: #999; margin-left: 10px; }

/* 学生列表 */
.student-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.student-card { display: flex; align-items: center; gap: 20px; padding: 14px 18px; background: rgba(255,255,255,.5); border-radius: 10px; border: 1px solid rgba(0,0,0,.04); }
.student-card.graded { opacity: .7; }

.stu-left { display: flex; align-items: center; gap: 12px; width: 160px; flex-shrink: 0; }
.stu-avatar { width: 36px; height: 36px; border-radius: 50%; background: linear-gradient(135deg,#4A90D9,#357ABD); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; }
.stu-name { font-size: 14px; font-weight: 600; color: #333; }
.stu-no { font-size: 11px; color: #aaa; }

.stu-mid { flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; }
.stu-submit { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #27ae60; }
.stu-nosubmit { font-size: 12px; color: #e74c3c; }
.stu-time { font-size: 11px; color: #aaa; }

.stu-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.score-input-row { display: flex; align-items: center; gap: 4px; }
.score-input { width: 56px; padding: 6px 8px; border: 1px solid rgba(0,0,0,.1); border-radius: 6px; font-size: 14px; text-align: center; outline: none; }
.score-input:focus { border-color: #1a73e8; }
.score-unit { font-size: 12px; color: #888; }
.score-display { }
.score-val { font-size: 15px; font-weight: 700; }
.score-val.green { color: #27ae60; }
.score-val.orange { color: #e67e22; }
.score-val.red { color: #e74c3c; }

.comment-input { width: 120px; padding: 6px 8px; border: 1px solid rgba(0,0,0,.1); border-radius: 6px; font-size: 12px; outline: none; }
.comment-input:focus { border-color: #1a73e8; }
.stu-comment { font-size: 12px; color: #888; max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.submit-score-btn { padding: 6px 14px; border: none; border-radius: 6px; background: #1a73e8; color: #fff; font-size: 12px; cursor: pointer; transition: all .15s; }
.submit-score-btn:hover { background: #1557b0; }
.edit-score-btn { padding: 6px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 6px; background: #fff; font-size: 12px; cursor: pointer; color: #888; transition: all .15s; }
.edit-score-btn:hover { border-color: #1a73e8; color: #1a73e8; }

.stu-waiting { font-size: 12px; color: #ccc; }

/* 批量操作 */
.batch-bar { padding: 12px 18px; background: rgba(230,126,34,.06); border-radius: 10px; text-align: center; }
.batch-hint { font-size: 13px; color: #e67e22; }
</style>
