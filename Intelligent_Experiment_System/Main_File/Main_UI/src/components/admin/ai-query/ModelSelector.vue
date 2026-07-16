<template>
  <label class="model-selector">
    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3a4 4 0 0 1 4 4v1h1a4 4 0 0 1 4 4 4 4 0 0 1-4 4h-1v1a4 4 0 0 1-8 0v-1H7a4 4 0 0 1 0-8h1V7a4 4 0 0 1 4-4Zm-2 5h4V7a2 2 0 1 0-4 0v1Zm0 8v1a2 2 0 1 0 4 0v-1h-4Zm-3-6a2 2 0 1 0 0 4h1v-4H7Zm9 0v4h1a2 2 0 1 0 0-4h-1Zm-6 0v4h4v-4h-4Z"/></svg>
    <select :value="modelValue" :disabled="disabled" @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)">
      <option v-for="model in models" :key="model.modelCode" :value="model.modelCode">
        {{ model.displayName }}{{ model.defaultModel ? '（默认）' : '' }}
      </option>
    </select>
  </label>
</template>

<script setup lang="ts">
import type { AiModel } from '../../../utils/aiChatApi'
defineProps<{ models: AiModel[]; modelValue: string; disabled?: boolean }>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<style scoped>
.model-selector{display:flex;align-items:center;gap:8px;padding:9px 11px;border:1px solid rgba(44,77,122,.14);border-radius:9px;background:rgba(255,255,255,.72);box-shadow:0 3px 10px rgba(49,89,138,.05)}
.model-selector svg{width:17px;height:17px;fill:#1a73e8}.model-selector select{max-width:210px;border:0;outline:0;color:#334057;background:transparent;font-size:12px;font-weight:500;cursor:pointer}.model-selector select:disabled{cursor:not-allowed;opacity:.6}.model-selector option{color:#26354a;background:#fff}
</style>
