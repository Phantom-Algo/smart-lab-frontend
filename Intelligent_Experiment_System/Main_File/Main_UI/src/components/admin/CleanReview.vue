<template>
  <div class="cr-page">
    <div class="page-header">
      <h2>检查评价</h2>
      <p>卫生检查打分、评分统计</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ evaluations.length }}</div><div class="stat-label">总评价</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#1a73e8">{{ avgScore }}</div><div class="stat-label">平均分</div></div>
      <div class="stat-card"><div class="stat-num green">{{ excellentRate }}%</div><div class="stat-label">优秀率</div></div>
      <div class="stat-card"><div class="stat-num red">{{ failCount }}</div><div class="stat-label">待整改</div></div>
    </div>

    <div class="filter-bar">
      <select v-model="filterLab" class="tool-select"><option value="">全部实验室</option><option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option></select>
      <select v-model="filterLevel" class="tool-select"><option value="">全部等级</option><option value="excellent">优秀 ≥90</option><option value="good">良好 80-89</option><option value="medium">中等 70-79</option><option value="pass">及格 60-69</option><option value="fail">不及格 &lt;60</option></select>
      <select v-model="filterTeacher" class="tool-select"><option value="">全部教师</option><option v-for="t in teacherOptions" :key="t" :value="t">{{ t }}</option></select>
    </div>

    <!-- 评分分布图 -->
    <div class="dist-card">
      <div class="dist-title">评分分布</div>
      <div class="dist-bars">
        <div v-for="d in distribution" :key="d.label" class="dist-row">
          <span class="dist-label">{{ d.label }}</span>
          <div class="dist-bar-wrap">
            <div class="dist-bar" :class="'bar-' + d.key" :style="{ width: d.pct + '%' }"></div>
          </div>
          <span class="dist-count">{{ d.count }} 人</span>
          <span class="dist-pct">{{ d.pct }}%</span>
        </div>
      </div>
    </div>

    <!-- 评价记录表格 -->
    <div class="table-wrap" v-if="pagedList.length > 0">
      <table class="eval-table">
        <thead>
          <tr>
            <th>学生</th>
            <th>班级</th>
            <th>实验室</th>
            <th>日期</th>
            <th>评分</th>
            <th>评价教师</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="e in pagedList" :key="e.id" class="eval-row" @click="openDetail(e)">
            <td>{{ e.studentName }}</td>
            <td class="class-cell">{{ e.className }}</td>
            <td>{{ e.labName }}</td>
            <td class="date-cell">{{ e.date }}</td>
            <td>
              <span class="score-badge" :class="scoreBadgeClass(e.score)">{{ e.score }} {{ scoreLevelShort(e.score) }}</span>
            </td>
            <td>{{ e.teacherName }}</td>
            <td><span class="detail-link" @click.stop="openDetail(e)">详情</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 12l2 2 4-4"/><circle cx="12" cy="12" r="10"/></svg>
      <p>暂无匹配的评价记录</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 详情弹窗（只读） -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>评价详情</h3><button class="modal-close" @click="showDetail = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="detail-section">
                <div class="ds-title">值日信息</div>
                <div class="detail-grid">
                  <div class="dg-item"><span class="dg-label">学生</span><span>{{ currentEval?.studentName }}</span></div>
                  <div class="dg-item"><span class="dg-label">班级</span><span>{{ currentEval?.className }}</span></div>
                  <div class="dg-item"><span class="dg-label">实验室</span><span>{{ currentEval?.labName }}</span></div>
                  <div class="dg-item"><span class="dg-label">值日日期</span><span>{{ currentEval?.date }}</span></div>
                  <div class="dg-item full" v-if="currentEval?.dutyDetail"><span class="dg-label">值日内容</span><span>{{ currentEval?.dutyDetail }}</span></div>
                </div>
              </div>
              <div class="detail-section">
                <div class="ds-title">评价结果</div>
                <div class="detail-grid">
                  <div class="dg-item"><span class="dg-label">卫生评分</span><span class="score-val" :class="scoreBadgeClass(currentEval?.score || 0)">{{ currentEval?.score }} 分</span></div>
                  <div class="dg-item"><span class="dg-label">等级</span><span>{{ scoreLevelFull(currentEval?.score || 0) }}</span></div>
                  <div class="dg-item"><span class="dg-label">评价教师</span><span>{{ currentEval?.teacherName }}</span></div>
                  <div class="dg-item"><span class="dg-label">评价时间</span><span>{{ currentEval?.evaluatedAt }}</span></div>
                  <div class="dg-item full" v-if="currentEval?.comment"><span class="dg-label">评语</span><span>{{ currentEval?.comment }}</span></div>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDetail = false">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface Evaluation {
  id: number; studentName: string; className: string; labName: string
  date: string; score: number; teacherName: string; comment: string | null
  evaluatedAt: string; dutyDetail: string | null
}

