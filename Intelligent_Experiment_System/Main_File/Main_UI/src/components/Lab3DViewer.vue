<template>
  <div class="lab3d-page">
    <template v-if="!activeLab">
      <div class="page-header">
        <h2>3D 全景展示</h2>
        <p>选择实验室进入虚拟漫游，沉浸式查看实验室环境与设备布局</p>
      </div>

      <div class="section">
        <h3 class="section-title">参观及预约</h3>
        <div class="lab-grid">
          <div
            v-for="lab in tourLabs"
            :key="lab.key"
            class="lab-card"
            :style="{ '--accent': lab.color }"
          >
            <div class="lab-card-left">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" :stroke="lab.color" stroke-width="1.5">
                <path :d="lab.iconPath"/>
              </svg>
            </div>
            <div class="lab-card-body">
              <h4 class="lab-name">{{ lab.name }}</h4>
              <p class="lab-desc">{{ lab.desc }}</p>
            </div>
            <div class="lab-card-right">
              <button class="enter-btn" @click="enterLab(lab.key)">
                进入场景
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">参观与借用</h3>
        <div class="lab-grid">
          <div
            v-for="lab in borrowLabs"
            :key="lab.key"
            class="lab-card"
            :style="{ '--accent': lab.color }"
          >
            <div class="lab-card-left">
              <svg viewBox="0 0 24 24" width="32" height="32" fill="none" :stroke="lab.color" stroke-width="1.5">
                <path :d="lab.iconPath"/>
              </svg>
            </div>
            <div class="lab-card-body">
              <h4 class="lab-name">{{ lab.name }}</h4>
              <p class="lab-desc">{{ lab.desc }}</p>
            </div>
            <div class="lab-card-right">
              <button class="enter-btn" @click="enterLab(lab.key)">
                进入场景
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12h14M12 5l7 7-7 7"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 全屏 3D 场景 -->
    <div v-else class="viewer-fullscreen">
      <div class="viewer-toolbar">
        <button class="toolbar-back" @click="closeViewer">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          返回
        </button>
        <span class="toolbar-title">{{ activeLab.name }}</span>
        <div class="toolbar-actions">
          <button class="toolbar-btn" @click="reloadViewer">刷新</button>
          <button class="toolbar-btn" @click="openStandalone">新窗口</button>
        </div>
      </div>
      <iframe
        ref="viewerFrame"
        :key="viewerFrameKey"
        class="viewer-frame"
        :src="viewerSrc"
        title="智慧实验室 3D 场景"
        allowfullscreen
        @load="handleFrameLoad"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { REALTIME_EVENT_NAME, type RealtimeEnvelope } from '../utils/wsClient'
import { userId, userName } from '../stores/authStore'

type LabCard = {
  key: string
  name: string
  desc: string
  color: string
  iconPath: string
}

const tourLabs: LabCard[] = [
  {
    key: '3d-class', name: '教学实验室 (Class Lab)',
    desc: '标准教学实验环境，配备常规实验器材与教学演示设备，适用于基础实验课程教学。',
    color: '#4A90D9',
    iconPath: 'M4 6h16M4 10h16M4 14h16M4 18h16M9 3v18',
  },
  {
    key: '3d-common', name: '通用实验室 (Common Lab)',
    desc: '多功能综合实验室，涵盖有机化学、分析化学等常规实验操作，设备配置齐全。',
    color: '#27AE60',
    iconPath: 'M9 3H5a2 2 0 00-2 2v4m6-6h10a2 2 0 012 2v4M9 3v18m0 0h10a2 2 0 002-2V9M9 21H5a2 2 0 01-2-2V9m0 0h18',
  },
  {
    key: '3d-computer', name: '计算机实验室 (Computer Lab)',
    desc: '计算机与网络实验专用实验室，配备高性能工作站，支持编程、仿真与数据分析实验。',
    color: '#E67E22',
    iconPath: 'M20 18V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12m16 0a2 2 0 01-2 2H6a2 2 0 01-2-2m16 0v-4M4 14v4m0-4h16',
  },
  {
    key: '3d-group', name: '小组研讨室 (Group Study Room)',
    desc: '小组讨论与协作空间，配置可移动桌椅与投影设备，适用于课题研讨与团队实验。',
    color: '#8E44AD',
    iconPath: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M23 21v-2a4 4 0 00-4-4h-1M7 7a4 4 0 108 0 4 4 0 00-8 0zm10-2a4 4 0 110 8',
  },
]

