<template>
  <el-card class="agent-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <h4>{{ title }}</h4>
        <el-button link type="primary" @click="handleTest" :loading="testing">
          <el-icon><Connection /></el-icon>
          测试连通性
        </el-button>
      </div>
    </template>

    <p class="description">{{ description }}</p>

    <el-form :model="localConfig" label-width="100px">
      <el-form-item label="提供商">
        <el-select v-model="localConfig.provider">
          <el-option label="Ollama (本地)" value="ollama" />
          <el-option label="DeepSeek" value="deepseek" />
          <el-option label="OpenAI" value="openai" />
        </el-select>
      </el-form-item>

      <el-form-item label="Base URL">
        <el-input v-model="localConfig.baseUrl" placeholder="http://localhost:11434" />
      </el-form-item>

      <el-form-item label="Model Name">
        <el-input v-model="localConfig.modelName" placeholder="qwen2.5:7b" />
      </el-form-item>

      <el-form-item label="Temperature">
        <el-slider v-model="localConfig.temperature" :min="0" :max="2" :step="0.1" show-input />
      </el-form-item>

      <el-form-item label="API Key">
        <el-input v-model="localConfig.apiKey" type="password" show-password placeholder="Ollama 可留空" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'

interface Props {
  title: string
  description: string
  provider: string
  baseUrl: string
  modelName: string
  temperature: number
  apiKey: string
}

interface Emits {
  (e: 'update:provider', value: string): void
  (e: 'update:baseUrl', value: string): void
  (e: 'update:modelName', value: string): void
  (e: 'update:temperature', value: number): void
  (e: 'update:apiKey', value: string): void
  (e: 'test'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const testing = ref(false)

const localConfig = reactive({
  provider: props.provider,
  baseUrl: props.baseUrl,
  modelName: props.modelName,
  temperature: props.temperature,
  apiKey: props.apiKey
})

// 监听变化并同步到父组件
watch(() => localConfig.provider, (v) => emit('update:provider', v))
watch(() => localConfig.baseUrl, (v) => emit('update:baseUrl', v))
watch(() => localConfig.modelName, (v) => emit('update:modelName', v))
watch(() => localConfig.temperature, (v) => emit('update:temperature', v))
watch(() => localConfig.apiKey, (v) => emit('update:apiKey', v))

function handleTest() {
  testing.value = true
  emit('test')
  setTimeout(() => {
    testing.value = false
  }, 2000)
}
</script>

<style scoped>
.agent-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h4 {
  margin: 0;
}

.description {
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}
</style>
