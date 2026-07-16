<template>
  <section class="ai-chat-page">
    <SessionSidebar :sessions="sessions" :active-session-no="activeSession?.sessionNo" :loading="loading" @create="createSession" @select="selectSession" @delete="removeSession" />
    <main class="chat-main">
      <header class="chat-head">
        <div class="assistant-heading">
          <span class="assistant-logo" aria-hidden="true">
            <img :src="aiRobotUrl" alt="" />
          </span>
          <div>
            <div class="assistant-title"><h2>智能数据助手</h2><span>AI</span></div>
            <p>可查询实验室、预约、设备与环境数据</p>
          </div>
        </div>
        <ModelSelector v-if="activeSession" :models="models" :model-value="activeSession.modelCode" :disabled="streaming" @update:model-value="changeModel" />
      </header>
      <ChatMessageList :messages="messages" :streaming="streaming" :status-message="statusMessage" @example="sendMessage" />
      <div class="composer-wrap">
        <div v-if="pageError" class="page-error">{{ pageError }}</div>
        <ChatComposer :disabled="!activeSession || loading" :streaming="streaming" @send="sendMessage" @stop="stopGeneration" />
        <p class="safety-note">回答由 AI 生成；查询仅访问脱敏白名单视图，重要结论请结合业务系统核验。</p>
      </div>
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import aiRobotUrl from '../../../assets/AI_Robot.png'
import {
  cancelAiRun,
  createAiSession,
  deleteAiSession,
  getAiMessages,
  getAiModels,
  getAiSessions,
  updateAiSession,
  type AiChatMessage,
  type AiChatSession,
  type AiModel,
} from '../../../utils/aiChatApi'
import { streamAiMessage, type AiStreamEvent } from '../../../utils/aiChatStream'
import ChatComposer from './ChatComposer.vue'
import ChatMessageList from './ChatMessageList.vue'
import ModelSelector from './ModelSelector.vue'
import SessionSidebar from './SessionSidebar.vue'

const sessions=ref<AiChatSession[]>([])
const models=ref<AiModel[]>([])
const messages=ref<AiChatMessage[]>([])
const activeSessionNo=ref('')
const loading=ref(false)
const streaming=ref(false)
const statusMessage=ref('')
const pageError=ref('')
const currentRunNo=ref('')
let abortController:AbortController|null=null
const activeSession=computed(()=>sessions.value.find(item=>item.sessionNo===activeSessionNo.value))

onMounted(loadInitial)
onBeforeUnmount(()=>abortController?.abort())

async function loadInitial():Promise<void>{
  loading.value=true;pageError.value=''
  try{
    const [availableModels,page]=await Promise.all([getAiModels(),getAiSessions()])
    models.value=availableModels;sessions.value=page.records
    if(!sessions.value.length){const created=await createAiSession(models.value.find(item=>item.defaultModel)?.modelCode);sessions.value=[created]}
    await selectSession(sessions.value[0].sessionNo)
  }catch(error){pageError.value=errorMessage(error)}finally{loading.value=false}
}

async function refreshSessions():Promise<void>{const page=await getAiSessions();sessions.value=page.records}
async function selectSession(sessionNo:string):Promise<void>{
  if(streaming.value||sessionNo===activeSessionNo.value&&messages.value.length)return
  activeSessionNo.value=sessionNo;loading.value=true;pageError.value=''
  try{messages.value=await getAiMessages(sessionNo)}catch(error){pageError.value=errorMessage(error)}finally{loading.value=false}
}
async function createSession():Promise<void>{
  if(streaming.value)return
  try{const created=await createAiSession(models.value.find(item=>item.defaultModel)?.modelCode);sessions.value.unshift(created);activeSessionNo.value=created.sessionNo;messages.value=[]}
  catch(error){pageError.value=errorMessage(error)}
}
async function removeSession(sessionNo:string):Promise<void>{
  if(streaming.value||!window.confirm('确认删除这个查询会话吗？'))return
  try{await deleteAiSession(sessionNo);sessions.value=sessions.value.filter(item=>item.sessionNo!==sessionNo);if(activeSessionNo.value===sessionNo){if(!sessions.value.length){await createSession()}else{activeSessionNo.value='';await selectSession(sessions.value[0].sessionNo)}}}
  catch(error){pageError.value=errorMessage(error)}
}
async function changeModel(modelCode:string):Promise<void>{
  if(!activeSession.value||streaming.value)return
  try{const updated=await updateAiSession(activeSession.value.sessionNo,{modelCode});const index=sessions.value.findIndex(item=>item.sessionNo===updated.sessionNo);sessions.value[index]=updated}
  catch(error){pageError.value=errorMessage(error)}
}

