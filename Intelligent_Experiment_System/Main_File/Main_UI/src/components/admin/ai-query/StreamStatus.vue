<template>
  <div v-if="visible" class="stream-status">
    <span class="pulse"></span><span>{{ message || '正在处理' }}</span>
  </div>
</template>
<script setup lang="ts">
defineProps<{
  visible: boolean
  message?: string
}>()
</script>
<style scoped>
.stream-status{display:flex;align-items:center;gap:8px;color:#6783a7;font-size:12px}.pulse{width:7px;height:7px;border-radius:50%;background:#1a73e8;box-shadow:0 0 0 0 rgba(26,115,232,.36);animation:pulse 1.5s infinite}@keyframes pulse{70%{box-shadow:0 0 0 7px rgba(26,115,232,0)}100%{box-shadow:0 0 0 0 rgba(26,115,232,0)}}
</style>
