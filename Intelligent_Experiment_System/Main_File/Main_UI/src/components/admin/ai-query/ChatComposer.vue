<template>
  <div class="composer">
    <textarea v-model="value" :disabled="disabled" maxlength="2000" rows="2" placeholder="询问实验室与预约数据，Enter 发送，Shift+Enter 换行" @keydown="onKeydown"></textarea>
    <div class="composer-actions">
      <span>{{ value.length }}/2000</span>
      <button v-if="streaming" class="stop-btn" type="button" @click="$emit('stop')">
        <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="7" y="7" width="10" height="10" rx="1"/></svg>停止
      </button>
      <button v-else class="send-btn" type="button" :disabled="disabled || !value.trim()" @click="submit">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m3.4 20.4 17.2-8.4L3.4 3.6 3 10l11 2-11 2 .4 6.4Z"/></svg><span>发送</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
defineProps<{ disabled?: boolean; streaming?: boolean }>()
const emit = defineEmits<{ send: [content: string]; stop: [] }>()
const value = ref('')
function submit(): void { const content=value.value.trim(); if(!content)return; emit('send',content); value.value='' }
function onKeydown(event: KeyboardEvent): void { if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();submit()} }
</script>

<style scoped>
.composer{padding:12px 14px;border:1px solid rgba(26,115,232,.2);border-radius:13px;background:rgba(255,255,255,.8);box-shadow:0 6px 20px rgba(49,89,138,.07);backdrop-filter:blur(12px)}
.composer:focus-within{border-color:rgba(26,115,232,.4);box-shadow:0 7px 24px rgba(26,115,232,.1)}.composer textarea{display:block;width:100%;min-height:52px;max-height:160px;resize:vertical;border:0;outline:0;background:transparent;color:#27354b;font:13px/1.55 inherit;box-sizing:border-box}.composer textarea::placeholder{color:#9aa8ba}
.composer-actions{display:flex;align-items:center;justify-content:flex-end;gap:10px;margin-top:6px;color:#9aa7b8;font-size:11px}.composer-actions button{display:flex;align-items:center;gap:6px;min-height:34px;padding:7px 14px;border:0;border-radius:8px;color:#fff;cursor:pointer}.composer-actions button:disabled{opacity:.42;cursor:not-allowed}.composer-actions svg{width:16px;height:16px;fill:currentColor}.send-btn{background:linear-gradient(135deg,#3488f4,#1a73e8);box-shadow:0 4px 10px rgba(26,115,232,.18)}.stop-btn{background:#d85262}.send-btn span{display:inline}@media(max-width:560px){.send-btn span{display:none}.composer-actions button{padding-inline:10px}}
</style>
