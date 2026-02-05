/**
 * 数据库实例表单组合式函数
 * 实现"先测试后保存"的核心逻辑
 */
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { DatabaseInstanceForm, DatabaseTestResult } from '@/types/instances'
import { testDatabaseConnection } from '@/api/instances'

export type TestState = 'none' | 'testing' | 'success' | 'fail'

export function useDatabaseForm(initialData?: DatabaseInstanceForm) {
  // 表单数据
  const form = reactive<DatabaseInstanceForm>({
    id: initialData?.id,
    name: initialData?.name || '',
    host: initialData?.host || 'localhost',
    port: initialData?.port || 3306,
    username: initialData?.username || '',
    password: initialData?.password || '',
    database: initialData?.database || 'information_schema',
    environment: initialData?.environment || 'development',
    isDefault: initialData?.isDefault || false
  })

  // 测试状态
  const testState = ref<TestState>('none')
  const testLatency = ref(0)
  const testError = ref('')
  const testResult = ref<DatabaseTestResult | null>(null)

  // 是否可以保存（只有环境检查全部通过才能保存）
  const canSave = computed(() => testResult.value?.success === true)

  // 是否编辑模式
  const isEdit = computed(() => !!form.id)

  // 监听表单变化，重置测试状态
  watch(
    () => [
      form.name,
      form.host,
      form.port,
      form.username,
      form.password,
      form.database
    ],
    () => {
      if (testState.value === 'success' || testState.value === 'fail') {
        testState.value = 'none'
        testError.value = ''
        testResult.value = null
      }
    },
    { deep: true }
  )

  /**
   * 测试数据库连接
   */
  async function handleTest(): Promise<boolean> {
    // 验证必填字段
    if (!form.host || !form.username || !form.password) {
      ElMessage.warning('请先填写完整的连接信息')
      return false
    }

    testState.value = 'testing'
    const startTime = Date.now()

    try {
      const response = await testDatabaseConnection({
        host: form.host,
        port: form.port,
        username: form.username,
        password: form.password,
        database: form.database
      })

      testLatency.value = Date.now() - startTime

      // 处理后端返回的数据格式
      const formattedResult: DatabaseTestResult = {
        success: response.overallPassed !== undefined ? response.overallPassed : true,
        latency: testLatency.value,
        databaseCount: response.availableDatabases?.length || 0,
        version: response.version,
        environmentChecks: response.items?.map((item: any) => ({
          name: item.name,
          displayName: getDisplayName(item.name),
          status: item.passed ? 'pass' : 'fail',
          currentValue: item.currentValue,
          description: item.errorMessage,
          fixSql: item.fixCommand,
          fixDescription: item.fixCommand ? '执行以下 SQL 语句来修复此问题' : undefined
        }))
      }

      testResult.value = formattedResult

      // 判断是否显示成功（连接成功但环境可能不通过）
      if (response.connectionSuccess) {
        if (response.overallPassed) {
          // 连接成功且环境检查全部通过
          testState.value = 'success'
          ElMessage.success(`连接成功！延迟 ${testLatency.value}ms`)
          return true
        } else {
          // 连接成功但环境检查未通过
          testState.value = 'fail'
          testError.value = '环境检查未通过，请修复以下问题后重试'
          ElMessage.warning('连接成功，但环境检查未通过')
          return false
        }
      } else {
        // 连接失败
        testState.value = 'fail'
        testError.value = response.connectionError || '连接失败'
        ElMessage.error(testError.value)
        return false
      }
    } catch (error: any) {
      testState.value = 'fail'
      testError.value = error.response?.data?.message || error.message || '连接失败'
      testResult.value = {
        success: false,
        latency: 0,
        error: testError.value
      }

      ElMessage.error(testError.value)
      return false
    }
  }

  /**
   * 获取检查项的显示名称
   */
  function getDisplayName(name: string): string {
    const displayNames: Record<string, string> = {
      'slow_query_log': '慢查询日志',
      'log_output': '日志输出方式',
      'long_query_time': '慢查询阈值',
      'mysql.slow_log': '慢查询表权限'
    }
    return displayNames[name] || name
  }

  /**
   * 重置表单
   */
  function reset() {
    Object.assign(form, {
      id: undefined,
      name: '',
      host: 'localhost',
      port: 3306,
      username: '',
      password: '',
      database: 'information_schema',
      environment: 'development' as const,
      isDefault: false
    })

    testState.value = 'none'
    testLatency.value = 0
    testError.value = ''
    testResult.value = null
  }

  return {
    form,
    testState,
    testLatency,
    testError,
    testResult,
    canSave,
    isEdit,
    handleTest,
    reset
  }
}
