<template>
  <div class="teacher-task">
    <div class="page-header">
      <h2>实验任务</h2>
      <p>创建和管理实验任务</p>
    </div>

    <!-- 统计 + 新建 -->
    <div class="top-bar">
      <div class="filter-tabs">
        <button v-for="t in tabs" :key="t.key" class="filter-btn" :class="{ active: activeTab === t.key }" @click="activeTab = t.key">
          {{ t.label }}
          <span class="tab-count" v-if="t.count !== undefined">{{ t.count }}</span>
        </button>
      </div>
      <button class="create-btn" @click="showCreate = true">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新建任务
      </button>
    </div>

    <!-- 任务卡片列表 -->
    <div class="task-list">
      <div v-for="task in filteredTasks" :key="task.id" class="task-card">
        <div class="task-left">
          <div class="task-name">{{ task.title }}</div>
          <div class="task-meta">
            <span class="meta-course">{{ task.courseName }}</span>
            <span class="meta-dot">·</span>
            <span>{{ task.className }}</span>
            <span class="meta-dot">·</span>
            <span>截止 {{ task.deadline }}</span>
          </div>
        </div>
        <div class="task-right">
          <div class="task-stats">
            <span class="task-stat">{{ task.submitCount }}/{{ task.studentCount }} 已提交</span>
            <span class="task-stat grade">{{ task.gradedCount }} 已批改</span>
          </div>
          <div class="task-progress-bar">
            <div class="tp-fill green" :style="{ width: (task.submitCount/task.studentCount*100) + '%' }"></div>
          </div>
        </div>
        <div class="task-actions">
          <button class="tbtn primary" @click="$emit('navigate', { key: 'my-score', name: '成绩评定' })">批改</button>
          <button class="tbtn" @click="openDetail(task)">详情</button>
        </div>
      </div>

      <div v-if="filteredTasks.length === 0" class="empty-state">暂无任务</div>
    </div>

    <!-- 详情弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showDetail" class="popup-overlay" @click.self="showDetail = false">
          <div class="popup-panel">
            <div class="popup-header">
              <h3>任务详情</h3>
              <button class="popup-close" @click="showDetail = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="popup-body" v-if="detailTask">
              <div class="detail-section">
                <div class="detail-label">任务名称</div>
                <div class="detail-value name">{{ detailTask.title }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-section">
                  <div class="detail-label">所属课程</div>
                  <div class="detail-value">{{ detailTask.courseName }}</div>
                </div>
                <div class="detail-section">
                  <div class="detail-label">班级</div>
                  <div class="detail-value">{{ detailTask.className }}</div>
                </div>
              </div>
              <div class="detail-section">
                <div class="detail-label">任务描述</div>
                <div class="detail-value desc">{{ detailTask.description }}</div>
              </div>
              <div class="detail-row">
                <div class="detail-section">
                  <div class="detail-label">截止时间</div>
                  <div class="detail-value">{{ detailTask.deadline }}</div>
                </div>
                <div class="detail-section">
                  <div class="detail-label">提交情况</div>
                  <div class="detail-value">{{ detailTask.submitCount }} / {{ detailTask.studentCount }} 人</div>
                </div>
              </div>
              <div class="detail-section" v-if="detailTask.attachments && detailTask.attachments.length > 0">
                <div class="detail-label">附件资料</div>
                <div class="attach-list">
                  <div v-for="(a, i) in detailTask.attachments" :key="i" class="attach-item">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    <span>{{ a.name }}</span>
                    <span class="attach-type">{{ a.type }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="popup-footer">
              <button class="popup-btn cancel" @click="showDetail = false">关闭</button>
              <button class="popup-btn primary" @click="showDetail = false; $emit('navigate', { key: 'my-score', name: '成绩评定' })">去批改</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 新建任务弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showCreate" class="popup-overlay" @click.self="showCreate = false">
          <div class="popup-panel wide">
            <div class="popup-header">
              <h3>新建实验任务</h3>
              <button class="popup-close" @click="showCreate = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="popup-body">
              <div class="form-group">
                <label>所属课程班级</label>
                <select class="form-input form-select">
                  <option>有机化学实验 - 化工2401班</option>
                  <option>有机化学实验 - 化工2402班</option>
                  <option>有机化学实验 - 应化2401班</option>
                </select>
              </div>
              <div class="form-group">
                <label>任务名称</label>
                <input class="form-input" placeholder="请输入实验任务名称" />
              </div>
              <div class="form-group">
                <label>任务描述</label>
                <textarea class="form-input form-textarea" placeholder="请输入任务详细描述、实验要求等" rows="4"></textarea>
              </div>
              <div class="form-row">
                <div class="form-group flex-1">
                  <label>截止日期</label>
                  <input class="form-input" type="date" />
                </div>
                <div class="form-group flex-1">
                  <label>截止时间</label>
                  <input class="form-input" type="time" value="23:59" />
                </div>
              </div>
              <div class="form-group">
                <label>附件资料</label>
                <div class="upload-zone">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#aaa" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                  <span>点击或拖拽上传附件（实验指导书、数据模板等）</span>
                </div>
              </div>
            </div>
            <div class="popup-footer">
              <button class="popup-btn cancel" @click="showCreate = false">取消</button>
              <button class="popup-btn primary" @click="showCreate = false">发布任务</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getTaskPage, type TaskListItem } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

const activeTab = ref('all')
const showCreate = ref(false)
const showDetail = ref(false)
const detailTask = ref<TeacherTask | null>(null)

interface TeacherTask {
  id: number; title: string; courseName: string; className: string
  deadline: string; studentCount: number; submitCount: number; gradedCount: number
  status: 'active' | 'grading' | 'done'
  description: string; attachments: { name: string; type: string }[]
}

const MOCK_TASKS: TeacherTask[] = [
  { id: 1, title: '酯化反应实验报告', courseName: '有机化学实验', className: '化工2401班', deadline: '7/9 23:59', studentCount: 32, submitCount: 24, gradedCount: 9, status: 'grading', description: '完成乙酸乙酯的制备实验', attachments: [{ name: '酯化反应实验指导书', type: 'PDF' }, { name: '实验数据记录模板', type: 'XLSX' }] },
  { id: 2, title: '蒸馏与沸点测定', courseName: '有机化学实验', className: '化工2402班', deadline: '7/10 23:59', studentCount: 30, submitCount: 22, gradedCount: 8, status: 'grading', description: '掌握常压蒸馏和分馏的基本操作', attachments: [{ name: '蒸馏实验操作规范', type: 'PDF' }] },
  { id: 3, title: '分光光度法测定铁含量', courseName: '有机化学实验', className: '化工2401班', deadline: '7/15 23:59', studentCount: 32, submitCount: 16, gradedCount: 16, status: 'active', description: '使用分光光度计测定铁离子含量', attachments: [{ name: '分光光度计使用说明', type: 'PDF' }] },
  { id: 4, title: '红外光谱分析实验', courseName: '有机化学实验', className: '应化2401班', deadline: '7/14 23:59', studentCount: 28, submitCount: 10, gradedCount: 10, status: 'active', description: '利用红外光谱仪进行结构分析', attachments: [{ name: '红外光谱解析指南', type: 'PDF' }] },
  { id: 5, title: '薄层色谱分离', courseName: '有机化学实验', className: '化工2402班', deadline: '7/18 23:59', studentCount: 30, submitCount: 5, gradedCount: 5, status: 'active', description: '学习薄层色谱的基本操作', attachments: [{ name: '色谱分析基础', type: 'PDF' }] },
  { id: 6, title: '熔点测定实验', courseName: '有机化学实验', className: '应化2401班', deadline: '7/20 23:59', studentCount: 28, submitCount: 0, gradedCount: 0, status: 'active', description: '掌握熔点测定技术', attachments: [] },
]

const tasks = ref<TeacherTask[]>([])

async function loadTasks() {
  if (isMockMode.value) { tasks.value = [...MOCK_TASKS]; return }
  try {
    const res = await getTaskPage({ pageNum: 1, pageSize: 100 })
    if (res.records.length > 0) {
      tasks.value = res.records.map(t => ({ id: t.id, title: t.taskName, courseName: t.courseName, className: '', deadline: t.endTime?.slice(0, 10) || '', studentCount: 30, submitCount: 15, gradedCount: 10, status: (t.publishStatus === 'published' ? 'active' : 'done') as TeacherTask['status'], description: t.description || '', attachments: [] }))
    } else tasks.value = [...MOCK_TASKS]
  } catch { /* API 不可用，请检查后端服务 */ }
}

const tabs = computed(() => [
  { key: 'all', label: '全部', count: tasks.value.length },
  { key: 'active', label: '进行中', count: tasks.value.filter(t => t.status === 'active').length },
  { key: 'grading', label: '待批改', count: tasks.value.filter(t => t.status === 'grading').length },
  { key: 'done', label: '已截止', count: tasks.value.filter(t => t.status === 'done').length },
])

onMounted(() => loadTasks())

const filteredTasks = computed(() => {
  if (activeTab.value === 'all') return tasks.value
  return tasks.value.filter(t => t.status === activeTab.value)
})

function openDetail(task: TeacherTask): void {
  detailTask.value = task
  showDetail.value = true
}
</script>

<style scoped>
.teacher-task { height: 100%; overflow-y: auto; padding: 28px 36px; }
.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

.top-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.filter-tabs { display: flex; gap: 4px; }
.filter-btn { padding: 7px 16px; border: 1px solid rgba(0,0,0,.06); border-radius: 8px; background: rgba(255,255,255,.4); font-size: 13px; color: #888; cursor: pointer; transition: all .15s; display: flex; align-items: center; gap: 6px; }
.filter-btn:hover { border-color: #1a73e8; color: #1a73e8; }
.filter-btn.active { background: rgba(26,115,232,.1); border-color: #1a73e8; color: #1a73e8; font-weight: 600; }
.tab-count { font-size: 11px; padding: 1px 7px; border-radius: 8px; background: rgba(0,0,0,.05); }
.filter-btn.active .tab-count { background: rgba(26,115,232,.15); }

.create-btn { display: flex; align-items: center; gap: 6px; padding: 10px 22px; border: none; border-radius: 10px; background: linear-gradient(135deg,#1a73e8,#4a90d9); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; transition: all .15s; }
.create-btn:hover { box-shadow: 0 4px 14px rgba(26,115,232,.35); }

.task-list { display: flex; flex-direction: column; gap: 10px; }
.task-card { display: flex; align-items: center; gap: 20px; padding: 16px 20px; background: rgba(255,255,255,.5); border-radius: 12px; border: 1px solid rgba(0,0,0,.04); }
.task-left { flex: 1; min-width: 0; }
.task-name { font-size: 14px; font-weight: 600; color: #1a1a2e; margin-bottom: 4px; }
.task-meta { font-size: 12px; color: #999; display: flex; align-items: center; gap: 4px; }
.meta-course { color: #1a73e8; font-weight: 500; }
.meta-dot { color: #ddd; }

.task-right { width: 180px; flex-shrink: 0; }
.task-stats { display: flex; gap: 12px; margin-bottom: 6px; }
.task-stat { font-size: 11px; color: #888; }
.task-stat.grade { color: #e67e22; }
.task-progress-bar { height: 5px; background: rgba(0,0,0,.06); border-radius: 3px; overflow: hidden; }
.tp-fill { height: 100%; border-radius: 3px; transition: width .3s; }
.tp-fill.green { background: #27ae60; }

.task-actions { display: flex; gap: 8px; flex-shrink: 0; }
.tbtn { padding: 7px 16px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; background: #fff; font-size: 12px; color: #555; cursor: pointer; transition: all .15s; }
.tbtn:hover { border-color: #1a73e8; color: #1a73e8; }
.tbtn.primary { background: #1a73e8; color: #fff; border: none; }
.tbtn.primary:hover { background: #1557b0; }

.empty-state { text-align: center; padding: 40px; color: #ccc; font-size: 13px; }

/* ===== 弹窗 ===== */
.popup-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.popup-panel { background: #fff; border-radius: 16px; width: 480px; max-height: 85vh; overflow-y: auto; box-shadow: 0 16px 48px rgba(0,0,0,.15); }
.popup-panel.wide { width: 560px; }
.popup-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px 0; }
.popup-header h3 { margin: 0; font-size: 17px; color: #1a1a2e; }
.popup-close { background: none; border: none; cursor: pointer; color: #999; padding: 4px; border-radius: 6px; }
.popup-close:hover { background: rgba(0,0,0,.05); color: #333; }
.popup-body { padding: 20px 24px; }
.popup-footer { padding: 0 24px 20px; display: flex; justify-content: flex-end; gap: 10px; }
.popup-btn { padding: 10px 24px; border-radius: 8px; font-size: 14px; cursor: pointer; border: none; transition: all .15s; }
.popup-btn.cancel { background: rgba(0,0,0,.04); color: #666; }
.popup-btn.cancel:hover { background: rgba(0,0,0,.08); }
.popup-btn.primary { background: linear-gradient(135deg,#1a73e8,#4a90d9); color: #fff; }
.popup-btn.primary:hover { box-shadow: 0 4px 12px rgba(26,115,232,.3); }
.popup-fade-enter-active, .popup-fade-leave-active { transition: all .2s ease; }
.popup-fade-enter-from, .popup-fade-leave-to { opacity: 0; }

/* 详情弹窗 */
.detail-section { margin-bottom: 14px; }
.detail-label { font-size: 11px; color: #aaa; margin-bottom: 3px; text-transform: uppercase; letter-spacing: .5px; }
.detail-value { font-size: 14px; color: #333; }
.detail-value.name { font-size: 16px; font-weight: 700; color: #1a1a2e; }
.detail-value.desc { font-size: 13px; color: #666; line-height: 1.6; background: rgba(0,0,0,.02); padding: 10px 12px; border-radius: 8px; }
.detail-row { display: flex; gap: 20px; }
.detail-row .detail-section { flex: 1; }

.attach-list { display: flex; flex-direction: column; gap: 6px; }
.attach-item { display: flex; align-items: center; gap: 8px; padding: 7px 10px; background: rgba(0,0,0,.02); border-radius: 6px; font-size: 13px; color: #555; }
.attach-type { font-size: 10px; color: #aaa; background: rgba(0,0,0,.04); padding: 1px 6px; border-radius: 4px; margin-left: auto; }

/* 新建表单 */
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group label { font-size: 13px; font-weight: 500; color: #555; }
.form-input { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; outline: none; transition: border .15s; }
.form-input:focus { border-color: #1a73e8; }
.form-select { background: #fff; cursor: pointer; }
.form-textarea { resize: vertical; font-family: inherit; }
.form-row { display: flex; gap: 12px; }
.flex-1 { flex: 1; }

.upload-zone { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 24px; border: 2px dashed rgba(0,0,0,.1); border-radius: 10px; cursor: pointer; transition: all .15s; color: #aaa; font-size: 12px; }
.upload-zone:hover { border-color: #1a73e8; color: #1a73e8; background: rgba(26,115,232,.02); }
</style>
