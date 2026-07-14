<template>
  <div class="rm-page">
    <div class="page-header">
      <h2>教学资源</h2>
      <p>PPT/Word/视频等资源管理</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ resources.length }}</div><div class="stat-label">资源总数</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ typeCount('ppt') }}</div><div class="stat-label">PPT</div></div>
      <div class="stat-card"><div class="stat-num blue">{{ typeCount('word') }}</div><div class="stat-label">Word</div></div>
      <div class="stat-card"><div class="stat-num purple">{{ typeCount('video') }}</div><div class="stat-label">视频</div></div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="searchName" placeholder="搜索资源名..." class="search-input" @keyup.enter="pageNum = 1" />
        <select v-model="filterType" class="tool-select">
          <option value="">全部类型</option>
          <option v-for="t in typeOptions" :key="t.key" :value="t.key">{{ t.label }}</option>
        </select>
        <select v-model="filterCourse" class="tool-select">
          <option value="">全部课程</option>
          <option v-for="c in courseOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 份资源</span>
        <button class="add-btn" @click="openCreateDialog">+ 上传资源</button>
      </div>
    </div>

    <div class="data-table-wrap">
      <table class="data-table">
        <thead><tr><th>资源名称</th><th>类型</th><th>关联课程</th><th>大小</th><th>上传时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="r in pagedList" :key="r.id">
            <td><span class="res-name">{{ r.name }}</span></td>
            <td><span class="type-tag" :class="'tag-' + r.fileType">{{ typeLabel(r.fileType) }}</span></td>
            <td>{{ r.courseName || '通用' }}</td>
            <td>{{ formatSize(r.fileSize) }}</td>
            <td>{{ r.uploadedAt }}</td>
            <td class="action-cell">
              <button class="row-btn" @click="editResource(r)">编辑</button>
              <button class="row-btn danger" @click="deleteResource(r)">删除</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="6" class="empty-cell">暂无教学资源</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 上传/编辑弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDialog" class="modal-overlay" @click.self="showDialog = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>{{ dialogMode === 'create' ? '上传资源' : '编辑资源' }}</h3>
              <button class="modal-close" @click="showDialog = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g"><label>资源名称 <span class="required">*</span></label><input v-model="form.name" class="form-input" placeholder="如：有机化学实验讲义" /></div>
              <div class="form-g"><label>资源类型 <span class="required">*</span></label>
                <select v-model="form.fileType" class="form-input">
                  <option v-for="t in typeOptions" :key="t.key" :value="t.key">{{ t.label }}</option>
                </select>
              </div>
              <div class="form-g"><label>关联课程</label>
                <select v-model="form.courseId" class="form-input"><option :value="0">未关联（通用资源）</option><option v-for="c in courseOptions" :key="c.id" :value="c.id">{{ c.name }}</option></select>
              </div>
              <div v-if="dialogMode === 'create'" class="form-g">
                <label>选择文件 <span class="required">*</span></label>
                <div class="file-upload" @click="simulateUpload">
                  <span v-if="!form.fileName" class="fu-placeholder">点击选择文件（PPT/Word/PDF/视频）</span>
                  <span v-else class="fu-name">{{ form.fileName }}（{{ formatSize(form.fileSize) }}）</span>
                </div>
              </div>
              <div v-else class="form-g">
                <label>文件</label>
                <span class="fu-name">{{ form.fileName }}（{{ formatSize(form.fileSize) }}）</span>
              </div>
              <div class="form-g"><label>资源描述</label><textarea v-model="form.description" class="form-input form-textarea" rows="2" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDialog = false">取消</button>
              <button class="modal-btn primary" @click="submitForm" :disabled="!canSubmit">{{ dialogMode === 'create' ? '确认上传' : '保存' }}</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { getResourcePage, type ResourceItem } from '../../utils/resourceApi'

interface Resource {
  id: number; name: string; fileType: string; courseId: number; courseName: string
  fileName: string; fileSize: number; description: string | null; uploadedAt: string
}

const typeOptions = [
  { key:'ppt', label:'PPT' }, { key:'word', label:'Word' }, { key:'pdf', label:'PDF' },
  { key:'video', label:'视频' }, { key:'other', label:'其他' },
]
const courseOptions = [
  { id:1, name:'有机化学实验' }, { id:2, name:'大学物理实验' }, { id:3, name:'生物培养技术' },
]

const resources = ref<Resource[]>([])

