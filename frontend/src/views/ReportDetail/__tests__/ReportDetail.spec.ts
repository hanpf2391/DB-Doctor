/**
 * ReportDetail 组件测试
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ReportDetail from '../ReportDetail.vue'
import { getReportDetail } from '@/api/config'
import type { ReportDetailData } from '../types'

// Mock API
vi.mock('@/api/config', () => ({
  getReportDetail: vi.fn()
}))

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn()
  }
}))

describe('ReportDetail.vue', () => {
  const mockData: ReportDetailData = {
    id: 1,
    fingerprint: 'abc123def456',
    dbName: 'test_db',
    tableName: 'users',
    sqlTemplate: 'SELECT * FROM users WHERE id = ?',
    avgQueryTime: 1.82,
    maxQueryTime: 2.5,
    lockTime: 0,
    rowsExamined: 5550,
    rowsSent: 100,
    occurrenceCount: 4,
    severityLevel: '🔴 严重',
    analysisStatus: 'SUCCESS',
    lastSeenTime: '2025-01-31 14:30:00',
    aiAnalysisReport: '# 诊断报告\n\n这是一个慢查询'
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该渲染抽屉组件', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    expect(wrapper.find('.el-drawer').exists()).toBe(true)
  })

  it('应该显示报告标题', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    expect(wrapper.text()).toContain('慢查询诊断报告 #1')
  })

  it('应该根据耗时设置危险级别 - danger', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    // @ts-ignore - 访问私有方法进行测试
    const level = wrapper.vm.getQueryTimeLevel(3.0)
    expect(level).toBe('danger')
  })

  it('应该根据耗时设置危险级别 - warning', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    // @ts-ignore
    const level = wrapper.vm.getQueryTimeLevel(1.5)
    expect(level).toBe('warning')
  })

  it('应该根据耗时设置危险级别 - success', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    // @ts-ignore
    const level = wrapper.vm.getQueryTimeLevel(0.3)
    expect(level).toBe('success')
  })

  it('应该根据扫描行数设置危险级别', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    // @ts-ignore
    const level = wrapper.vm.getRowsExaminedLevel(15000)
    expect(level).toBe('warning')
  })

  it('应该根据锁等待时间设置危险级别', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    // @ts-ignore
    const level = wrapper.vm.getLockTimeLevel(500)
    expect(level).toBe('danger')
  })

  it('应该正确生成指标卡片数据', () => {
    const wrapper = mount(ReportDetail, {
      props: {
        modelValue: true,
        reportId: 1
      }
    })

    // 设置模拟数据
    // @ts-ignore
    wrapper.vm.reportData = mockData

    // @ts-ignore
    const vitalSigns = wrapper.vm.vitalSigns
    expect(vitalSigns).toHaveLength(4)
    expect(vitalSigns[0].key).toBe('queryTime')
    expect(vitalSigns[0].label).toBe('平均耗时')
  })
})