const borrowLabs: LabCard[] = [
  {
    key: '3d-reagent', name: '试剂与器材借用室 (Reagent & Equipment)',
    desc: '试剂与实验器材领用管理中心，支持在线查询库存、预约借用与归还登记。',
    color: '#E74C3C',
    iconPath: 'M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16zM3.27 6.96L12 12.01l8.73-5.05M12 22.08V12',
  },
]

const SCENE_URL_MAP: Record<string, string> = {
  '3d-class': 'class_lab',
  '3d-common': 'common_lab',
  '3d-computer': 'computer_lab',
  '3d-group': 'group_study',
  '3d-reagent': 'reagent_loan',
}

const WEBGL_ENTRY_URL = '/3D_Lab/index.html'

const allLabs = [...tourLabs, ...borrowLabs]
const viewerFrame = ref<HTMLIFrameElement | null>(null)
const viewerFrameKey = ref(0)
const viewerReady = ref(false)
const activeLab = ref<LabCard | null>(null)

const viewerSceneKey = computed(() => {
  if (!activeLab.value) return ''
  return SCENE_URL_MAP[activeLab.value.key] ?? ''
})

const viewerSrc = computed(() => {
  if (!activeLab.value) return 'about:blank'
  const params = new URLSearchParams({
    scene: viewerSceneKey.value,
    token: localStorage.getItem('accessToken') ?? '',
    userId: userId.value ? String(userId.value) : (localStorage.getItem('userId') ?? ''),
    userName: userName.value || localStorage.getItem('userName') || '',
    apiBase: `${window.location.origin}/api`,
  })
  return `${WEBGL_ENTRY_URL}?${params.toString()}`
})

type ViewerMessage = {
  type: string
  payload?: Record<string, unknown>
}

function getViewerWindow(): Window | null {
  return viewerFrame.value?.contentWindow ?? null
}

function postToViewer(message: ViewerMessage): void {
  const targetWindow = getViewerWindow()
  if (!targetWindow) return
  targetWindow.postMessage(message, window.location.origin)
}

function syncViewerContext(): void {
  if (!activeLab.value) return
  postToViewer({
    type: 'unity-context',
    payload: {
      scene: viewerSceneKey.value,
      token: localStorage.getItem('accessToken') ?? '',
      userId: userId.value ? String(userId.value) : (localStorage.getItem('userId') ?? ''),
      userName: userName.value || localStorage.getItem('userName') || '',
      apiBase: `${window.location.origin}/api`,
    },
  })
}

function handleRealtimeEvent(event: Event): void {
  if (!activeLab.value) return
  const envelope = (event as CustomEvent<RealtimeEnvelope>).detail
  if (!envelope) return
  postToViewer({
    type: 'unity-realtime-event',
    payload: envelope as unknown as Record<string, unknown>,
  })
}

function handleViewerMessage(event: MessageEvent): void {
  if (event.origin !== window.location.origin) return
  const message = event.data
  if (!message || typeof message !== 'object') return

  if (message.type === 'unity-ready') {
    viewerReady.value = true
    syncViewerContext()
    return
  }
  if (message.type === 'unity-request-context') {
    syncViewerContext()
    return
  }
  // Unity 发出的业务事件（预约提交、数据同步等），通过全局自定义事件转发给其他组件
  if (message.type === 'unity-event' && message.payload) {
    const payload = message.payload
    const eventName = payload.eventName || payload.event || ''
    window.dispatchEvent(new CustomEvent('unity-biz-event', { detail: payload }))
    // 预约相关事件：刷新借用/预约列表
    if (eventName && (eventName.includes('reservation') || eventName.includes('borrow'))) {
      window.dispatchEvent(new CustomEvent('refresh-reservation-list'))
    }
    return
  }
}

