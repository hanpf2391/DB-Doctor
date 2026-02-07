<template>
  <div class="alert-rule-config">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>告警规则配置</h1>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon> 新建规则
      </el-button>
    </div>

    <!-- 规则列表 -->
    <el-card class="table-card" shadow="never">
      <el-table
        :data="rules"
        v-loading="loading"
        stripe
      >
        <el-table-column prop="displayName" label="规则名称" width="200" />
        <el-table-column prop="type" label="规则类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getRuleTypeTagType(row.type)">
              {{ getRuleTypeText(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="metricName" label="监控指标" width="180" />
        <el-table-column prop="condition" label="触发条件" width="180">
          <template #default="{ row }">
            <span>{{ row.conditionOperator }} {{ row.thresholdValue || '异常' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="severity" label="严重程度" width="120">
          <template #default="{ row }">
            <el-tag :type="getSeverityTagType(row.severity)">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              @change="handleToggleEnabled(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="coolDownMinutes" label="冷却期(分钟)" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="showEditDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadRules"
        @current-change="loadRules"
        style="margin-top: 20px; justify-content: center"
      />
    </el-card>

    <!-- 创建/编辑规则弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑告警规则' : '新建告警规则'"
      width="700px"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="如：slow-query-qps"
            :disabled="isEdit"
          />
          <span class="form-tip">唯一标识，创建后不可修改</span>
        </el-form-item>

        <el-form-item label="显示名称" prop="displayName">
          <el-input
            v-model="formData.displayName"
            placeholder="如：慢查询处理速率告警"
          />
        </el-form-item>

        <el-form-item label="规则类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择">
            <el-option label="阈值告警" value="THRESHOLD" />
            <el-option label="异常告警" value="ANOMALY" />
            <el-option label="趋势告警" value="TREND" />
          </el-select>
          <span class="form-tip">
            阈值告警：指标超过/低于阈值时触发<br>
            异常告警：指标出现异常值时触发<br>
            趋势告警：指标出现异常趋势时触发
          </span>
        </el-form-item>

        <el-form-item label="监控指标" prop="metricName">
          <el-select v-model="formData.metricName" placeholder="请选择">
            <el-option label="慢查询处理速率" value="slowQueryQps" />
            <el-option label="AI 分析平均耗时" value="aiAnalysisDurationAvg" />
            <el-option label="AI 分析最大耗时" value="aiAnalysisDurationMax" />
            <el-option label="队列积压数量" value="queueBacklog" />
            <el-option label="数据源状态" value="datasourceStatus" />
            <el-option label="JVM 内存使用率" value="jvmMemoryUsage" />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.type === 'THRESHOLD'"
          label="比较操作符"
          prop="conditionOperator"
        >
          <el-select v-model="formData.conditionOperator" placeholder="请选择">
            <el-option label=">" value=">" />
            <el-option label=">=" value=">=" />
            <el-option label="<" value="<" />
            <el-option label="<=" value="<=" />
            <el-option label="=" value="=" />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="formData.type === 'THRESHOLD' || formData.type === 'ANOMALY'"
          label="阈值"
          prop="thresholdValue"
        >
          <el-input-number
            v-model="formData.thresholdValue"
            :precision="2"
            :step="0.1"
          />
          <span class="form-tip" v-if="formData.type === 'ANOMALY'">
            异常值（如：1 表示连接失败，0 表示正常）
          </span>
        </el-form-item>

        <el-form-item label="严重程度" prop="severity">
          <el-radio-group v-model="formData.severity">
            <el-radio label="CRITICAL">严重</el-radio>
            <el-radio label="WARNING">警告</el-radio>
            <el-radio label="INFO">信息</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="冷却期" prop="coolDownMinutes">
          <el-input-number
            v-model="formData.coolDownMinutes"
            :min="1"
            :max="1440"
          />
          <span class="form-tip">同一规则在冷却期内不会重复告警（分钟）</span>
        </el-form-item>

        <el-form-item label="规则描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入规则描述"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { alertRuleApi } from '@/api/monitoring'

// 响应式数据
const loading = ref(false)
const rules = ref<any[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  displayName: '',
  type: 'THRESHOLD',
  metricName: '',
  conditionOperator: '>',
  thresholdValue: undefined as number | undefined,
  severity: 'WARNING',
  enabled: true,
  coolDownMinutes: 30,
  description: ''
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能包含小写字母、数字和连字符', trigger: 'blur' }
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择规则类型', trigger: 'change' }
  ],
  metricName: [
    { required: true, message: '请选择监控指标', trigger: 'change' }
  ],
  conditionOperator: [
    { required: true, message: '请选择比较操作符', trigger: 'change' }
  ],
  thresholdValue: [
    { required: true, message: '请输入阈值', trigger: 'blur' }
  ],
  severity: [
    { required: true, message: '请选择严重程度', trigger: 'change' }
  ],
  coolDownMinutes: [
    { required: true, message: '请输入冷却期', trigger: 'blur' }
  ]
}

// 获取规则类型标签类型
const getRuleTypeTagType = (type: string) => {
  const map: Record<string, any> = {
    'THRESHOLD': 'danger',
    'ANOMALY': 'warning',
    'TREND': 'info'
  }
  return map[type] || ''
}

// 获取规则类型文本
const getRuleTypeText = (type: string) => {
  const map: Record<string, string> = {
    'THRESHOLD': '阈值告警',
    'ANOMALY': '异常告警',
    'TREND': '趋势告警'
  }
  return map[type] || type
}

// 获取严重程度标签类型
const getSeverityTagType = (severity: string) => {
  const map: Record<string, any> = {
    'CRITICAL': 'danger',
    'WARNING': 'warning',
    'INFO': 'info'
  }
  return map[severity] || ''
}

// 加载规则列表
const loadRules = async () => {
  loading.value = true
  try {
    const res = await alertRuleApi.getAlertRules({
      page: pagination.page - 1,
      size: pagination.size
    })

    if (res.code === 'SUCCESS') {
      rules.value = res.data.list || []
      pagination.total = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载告警规则失败:', error)
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  Object.assign(formData, {
    id: undefined,
    name: '',
    displayName: '',
    type: 'THRESHOLD',
    metricName: '',
    conditionOperator: '>',
    thresholdValue: undefined,
    severity: 'WARNING',
    enabled: true,
    coolDownMinutes: 30,
    description: ''
  })
  dialogVisible.value = true
}

// 显示编辑对话框
const showEditDialog = (rule: any) => {
  isEdit.value = true
  Object.assign(formData, rule)
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    const data = {
      name: formData.name,
      displayName: formData.displayName,
      type: formData.type,
      metricName: formData.metricName,
      conditionOperator: formData.conditionOperator,
      thresholdValue: formData.thresholdValue,
      severity: formData.severity,
      enabled: formData.enabled,
      coolDownMinutes: formData.coolDownMinutes,
      description: formData.description
    }

    let res
    if (isEdit.value) {
      res = await alertRuleApi.updateAlertRule(formData.id!, data)
    } else {
      res = await alertRuleApi.createAlertRule(data)
    }

    if (res.code === 'SUCCESS') {
      ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
      dialogVisible.value = false
      loadRules()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error: any) {
    if (error !== false) {
      console.error('提交失败:', error)
      ElMessage.error(error.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

// 切换启用状态
const handleToggleEnabled = async (rule: any) => {
  try {
    const res = await alertRuleApi.toggleAlertRule(rule.id, rule.enabled)
    if (res.code === 'SUCCESS') {
      ElMessage.success(rule.enabled ? '启用成功' : '禁用成功')
    } else {
      rule.enabled = !rule.enabled
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error: any) {
    rule.enabled = !rule.enabled
    console.error('切换状态失败:', error)
    ElMessage.error(error.message || '操作失败')
  }
}

// 删除规则
const handleDelete = async (rule: any) => {
  try {
    await ElMessageBox.confirm(
      `确认删除告警规则 "${rule.displayName}"？`,
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )

    const res = await alertRuleApi.deleteAlertRule(rule.id)
    if (res.code === 'SUCCESS') {
      ElMessage.success('删除成功')
      loadRules()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped lang="scss">
.alert-rule-config {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }
  }

  .table-card {
    .form-tip {
      display: block;
      margin-top: 4px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
      line-height: 1.5;
    }
  }
}
</style>
