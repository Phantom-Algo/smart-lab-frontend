<template>
  <div class="hazard-page">
    <div class="page-header">
      <h2>隐患上报</h2>
      <p>发现安全问题后自主上报，追踪处理进度</p>
    </div>

    <div class="hazard-body">
      <!-- 左侧：我的上报记录 -->
      <div class="report-list-col">
        <div class="filter-bar">
          <div
            v-for="f in statusFilters"
            :key="f.key"
            class="filter-tab"
            :class="{ active: activeFilter === f.key }"
            @click="activeFilter = f.key"
          >
            {{ f.label }}
            <span class="filter-count">{{ f.count }}</span>
          </div>
        </div>

        <div class="report-list" v-if="filteredReports.length > 0">
          <div
            v-for="report in filteredReports"
            :key="report.id"
            class="report-card"
            :class="'status-' + report.status"
          >
            <div class="card-top">
              <div class="card-status-icon" :class="'icon-' + report.status">
                <svg v-if="report.status === 'pending'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <svg v-else-if="report.status === 'processing'" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              </div>
              <div class="card-info">
                <span class="card-title">{{ report.title }}</span>
                <span class="card-lab">{{ report.labName }}</span>
              </div>
              <span class="status-tag" :class="'tag-' + report.status">{{ statusLabel(report.status) }}</span>
            </div>
            <div class="card-meta">
              <span class="meta-item" v-if="report.level === 'important'" style="color: #e74c3c; font-weight: 600;">重要隐患</span>
              <span class="meta-item" v-else style="color: #e67e22;">一般隐患</span>
              <span class="meta-sep">|</span>
              <span class="meta-item">{{ report.createdAt }}</span>
              <template v-if="report.deadline">
                <span class="meta-sep">|</span>
                <span class="meta-item">整改期限 {{ report.deadline }}</span>
              </template>
            </div>
            <div v-if="report.reviewComment" class="card-review">
              <span class="review-label">复核意见：</span>{{ report.reviewComment }}
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#ccc" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <p>暂无隐患上报记录</p>
          <span class="empty-hint">发现安全问题请及时通过右侧表单上报</span>
        </div>
      </div>

      <!-- 右侧：新建上报表单 -->
      <div class="form-panel">
        <div class="panel-card">
          <h4 class="panel-card-title">新建隐患上报</h4>

          <div class="form-group">
            <label>所属实验室 <span class="required">*</span></label>
            <select v-model="form.lab" class="form-select">
              <option value="">请选择实验室</option>
              <option v-for="lab in labOptions" :key="lab" :value="lab">{{ lab }}</option>
            </select>
          </div>

          <div class="form-group">
            <label>隐患等级 <span class="required">*</span></label>
            <div class="level-options">
              <label class="level-option" :class="{ active: form.level === 'normal' }">
                <input v-model="form.level" type="radio" value="normal" />
                <span class="level-label">一般隐患</span>
              </label>
              <label class="level-option" :class="{ active: form.level === 'important' }">
                <input v-model="form.level" type="radio" value="important" />
                <span class="level-label">重要隐患</span>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label>隐患标题 <span class="required">*</span></label>
            <input v-model="form.title" type="text" class="form-input" placeholder="请简要概括隐患问题" />
          </div>

          <div class="form-group">
            <label>详细描述 <span class="required">*</span></label>
            <textarea
              v-model="form.description"
              class="form-textarea"
              placeholder="请详细描述发现的安全隐患，包括位置、现象、可能的风险..."
              rows="4"
            ></textarea>
          </div>

          <div class="form-group">
            <label>上传照片（选填）</label>
            <div class="upload-zone">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              <span>点击或拖拽上传现场照片</span>
              <span class="upload-hint">支持 JPG/PNG，单张不超过 10MB</span>
            </div>
          </div>

          <button class="submit-btn" :disabled="!formValid" @click="submitReport">
            提交上报
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface HazardReport {
  id: number
  title: string
  labName: string
  level: 'normal' | 'important'
  status: 'pending' | 'processing' | 'completed'
  createdAt: string
  deadline?: string
  reviewComment?: string
}

const activeFilter = ref('all')

const statusFilters = computed(() => [
  { key: 'all', label: '全部', count: reports.length },
  { key: 'pending', label: '待处理', count: reports.filter(r => r.status === 'pending').length },
  { key: 'processing', label: '整改中', count: reports.filter(r => r.status === 'processing').length },
  { key: 'completed', label: '已完成', count: reports.filter(r => r.status === 'completed').length },
])

const reports: HazardReport[] = [
  { id: 1, title: '化学实验室通风橱排风故障', labName: 'A201 化学实验室', level: 'important', status: 'pending',
    createdAt: '2026-07-08 14:00', deadline: '2026-07-10' },
  { id: 2, title: 'B103 实验室天花板漏水', labName: 'B103 物理实验室', level: 'important', status: 'processing',
    createdAt: '2026-07-05 09:00', deadline: '2026-07-12' },
  { id: 3, title: '信息楼消防通道堆放杂物', labName: 'C202 计算机实验室', level: 'normal', status: 'completed',
    createdAt: '2026-07-01 10:00', reviewComment: '已清理完毕，通道畅通。提醒物业加强巡查。' },
  { id: 4, title: 'D305 实验室洗眼器水压不足', labName: 'D305 生物实验室', level: 'normal', status: 'processing',
    createdAt: '2026-07-06 16:00', deadline: '2026-07-09' },
]

const filteredReports = computed(() => {
  if (activeFilter.value === 'all') return reports
  return reports.filter(r => r.status === activeFilter.value)
})

function statusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待处理', processing: '整改中', completed: '已完成' }
  return m[s] || s
}

