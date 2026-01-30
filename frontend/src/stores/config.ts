import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAllConfigs, saveConfig } from '@/api/config'
import type { ConfigData } from '@/api/types'

export const useConfigStore = defineStore('config', () => {
  const config = ref<ConfigData>({})
  const loading = ref(false)

  /**
   * 加载所有配置
   */
  async function loadConfig() {
    loading.value = true
    try {
      const data = await getAllConfigs()
      config.value = data
    } catch (error) {
      console.error('加载配置失败', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 保存配置
   */
  async function saveConfigs(category: string, configs: Record<string, string>) {
    loading.value = true
    try {
      const result = await saveConfig(category, configs)

      // 重新加载配置
      await loadConfig()

      return result
    } catch (error) {
      console.error('保存配置失败', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  return {
    config,
    loading,
    loadConfig,
    saveConfigs
  }
})