const labOptions = ['A305 分析化学实验室', 'B103 物理实验室', 'C202 计算机实验室', 'D108 生物培养实验室']
const teacherOptions = ['李老师', '孙老师', '周老师']

const evaluations = ref<Evaluation[]>([
  { id:1, studentName:'张同学', className:'计科2401', labName:'A305 分析化学实验室', date:'2026-07-08', score:92, teacherName:'李老师', comment:'台面擦拭干净，器材归位整齐', evaluatedAt:'07-08 10:30', dutyDetail:'上午 8:00-12:00 值日，重点检查通风橱' },
  { id:2, studentName:'赵同学', className:'软工2402', labName:'B103 物理实验室', date:'2026-07-08', score:75, teacherName:'李老师', comment:'地面清扫不够彻底，注意角落', evaluatedAt:'07-08 16:20', dutyDetail:'下午 14:00-18:00 值日' },
  { id:3, studentName:'李同学', className:'计科2401', labName:'C202 计算机实验室', date:'2026-07-07', score:88, teacherName:'孙老师', comment:'完成质量高，继续保持', evaluatedAt:'07-07 12:10', dutyDetail:'上午 8:00-12:00 值日，机房清洁' },
  { id:4, studentName:'王同学', className:'软工2402', labName:'D108 生物培养实验室', date:'2026-07-07', score:80, teacherName:'孙老师', comment:null, evaluatedAt:'07-07 16:45', dutyDetail:'下午 14:00-18:00 值日，灭菌设备检查' },
  { id:5, studentName:'刘同学', className:'计科2401', labName:'A305 分析化学实验室', date:'2026-07-07', score:58, teacherName:'李老师', comment:'废液未按规定分类处理，操作台有残留试剂', evaluatedAt:'07-07 10:15', dutyDetail:'上午 8:00-12:00 值日' },
  { id:6, studentName:'陈同学', className:'软工2402', labName:'B103 物理实验室', date:'2026-07-07', score:95, teacherName:'周老师', comment:'非常认真，实验器材全部归位校准', evaluatedAt:'07-07 16:30', dutyDetail:'下午 14:00-18:00 值日' },
  { id:7, studentName:'张同学', className:'计科2401', labName:'C202 计算机实验室', date:'2026-07-06', score:72, teacherName:'孙老师', comment:'键盘鼠标未整理，线缆杂乱', evaluatedAt:'07-06 12:00', dutyDetail:'上午 8:00-12:00 值日' },
  { id:8, studentName:'赵同学', className:'软工2402', labName:'A305 分析化学实验室', date:'2026-07-06', score:90, teacherName:'李老师', comment:'整体良好', evaluatedAt:'07-06 10:40', dutyDetail:'上午 8:00-12:00 值日' },
  { id:9, studentName:'李同学', className:'计科2401', labName:'D108 生物培养实验室', date:'2026-07-06', score:65, teacherName:'周老师', comment:'培养皿未及时清理', evaluatedAt:'07-06 16:10', dutyDetail:'下午 14:00-18:00 值日' },
  { id:10, studentName:'王同学', className:'软工2402', labName:'B103 物理实验室', date:'2026-07-06', score:85, teacherName:'李老师', comment:null, evaluatedAt:'07-06 16:50', dutyDetail:'下午 14:00-18:00 值日' },
  { id:11, studentName:'刘同学', className:'计科2401', labName:'C202 计算机实验室', date:'2026-07-05', score:93, teacherName:'孙老师', comment:'优秀', evaluatedAt:'07-05 12:05', dutyDetail:'上午 8:00-12:00 值日' },
  { id:12, studentName:'陈同学', className:'软工2402', labName:'A305 分析化学实验室', date:'2026-07-05', score:78, teacherName:'李老师', comment:'试剂瓶标签未更新', evaluatedAt:'07-05 10:20', dutyDetail:'上午 8:00-12:00 值日' },
])

const filterLab = ref('')
const filterLevel = ref('')
const filterTeacher = ref('')
const pageNum = ref(1)
const pageSize = 8

// 统计
const avgScore = computed(() => {
  if (evaluations.value.length === 0) return '--'
  return (evaluations.value.reduce((s, e) => s + e.score, 0) / evaluations.value.length).toFixed(1)
})
const excellentRate = computed(() => {
  if (evaluations.value.length === 0) return 0
  return Math.round(evaluations.value.filter(e => e.score >= 90).length / evaluations.value.length * 100)
})
const failCount = computed(() => evaluations.value.filter(e => e.score < 60).length)

// 分布
const distribution = computed(() => {
  const total = evaluations.value.length || 1
  const levels = [
    { key:'excellent', label:'优秀 90-100', min:90, max:100 },
    { key:'good', label:'良好 80-89', min:80, max:89 },
    { key:'medium', label:'中等 70-79', min:70, max:79 },
    { key:'pass', label:'及格 60-69', min:60, max:69 },
    { key:'fail', label:'不及格 <60', min:0, max:59 },
  ]
  return levels.map(l => {
    const count = evaluations.value.filter(e => e.score >= l.min && e.score <= l.max).length
    return { ...l, count, pct: Math.round(count / total * 100) }
  })
})

