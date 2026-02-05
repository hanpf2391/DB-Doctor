<template>
  <div class="database-instances-page">
    <PageHeader
      title="数据库实例管理"
      subtitle="统一管理 MySQL 数据库连接，测试通过后即可用于监控"
      :icon="Coin"
      :breadcrumb="[
        { title: '设置中心' },
        { title: '数据库实例' }
      ]"
    >
      <template #extra>
        <el-button type="primary" size="large" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增数据库实例
        </el-button>
      </template>
    </PageHeader>

    <!-- 操作栏 -->
    <PageCard class="action-bar">
      <el-row :gutter="16" align="middle">
        <el-col :span="12">
          <div class="stats-info">
            <span class="stat-item">
              共 <strong>{{ instances.length }}</strong> 个实例
            </span>
            <span class="stat-divider">|</span>
            <span class="stat-item">
              <StatusBadge type="success" :text="`${enabledCount} 个在线`" />
            </span>
          </div>
        </el-col>
        <el-col :span="12">
          <el-input
            v-model="searchText"
            placeholder="搜索实例名称、地址..."
            prefix-icon="Search"
            clearable
          >
            <template #append>
              <el-button :icon="Search" />
            </template>
          </el-input>
        </el-col>
      </el-row>
    </PageCard>

    <!-- 实例卡片网格 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <div v-else-if="filteredInstances.length === 0" class="empty-container">
      <el-empty
        :description="searchText ? '未找到匹配的实例' : '暂无数据库实例'"
      >
        <el-button v-if="!searchText" type="primary" @click="openCreateDialog">
          创建第一个实例
        </el-button>
      </el-empty>
    </div>

    <el-row v-else :gutter="20" class="instances-grid">
      <el-col
        v-for="instance in filteredInstances"
        :key="instance.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <DatabaseInstanceCard
          :instance="instance"
          @edit="handleEdit"
          @delete="handleDelete"
          @toggle="handleToggle"
        />
      </el-col>
    </el-row>

    <!-- 新增/编辑对话框 -->
    <DatabaseInstanceDialog
      v-model:visible="dialogVisible"
      :instance="editingInstance"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Coin } from '@element-plus/icons-vue'
import PageHeader from '@/components/base/PageHeader.vue'
import PageCard from '@/components/base/PageCard.vue'
import StatusBadge from '@/components/base/StatusBadge.vue'
import DatabaseInstanceCard from '@/components/instances/DatabaseInstanceCard.vue'
import DatabaseInstanceDialog from '@/components/instances/DatabaseInstanceDialog.vue'
import type { DatabaseInstance } from '@/types/instances'

// 状态
const loading = ref(false)
const instances = ref<DatabaseInstance[]>([])
const searchText = ref('')
const dialogVisible = ref(false)
const editingInstance = ref<DatabaseInstance | null>(null)

// 计算属性
const enabledCount = computed(() => {
  return instances.value.filter(i => i.isEnabled).length
})

const filteredInstances = computed(() => {
  if (!searchText.value) {
    return instances.value
  }

  const search = searchText.value.toLowerCase()
  return instances.value.filter(instance => {
    return (
      instance.name.toLowerCase().includes(search) ||
      instance.host.toLowerCase().includes(search) ||
      instance.username.toLowerCase().includes(search)
    )
  })
})

// 打开创建对话框
function openCreateDialog() {
  editingInstance.value = null
  dialogVisible.value = true
}

// 编辑实例
function handleEdit(instance: DatabaseInstance) {
  editingInstance.value = instance
  dialogVisible.value = true
}

// 删除实例
async function handleDelete(instance: DatabaseInstance) {
  try {
    await ElMessageBox.confirm(
      `确定要删除数据库实例 "${instance.name}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
    )

    // TODO: 调用删除 API
    loading.value = true
    await new Promise(resolve => setTimeout(resolve, 500)) // 模拟 API 调用

    instances.value = instances.value.filter(i => i.id !== instance.id)
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  } finally {
    loading.value = false
  }
}

// 切换启用状态
async function handleToggle(instance: DatabaseInstance, value: boolean) {
  try {
    // 添加 _toggling 状态
    instance._toggling = true

    // TODO: 调用切换 API
    await new Promise(resolve => setTimeout(resolve, 500)) // 模拟 API 调用

    instance.isEnabled = value
    ElMessage.success(value ? '已启用' : '已禁用')
  } catch (error: any) {
    ElMessage.error('操作失败：' + error.message)
    // 恢复原状态
    instance.isEnabled = !value
  } finally {
    instance._toggling = false
  }
}

// 保存成功回调
function handleSaved() {
  // 重新加载数据
  loadInstances()
}

// 加载实例列表
async function loadInstances() {
  loading.value = true
  try {
    // TODO: 调用 API
    await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟 API 调用

    // 模拟数据
    instances.value = [
      {
        id: '1',
        name: '生产环境主库',
        host: '192.168.1.100',
        port: 3306,
        username: 'root',
        password: '******',
        database: 'information_schema',
        environment: 'production',
        isEnabled: true,
        isDefault: true,
        isValid: true,
        lastValidatedAt: new Date(Date.now() - 1000 * 60 * 5) // 5分钟前
      },
      {
        id: '2',
        name: '测试环境库',
        host: '192.168.1.101',
        port: 3306,
        username: 'admin',
        password: '******',
        database: 'information_schema',
        environment: 'testing',
        isEnabled: true,
        isDefault: false,
        isValid: true,
        lastValidatedAt: new Date(Date.now() - 1000 * 60 * 60) // 1小时前
      },
      {
        id: '3',
        name: '开发环境库',
        host: 'localhost',
        port: 3306,
        username: 'dev',
        password: '******',
        database: 'information_schema',
        environment: 'development',
        isEnabled: false,
        isDefault: false,
        isValid: false
      }
    ]
  } catch (error: any) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadInstances()
})
</script>

<style scoped>
.database-instances-page {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.action-bar {
  margin-bottom: var(--spacing-md);
  background: var(--color-bg-page);
  border: 1px solid var(--color-border);
}

.stats-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  font-size: 14px;
  color: var(--color-text-secondary);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.stat-item strong {
  color: var(--color-text-primary);
  font-size: 15px;
}

.stat-divider {
  color: var(--color-border);
}

.instances-grid {
  margin-top: var(--spacing-md);
}

.loading-container,
.empty-container {
  padding: var(--spacing-2xl);
  text-align: center;
}
</style>
