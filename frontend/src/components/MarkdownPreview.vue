<template>
  <div class="markdown-preview" v-html="renderedHtml" ref markdownRef></div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

const props = defineProps<{
  text: string
}>()

const markdownRef = ref<HTMLElement>()

// 配置 marked renderer
const renderer = new marked.Renderer()

renderer.code = function(code, language) {
  const validLanguage = hljs.getLanguage(language) ? language : 'plaintext'
  const highlighted = hljs.highlight(code, { language: validLanguage }).value

  // 生成唯一的代码块 ID
  const codeId = `code-${Math.random().toString(36).substr(2, 9)}`

  return `
    <div class="code-block-wrapper" style="position: relative; margin: 1em 0;">
      <div class="code-block-header" style="display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background-color: #f6f8fa; border: 1px solid #ddd; border-radius: 6px 6px 0 0; font-size: 12px; color: #666;">
        <span>${language || 'code'}</span>
        <button class="copy-btn" data-code-id="${codeId}" style="background: none; border: 1px solid #ddd; border-radius: 4px; padding: 4px 12px; cursor: pointer; display: flex; align-items: center; gap: 4px; transition: all 0.2s;">
          <svg viewBox="0 0 1024 1024" width="14" height="14" fill="currentColor">
            <path d="M768 256h-64q-13 0-22.5-9.5t-9.5-22.5v-64q0-13 9.5-22.5t22.5-9.5h64q13 0 22.5 9.5t9.5 22.5v64q0 13-9.5 22.5t-22.5 9.5zm-64 640h64q13 0 22.5 9.5t9.5 22.5v64q0 13-9.5 22.5t-22.5 9.5h-64q-13 0-22.5-9.5t-9.5-22.5v-64q0-13 9.5-22.5t22.5-9.5zm0 128h64q13 0 22.5 9.5t9.5 22.5v64q0 13-9.5 22.5t-22.5 9.5h-64q-13 0-22.5-9.5t-9.5-22.5v-64q0-13 9.5-22.5t22.5-9.5zm0-128h64q13 0 22.5 9.5t9.5 22.5v64q0 13-9.5 22.5t-22.5 9.5h-64q-13 0-22.5-9.5t-9.5-22.5v-64q0-13 9.5-22.5t22.5-9.5z" fill="currentColor"></path>
          </svg>
          <span>复制</span>
        </button>
      </div>
      <pre style="margin: 0; padding: 16px; background-color: #f6f8fa; border: 1px solid #ddd; border-top: none; border-radius: 0 0 6px 6px; overflow-x: auto;"><code id="${codeId}" class="hljs language-${validLanguage}">${highlighted}</code></pre>
      <textarea class="code-textarea" id="textarea-${codeId}" style="position: absolute; left: -9999px;">${code}</textarea>
    </div>
  `
}

// 配置 marked
marked.setOptions({
  renderer,
  breaks: true,
  gfm: true
})

const renderedHtml = computed(() => {
  if (!props.text) return ''
  return marked.parse(props.text) as string
})

// 添加复制功能
onMounted(() => {
  if (markdownRef.value) {
    markdownRef.value.addEventListener('click', handleCopyClick)
  }
})

async function handleCopyClick(event: Event) {
  const target = event.target as HTMLElement
  const copyBtn = target.closest('.copy-btn')

  if (copyBtn) {
    const codeId = copyBtn.getAttribute('data-code-id')
    const textarea = document.getElementById(`textarea-${codeId}`) as HTMLTextAreaElement

    if (textarea) {
      try {
        textarea.select()
        await navigator.clipboard.writeText(textarea.value)
        ElMessage.success('已复制到剪贴板')
      } catch (error) {
        ElMessage.error('复制失败')
      }
    }
  }
}
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