// ===== 表单 =====
const labOptions = ['B103 物理实验室', 'A201 化学实验室', 'C202 计算机实验室', 'D305 生物实验室', 'E108 电子实验室', 'A305 分析化学实验室', 'F201 材料实验室', 'G102 环境监测实验室']

const form = ref({
  lab: '',
  level: 'normal' as string,
  title: '',
  description: '',
})

const formValid = computed(() => {
  return form.value.lab !== '' && form.value.title.trim() !== '' && form.value.description.trim() !== ''
})

function submitReport(): void {
  if (!formValid.value) return
  alert(`隐患上报已提交！\n\n实验室：${form.value.lab}\n标题：${form.value.title}\n等级：${form.value.level === 'important' ? '重要隐患' : '一般隐患'}\n\n安全管理员将尽快处理，处理进度可在"我的上报"中查看。`)
  form.value = { lab: '', level: 'normal', title: '', description: '' }
}
</script>

<style scoped>
.hazard-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 两栏布局 ===== */
.hazard-body { display: flex; gap: 24px; align-items: flex-start; }

.report-list-col { flex: 1; min-width: 0; }

/* 筛选栏 */
.filter-bar { display: flex; gap: 4px; background: rgba(255,255,255,.4); border-radius: 10px; padding: 4px; margin-bottom: 16px; width: fit-content; }
.filter-tab { padding: 8px 16px; border-radius: 8px; font-size: 13px; color: #666; cursor: pointer; transition: all .15s; display: flex; align-items: center; gap: 6px; user-select: none; }
.filter-tab:hover { color: #333; background: rgba(0,0,0,.03); }
.filter-tab.active { background: #fff; color: #1a73e8; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.filter-count { font-size: 11px; background: rgba(0,0,0,.06); padding: 1px 7px; border-radius: 10px; color: #888; }
.filter-tab.active .filter-count { background: rgba(26,115,232,.1); color: #1a73e8; }

/* 上报卡片 */
.report-list { display: flex; flex-direction: column; gap: 10px; }

.report-card { background: rgba(255,255,255,.5); border-radius: 12px; border: 1px solid rgba(0,0,0,.04); padding: 16px 18px; }
.report-card.status-pending { border-left: 3px solid #e67e22; }
.report-card.status-processing { border-left: 3px solid #1a73e8; }
.report-card.status-completed { border-left: 3px solid #27ae60; }

.card-top { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.card-status-icon { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-pending { background: rgba(230,126,34,.1); color: #e67e22; }
.icon-processing { background: rgba(26,115,232,.1); color: #1a73e8; }
.icon-completed { background: rgba(39,174,96,.1); color: #27ae60; }

.card-info { flex: 1; min-width: 0; }
.card-title { display: block; font-size: 14px; font-weight: 600; color: #1a1a2e; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-lab { display: block; font-size: 12px; color: #aaa; margin-top: 1px; }

.status-tag { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; white-space: nowrap; }
.tag-pending { background: rgba(230,126,34,.1); color: #e67e22; }
.tag-processing { background: rgba(26,115,232,.1); color: #1a73e8; }
.tag-completed { background: rgba(39,174,96,.1); color: #27ae60; }

.card-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #aaa; margin-bottom: 6px; }
.meta-sep { color: #ddd; }

.card-review { font-size: 12px; color: #27ae60; padding: 6px 10px; background: rgba(39,174,96,.04); border-radius: 6px; line-height: 1.5; }
.review-label { font-weight: 600; }

/* 空状态 */
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 60px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin: 10px 0 4px; }
.empty-hint { font-size: 12px; color: #ddd; }

/* ===== 右侧表单面板 ===== */
.form-panel { width: 400px; flex-shrink: 0; position: sticky; top: 0; }
.panel-card { background: rgba(255,255,255,.55); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 24px 22px; }
.panel-card-title { font-size: 14px; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 20px; text-align: center; }

.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.form-group label { font-size: 13px; font-weight: 600; color: #555; }
.required { color: #e74c3c; }

.form-select { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; background: #fff; cursor: pointer; }
.form-select:focus { border-color: #1a73e8; }

.level-options { display: flex; gap: 10px; }
.level-option { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; cursor: pointer; transition: all .15s; }
.level-option input { display: none; }
.level-option:hover { border-color: #1a73e8; }
.level-option.active { border-color: #e67e22; background: rgba(230,126,34,.04); }
.level-option.active .level-label { color: #e67e22; font-weight: 600; }
.level-label { font-size: 13px; color: #888; }

.form-input { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; transition: border .15s; }
.form-input:focus { border-color: #1a73e8; }
.form-input::placeholder { color: #ccc; }

.form-textarea { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; resize: vertical; font-family: inherit; transition: border .15s; }
.form-textarea:focus { border-color: #1a73e8; }
.form-textarea::placeholder { color: #ccc; }

.upload-zone { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 20px; border: 2px dashed rgba(0,0,0,.1); border-radius: 10px; cursor: pointer; transition: all .15s; color: #aaa; font-size: 13px; }
.upload-zone:hover { border-color: #1a73e8; color: #1a73e8; background: rgba(26,115,232,.02); }
.upload-hint { font-size: 11px; color: #ccc; }

.submit-btn { width: 100%; margin-top: 18px; padding: 12px 0; border: none; border-radius: 10px; background: linear-gradient(135deg, #e67e22, #f39c12); color: #fff; font-size: 15px; font-weight: 600; cursor: pointer; transition: all .15s; }
.submit-btn:hover:not(:disabled) { box-shadow: 0 4px 14px rgba(230,126,34,.35); }
.submit-btn:disabled { background: rgba(0,0,0,.06); color: #bbb; cursor: not-allowed; }
</style>
