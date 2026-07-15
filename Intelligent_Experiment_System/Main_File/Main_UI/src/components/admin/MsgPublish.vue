<template>
  <div class="mp-page">
    <div class="page-header">
      <h2>消息发布</h2>
      <p>向指定用户/群组发送消息</p>
    </div>

    <!-- 模式切换 -->
    <div class="mode-tabs">
      <button class="mode-tab" :class="{ active: mode === 'template' }" @click="switchMode('template')">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
        模板发送
      </button>
      <button class="mode-tab" :class="{ active: mode === 'direct' }" @click="switchMode('direct')">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        自由编辑
      </button>
    </div>

    <div class="two-col">
      <!-- 左栏：输入区 -->
      <div class="col-left">
        <!-- ====== 模板模式 ====== -->
        <template v-if="mode === 'template'">
          <div class="form-row-2">
            <div class="form-g flex-1">
              <label>选择模板 <span class="required">*</span></label>
              <select v-model="selectedTemplateId" class="form-input" @change="onTemplateChange">
                <option :value="0" disabled>请选择消息模板</option>
                <option v-for="t in availableTemplates" :key="t.id" :value="t.id">{{ t.templateName }}</option>
              </select>
            </div>
          </div>

          <div v-if="selectedTemplate" class="section-card">
            <div class="sc-title">模板内容</div>
            <div class="tmpl-box">
              <div class="tmpl-title">{{ selectedTemplate.titleTemplate }}</div>
              <div class="tmpl-body">{{ selectedTemplate.contentTemplate }}</div>
            </div>
            <div class="tmpl-meta">
              <span>消息类型：<code>{{ selectedTemplate.messageType }}</code></span>
              <span class="pri-badge" :class="'pri-' + templatePriority">
                {{ priorityLabel(templatePriority) }}
              </span>
            </div>
          </div>

          <div v-if="paramList.length > 0" class="section-card">
            <div class="sc-title">填写参数</div>
            <div class="param-grid">
              <div v-for="p in paramList" :key="p" class="form-g">
                <label class="param-label">{{ '{' + p + '}' }}</label>
                <input v-model="paramValues[p]" class="form-input" :placeholder="p" />
              </div>
            </div>
          </div>
        </template>

        <!-- ====== 自由编辑模式 ====== -->
        <template v-if="mode === 'direct'">
          <div class="section-card">
            <div class="sc-title">消息类型 <span class="required">*</span></div>
            <div class="msg-type-builder">
              <div class="mtb-row">
                <div class="form-g" style="width:180px">
                  <label>来自</label>
                  <select v-model="directSource" class="form-input">
                    <option v-for="s in sourceOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
                  </select>
                </div>
                <span class="mtb-dot">.</span>
                <div class="form-g" style="width:140px">
                  <label>优先级</label>
                  <select v-model="directPriority" class="form-input">
                    <option v-for="p in priorityOptions" :key="p.value" :value="p.value">{{ p.label }}</option>
                  </select>
                </div>
                <span class="mtb-dot">.</span>
                <div class="form-g" style="flex:1">
                  <label>事件动作</label>
                  <input v-model="directEvent" class="form-input" placeholder="notice" @blur="normalizeEvent" />
                </div>
              </div>
              <div class="mtb-preview">
                完整消息类型：<code>{{ directMessageType }}</code>
                <span v-if="!eventActionValid" class="field-err">格式：小写字母开头，字母数字下划线，多级用 . 分隔</span>
              </div>
            </div>
          </div>

          <div class="section-card">
            <div class="sc-title">消息标题 <span class="required">*</span></div>
            <input v-model="directTitle" class="form-input" placeholder="请输入消息标题" maxlength="255" />
            <div class="char-count">{{ directTitle.length }} / 255</div>
          </div>

          <div class="section-card">
            <div class="sc-title">消息内容 <span class="required">*</span></div>
            <textarea v-model="directContent" class="form-textarea" placeholder="请输入消息内容（支持多行文本）" rows="6"></textarea>
          </div>

          <div class="section-card">
            <div class="sc-title">内容格式</div>
            <select v-model="directContentFormat" class="form-input" style="max-width:200px">
              <option value="TEXT">纯文本</option>
              <option value="MARKDOWN">Markdown</option>
              <option value="HTML">HTML</option>
            </select>
          </div>
        </template>

        <!-- ====== 发送目标（共用） ====== -->
        <div class="section-card">
          <div class="sc-title">发送目标 <span class="required">*</span></div>
          <div class="target-radios">
            <label v-for="t in targetTypes" :key="t.value" class="radio-card" :class="{ active: targetType === t.value }">
              <input type="radio" v-model="targetType" :value="t.value" />
              <span>{{ t.label }}</span>
            </label>
          </div>
          <div v-if="targetType === 'single' || targetType === 'multi'" class="user-picker">
            <div class="search-row">
              <div class="search-input-wrap">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#aaa" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/></svg>
                <input
                  v-model="userSearchKeyword"
                  class="form-input search-input"
                  placeholder="输入姓名 / 用户名 / 人员编号搜索..."
                  @input="onUserSearchInput"
                  @focus="onUserSearchInput"
                  @blur="onUserSearchBlur"
                />
                <span v-if="userSearchLoading" class="search-spin"></span>
              </div>
            </div>
            <!-- 搜索结果下拉 -->
            <div v-if="showSearchDropdown && userSearchResults.length > 0" class="search-dropdown">
              <div
                v-for="r in userSearchResults"
                :key="r.userId"
                class="search-item"
                :class="{ added: isRecipientSelected(r.userId) }"
                @mousedown.prevent="addRecipient(r)"
              >
                <span class="si-name">{{ r.personName }}</span>
                <span class="si-detail">@{{ r.username }} · {{ r.personNo }}</span>
                <span v-if="isRecipientSelected(r.userId)" class="si-added">✓ 已选</span>
              </div>
            </div>
            <div v-else-if="showSearchDropdown && userSearchKeyword.trim() && !userSearchLoading" class="search-dropdown">
              <div class="search-empty">无匹配结果</div>
            </div>
            <!-- 已选用户标签 -->
            <div class="selected-users" v-if="selectedRecipients.length > 0">
              <span v-for="(r, i) in selectedRecipients" :key="r.userId" class="user-tag">
                {{ r.personName }} (@{{ r.username }})
                <button class="remove-user" @click="selectedRecipients.splice(i, 1)">&times;</button>
              </span>
            </div>
            <div v-else class="no-user-hint">尚未选择用户，请输入关键词搜索</div>
          </div>
        </div>

        <!-- 跳转链接（共用） -->
        <div class="section-card">
          <div class="sc-title">跳转链接</div>
          <input v-model="actionUrl" class="form-input" placeholder="可选，用户点击消息后的跳转路径" />
        </div>

        <!-- 操作按钮 -->
        <div v-if="sendError" class="send-error">{{ sendError }}</div>
        <div class="form-actions">
          <button class="btn-cancel" @click="resetForm">重置</button>
          <button class="btn-send" @click="doSend" :disabled="!canSend || sending">
            {{ sending ? '发送中...' : '发送消息' }}
          </button>
        </div>
      </div>

      <!-- 右栏：实时预览 -->
      <div class="col-right">
        <div class="preview-panel">
          <div class="preview-head">
            <span class="preview-dot"></span>实时预览
          </div>

          <div v-if="!hasPreviewContent" class="preview-empty">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#ddd" stroke-width="1.2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <p>{{ mode === 'template' ? '请选择模板并填写参数' : '请填写消息标题和内容' }}</p>
          </div>

          <div v-else class="preview-body">
            <div class="msg-card">
              <div class="msg-card-head">
                <span class="msg-pri-tag" :class="'pri-' + currentPriority">{{ currentPriorityLabel }}</span>
                <span class="msg-time">刚刚</span>
              </div>
              <div class="msg-card-title">{{ previewTitle }}</div>
              <div class="msg-card-content">{{ previewContent }}</div>
              <div v-if="actionUrl" class="msg-card-link">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                {{ actionUrl }}
              </div>
            </div>

            <div class="send-summary">
              <div class="sum-title">发送摘要</div>
              <div class="sum-row"><span class="sl">模式</span><span class="sv">{{ mode === 'template' ? '模板发送' : '自由编辑' }}</span></div>
              <div class="sum-row"><span class="sl">消息类型</span><span class="sv mono">{{ summaryMessageType }}</span></div>
              <div class="sum-row"><span class="sl">优先级</span><span class="sv">{{ currentPriorityLabel }}</span></div>
              <div class="sum-row"><span class="sl">目标</span><span class="sv">{{ targetDesc }}</span></div>
              <div class="sum-row" v-if="actionUrl"><span class="sl">链接</span><span class="sv mono">{{ actionUrl }}</span></div>
              <div class="sum-div"></div>
              <div class="sum-row">
                <span class="sl">状态</span>
                <span v-if="canSend" class="sv ready">就绪，可以发送</span>
                <span v-else class="sv not-ready">{{ missingHint }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发送成功 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showSuccess" class="modal-overlay" @click.self="showSuccess = false">
          <div class="modal-panel modal-small">
            <div class="modal-header"><h3>发送成功</h3><button class="modal-close" @click="showSuccess = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body" style="text-align:center">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#27ae60" stroke-width="1.5" style="margin:0 auto"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <p style="margin:12px 0 4px;font-size:15px;color:#333">消息已成功发送</p>
              <p style="margin:0;font-size:13px;color:#999">已推送到 {{ targetDesc }} 的收件箱</p>
            </div>
            <div class="modal-footer" style="justify-content:center">
              <button class="modal-btn primary" @click="showSuccess = false; resetForm()">确定</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import {
  publishMessage, searchRecipients,
  getTemplatePage, type MessageTemplateItem, type RecipientSearchResult
} from '../../utils/messageApi'

// ======================== 类型定义 ========================
interface Template {
  id: number; templateCode: string; templateName: string
  titleTemplate: string; contentTemplate: string; messageType: string
}

// ======================== 模式切换 ========================
const mode = ref<'template' | 'direct'>('template')

function switchMode(m: 'template' | 'direct') {
  if (mode.value === m) return
  mode.value = m
  sendError.value = ''
}

// ======================== 模板加载（模板模式） ========================
const availableTemplates = ref<Template[]>([])
const templateLoading = ref(false)

async function loadTemplates() {
  templateLoading.value = true
  try {
    const res = await getTemplatePage({ pageNum: 1, pageSize: 100 })
    availableTemplates.value = res.records.map(t => ({
      id: t.id, templateCode: t.templateCode, templateName: t.templateName,
      titleTemplate: t.titleTemplate, contentTemplate: t.contentTemplate,
      messageType: t.messageType
    }))
  } catch {
    // API 不可用时模板列表为空
  } finally {
    templateLoading.value = false
  }
}

// ======================== 模板模式状态 ========================
const selectedTemplateId = ref(0)
const paramValues = reactive<Record<string, string>>({})

const selectedTemplate = computed(() =>
  availableTemplates.value.find(t => t.id === selectedTemplateId.value)
)

const paramList = computed(() => {
  if (!selectedTemplate.value) return []
  const combined = selectedTemplate.value.titleTemplate + selectedTemplate.value.contentTemplate
  const matches = combined.match(/\{(\w+)\}/g)
  if (!matches) return []
  return [...new Set(matches.map(m => m.slice(1, -1)))]
})

/** 从模板 messageType 提取第二段作为优先级 */
const templatePriority = computed(() => {
  if (!selectedTemplate.value) return 3
  const seg = selectedTemplate.value.messageType.split('.')[1]
  if (seg === 'warning') return 1
  if (seg === 'system') return 2
  return 3
})

function onTemplateChange() {
  paramList.value.forEach(p => { if (!paramValues[p]) paramValues[p] = '' })
}

function replaceParams(t: string): string {
  let r = t
  for (const p of paramList.value) r = r.replace(new RegExp('\\{' + p + '\\}', 'g'), paramValues[p] || '{' + p + '}')
  return r
}

// ======================== 自由编辑模式状态 ========================
const directSource = ref('admin')
const directPriority = ref('normal')
const directEvent = ref('notice')
const directTitle = ref('')
const directContent = ref('')
const directContentFormat = ref<'TEXT' | 'MARKDOWN' | 'HTML'>('TEXT')

const sourceOptions = [
  { value: 'admin', label: '管理员' },
  { value: 'global_system', label: '全局系统' },
  { value: 'auth_profile', label: '登录鉴权与个人信息子系统' },
  { value: 'personnel', label: '人员管理子系统' },
  { value: 'device', label: '智能设备管理子系统' },
  { value: 'lab', label: '实验场地管理子系统' },
  { value: 'message_inbox', label: '消息通知与收件箱子系统' },
  { value: 'iot_env', label: '物联网环境子系统' },
  { value: 'consumable_reagent', label: '耗材与药剂子系统' },
  { value: 'panoramic_3d', label: '3D全景展示子系统' },
  { value: 'smart_teach', label: '智能化教学子系统' },
]

const priorityOptions = [
  { value: 'warning', label: '预警通知' },
  { value: 'system', label: '系统通知' },
  { value: 'normal', label: '普通通知' },
]

const directMessageType = computed(() =>
  directSource.value + '.' + directPriority.value + '.' + (directEvent.value || 'notice')
)

/** 事件动作格式校验：小写字母开头，支持字母数字下划线，多级用 . 分隔 */
const EVENT_ACTION_RE = /^[a-z][a-z0-9_]*(?:\.[a-z0-9_]+)*$/
const eventActionValid = computed(() => {
  const v = directEvent.value.trim()
  if (!v) return true // 允许为空，发送时默认用 notice
  return EVENT_ACTION_RE.test(v)
})

function normalizeEvent() {
  const v = directEvent.value.trim()
  if (!v) { directEvent.value = 'notice'; return }
  // 移除首尾的点
  directEvent.value = v.replace(/^\.+|\.+$/g, '')
}

function priorityLabel(p: number): string {
  if (p === 1) return '预警'
  if (p === 2) return '系统'
  return '通知'
}

// ======================== 发送目标（共用） ========================
const targetType = ref('single')
const actionUrl = ref('')

const targetTypes = [
  { value: 'single', label: '指定用户' },
  { value: 'all_student', label: '全部学生' },
  { value: 'all_teacher', label: '全部教师' },
  { value: 'all_manager', label: '全部管理员' },
  { value: 'all', label: '全体用户' },
]

// ---- 用户搜索 ----
const userSearchKeyword = ref('')
const userSearchResults = ref<RecipientSearchResult[]>([])
const userSearchLoading = ref(false)
const selectedRecipients = ref<RecipientSearchResult[]>([])
const showSearchDropdown = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

function isRecipientSelected(userId: number): boolean {
  return selectedRecipients.value.some(r => r.userId === userId)
}

function addRecipient(r: RecipientSearchResult) {
  if (!isRecipientSelected(r.userId)) {
    selectedRecipients.value.push(r)
  }
  userSearchKeyword.value = ''
  userSearchResults.value = []
  showSearchDropdown.value = false
}

function removeRecipient(index: number) {
  selectedRecipients.value.splice(index, 1)
}

async function onUserSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  const kw = userSearchKeyword.value.trim()
  if (!kw) {
    userSearchResults.value = []
    showSearchDropdown.value = false
    return
  }
  searchTimer = setTimeout(async () => {
    userSearchLoading.value = true
    try {
      userSearchResults.value = await searchRecipients(kw, 20)
      showSearchDropdown.value = true
    } catch {
      userSearchResults.value = []
    } finally {
      userSearchLoading.value = false
    }
  }, 300)
}

