<template>
  <aside class="session-sidebar">
    <div class="sidebar-head">
      <div class="sidebar-title"><h3>智能数据查询</h3><span>AI</span></div>
    </div>
    <button class="create-button" type="button" :disabled="loading" @click="$emit('create')">
      <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5Z"/></svg>
      新建对话
    </button>
    <div class="history-label">对话历史</div>
    <div class="session-list">
      <button v-for="session in sessions" :key="session.sessionNo" type="button" class="session-item" :class="{active:session.sessionNo===activeSessionNo}" @click="$emit('select',session.sessionNo)">
        <span class="session-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 4h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H9l-5 3v-3a2 2 0 0 1-1-1.73V6a2 2 0 0 1 2-2Zm0 2v10h1v1.46L8.45 16H19V6H5Z"/></svg></span>
        <span class="session-copy"><strong>{{ session.title }}</strong><small>{{ formatTime(session.lastMessageAt || session.createdAt) }}</small></span>
        <span class="delete" title="删除会话" @click.stop="$emit('delete',session.sessionNo)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 5V3h8v2h5v2h-2l-1 14H6L5 7H3V5h5Zm2 0h4V5h-4Zm-3 2 .86 12h8.28L17 7H7Zm3 2h2v8h-2V9Zm4 0h2v8h-2V9Z"/></svg></span>
      </button>
      <div v-if="!loading&&!sessions.length" class="empty">暂无会话</div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { AiChatSession } from '../../../utils/aiChatApi'
defineProps<{sessions:AiChatSession[];activeSessionNo?:string;loading?:boolean}>()
defineEmits<{create:[];select:[sessionNo:string];delete:[sessionNo:string]}>()
function formatTime(value:string):string{const date=new Date(value);if(Number.isNaN(date.getTime()))return'';return new Intl.DateTimeFormat('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}).format(date)}
</script>

<style scoped>
.session-sidebar{display:flex;flex-direction:column;width:280px;min-width:280px;border:1px solid rgba(30,65,110,.08);border-width:0 1px 0 0;background:rgba(255,255,255,.58);box-shadow:5px 0 20px rgba(49,89,138,.035);backdrop-filter:blur(12px)}.sidebar-head{display:flex;align-items:center;min-height:60px;padding:0 18px;border-bottom:1px solid rgba(30,65,110,.07)}.sidebar-title{display:flex;align-items:center;gap:9px}.sidebar-title h3{margin:0;color:#1a1a2e;font-size:16px;font-weight:650}.sidebar-title span{padding:2px 8px;border:1px solid rgba(26,115,232,.15);border-radius:999px;background:rgba(26,115,232,.08);color:#1a73e8;font-size:10px;font-weight:700}.create-button{display:flex;align-items:center;justify-content:center;gap:8px;height:40px;margin:14px 14px 0;border:0;border-radius:9px;background:linear-gradient(135deg,#2f80ed,#1a73e8);box-shadow:0 5px 12px rgba(26,115,232,.17);color:#fff;font-size:13px;cursor:pointer;transition:transform .16s,box-shadow .16s}.create-button:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 7px 16px rgba(26,115,232,.24)}.create-button:disabled{opacity:.55;cursor:not-allowed}.create-button svg{width:17px;height:17px;fill:currentColor}.history-label{padding:25px 16px 7px;color:#8793a4;font-size:12px}.session-list{flex:1;padding:0 9px 12px;overflow:auto}.session-item{display:flex;align-items:center;width:100%;gap:10px;padding:10px 9px;border:1px solid transparent;border-radius:10px;background:transparent;color:#526176;text-align:left;cursor:pointer;transition:background .15s,border-color .15s}.session-item:hover{background:rgba(26,115,232,.045)}.session-item.active{border-color:rgba(26,115,232,.04);background:rgba(26,115,232,.085)}.session-icon{display:grid;place-items:center;width:28px;height:28px;flex:0 0 28px;border-radius:8px;background:rgba(26,115,232,.07)}.session-icon svg,.delete svg{width:15px;height:15px;fill:#1a73e8}.session-copy{min-width:0;flex:1}.session-copy strong,.session-copy small{display:block}.session-copy strong{overflow:hidden;color:#334057;font-size:12px;font-weight:550;text-overflow:ellipsis;white-space:nowrap}.session-copy small{margin-top:4px;color:#98a3b2;font-size:10px}.delete{display:none;padding:4px;border-radius:5px}.session-item:hover .delete{display:grid}.delete svg{fill:#9aa5b4}.delete:hover{background:rgba(229,57,53,.08)}.delete:hover svg{fill:#e53935}.empty{padding:30px 0;color:#9aa5b4;font-size:12px;text-align:center}
</style>
