<template>
  <div class="resource-page">
    <div class="page-header">
      <h2>教学资源</h2>
      <p>上传PPT/Word/视频等教学资料</p>
    </div>

    <!-- 顶部操作栏 -->
    <div class="top-bar">
      <div class="top-left">
        <button class="upload-btn" @click="showUpload = true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
          上传资源
        </button>
        <div class="search-box">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#aaa" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchText" placeholder="搜索资源..." class="search-input" />
        </div>
      </div>
      <div class="top-right">
        <span class="count-label">共 {{ filteredResources.length }} 个资源</span>
        <select v-model="courseFilter" class="filter-select">
          <option value="">全部课程</option>
          <option v-for="c in courseOptions" :key="c.id" :value="c.name">{{ c.name }}</option>
        </select>
      </div>
    </div>

    <!-- 统计条 -->
    <div class="type-bar">
      <button v-for="t in typeTabs" :key="t.key" class="type-btn" :class="{ active: typeFilter === t.key }" @click="typeFilter = t.key">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path :d="t.icon"/></svg>
        {{ t.label }}
        <span class="type-count">{{ t.count }}</span>
      </button>
    </div>

    <!-- 资源卡片网格 -->
    <div class="resource-grid" v-if="filteredResources.length > 0">
      <div v-for="r in filteredResources" :key="r.id" class="resource-card">
        <div class="card-icon" :class="'icon-' + r.fileType">
          <svg v-if="r.fileType === 'pdf'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          <svg v-else-if="r.fileType === 'ppt'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="4" width="20" height="16" rx="2"/><line x1="6" y1="8" x2="18" y2="8"/><line x1="6" y1="12" x2="18" y2="12"/><line x1="6" y1="16" x2="12" y2="16"/></svg>
          <svg v-else-if="r.fileType === 'xlsx'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/></svg>
          <svg v-else-if="r.fileType === 'mp4'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="5 3 19 12 5 21 5 3"/></svg>
          <svg v-else-if="r.fileType === 'docx'" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <svg v-else viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        </div>
        <div class="card-body">
          <div class="card-name" :title="r.name">{{ r.name }}</div>
          <div class="card-meta">
            <span class="meta-type">{{ r.fileType.toUpperCase() }}</span>
            <span class="meta-dot">·</span>
            <span>{{ r.size }}</span>
          </div>
          <div class="card-info">
            <span class="info-class">{{ r.className }}</span>
            <span class="info-date">{{ r.uploadDate }}</span>
          </div>
        </div>
        <div class="card-actions">
          <button class="card-btn" title="下载" @click="downloadResource(r)">下载</button>
          <button v-if="r.fileType === 'mp4'" class="card-btn primary" title="预览" @click="previewResource(r)">预览</button>
          <button class="card-btn danger" title="删除" @click="deleteResource(r)">删除</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p>暂无教学资源，点击上方按钮上传</p>
    </div>

    <!-- 上传弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showUpload" class="popup-overlay" @click.self="showUpload = false">
          <div class="popup-panel wide">
            <div class="popup-header">
              <h3>上传教学资源</h3>
              <button class="popup-close" @click="showUpload = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="popup-body">
              <div class="form-group">
                <label>所属课程班级</label>
                <select v-model.number="uploadForm.courseId" class="form-input form-select">
                  <option :value="0">请选择课程</option>
                  <option v-for="c in courseOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
                </select>
              </div>
              <div class="form-group">
                <label>资源名称</label>
                <input v-model="uploadForm.resourceName" class="form-input" placeholder="请输入资源名称" />
              </div>
              <div class="form-group">
                <label>资源描述（可选）</label>
                <textarea v-model="uploadForm.description" class="form-input form-textarea" placeholder="简要描述资源内容、适用实验等" rows="2"></textarea>
              </div>
              <div class="form-group">
                <label>上传文件</label>
                <div class="upload-zone" @click="fileInput?.click()">
                  <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#aaa" stroke-width="1.5"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                  <span v-if="uploadForm.file" class="upload-hint">{{ uploadForm.file.name }}（{{ (uploadForm.file.size / 1024).toFixed(1) }} KB）</span>
                  <span v-else class="upload-hint">点击或拖拽文件到此处上传</span>
                  <span class="upload-types">支持 PDF、Word、PPT、Excel、视频（≤100MB）</span>
                </div>
                <input ref="fileInput" type="file" class="file-input-hidden" accept=".pdf,.ppt,.pptx,.xls,.xlsx,.doc,.docx,.mp4,.avi,.mov" @change="handleFileChange" style="display:none" />
              </div>
            </div>
            <div class="popup-footer">
              <button class="popup-btn cancel" @click="showUpload = false">取消</button>
              <button class="popup-btn primary" @click="confirmUpload" :disabled="uploading || !uploadForm.courseId || !uploadForm.resourceName || !uploadForm.file">
                {{ uploading ? '上传中…' : '确认上传' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 预览弹窗（视频） -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showPreview" class="popup-overlay" @click.self="showPreview = false">
          <div class="popup-panel preview-panel">
            <div class="popup-header">
              <h3>{{ previewResourceData?.name }}</h3>
              <button class="popup-close" @click="showPreview = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="popup-body preview-body">
              <div class="video-placeholder">
                <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                <span>视频预览区域</span>
                <span class="video-name">{{ previewResourceData?.name }}</span>
                <span class="video-size">{{ previewResourceData?.size }}</span>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import api from '../../utils/api'

// 文件 input 引用
const fileInput = ref<HTMLInputElement | null>(null)

interface Resource {
  id: number; name: string; fileType: 'pdf' | 'ppt' | 'xlsx' | 'mp4' | 'docx'
  size: string; className: string; uploadDate: string
  downloadUrl: string
}

// 上传表单
const uploadForm = reactive({
  courseId: 0,
  resourceName: '',
  description: '',
  file: null as File | null,
})

const showUpload = ref(false)
const showPreview = ref(false)
const previewResourceData = ref<Resource | null>(null)
const searchText = ref('')
const courseFilter = ref('')
const typeFilter = ref('')
const resources = ref<Resource[]>([])
const loadError = ref(false)
const uploading = ref(false)

// 课程选项（从 API 获取）
const courseOptions = ref<{ id: number; name: string }[]>([])

async function loadCourses() {
  try {
    const res = await api.get('/teaching/courses', { params: { pageNum: 1, pageSize: 100 } })
    const records = res.data?.data?.records || []
    courseOptions.value = records.map((r: any) => ({ id: r.courseId ?? r.id, name: r.courseName || '' }))
  } catch {
    courseOptions.value = []
  }
}

async function loadResources() {
  loadError.value = false
  try {
    const res = await api.get('/teaching/resources', { params: { pageNum: 1, pageSize: 100 } })
    const records = res.data?.data?.records || []
    resources.value = records.map((r: any) => ({
      id: r.id,
      name: r.resourceName || '',
      fileType: (r.resourceTypeLabel || r.resourceType || 'pdf').toLowerCase() as Resource['fileType'],
      size: r.fileSize ? formatFileSize(r.fileSize) : '',
      className: r.courseName || '',
      uploadDate: r.createdAt?.slice(0, 10) || '',
      downloadUrl: r.downloadUrl || r.fileUrl || '',
    }))
  } catch (e) {
    console.error('[TeacherResource] load error:', e)
    loadError.value = true
  }
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

onMounted(() => {
  loadCourses()
  loadResources()
})

const typeTabs = computed(() => [
  { key: '', label: '全部', count: resources.value.length, icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z M14 2v6h6' },
  { key: 'pdf', label: 'PDF', count: resources.value.filter(r => r.fileType === 'pdf').length, icon: '' },
  { key: 'ppt', label: 'PPT', count: resources.value.filter(r => r.fileType === 'ppt').length, icon: '' },
  { key: 'xlsx', label: 'Excel', count: resources.value.filter(r => r.fileType === 'xlsx').length, icon: '' },
  { key: 'docx', label: 'Word', count: resources.value.filter(r => r.fileType === 'docx').length, icon: '' },
  { key: 'mp4', label: '视频', count: resources.value.filter(r => r.fileType === 'mp4').length, icon: '' },
])

const filteredResources = computed(() => {
  let list = resources.value
  if (searchText.value) {
    const q = searchText.value.toLowerCase()
    list = list.filter(r => r.name.toLowerCase().includes(q))
  }
  if (courseFilter.value) {
    list = list.filter(r => r.className === courseFilter.value)
  }
  if (typeFilter.value) {
    list = list.filter(r => r.fileType === typeFilter.value)
  }
  return list
})

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files?.length) {
    uploadForm.file = target.files[0]
    if (!uploadForm.resourceName) {
      uploadForm.resourceName = target.files[0].name.replace(/\.[^.]+$/, '')
    }
  }
}

async function confirmUpload() {
  if (!uploadForm.courseId || !uploadForm.resourceName || !uploadForm.file) {
    alert('请选择课程、填写资源名称、选择文件')
    return
  }
  uploading.value = true
  try {
    // 1. 获取上传 token
    const tokenRes = await api.post('/teaching/resources/upload-token', {
      fileName: uploadForm.file.name,
      fileSize: uploadForm.file.size,
      contentType: uploadForm.file.type,
    })
    const uploadInfo = tokenRes.data?.data

    // 2. 上传文件到对象存储（这步取决于实际存储后端；如果上传 token 返回的是直接上传 URL）
    let fileUrl = ''
    if (uploadInfo?.uploadMode === 'direct' && uploadInfo?.uploadUrl) {
      const formData = new FormData()
      formData.append('file', uploadForm.file)
      // 使用默认的 axios 实例可能会加 Authorization header，根据实际情况调整
      await api.post(uploadInfo.uploadUrl, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      fileUrl = uploadInfo.objectKey || ''
    }

    // 3. 创建资源记录
    await api.post('/teaching/resources', {
      courseId: uploadForm.courseId,
      resourceName: uploadForm.resourceName,
      resourceType: mapFileType(uploadForm.file.name),
      fileUrl: fileUrl || uploadInfo?.objectKey || '',
      fileSize: uploadForm.file.size,
    })

    showUpload.value = false
    uploadForm.file = null
    // 重置文件 input
    const fileInput = document.querySelector('.file-input-hidden') as HTMLInputElement
    if (fileInput) fileInput.value = ''

    await loadResources()
    alert('上传成功')
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function mapFileType(fileName: string): number {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  const map: Record<string, number> = { pdf: 1, ppt: 2, pptx: 2, xls: 3, xlsx: 3, doc: 4, docx: 4, mp4: 5, avi: 5, mov: 5 }
  return map[ext] || 1
}

async function downloadResource(r: Resource) {
  if (r.downloadUrl) {
    window.open(r.downloadUrl, '_blank')
  } else {
    alert('该资源暂无下载地址')
  }
}

function previewResource(r: Resource): void {
  previewResourceData.value = r
  showPreview.value = true
}

async function deleteResource(r: Resource): Promise<void> {
  if (!confirm(`确认删除「${r.name}」吗？`)) return
  try {
    await api.delete(`/teaching/resources/${r.id}`)
    await loadResources()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '删除失败')
  }
}
</script>

<style scoped>
.resource-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 顶部操作栏 ===== */
.top-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.top-left { display: flex; align-items: center; gap: 12px; }
.upload-btn { display: flex; align-items: center; gap: 6px; padding: 10px 22px; border: none; border-radius: 10px; background: linear-gradient(135deg,#1a73e8,#4a90d9); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; transition: all .15s; white-space: nowrap; }
.upload-btn:hover { box-shadow: 0 4px 14px rgba(26,115,232,.35); }

.search-box { display: flex; align-items: center; gap: 8px; padding: 8px 14px; background: rgba(255,255,255,.5); border-radius: 10px; border: 1px solid rgba(0,0,0,.06); }
.search-input { border: none; background: none; outline: none; font-size: 13px; color: #333; width: 180px; }
.search-input::placeholder { color: #bbb; }

.top-right { display: flex; align-items: center; gap: 12px; }
.count-label { font-size: 13px; color: #999; }
.filter-select { padding: 8px 14px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; background: rgba(255,255,255,.5); font-size: 13px; color: #555; outline: none; cursor: pointer; }

/* ===== 类型筛选 ===== */
.type-bar { display: flex; gap: 6px; margin-bottom: 18px; }
.type-btn { display: flex; align-items: center; gap: 6px; padding: 7px 14px; border: 1px solid rgba(0,0,0,.06); border-radius: 8px; background: rgba(255,255,255,.4); font-size: 12px; color: #888; cursor: pointer; transition: all .15s; }
.type-btn:hover { border-color: #1a73e8; color: #1a73e8; }
.type-btn.active { background: rgba(26,115,232,.1); border-color: #1a73e8; color: #1a73e8; font-weight: 600; }
.type-count { font-size: 10px; padding: 1px 6px; border-radius: 6px; background: rgba(0,0,0,.05); }
.type-btn.active .type-count { background: rgba(26,115,232,.15); }

/* ===== 资源卡片网格 ===== */
.resource-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }

.resource-card { background: rgba(255,255,255,.5); border-radius: 12px; border: 1px solid rgba(0,0,0,.04); padding: 20px 18px 16px; display: flex; flex-direction: column; transition: all .15s; }
.resource-card:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); box-shadow: 0 4px 14px rgba(0,0,0,.05); }

.card-icon { width: 48px; height: 48px; border-radius: 10px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; }
.icon-pdf { background: rgba(231,76,60,.1); color: #e74c3c; }
.icon-ppt { background: rgba(230,126,34,.1); color: #e67e22; }
.icon-xlsx { background: rgba(39,174,96,.1); color: #27ae60; }
.icon-mp4 { background: rgba(142,68,173,.1); color: #8e44ad; }
.icon-docx { background: rgba(26,115,232,.1); color: #1a73e8; }

.card-body { flex: 1; margin-bottom: 14px; }
.card-name { font-size: 13px; font-weight: 600; color: #1a1a2e; line-height: 1.4; margin-bottom: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { font-size: 11px; color: #aaa; display: flex; align-items: center; gap: 4px; margin-bottom: 6px; }
.meta-type { font-weight: 600; }
.meta-dot { color: #ddd; }
.card-info { display: flex; align-items: center; justify-content: space-between; }
.info-class { font-size: 11px; color: #1a73e8; background: rgba(26,115,232,.06); padding: 2px 8px; border-radius: 4px; }
.info-date { font-size: 11px; color: #bbb; }

.card-actions { display: flex; gap: 6px; padding-top: 10px; border-top: 1px solid rgba(0,0,0,.04); }
.card-btn { flex: 1; padding: 6px 0; border: 1px solid rgba(0,0,0,.08); border-radius: 6px; background: #fff; font-size: 11px; color: #666; cursor: pointer; text-align: center; transition: all .15s; }
.card-btn:hover { border-color: #1a73e8; color: #1a73e8; }
.card-btn.primary { background: #1a73e8; color: #fff; border: none; }
.card-btn.primary:hover { background: #1557b0; }
.card-btn.danger:hover { border-color: #e74c3c; color: #e74c3c; }

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin-top: 12px; }

/* ===== 弹窗 ===== */
.popup-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.popup-panel { background: #fff; border-radius: 16px; width: 480px; max-height: 85vh; overflow-y: auto; box-shadow: 0 16px 48px rgba(0,0,0,.15); }
.popup-panel.wide { width: 560px; }
.popup-panel.preview-panel { width: 640px; }
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

.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.form-group label { font-size: 13px; font-weight: 500; color: #555; }
.form-input { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; outline: none; transition: border .15s; }
.form-input:focus { border-color: #1a73e8; }
.form-select { background: #fff; cursor: pointer; }
.form-textarea { resize: vertical; font-family: inherit; }

.upload-zone { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 28px; border: 2px dashed rgba(0,0,0,.1); border-radius: 10px; cursor: pointer; transition: all .15s; }
.upload-zone:hover { border-color: #1a73e8; background: rgba(26,115,232,.02); }
.upload-hint { font-size: 14px; color: #888; font-weight: 500; }
.upload-types { font-size: 11px; color: #bbb; }
.file-input-hidden { display: none; }

.preview-body { padding: 24px; }
.video-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 60px 20px; background: rgba(0,0,0,.02); border-radius: 10px; color: #ccc; font-size: 14px; }
.video-name { font-size: 13px; font-weight: 600; color: #888; }
.video-size { font-size: 12px; color: #bbb; }
</style>
