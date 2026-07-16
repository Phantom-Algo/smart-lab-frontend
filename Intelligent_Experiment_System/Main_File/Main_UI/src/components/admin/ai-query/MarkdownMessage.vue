<template>
  <div class="markdown-message" v-html="safeHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'

const props = defineProps<{ content: string }>()
const markdown = new MarkdownIt({ html: false, linkify: true, breaks: true })
const safeHtml = computed(() => DOMPurify.sanitize(markdown.render(props.content), {
  FORBID_TAGS: ['style', 'iframe', 'object', 'embed', 'form', 'input'],
  FORBID_ATTR: ['style', 'onerror', 'onclick', 'onload'],
  ALLOW_UNKNOWN_PROTOCOLS: false,
}))
</script>

<style scoped>
.markdown-message{font-size:13px;line-height:1.75;color:#344156;overflow-wrap:anywhere}
.markdown-message :deep(p){margin:0 0 10px}.markdown-message :deep(p:last-child){margin-bottom:0}
.markdown-message :deep(h1),.markdown-message :deep(h2),.markdown-message :deep(h3){margin:14px 0 8px;color:#25334a;line-height:1.35}
.markdown-message :deep(ul),.markdown-message :deep(ol){padding-left:22px;margin:8px 0}
.markdown-message :deep(code){padding:2px 5px;border-radius:5px;background:rgba(26,115,232,.07);color:#245a9b;font-family:ui-monospace,SFMono-Regular,Menlo,monospace}
.markdown-message :deep(pre){overflow:auto;padding:12px;border:1px solid rgba(44,77,122,.12);border-radius:10px;background:rgba(245,248,252,.9)}
.markdown-message :deep(pre code){padding:0;background:transparent}
.markdown-message :deep(table){display:block;width:max-content;min-width:100%;max-width:100%;overflow-x:auto;border-collapse:collapse;margin:12px 0}
.markdown-message :deep(th),.markdown-message :deep(td){padding:8px 11px;border:1px solid rgba(44,77,122,.12);text-align:left;white-space:nowrap}
.markdown-message :deep(th){background:rgba(26,115,232,.07);color:#25334a}
.markdown-message :deep(blockquote){margin:10px 0;padding:8px 12px;border-left:3px solid #1a73e8;background:rgba(26,115,232,.055);color:#607793}
.markdown-message :deep(a){color:#1a73e8;text-decoration:none}.markdown-message :deep(a:hover){text-decoration:underline}
</style>