function onUserSearchBlur() {
  // 延迟关闭，让 mousedown 先触发
  setTimeout(() => { showSearchDropdown.value = false }, 200)
}

// ======================== 校验 ========================
const targetDesc = computed(() => {
  const m: Record<string, string> = { all: '全体用户', all_student: '全部学生', all_teacher: '全部教师', all_manager: '全部管理员' }
  return m[targetType.value] || selectedRecipients.value.map(r => r.personName).join('、') || '未选择'
})

const isTargetValid = computed(() => {
  if (targetType.value === 'single' || targetType.value === 'multi') return selectedRecipients.value.length > 0
  return true
})

const allParamsFilled = computed(() =>
  paramList.value.length === 0 || paramList.value.every(p => paramValues[p]?.trim())
)

const canSend = computed(() => {
  if (!isTargetValid.value) return false
  if (mode.value === 'template') {
    return selectedTemplateId.value > 0 && allParamsFilled.value
  }
  // 自由编辑模式
  return directSource.value !== ''
    && directPriority.value !== ''
    && eventActionValid.value
    && directTitle.value.trim() !== ''
    && directContent.value.trim() !== ''
})

const missingHint = computed(() => {
  if (!isTargetValid.value) return '请选择发送目标'
  if (mode.value === 'template') {
    if (!selectedTemplate.value) return '请选择模板'
    if (!allParamsFilled.value) return '请填写所有参数'
  } else {
    if (!eventActionValid.value) return '事件动作格式不正确'
    if (!directTitle.value.trim()) return '请填写消息标题'
    if (!directContent.value.trim()) return '请填写消息内容'
  }
  return ''
})