async function loadResources() {
  try {
    const res = await getResourcePage({ pageNum: 1, pageSize: 200 })
    resources.value = res.records.map(r => ({
      id: r.id, name: r.name, fileType: r.fileType || 'other',
      courseId: r.courseId ?? 0, courseName: r.courseName || '',
      fileName: r.fileName || '未命名', fileSize: r.fileSize ?? 0,
      description: r.description, uploadedAt: r.uploadedAt || ''
    }))
  } catch { /* API 不可用 */ }
}
onMounted(() => { loadResources() })

const searchName = ref('')
const filterType = ref('')
const filterCourse = ref<number | ''>('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = resources.value
  if (searchName.value) list = list.filter(r => r.name.includes(searchName.value))
  if (filterType.value) list = list.filter(r => r.fileType === filterType.value)
  if (filterCourse.value !== '') list = list.filter(r => r.courseId === filterCourse.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function typeCount(t: string) { return resources.value.filter(r => r.fileType === t).length }
function typeLabel(t: string): string { return typeOptions.find(o => o.key === t)?.label || t }
function formatSize(bytes: number): string {
  if (bytes >= 1048576) return (bytes / 1048576).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

const showDialog = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref(0)
const form = reactive({ name: '', fileType: 'ppt', courseId: 0, fileName: '', fileSize: 0, description: '' })
const canSubmit = computed(() => form.name.trim() && form.fileType)

function openCreateDialog() {
  form.name = ''; form.fileType = 'ppt'; form.courseId = 0; form.fileName = ''; form.fileSize = 0; form.description = ''
  editingId.value = 0; dialogMode.value = 'create'; showDialog.value = true
}
function editResource(r: Resource) {
  form.name = r.name; form.fileType = r.fileType; form.courseId = r.courseId
  form.fileName = r.fileName; form.fileSize = r.fileSize; form.description = r.description || ''
  editingId.value = r.id; dialogMode.value = 'edit'; showDialog.value = true
}
function simulateUpload() {
  const mockFiles = ['实验讲义.pptx', '实验指导.docx', '操作视频.mp4', '参考资料.pdf']
  const picked = mockFiles[Math.floor(Math.random() * mockFiles.length)]
  const sizes = [1200000, 2500000, 32000000, 1500000]
  form.fileName = picked
  form.fileSize = sizes[mockFiles.indexOf(picked)]
}
function submitForm() {
  if (!canSubmit.value) return
  if (dialogMode.value === 'create') {
    resources.value.unshift({
      id: Date.now(), name: form.name.trim(), fileType: form.fileType,
      courseId: form.courseId, courseName: courseOptions.find(c => c.id === form.courseId)?.name || '',
      fileName: form.fileName || '未命名文件', fileSize: form.fileSize || 0,
      description: form.description || null, uploadedAt: '2026-07-09 ' + new Date().toTimeString().slice(0, 5),
    })
  } else {
    const r = resources.value.find(r => r.id === editingId.value)
    if (r) {
      r.name = form.name.trim(); r.fileType = form.fileType
      r.courseId = form.courseId; r.courseName = courseOptions.find(c => c.id === form.courseId)?.name || ''
      r.description = form.description || null
    }
  }
  showDialog.value = false
}
function deleteResource(r: Resource) {
  if (confirm(`确定删除「${r.name}」吗？`)) {
    const idx = resources.value.findIndex(x => x.id === r.id)
    if (idx >= 0) resources.value.splice(idx, 1)
  }
}
</script>

<style scoped>
.rm-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 }
.orange { color:#f39c12 }
.purple { color:#8e44ad }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:150px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.data-table { width:100%; border-collapse:collapse }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04) }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.res-name { font-weight:600; color:#1a1a2e }
.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.tag-ppt { background:rgba(243,156,18,.1); color:#e67e22 }
.tag-word { background:rgba(26,115,232,.1); color:#1557b0 }
.tag-pdf { background:rgba(231,76,60,.1); color:#c0392b }
.tag-video { background:rgba(142,68,173,.1); color:#8e44ad }
.tag-other { background:rgba(0,0,0,.05); color:#888 }

.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.row-btn.danger { color:#e74c3c }
.row-btn.danger:hover { border-color:#e74c3c }
.empty-cell { text-align:center; color:#ccc; padding:40px }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

.file-upload { padding:16px; border:2px dashed rgba(26,115,232,.2); border-radius:10px; text-align:center; cursor:pointer; transition:.15s }
.file-upload:hover { border-color:#1a73e8; background:rgba(26,115,232,.02) }
.fu-placeholder { color:#aaa; font-size:13px }
.fu-name { color:#333; font-size:13px; font-weight:500 }
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
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { resize:vertical; min-height:60px }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
