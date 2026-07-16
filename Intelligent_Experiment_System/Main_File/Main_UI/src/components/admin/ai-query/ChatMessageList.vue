<template>
  <div ref="container" class="message-list">
    <div v-if="!messages.length" class="welcome">
      <div class="welcome-message">
        <div class="welcome-icon"><img :src="aiRobotUrl" alt="" /></div>
        <div class="welcome-card">
          <h2>您好，我是智能数据助手</h2>
          <p>我可以帮您查询和分析实验室相关数据，包括实验室预约、设备状态与实验室使用情况。</p>
          <p>您可以直接输入问题，或选择下方的快捷问题开始查询。</p>
        </div>
      </div>
      <div class="examples"><button v-for="example in examples" :key="example" type="button" @click="$emit('example',example)"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M19 4h-1V2h-2v2H8V2H6v2H5a3 3 0 0 0-3 3v12a3 3 0 0 0 3 3h14a3 3 0 0 0 3-3V7a3 3 0 0 0-3-3Zm1 15a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-8h16v8ZM4 9V7a1 1 0 0 1 1-1h1v2h2V6h8v2h2V6h1a1 1 0 0 1 1 1v2H4Z"/></svg>{{ example }}</button></div>
    </div>
    <article v-for="message in messages" :key="message.messageNo" class="message" :class="message.messageType.toLowerCase()">
      <div class="avatar"><img v-if="message.messageType==='AI'" :src="aiRobotUrl" alt="" /><svg v-else viewBox="0 0 24 24" aria-hidden="true"><path d="M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10Zm0 2c-5 0-9 2.5-9 6v2h18v-2c0-3.5-4-6-9-6Z"/></svg></div>
      <div class="bubble" :class="{'thinking-bubble':message.messageType==='AI'&&message.messageStatus==='GENERATING'&&!message.content}">
        <div v-if="message.messageType==='AI'&&message.messageStatus==='GENERATING'&&!message.content" class="thinking-indicator" role="status" aria-label="AI 正在思考">
          <span></span><span></span><span></span>
        </div>
        <MarkdownMessage v-else-if="message.messageType==='AI'" :content="message.content" />
        <p v-else>{{ message.content }}</p>
        <span v-if="message.messageStatus!=='COMPLETED'&&message.messageStatus!=='GENERATING'" class="message-error">{{ statusLabel(message.messageStatus) }}</span>
      </div>
    </article>
    <StreamStatus :visible="streaming" :message="statusMessage" />
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import aiRobotUrl from '../../../assets/AI_Robot.png'
import type { AiChatMessage } from '../../../utils/aiChatApi'
import MarkdownMessage from './MarkdownMessage.vue'
import StreamStatus from './StreamStatus.vue'
const props=defineProps<{messages:AiChatMessage[];streaming:boolean;statusMessage?:string}>()
defineEmits<{example:[value:string]}>()
const container=ref<HTMLElement|null>(null)
const examples=['查询今天待审核的预约','统计过去一周每个实验室的预约次数','查询已签到的实验室预约记录']
watch(()=>[props.messages.length,props.messages[props.messages.length-1]?.content],async()=>{await nextTick();if(container.value)container.value.scrollTop=container.value.scrollHeight},{deep:true})
function statusLabel(status:string):string{return({FAILED:'生成失败',TIMEOUT:'查询超时',CANCELLED:'已停止',INTERRUPTED:'服务中断'} as Record<string,string>)[status]||status}
</script>

<style scoped>
.message-list{flex:1;overflow:auto;padding:26px clamp(24px,6vw,96px);position:relative;z-index:1}.welcome{display:flex;min-height:100%;max-width:980px;margin:0 auto;flex-direction:column}.welcome-message{display:flex;align-items:flex-start;gap:13px;margin-top:2px}.welcome-icon,.avatar{display:grid;place-items:center;width:38px;height:38px;min-width:38px;padding:2px;border:3px solid rgba(26,115,232,.09);border-radius:50%;background:rgba(255,255,255,.72);box-sizing:border-box;overflow:hidden}.welcome-icon img,.avatar img{width:100%;height:100%;border-radius:50%;object-fit:cover}.avatar svg{width:18px;height:18px;fill:#fff}.welcome-card{max-width:600px;padding:16px 19px;border:1px solid rgba(44,77,122,.1);border-radius:6px 14px 14px 14px;background:rgba(255,255,255,.75);box-shadow:0 4px 13px rgba(49,89,138,.06);backdrop-filter:blur(8px)}.welcome-card h2{margin:0 0 7px;color:#25334a;font-size:14px;font-weight:650}.welcome-card p{margin:3px 0;color:#7c8999;font-size:12px;line-height:1.7}.examples{display:flex;flex-wrap:wrap;justify-content:center;gap:10px;margin-top:auto;padding-top:34px}.examples button{display:flex;align-items:center;gap:8px;padding:9px 15px;border:1px solid rgba(44,77,122,.12);border-radius:999px;background:rgba(255,255,255,.72);box-shadow:0 3px 10px rgba(49,89,138,.045);color:#435168;font-size:12px;cursor:pointer;transition:all .16s}.examples button svg{width:15px;height:15px;fill:#1a73e8}.examples button:hover{border-color:rgba(26,115,232,.25);background:#fff;color:#1a73e8;transform:translateY(-1px)}.message{display:flex;gap:11px;max-width:980px;margin:0 auto 20px}.message.human{flex-direction:row-reverse}.human .avatar{padding:0;border-width:0;background:linear-gradient(135deg,#5e9fea,#2b75cf)}.human .avatar svg{width:18px;height:18px;fill:#fff}.bubble{min-width:0;max-width:min(820px,82%);padding:13px 16px;border:1px solid rgba(44,77,122,.1);border-radius:6px 14px 14px 14px;background:rgba(255,255,255,.78);box-shadow:0 3px 10px rgba(49,89,138,.045);backdrop-filter:blur(8px)}.thinking-bubble{display:flex;align-items:center;min-width:58px;min-height:44px;padding:12px 15px}.thinking-indicator{display:flex;align-items:center;gap:5px;height:18px}.thinking-indicator span{width:7px;height:7px;border-radius:50%;background:#4389df;animation:thinking-dot 1.15s ease-in-out infinite}.thinking-indicator span:nth-child(2){animation-delay:.16s}.thinking-indicator span:nth-child(3){animation-delay:.32s}.human .bubble{border-color:rgba(26,115,232,.06);border-radius:14px 6px 14px 14px;background:rgba(26,115,232,.09);box-shadow:none}.bubble p{margin:0;color:#344156;font-size:13px;line-height:1.7;white-space:pre-wrap}.message-error{display:block;margin-top:8px;color:#d74343;font-size:11px}.stream-status{max-width:980px;margin:0 auto;padding-left:50px}@keyframes thinking-dot{0%,60%,100%{opacity:.35;transform:translateY(0) scale(.82)}30%{opacity:1;transform:translateY(-5px) scale(1)}}@media(prefers-reduced-motion:reduce){.thinking-indicator span{animation:none;opacity:.8}}@media(max-width:700px){.message-list{padding:18px 14px}.welcome-card{padding:14px}.examples{justify-content:flex-start}.examples button{width:100%;justify-content:flex-start}.bubble{max-width:88%}}
</style>