// ======================== 预览 ========================
const hasPreviewContent = computed(() => {
  if (mode.value === 'template') return !!selectedTemplate.value
  return directTitle.value.trim() !== '' || directContent.value.trim() !== ''
})

const previewTitle = computed(() => {
  if (mode.value === 'template') return selectedTemplate.value ? replaceParams(selectedTemplate.value.titleTemplate) : ''
  return directTitle.value
})

const previewContent = computed(() => {
  if (mode.value === 'template') return selectedTemplate.value ? replaceParams(selectedTemplate.value.contentTemplate) : ''
  return directContent.value
})

const summaryMessageType = computed(() => {
  if (mode.value === 'template') return selectedTemplate.value?.messageType || '-'
  return directMessageType.value
})

const currentPriority = computed(() => {
  if (mode.value === 'template') return templatePriority.value
  if (directPriority.value === 'warning') return 1
  if (directPriority.value === 'system') return 2
  return 3
})

const currentPriorityLabel = computed(() => priorityLabel(currentPriority.value))

// ======================== 发送 ========================
const sending = ref(false)
const sendError = ref('')
const showSuccess = ref(false)

function resetForm() {
  selectedTemplateId.value = 0
  directSource.value = 'admin'
  directPriority.value = 'normal'
  directEvent.value = 'notice'
  directTitle.value = ''
  directContent.value = ''
  directContentFormat.value = 'TEXT'
  targetType.value = 'single'
  actionUrl.value = ''
  selectedRecipients.value = []
  userSearchKeyword.value = ''
  userSearchResults.value = []
  showSearchDropdown.value = false
  Object.keys(paramValues).forEach(k => delete paramValues[k])
  sendError.value = ''
}