async function sendMessage(content:string):Promise<void>{
  if(!activeSession.value||streaming.value||!content.trim())return
  pageError.value='';statusMessage.value='正在连接 AI 查询服务';streaming.value=true;currentRunNo.value=''
  const session=activeSession.value
  const clientMessageId=crypto.randomUUID()
  const now=new Date().toISOString()
  const human:AiChatMessage={messageNo:`local-human-${clientMessageId}`,sequenceNo:messages.value.length+1,messageType:'HUMAN',content:content.trim(),contentFormat:'TEXT',messageStatus:'COMPLETED',modelCode:session.modelCode,finishReason:null,errorCode:null,createdAt:now,completedAt:now}
  const assistant:AiChatMessage={messageNo:`local-ai-${clientMessageId}`,sequenceNo:messages.value.length+2,messageType:'AI',content:'',contentFormat:'MARKDOWN',messageStatus:'GENERATING',modelCode:session.modelCode,finishReason:null,errorCode:null,createdAt:now,completedAt:null}
  messages.value.push(human,assistant)
  abortController=new AbortController()
  try{
    await streamAiMessage({sessionNo:session.sessionNo,clientMessageId,content:content.trim(),modelCode:session.modelCode,signal:abortController.signal,onEvent:event=>handleStreamEvent(event,assistant)})
  }catch(error){
    if((error as Error).name!=='AbortError'){assistant.messageStatus='FAILED';assistant.errorCode='STREAM_CONNECTION_ERROR';pageError.value=errorMessage(error)}
  }finally{
    streaming.value=false;statusMessage.value='';abortController=null
    void refreshSessions().catch(()=>undefined)
  }
}

function handleStreamEvent(event:AiStreamEvent,assistant:AiChatMessage):void{
  if(event.event==='meta'){currentRunNo.value=String(event.data.runNo||'')}
  else if(event.event==='status'){statusMessage.value=String(event.data.message||'正在处理')}
  else if(event.event==='delta'){assistant.content+=String(event.data.content||'')}
  else if(event.event==='done'){assistant.messageNo=String(event.data.assistantMessageNo||assistant.messageNo);assistant.messageStatus='COMPLETED';assistant.finishReason=String(event.data.finishReason||'STOP');streaming.value=false}
  else if(event.event==='error'){assistant.messageNo=String(event.data.assistantMessageNo||assistant.messageNo);assistant.messageStatus='FAILED';assistant.errorCode=String(event.data.code||'AGENT_ERROR');pageError.value=String(event.data.message||'查询失败');streaming.value=false}
}

async function stopGeneration():Promise<void>{
  const runNo=currentRunNo.value
  abortController?.abort()
  if(runNo){try{await cancelAiRun(runNo)}catch{/* 连接中断时服务端可能已经完成取消 */}}
  const assistant=messages.value[messages.value.length-1]
  if(assistant?.messageType==='AI'&&assistant.messageStatus==='GENERATING')assistant.messageStatus='CANCELLED'
  streaming.value=false;statusMessage.value=''
}
function errorMessage(error:unknown):string{return error instanceof Error?error.message:'操作失败，请稍后重试'}
</script>

<style scoped>
.ai-chat-page{display:flex;height:100%;min-height:520px;overflow:hidden;background:linear-gradient(135deg,rgba(248,251,255,.86),rgba(238,246,255,.58));color:#1a1a2e}.chat-main{display:flex;min-width:0;flex:1;flex-direction:column;position:relative}.chat-main::before{position:absolute;inset:0;content:"";pointer-events:none;background:radial-gradient(circle at 75% 28%,rgba(26,115,232,.08),transparent 30%),radial-gradient(circle at 20% 90%,rgba(74,144,217,.06),transparent 24%)}.chat-head{display:flex;align-items:center;justify-content:space-between;min-height:92px;padding:0 clamp(24px,3vw,48px);border-bottom:1px solid rgba(26,115,232,.08);background:rgba(255,255,255,.28);position:relative;z-index:1}.assistant-heading{display:flex;align-items:center;gap:14px}.assistant-logo{display:block;width:52px;height:52px;padding:4px;border:4px solid rgba(26,115,232,.1);border-radius:50%;background:rgba(255,255,255,.68);box-shadow:0 5px 14px rgba(26,115,232,.18);overflow:hidden}.assistant-logo img{width:100%;height:100%;border-radius:50%;object-fit:cover}.assistant-title{display:flex;align-items:center;gap:9px}.assistant-title h2{margin:0;color:#1a1a2e;font-size:19px;font-weight:650}.assistant-title span{padding:2px 8px;border-radius:999px;background:rgba(26,115,232,.1);color:#1a73e8;font-size:10px;font-weight:700;letter-spacing:.4px}.assistant-heading p{margin:5px 0 0;color:#7a8799;font-size:12px}.composer-wrap{padding:0 clamp(24px,6vw,96px) 13px;position:relative;z-index:1}.page-error{margin:0 0 8px;padding:8px 12px;border:1px solid rgba(229,57,53,.16);border-radius:9px;background:rgba(229,57,53,.07);color:#c62828;font-size:12px}.safety-note{margin:7px 0 0;color:#9aa7b8;font-size:10px;text-align:center}@media(max-width:800px){.ai-chat-page :deep(.session-sidebar){width:220px;min-width:220px}.chat-head{min-height:78px;padding:0 16px}.assistant-logo{width:44px;height:44px;padding:3px;border-width:3px}.assistant-heading p{display:none}.composer-wrap{padding-inline:14px}}@media(max-width:620px){.ai-chat-page :deep(.session-sidebar){display:none}.assistant-title h2{font-size:17px}}
</style>
