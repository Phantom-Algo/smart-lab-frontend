<template>
  <div class="cm-page">
    <div class="page-header">
      <h2>课程管理</h2>
      <p>实验课程创建、排课选课监督</p>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num blue">{{ courses.length }}</div>
        <div class="stat-label">课程总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ activeCount }}</div>
        <div class="stat-label">进行中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ endedCount }}</div>
        <div class="stat-label">已结课</div>
      </div>
      <div class="stat-card">
        <div class="stat-num blue">{{ totalEnrolled }}</div>
        <div class="stat-label">选课总人数</div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="searchName" placeholder="搜索课程名..." class="search-input" @keyup.enter="pageNum = 1" />
        <select v-model="filterType" class="tool-select">
          <option value="">全部类型</option>
          <option v-for="t in typeOptions" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
        <select v-model="filterStatus" class="tool-select">
          <option value="">全部状态</option>
          <option :value="1">未开始</option>
          <option :value="2">进行中</option>
          <option :value="3">已结课</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 门课程</span>
        <button class="add-btn" @click="openCreateDialog">+ 创建课程</button>
      </div>
    </div>

    <div class="data-table-wrap">
      <table class="data-table">
        <thead>
          <tr>
            <th>课程编号</th><th>课程名称</th><th>类型</th><th>授课教师</th>
            <th>实验室</th><th>上课时间</th><th>容量</th><th>状态</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in pagedList" :key="c.id">
            <td><code>{{ c.courseNo }}</code></td>
            <td><span class="course-name">{{ c.courseName }}</span></td>
            <td><span class="type-tag" :class="'type-' + c.courseTypeId">{{ c.courseTypeName }}</span></td>
            <td>{{ c.teacherName || '未分配' }}</td>
            <td>{{ c.labName || '未分配' }}</td>
            <td>{{ c.weekdayName }} {{ c.timeSlot }}</td>
            <td>
              <div class="capacity-wrap">
                <div class="capacity-bar"><div class="capacity-fill" :class="c.enrolledCount >= (c.maxStudents || 1) ? 'full' : 'open'" :style="{ width: (c.enrolledCount / (c.maxStudents || 1) * 100) + '%' }"></div></div>
                <span class="capacity-text">{{ c.enrolledCount }}/{{ c.maxStudents || 0 }}</span>
              </div>
            </td>
            <td><span class="status-dot" :class="'status-' + c.status"></span> {{ c.statusName }}</td>
            <td class="action-cell">
              <button class="row-btn" @click="viewCourse(c)">查看</button>
              <button class="row-btn" @click="editCourse(c)">编辑</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="9" class="empty-cell">暂无课程数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 创建/编辑弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog && dialogMode !== 'view'" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel wide">
            <div class="modal-header">
              <h3>{{ dialogMode === 'create' ? '创建课程' : '编辑课程' }}</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body" v-if="form">
              <div class="section-label">基本信息</div>
              <div class="form-row-2">
                <div class="form-g"><label>课程编号 <span class="required">*</span></label><input v-model="form.courseNo" class="form-input" :disabled="dialogMode === 'edit'" /></div>
                <div class="form-g"><label>课程名称 <span class="required">*</span></label><input v-model="form.courseName" class="form-input" /></div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>课程类型 <span class="required">*</span></label>
                  <select v-model="form.courseTypeId" class="form-input"><option :value="0" disabled>请选择</option><option v-for="t in typeOptions" :key="t.id" :value="t.id">{{ t.name }}</option></select>
                </div>
                <div class="form-g"><label>学期</label><input v-model="form.semester" class="form-input" placeholder="如 2026-2027-1" /></div>
              </div>
              <div class="section-label">排课信息</div>
              <div class="form-row-2">
                <div class="form-g"><label>授课教师</label>
                  <select v-model="form.teacherId" class="form-input"><option :value="0">未分配</option><option v-for="t in teacherOptions" :key="t.id" :value="t.id">{{ t.name }}</option></select>
                </div>
                <div class="form-g"><label>实验室</label>
                  <select v-model="form.labId" class="form-input"><option :value="0">未分配</option><option v-for="l in labOptions" :key="l.id" :value="l.id">{{ l.name }}</option></select>
                </div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>上课星期</label>
                  <select v-model="form.weekday" class="form-input"><option :value="0">未设置</option><option v-for="w in weekOptions" :key="w.value" :value="w.value">{{ w.label }}</option></select>
                </div>
                <div class="form-g"><label>上课时段</label><input v-model="form.timeSlot" class="form-input" placeholder="如 09:00~11:00" /></div>
              </div>
              <div class="section-label">容量与状态</div>
              <div class="form-row-2">
                <div class="form-g"><label>最大选课人数</label><input v-model.number="form.maxStudents" type="number" class="form-input" min="1" /></div>
                <div class="form-g"><label>课程状态</label>
                  <select v-model="form.status" class="form-input"><option :value="1">未开始</option><option :value="2">进行中</option><option :value="3">已结课</option></select>
                </div>
              </div>
              <div class="form-g"><label>课程描述</label><textarea v-model="form.description" class="form-input form-textarea" rows="2" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">取消</button>
              <button class="modal-btn primary" @click="submitForm" :disabled="!canSubmit">保存</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 查看详情弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog && dialogMode === 'view' && viewData" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel wide">
            <div class="modal-header">
              <h3>课程详情</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>课程编号</span><span>{{ viewData.courseNo }}</span></div>
                <div class="ci-row"><span>课程名称</span><span>{{ viewData.courseName }}</span></div>
                <div class="ci-row"><span>类型</span><span>{{ viewData.courseTypeName }}</span></div>
                <div class="ci-row"><span>学期</span><span>{{ viewData.semester }}</span></div>
                <div class="ci-row"><span>授课教师</span><span>{{ viewData.teacherName || '未分配' }}</span></div>
                <div class="ci-row"><span>实验室</span><span>{{ viewData.labName || '未分配' }}</span></div>
                <div class="ci-row"><span>上课时间</span><span>{{ viewData.weekdayName }} {{ viewData.timeSlot }}</span></div>
                <div class="ci-row"><span>选课情况</span><span>{{ viewData.enrolledCount }}/{{ viewData.maxStudents }} 人</span></div>
                <div class="ci-row"><span>状态</span><span>{{ viewData.statusName }}</span></div>
                <div class="ci-row"><span>描述</span><span>{{ viewData.description || '无' }}</span></div>
              </div>
              <div v-if="viewData.students && viewData.students.length > 0" class="section-label" style="margin-top:8px">选课学生</div>
              <table v-if="viewData.students && viewData.students.length > 0" class="mini-table">
                <thead><tr><th>学号</th><th>姓名</th><th>班级</th><th>选课时间</th></tr></thead>
                <tbody><tr v-for="s in viewData.students" :key="s.studentNo"><td>{{ s.studentNo }}</td><td>{{ s.name }}</td><td>{{ s.className }}</td><td>{{ s.enrolledAt }}</td></tr></tbody>
              </table>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { getCoursePage, getCourseDetail, createCourse, updateCourse, type CourseListItem, type CourseStatus } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

