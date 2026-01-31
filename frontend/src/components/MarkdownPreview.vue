<template>
  <div class="markdown-preview" v-html="renderedHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps<{
  text: string
}>()

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext'
    return hljs.highlight(code, { language }).value
  },
  langPrefix: 'hljs language-',
  breaks: true,
  gfm: true
})

const renderedHtml = computed(() => {
  if (!props.text) return ''
  return marked.parse(props.text) as string
})
</script>

<style scoped>
.markdown-preview {
  line-height: 1.6;
  color: #333;
}

.markdown-preview :deep(h1) {
  font-size: 2em;
  font-weight: bold;
  margin: 0.8em 0 0.4em 0;
  border-bottom: 2px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-preview :deep(h2) {
  font-size: 1.5em;
  font-weight: bold;
  margin: 0.8em 0 0.4em 0;
  border-bottom: 1px solid #eee;
  padding-bottom: 0.3em;
}

.markdown-preview :deep(h3) {
  font-size: 1.25em;
  font-weight: bold;
  margin: 0.8em 0 0.4em 0;
}

.markdown-preview :deep(p) {
  margin: 0.8em 0;
}

.markdown-preview :deep(code) {
  background-color: #f6f8fa;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.markdown-preview :deep(pre) {
  background-color: #f6f8fa;
  padding: 16px;
  overflow: auto;
  border-radius: 6px;
  margin: 1em 0;
}

.markdown-preview :deep(pre code) {
  background-color: transparent;
  padding: 0;
}

.markdown-preview :deep(ul),
.markdown-preview :deep(ol) {
  margin: 0.8em 0;
  padding-left: 2em;
}

.markdown-preview :deep(li) {
  margin: 0.4em 0;
}

.markdown-preview :deep(blockquote) {
  border-left: 4px solid #ddd;
  padding-left: 1em;
  margin: 1em 0;
  color: #666;
}

.markdown-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}

.markdown-preview :deep(th),
.markdown-preview :deep(td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-preview :deep(th) {
  background-color: #f6f8fa;
  font-weight: bold;
}

.markdown-preview :deep(strong) {
  font-weight: bold;
}

.markdown-preview :deep(em) {
  font-style: italic;
}
</style>