async function doSend() {
  if (!canSend.value) return
  sendError.value = ''
  sending.value = true
  try {
    const base: Record<string, unknown> = {
      targetType: targetType.value,
      actionUrl: actionUrl.value || undefined,
    }
    if (targetType.value === 'single' || targetType.value === 'multi') {
      base.targetUserIds = selectedRecipients.value.map(r => r.userId)
    }

    if (mode.value === 'template') {
      // 模板模式：只发送 templateCode + params
      await publishMessage({
        messageType: selectedTemplate.value!.messageType,
        targetType: targetType.value,
        targetUserIds: base.targetUserIds as number[] | undefined,
        templateCode: selectedTemplate.value!.templateCode,
        params: { ...paramValues },
        actionUrl: actionUrl.value || undefined,
      })
    } else {
      // 自由编辑模式：只发送 title + content + contentFormat
      await publishMessage({
        messageType: directMessageType.value,
        targetType: targetType.value,
        targetUserIds: base.targetUserIds as number[] | undefined,
        title: directTitle.value.trim(),
        content: directContent.value.trim(),
        contentFormat: directContentFormat.value,
        actionUrl: actionUrl.value || undefined,
      })
    }
    showSuccess.value = true
  } catch (e: any) {
    sendError.value = e?.message || e?.response?.data?.message || '发送失败，请检查后端服务'
  } finally {
    sending.value = false
  }
}