function enterLab(key: string): void {
  const targetLab = allLabs.find((lab) => lab.key === key)
  if (!targetLab || !SCENE_URL_MAP[key]) return
  activeLab.value = targetLab
  viewerReady.value = false
  viewerFrameKey.value += 1
}

function handleFrameLoad(): void {
  if (!activeLab.value) return
  syncViewerContext()
}

function reloadViewer(): void {
  if (!activeLab.value) return
  viewerReady.value = false
  viewerFrameKey.value += 1
}

function openStandalone(): void {
  if (!activeLab.value) return
  window.open(viewerSrc.value, '_blank', 'noopener')
}

function closeViewer(): void {
  activeLab.value = null
  viewerReady.value = false
}

onMounted(() => {
  window.addEventListener('message', handleViewerMessage)
  window.addEventListener(REALTIME_EVENT_NAME, handleRealtimeEvent as EventListener)
})

onBeforeUnmount(() => {
  window.removeEventListener('message', handleViewerMessage)
  window.removeEventListener(REALTIME_EVENT_NAME, handleRealtimeEvent as EventListener)
})
</script>

<style scoped>
.lab3d-page {
  height: 100%;
  overflow-y: auto;
  padding: 28px 36px;
}

.page-header {
  margin-bottom: 32px;
}
.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px;
}
.page-header p {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.section {
  margin-bottom: 32px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin: 0 0 16px;
}

.lab-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lab-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: rgba(255,255,255,.5);
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,.04);
  border-left: 4px solid var(--accent);
  transition: all .2s;
}
.lab-card:hover {
  background: rgba(255,255,255,.8);
  box-shadow: 0 4px 20px rgba(0,0,0,.06);
  transform: translateX(4px);
}

.lab-card-left {
  width: 56px; height: 56px;
  border-radius: 12px;
  background: rgba(255,255,255,.6);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lab-card-body { flex: 1; min-width: 0; }
.lab-name {
  font-size: 16px; font-weight: 600;
  color: #1a1a2e; margin: 0 0 6px;
}
.lab-desc {
  font-size: 13px; color: #888;
  line-height: 1.6; margin: 0;
}

.lab-card-right { flex-shrink: 0; }
.enter-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 22px;
  border: 1px solid var(--accent); border-radius: 8px;
  background: transparent; color: var(--accent);
  font-size: 14px; font-weight: 500; cursor: pointer;
  white-space: nowrap;
  transition: all .2s;
}
.enter-btn:hover { background: var(--accent); color: #fff; }

/* 全屏场景 */
.viewer-fullscreen {
  position: fixed; inset: 0; z-index: 100;
  background: #0f172a;
  display: flex; flex-direction: column;
}

.viewer-toolbar {
  display: flex; align-items: center;
  padding: 12px 20px;
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
  gap: 16px;
  flex-shrink: 0;
}

.toolbar-back {
  display: flex; align-items: center; gap: 6px;
  border: none; background: transparent;
  color: #94a3b8; font-size: 14px; cursor: pointer;
  padding: 6px 10px; border-radius: 8px;
  transition: all .2s;
}
.toolbar-back:hover { color: #fff; background: rgba(51,65,85,.6); }

.toolbar-title {
  flex: 1; font-size: 15px; font-weight: 600;
  color: #e2e8f0;
}

.toolbar-actions { display: flex; gap: 8px; }
.toolbar-btn {
  padding: 6px 14px; border-radius: 8px;
  border: 1px solid rgba(148,163,184,.3);
  background: transparent; color: #cbd5e1;
  font-size: 13px; cursor: pointer;
  transition: all .2s;
}
.toolbar-btn:hover { background: rgba(51,65,85,.8); color: #fff; }

.viewer-frame {
  flex: 1; width: 100%;
  border: 0; background: #020617;
}
</style>