interface Course extends CourseListItem { students?: { studentNo: string; name: string; className: string; enrolledAt: string }[] }

const typeOptions = [
  { id:1, name:'化学实验' }, { id:2, name:'物理实验' }, { id:3, name:'生物实验' },
  { id:4, name:'计算机实验' }, { id:5, name:'综合实验' },
]
const teacherOptions = [{ id:1, name:'李老师' }, { id:2, name:'孙老师' }, { id:3, name:'周老师' }]
const labOptions = [
  { id:1, name:'A305 分析化学实验室' }, { id:2, name:'B103 物理实验室' },
  { id:3, name:'C202 计算机实验室' }, { id:4, name:'D108 生物培养实验室' },
]
const weekOptions = [
  { value:1, label:'周一' }, { value:2, label:'周二' }, { value:3, label:'周三' },
  { value:4, label:'周四' }, { value:5, label:'周五' },
]

const courses = ref<Course[]>([])
const apiLoading = ref(false)

const mockCourses: Course[] = [
  { id:1, courseNo:'COURSE-2024-001', courseName:'有机化学实验', courseTypeId:1, courseTypeName:'化学实验', semester:'2026-2027-1', teacherId:1, teacherName:'李老师', departmentName:'化学与材料学院', labId:1, labName:'A305 分析化学实验室', weekday:1, weekdayName:'周一', timeSlot:'09:00~11:00', maxStudents:30, enrolledCount:28, status:2, statusName:'进行中', description:'掌握有机化学基本实验操作技能', createdAt:'' },
  { id:2, courseNo:'COURSE-2024-002', courseName:'大学物理实验', courseTypeId:2, courseTypeName:'物理实验', semester:'2026-2027-1', teacherId:2, teacherName:'孙老师', departmentName:'物理与光电学院', labId:2, labName:'B103 物理实验室', weekday:3, weekdayName:'周三', timeSlot:'14:00~17:00', maxStudents:25, enrolledCount:25, status:2, statusName:'进行中', description:null, createdAt:'' },
  { id:3, courseNo:'COURSE-2024-003', courseName:'生物培养技术', courseTypeId:3, courseTypeName:'生物实验', semester:'2026-2027-1', teacherId:3, teacherName:'周老师', departmentName:'生物科学与工程学院', labId:4, labName:'D108 生物培养实验室', weekday:2, weekdayName:'周二', timeSlot:'08:00~10:00', maxStudents:20, enrolledCount:16, status:2, statusName:'进行中', description:'学习细胞培养与无菌操作技术', createdAt:'' },
  { id:4, courseNo:'COURSE-2024-004', courseName:'Python程序设计', courseTypeId:4, courseTypeName:'计算机实验', semester:'2026-2027-1', teacherId:1, teacherName:'李老师', departmentName:'计算机科学与技术学院', labId:3, labName:'C202 计算机实验室', weekday:5, weekdayName:'周五', timeSlot:'14:00~16:00', maxStudents:40, enrolledCount:38, status:2, statusName:'进行中', description:null, createdAt:'' },
]

