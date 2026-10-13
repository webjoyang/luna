import { ref } from 'vue'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import type { ColorInstructionVO } from '@/api/lunaerp-elmui/production/color-instruction'

export interface UseColorInstructionSelectOptions {
  /**
   * 变更选中时的回调（传回选中打色单对象，若清空传 undefined）
   */
  onSelected?: (item?: ColorInstructionVO) => void
}

/**
 * 下游单据（胚布单、染色单）远程选择打色指示单的通用 Composable
 *
 * 职责：
 * 1. 维护打色单选项列表与加载状态
 * 2. 状态严格限定 CONFIRMED 与 IN_PRODUCTION 态打色单
 * 3. 支持 keyword 统一模糊搜索
 * 4. 选中 / 清空事件分发与单号快照联动
 * 5. 编辑/回显时异步拉取打色单完整详情（含客户/产品/颜色行摘要），避免只展示孤立单号
 */
export function useColorInstructionSelect(options?: UseColorInstructionSelectOptions) {
  const colorInstructionLoading = ref(false)
  const colorInstructionOptions = ref<ColorInstructionVO[]>([])

  const formatColorInstructionLabel = ColorInstructionApi.formatColorInstructionOptionLabel

  /**
   * 远程搜索打色指示单（限定 CONFIRMED / IN_PRODUCTION）
   */
  const searchColorInstructions = async (query: string = '') => {
    colorInstructionLoading.value = true
    try {
      const res = await ColorInstructionApi.getColorInstructionPage({
        pageNo: 1,
        pageSize: 50,
        keyword: query?.trim() || undefined,
        statusList: ['CONFIRMED', 'IN_PRODUCTION']
      })
      colorInstructionOptions.value = res?.list || []
    } catch {
      colorInstructionOptions.value = []
    } finally {
      colorInstructionLoading.value = false
    }
  }

  /**
   * 处理选择器选项变更
   */
  const handleColorInstructionChange = (id?: number) => {
    const item = id != null ? colorInstructionOptions.value.find((c) => c.id === id) : undefined
    if (options?.onSelected) {
      options.onSelected(item)
    }
    return item
  }

  /**
   * 预加载单条打色指示单详情用于编辑/详情回显
   * 若获取详情成功则将详情注入选项列表，使得 label 可以展示完整【单号 | 客户 | 品名 | 颜色行摘要】
   * 若失败降级保留单号快照兜底
   */
  const preloadColorInstruction = async (id?: number, snapshotNo?: string | null) => {
    if (!id) {
      return undefined
    }
    try {
      const detail = await ColorInstructionApi.getColorInstruction(id)
      if (detail && detail.id) {
        colorInstructionOptions.value = [detail]
        return detail
      }
    } catch {
      // 容错降级
    }
    const fallback: ColorInstructionVO = {
      id,
      instructionNo: snapshotNo || ''
    }
    colorInstructionOptions.value = [fallback]
    return fallback
  }

  return {
    colorInstructionLoading,
    colorInstructionOptions,
    formatColorInstructionLabel,
    searchColorInstructions,
    handleColorInstructionChange,
    preloadColorInstruction
  }
}