// 筛选
const filteredList = computed(() => {
  let list = evaluations.value
  if (filterLab.value) list = list.filter(e => e.labName === filterLab.value)
  if (filterTeacher.value) list = list.filter(e => e.teacherName === filterTeacher.value)
  if (filterLevel.value) {
    const ranges: Record<string, [number, number]> = {
      excellent: [90, 100], good: [80, 89], medium: [70, 79], pass: [60, 69], fail: [0, 59]
    }
    const [min, max] = ranges[filterLevel.value]
    list = list.filter(e => e.score >= min && e.score <= max)
  }
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function scoreLevelShort(s: number) { if (s >= 90) return '优'; if (s >= 80) return '良'; if (s >= 70) return '中'; if (s >= 60) return '及格'; return '不及格' }
function scoreLevelFull(s: number) { if (s >= 90) return '优秀'; if (s >= 80) return '良好'; if (s >= 70) return '中等'; if (s >= 60) return '及格'; return '不及格' }
function scoreBadgeClass(s: number) { if (s >= 90) return 's-excellent'; if (s >= 80) return 's-good'; if (s >= 70) return 's-medium'; if (s >= 60) return 's-pass'; return 's-fail' }

// 详情
const showDetail = ref(false)
const currentEval = ref<Evaluation | null>(null)
function openDetail(e: Evaluation) { currentEval.value = e; showDetail.value = true }
</script>

<style scoped>
.cr-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:90px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .green { color:#27ae60 } .red { color:#e74c3c }

.filter-bar { display:flex; align-items:center; gap:10px; margin-bottom:16px; flex-wrap:wrap }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }

/* 分布图 */
.dist-card { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:18px 20px; margin-bottom:16px }
.dist-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:14px }
.dist-bars { display:flex; flex-direction:column; gap:10px }
.dist-row { display:flex; align-items:center; gap:12px }
.dist-label { font-size:12px; color:#666; width:90px; flex-shrink:0; text-align:right }
.dist-bar-wrap { flex:1; height:22px; background:rgba(0,0,0,.04); border-radius:6px; overflow:hidden; position:relative }
.dist-bar { height:100%; border-radius:6px; transition:width .4s ease; min-width:2px }
.bar-excellent { background:linear-gradient(90deg,#27ae60,#2ecc71) }
.bar-good { background:linear-gradient(90deg,#2ecc71,#58d68d) }
.bar-medium { background:linear-gradient(90deg,#f39c12,#f1c40f) }
.bar-pass { background:linear-gradient(90deg,#e67e22,#f39c12) }
.bar-fail { background:linear-gradient(90deg,#e74c3c,#c0392b) }
.dist-count { font-size:12px; color:#555; width:36px; text-align:right; flex-shrink:0 }
.dist-pct { font-size:11px; color:#999; width:36px; flex-shrink:0 }

/* 表格 */
.table-wrap { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.eval-table { width:100%; border-collapse:collapse; font-size:13px }
.eval-table thead { background:rgba(0,0,0,.02) }
.eval-table th { padding:12px 16px; text-align:left; font-weight:600; color:#888; font-size:12px; border-bottom:1px solid rgba(0,0,0,.06); white-space:nowrap }
.eval-table td { padding:10px 16px; border-bottom:1px solid rgba(0,0,0,.03); color:#333; vertical-align:middle }
.eval-row { cursor:pointer; transition:background .12s }
.eval-row:hover { background:rgba(26,115,232,.03) }
.class-cell { color:#888; font-size:12px }
.date-cell { color:#888; font-size:12px; white-space:nowrap }

.score-badge { padding:2px 8px; border-radius:4px; font-size:12px; font-weight:600; white-space:nowrap }
.s-excellent { background:rgba(39,174,96,.1); color:#27ae60 }
.s-good { background:rgba(46,204,113,.1); color:#27ae60 }
.s-medium { background:rgba(243,156,18,.1); color:#e67e22 }
.s-pass { background:rgba(230,126,34,.1); color:#e67e22 }
.s-fail { background:rgba(231,76,60,.1); color:#e74c3c }

.detail-link { color:#1a73e8; font-size:12px; cursor:pointer; font-weight:500 }
.detail-link:hover { text-decoration:underline }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }

.detail-section { margin-bottom:6px }
.ds-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px; padding-bottom:6px; border-bottom:1px solid rgba(0,0,0,.06) }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px 16px }
.dg-item { display:flex; justify-content:space-between; font-size:13px; color:#555; padding:4px 0 }
.dg-item.full { grid-column:1 / -1 }
.dg-label { color:#999; flex-shrink:0; margin-right:12px }
.score-val { font-weight:600 }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
