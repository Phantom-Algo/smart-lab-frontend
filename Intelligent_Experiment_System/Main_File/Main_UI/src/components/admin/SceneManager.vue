<template>
  <div class="sm-page">
    <div class="page-header">
      <h2>场景映射管理</h2>
      <p>实验室到3D场景的映射CRUD</p>
    </div>

    <div class="toolbar">
      <span class="toolbar-info">已有映射 {{ mappings.length }} 个</span>
      <button class="btn-add" @click="openCreate">+ 添加映射</button>
    </div>

    <div class="table-wrap" v-if="mappings.length > 0">
      <table class="map-table">
        <thead>
          <tr>
            <th>实验室</th>
            <th>构建路径</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in mappings" :key="m.id">
            <td class="lab-cell">{{ m.labName }}</td>
            <td>
              <code class="path-code">{{ m.buildPath }}</code>
              <span v-if="!m.buildPath" class="no-path">未配置</span>
            </td>
            <td class="date-cell">{{ m.createdAt }}</td>
            <td class="act-cell">
              <button class="act-btn" @click="openEdit(m)">编辑</button>
              <button class="act-btn preview" @click="goPreview(m)">预览</button>
              <button class="act-btn del" @click="confirmDelete(m)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/></svg>
      <p>暂无场景映射，请点击"添加映射"创建</p>
    </div>

    <!-- 创建/编辑弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>{{ isEditing ? '编辑映射' : '添加映射' }}</h3>
              <button class="modal-close" @click="showForm = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>实验室 <span class="required">*</span></label>
                <select v-model="form.labName" class="form-input" :disabled="isEditing">
                  <option value="" disabled>请选择实验室</option>
                  <option v-for="l in availableLabs" :key="l" :value="l">{{ l }}</option>
                </select>
                <span class="form-hint" v-if="isEditing">实验室不可更改</span>
              </div>
              <div class="form-g">
                <label>构建路径 <span class="required">*</span></label>
                <input v-model="form.buildPath" class="form-input" placeholder="如 unity-game/3D_Lab/" />
                <span class="form-hint">Unity WebGL 构建产物文件夹路径，前端自动拼接 index.html</span>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showForm = false">取消</button>
              <button class="modal-btn primary" @click="doSave" :disabled="!form.labName || !form.buildPath">{{ isEditing ? '保存' : '确认添加' }}</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 删除确认弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDelete" class="modal-overlay" @click.self="showDelete = false">
          <div class="modal-panel modal-small">
            <div class="modal-header"><h3>确认删除</h3><button class="modal-close" @click="showDelete = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <p class="delete-warn">确定要删除 <strong>{{ deleteTarget?.labName }}</strong> 的场景映射吗？删除后该实验室将无法在3D漫游中展示。</p>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDelete = false">取消</button>
              <button class="modal-btn danger" @click="doDelete">确认删除</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

const emit = defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

interface SceneMapping {
  id: number; labName: string; buildPath: string; createdAt: string
}

const allLabs = ['A305 分析化学实验室', 'B103 物理实验室', 'C202 计算机实验室', 'D108 生物培养实验室']

const mappings = ref<SceneMapping[]>([
  { id:1, labName:'A305 分析化学实验室', buildPath:'unity-game/3D_Lab/', createdAt:'2026-06-15' },
  { id:2, labName:'B103 物理实验室', buildPath:'unity-game/Physics_Lab/', createdAt:'2026-06-20' },
  { id:3, labName:'C202 计算机实验室', buildPath:'unity-game/CS_Lab/', createdAt:'2026-07-01' },
])

// Labs that don't have a mapping yet (for create dropdown)
const availableLabs = computed(() => {
  const mapped = new Set(mappings.value.map(m => m.labName))
  return allLabs.filter(l => !mapped.has(l))
})

// Create/Edit
const showForm = ref(false)
const isEditing = ref(false)
const editId = ref(0)
const form = reactive({ labName:'', buildPath:'' })

function openCreate() {
  isEditing.value = false; editId.value = 0
  form.labName = ''; form.buildPath = ''
  showForm.value = true
}
function openEdit(m: SceneMapping) {
  isEditing.value = true; editId.value = m.id
  form.labName = m.labName; form.buildPath = m.buildPath
  showForm.value = true
}
function doSave() {
  if (!form.labName || !form.buildPath) return
  if (isEditing.value) {
    const m = mappings.value.find(m => m.id === editId.value)
    if (m) { m.buildPath = form.buildPath }
  } else {
    const newId = Math.max(...mappings.value.map(m => m.id), 0) + 1
    mappings.value.push({
      id: newId, labName: form.labName, buildPath: form.buildPath,
      createdAt: new Date().toISOString().slice(0, 10)
    })
  }
  showForm.value = false
}

// Delete
const showDelete = ref(false)
const deleteTarget = ref<SceneMapping | null>(null)
function confirmDelete(m: SceneMapping) { deleteTarget.value = m; showDelete.value = true }
function doDelete() {
  if (!deleteTarget.value) return
  const idx = mappings.value.findIndex(m => m.id === deleteTarget.value!.id)
  if (idx !== -1) mappings.value.splice(idx, 1)
  showDelete.value = false; deleteTarget.value = null
}

// Preview - navigate to 3D viewer
function goPreview(m: SceneMapping) {
  emit('navigate', { key: '3d-show', name: '3D漫游' })
}
</script>

<style scoped>
.sm-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.toolbar-info { font-size:13px; color:#888 }
.btn-add { padding:9px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }
.btn-add:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }

.table-wrap { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.map-table { width:100%; border-collapse:collapse; font-size:13px }
.map-table thead { background:rgba(0,0,0,.02) }
.map-table th { padding:12px 16px; text-align:left; font-weight:600; color:#888; font-size:12px; border-bottom:1px solid rgba(0,0,0,.06); white-space:nowrap }
.map-table td { padding:11px 16px; border-bottom:1px solid rgba(0,0,0,.03); color:#333; vertical-align:middle }
.map-table tr:last-child td { border-bottom:none }

.lab-cell { font-weight:500; color:#1a1a2e }
.path-code { font-family:'SF Mono','Menlo',monospace; font-size:12px; background:rgba(0,0,0,.03); padding:3px 8px; border-radius:4px; color:#555 }
.no-path { font-size:12px; color:#ccc }
.date-cell { color:#888; font-size:12px; white-space:nowrap }
.act-cell { display:flex; gap:6px }

.act-btn { padding:5px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; color:#555; cursor:pointer; white-space:nowrap }
.act-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.act-btn.preview { color:#8e44ad; border-color:rgba(142,68,173,.2) }
.act-btn.preview:hover { border-color:#8e44ad; background:rgba(142,68,173,.04) }
.act-btn.del:hover { border-color:#e74c3c; color:#e74c3c }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.form-hint { font-size:11px; color:#aaa; margin-top:2px }
.delete-warn { font-size:14px; color:#555; line-height:1.6; margin:0 }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-small { max-width:400px }
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
.modal-btn.danger { background:linear-gradient(135deg,#e74c3c,#c0392b); color:#fff }
.modal-btn.danger:hover { box-shadow:0 4px 12px rgba(231,76,60,.3) }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