async function loadData() {
  apiLoading.value = true
  try {
    const res = await getCoursePage({ pageNum: 1, pageSize: 200 })
    courses.value = res.records
  } catch { /* API 不可用，请检查后端服务 */ }
  finally { apiLoading.value = false }
}

const searchName = ref('')
const filterType = ref<number | ''>('')
const filterStatus = ref<CourseStatus | ''>('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = courses.value
  if (searchName.value) list = list.filter(c => c.courseName.includes(searchName.value))
  if (filterType.value !== '') list = list.filter(c => c.courseTypeId === filterType.value)
  if (filterStatus.value !== '') list = list.filter(c => c.status === filterStatus.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const activeCount = computed(() => courses.value.filter(c => c.status === 2).length)
const endedCount = computed(() => courses.value.filter(c => c.status === 3).length)
const totalEnrolled = computed(() => courses.value.reduce((s, c) => s + c.enrolledCount, 0))

const showDialog = ref(false)
const dialogMode = ref<'create' | 'edit' | 'view'>('create')
const viewData = ref<Course | null>(null)
const saving = ref(false)
interface CourseForm { id: number; courseNo: string; courseName: string; courseTypeId: number; semester: string; teacherId: number; labId: number; weekday: number; timeSlot: string; maxStudents: number; status: CourseStatus; description: string }
const form = ref<CourseForm | null>(null)
const canSubmit = computed(() => form.value && form.value.courseNo.trim() && form.value.courseName.trim() && form.value.courseTypeId > 0)

function openCreateDialog() {
  form.value = { id:0, courseNo:'', courseName:'', courseTypeId:0, semester:'2026-2027-1', teacherId:0, labId:0, weekday:0, timeSlot:'', maxStudents:30, status:1, description:'' }
  dialogMode.value = 'create'; showDialog.value = true
}
function viewCourse(c: Course) {
  viewData.value = { ...c, students: c.enrolledCount > 0 ? [
    { studentNo:'S20240001', name:'张同学', className:'计科2401班', enrolledAt:'2026-02-20' },
    { studentNo:'S20240002', name:'王同学', className:'计科2401班', enrolledAt:'2026-02-20' },
    { studentNo:'S20240003', name:'赵同学', className:'计科2402班', enrolledAt:'2026-02-21' },
  ].slice(0, Math.min(c.enrolledCount, 3)) : [] }
  dialogMode.value = 'view'; showDialog.value = true
}
function editCourse(c: Course) {
  form.value = { id:c.id, courseNo:c.courseNo, courseName:c.courseName, courseTypeId:c.courseTypeId || 0, semester:c.semester, teacherId:c.teacherId || 0, labId:c.labId || 0, weekday:c.weekday || 0, timeSlot:c.timeSlot || '', maxStudents:c.maxStudents || 0, status:c.status, description:c.description || '' }
  dialogMode.value = 'edit'; showDialog.value = true
}

async function submitForm() {
  if (!form.value || !canSubmit.value) return
  saving.value = true
  const f = form.value
  const data = { courseNo: f.courseNo.trim(), courseName: f.courseName.trim(), courseTypeId: f.courseTypeId, semester: f.semester, teacherId: f.teacherId || undefined, labId: f.labId || undefined, weekday: f.weekday || undefined, timeSlot: f.timeSlot || undefined, maxStudents: f.maxStudents, status: f.status, description: f.description || undefined }

  try {
    if (isMockMode.value) {
      if (dialogMode.value === 'create') {
        const w = weekOptions.find(w => w.value === f.weekday)
        const t = typeOptions.find(t => t.id === f.courseTypeId)
        courses.value.unshift({
          id: Date.now(), courseNo: f.courseNo.trim(), courseName: f.courseName.trim(), courseTypeId: f.courseTypeId, courseTypeName: t?.name || '', semester: f.semester, teacherId: f.teacherId, teacherName: teacherOptions.find(t => t.id === f.teacherId)?.name || '', departmentName:'', labId: f.labId, labName: labOptions.find(l => l.id === f.labId)?.name || '', weekday: f.weekday, weekdayName: w?.label || '未设置', timeSlot: f.timeSlot, maxStudents: f.maxStudents, enrolledCount: 0, status: f.status, statusName: statusLabel(f.status), description: f.description || null, createdAt: ''
        })
      } else {
        const c = courses.value.find(c => c.id === f.id)
        if (c) Object.assign(c, { courseName: f.courseName.trim(), courseTypeId: f.courseTypeId, semester: f.semester, teacherId: f.teacherId, labId: f.labId, weekday: f.weekday, timeSlot: f.timeSlot, maxStudents: f.maxStudents, status: f.status, statusName: statusLabel(f.status), description: f.description || null })
      }
    } else {
      if (dialogMode.value === 'create') await createCourse(data)
      else await updateCourse(f.id, data)
      await loadData()
    }
    showDialog.value = false
  } catch (e: any) { alert(e.message || '保存失败') }
  finally { saving.value = false }
}

function statusLabel(s: CourseStatus): string { const m: Record<number,string> = {1:'未开始',2:'进行中',3:'已结课'}; return m[s] || '' }

onMounted(() => loadData())
</script>

<style scoped>
.cm-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 }
.green { color:#27ae60 }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:150px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; min-width:960px }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04); white-space:nowrap }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.course-name { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-1 { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-2 { background:rgba(26,115,232,.1); color:#1557b0 }
.type-3 { background:rgba(142,68,173,.1); color:#8e44ad }
.type-4 { background:rgba(243,156,18,.1); color:#e67e22 }
.type-5 { background:rgba(233,30,99,.1); color:#c2185b }

.capacity-wrap { display:flex; align-items:center; gap:6px }
.capacity-bar { width:60px; height:6px; border-radius:3px; background:rgba(0,0,0,.06); overflow:hidden }
.capacity-fill { height:100%; border-radius:3px; transition:width .3s }
.capacity-fill.open { background:#27ae60 }
.capacity-fill.full { background:#e74c3c }
.capacity-text { font-size:11px; color:#888 }

.status-dot { display:inline-block; width:6px; height:6px; border-radius:50%; margin-right:4px }
.status-1 { background:#1a73e8 }
.status-2 { background:#27ae60 }
.status-3 { background:#bbb }

.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.empty-cell { text-align:center; color:#ccc; padding:40px }
code { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

.mini-table { width:100%; border-collapse:collapse; margin-top:6px }
.mini-table th,.mini-table td { padding:8px 12px; text-align:left; font-size:12px; border-bottom:1px solid rgba(0,0,0,.04) }
.mini-table th { font-weight:500; color:#999; font-size:11px }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:560px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-panel.wide { max-width:600px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.section-label { font-size:13px; font-weight:600; color:#888; padding-bottom:4px; border-bottom:1px solid rgba(0,0,0,.05); margin-bottom:-6px }
.form-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-input:disabled { background:rgba(0,0,0,.03); color:#999 }
.form-textarea { resize:vertical; min-height:60px }
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