// ======================== 生命周期 ========================
onMounted(() => loadTemplates())
onUnmounted(() => { if (searchTimer) clearTimeout(searchTimer) })
</script>

<style scoped>
.mp-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:12px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

/* 模式切换 */
.mode-tabs { display:flex; gap:0; margin-bottom:16px; background:rgba(0,0,0,.03); border-radius:10px; padding:3px; width:fit-content }
.mode-tab { display:flex; align-items:center; gap:6px; padding:8px 18px; border:none; background:transparent; border-radius:8px; font-size:13px; font-weight:500; color:#777; cursor:pointer; transition:.15s; font-family:inherit }
.mode-tab:hover { color:#333 }
.mode-tab.active { background:#fff; color:#1a73e8; box-shadow:0 1px 3px rgba(0,0,0,.08) }

.two-col { display:flex; gap:20px; align-items:flex-start }
.col-left { flex:1; min-width:0; display:flex; flex-direction:column; gap:14px }
.col-right { width:340px; flex-shrink:0 }

.form-row-2 { display:flex; gap:12px; align-items:flex-end; background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:16px }
.flex-1 { flex:1 }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box; resize:vertical }
.form-textarea:focus { border-color:#1a73e8 }
.char-count { text-align:right; font-size:11px; color:#bbb; margin-top:2px }

.section-card { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:16px }
.sc-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid rgba(0,0,0,.05) }

/* 模板模式 */
.tmpl-box { background:rgba(0,0,0,.02); border:1px solid rgba(0,0,0,.05); border-radius:10px; padding:14px }
.tmpl-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px }
.tmpl-body { font-size:13px; color:#555; white-space:pre-wrap; line-height:1.7 }
.tmpl-meta { display:flex; align-items:center; gap:10px; margin-top:10px; padding-top:8px; border-top:1px solid rgba(0,0,0,.04); font-size:12px; color:#888 }
.tmpl-meta code { background:rgba(0,0,0,.04); padding:1px 6px; border-radius:3px; font-size:11px }
.pri-badge { padding:2px 8px; border-radius:3px; font-size:11px; font-weight:600; color:#fff }
.pri-1 { background:#e74c3c } .pri-2 { background:#f39c12 } .pri-3 { background:#1a73e8 }

.param-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px }
.param-label { font-family:'SF Mono','Menlo',monospace; font-size:12px }

/* 自由编辑：消息类型拼接 */
.msg-type-builder { background:rgba(0,0,0,.01); border-radius:10px; padding:12px }
.mtb-row { display:flex; align-items:flex-end; gap:6px }
.mtb-dot { font-size:18px; font-weight:700; color:#aaa; padding-bottom:8px }
.mtb-preview { margin-top:10px; padding:8px 12px; background:rgba(26,115,232,.04); border-radius:6px; font-size:12px; color:#666 }
.mtb-preview code { font-weight:600; color:#1a73e8 }
.field-err { display:block; color:#e74c3c; font-size:11px; margin-top:4px }

/* 发送目标 */
.target-radios { display:flex; gap:6px; flex-wrap:wrap }
.radio-card { padding:7px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; cursor:pointer; color:#666; transition:.15s; display:flex; align-items:center; gap:4px }
.radio-card input { display:none }
.radio-card.active { border-color:#1a73e8; background:rgba(26,115,232,.04); color:#1a73e8; font-weight:500 }

.user-picker { background:rgba(0,0,0,.01); border-radius:10px; padding:12px; margin-top:8px }

/* 搜索 */
.search-row { margin-bottom:8px }
.search-input-wrap { position:relative; display:flex; align-items:center }
.search-icon { position:absolute; left:10px; pointer-events:none }
.search-input { padding-left:34px !important }
.search-spin { position:absolute; right:10px; width:14px; height:14px; border:2px solid rgba(26,115,232,.15); border-top-color:#1a73e8; border-radius:50%; animation:spin .6s linear infinite }
@keyframes spin { to { transform:rotate(360deg) } }

/* 搜索结果下拉 */
.search-dropdown { position:relative; background:#fff; border:1px solid rgba(0,0,0,.08); border-radius:10px; box-shadow:0 8px 24px rgba(0,0,0,.08); max-height:220px; overflow-y:auto; z-index:10 }
.search-item { display:flex; align-items:center; gap:8px; padding:10px 14px; cursor:pointer; font-size:13px; transition:.1s; border-bottom:1px solid rgba(0,0,0,.03) }
.search-item:last-child { border-bottom:none }
.search-item:hover { background:rgba(26,115,232,.04) }
.search-item.added { background:rgba(39,174,96,.04); color:#999 }
.si-name { font-weight:500; color:#333; white-space:nowrap }
.si-detail { color:#999; font-size:12px; flex:1; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.si-added { color:#27ae60; font-size:11px; font-weight:500; white-space:nowrap }
.search-empty { padding:16px; text-align:center; font-size:13px; color:#ccc }

/* 已选用户 */
.selected-users { display:flex; gap:6px; flex-wrap:wrap; margin-top:10px }
.user-tag { display:flex; align-items:center; gap:2px; padding:4px 10px; background:rgba(26,115,232,.08); border:1px solid rgba(26,115,232,.15); border-radius:6px; font-size:12px; color:#1a73e8 }
.remove-user { background:none; border:none; cursor:pointer; color:#1a73e8; font-size:16px; line-height:1; padding:0 2px; opacity:.6 }
.remove-user:hover { opacity:1 }
.no-user-hint { font-size:12px; color:#ccc; padding:6px 0; text-align:center }

/* 操作按钮 */
.send-error { padding:8px 14px; background:rgba(231,76,60,.06); border:1px solid rgba(231,76,60,.15); border-radius:8px; color:#e74c3c; font-size:13px }
.form-actions { display:flex; justify-content:flex-end; gap:12px; padding-top:4px }
.btn-cancel { padding:10px 22px; border:1px solid rgba(0,0,0,.1); border-radius:10px; background:#fff; font-size:14px; color:#666; cursor:pointer; font-family:inherit }
.btn-cancel:hover { border-color:#aaa }
.btn-send { padding:10px 28px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:14px; font-weight:500; cursor:pointer; font-family:inherit }
.btn-send:hover:not(:disabled) { box-shadow:0 4px 14px rgba(26,115,232,.35) }
.btn-send:disabled { opacity:.4; cursor:not-allowed }

/* 右栏预览 */
.preview-panel { position:sticky; top:20px; background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:14px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.preview-head { display:flex; align-items:center; gap:8px; padding:14px 18px; font-size:14px; font-weight:600; color:#1a1a2e; border-bottom:1px solid rgba(0,0,0,.05) }
.preview-dot { width:8px; height:8px; border-radius:50%; background:#27ae60 }
.preview-empty { display:flex; flex-direction:column; align-items:center; padding:50px 20px; gap:10px }
.preview-empty p { margin:0; font-size:13px; color:#ccc }
.preview-body { padding:16px }

.msg-card { background:#fff; border:1px solid rgba(0,0,0,.06); border-radius:10px; padding:14px; margin-bottom:14px }
.msg-card-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px }
.msg-pri-tag { padding:2px 8px; border-radius:3px; font-size:11px; font-weight:600; color:#fff }
.pri-1 { background:#e74c3c } .pri-2 { background:#f39c12 } .pri-3 { background:#1a73e8 }
.msg-time { font-size:11px; color:#bbb }
.msg-card-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px }
.msg-card-content { font-size:13px; color:#555; white-space:pre-wrap; line-height:1.7 }
.msg-card-link { display:flex; align-items:center; gap:4px; margin-top:10px; padding-top:10px; border-top:1px solid rgba(0,0,0,.04); font-size:12px; color:#1a73e8 }

.send-summary { background:rgba(0,0,0,.02); border-radius:10px; padding:14px }
.sum-title { font-size:13px; font-weight:600; color:#888; margin-bottom:10px }
.sum-row { display:flex; justify-content:space-between; padding:5px 0; font-size:13px }
.sl { color:#999 } .sv { color:#333; text-align:right; max-width:180px; overflow:hidden; text-overflow:ellipsis }
.sv.mono { font-family:'SF Mono','Menlo',monospace; font-size:11px }
.sum-div { height:1px; background:rgba(0,0,0,.05); margin:8px 0 }
.sv.ready { color:#27ae60; font-weight:500 } .sv.not-ready { color:#e74c3c; font-weight:500 }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-small { max-width:380px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1 }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s; font-family:inherit }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
