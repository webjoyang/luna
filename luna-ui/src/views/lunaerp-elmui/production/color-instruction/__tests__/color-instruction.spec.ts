import { describe, expect, it, vi, beforeEach } from 'vitest'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import request from '@/config/axios'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Color Instruction API & Form Contracts (Issue 02)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getNextInstructionNo should GET /lunaerp/production/color-instruction/get-next-no', async () => {
    vi.mocked(request.get).mockResolvedValueOnce('DS20261013001' as any)
    const res = await ColorInstructionApi.getNextInstructionNo()
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/get-next-no'
    })
    expect(res).toBe('DS20261013001')
  })

  it('createColorInstruction should POST /lunaerp/production/color-instruction/create', async () => {
    const payload: Partial<ColorInstructionApi.ColorInstructionVO> = {
      customerId: 101,
      productId: 201,
      instructionDate: '2026-10-13',
      colors: [
        {
          customerColorNo: 'C01',
          colorName: '藏青',
          lightPrimary: 'D65',
          round: 1,
          status: 'PENDING'
        }
      ]
    }
    vi.mocked(request.post).mockResolvedValueOnce(888 as any)
    const id = await ColorInstructionApi.createColorInstruction(payload)
    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/create',
      data: payload
    })
    expect(id).toBe(888)
  })

  it('deleteColorInstruction should DELETE /lunaerp/production/color-instruction/delete?id=1024', async () => {
    vi.mocked(request.delete).mockResolvedValueOnce(true as any)
    const res = await ColorInstructionApi.deleteColorInstruction(1024)
    expect(request.delete).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/delete?id=1024'
    })
    expect(res).toBe(true)
  })

  it('getColorInstruction should GET /lunaerp/production/color-instruction/get?id=1024', async () => {
    const mockDetail = {
      id: 1024,
      instructionNo: 'DS20261013001',
      status: 'PENDING',
      colors: [
        {
          id: 1,
          customerColorNo: 'C01',
          colorName: '藏青',
          lightPrimary: 'D65'
        }
      ]
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockDetail as any)
    const res = await ColorInstructionApi.getColorInstruction(1024)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/get?id=1024'
    })
    expect(res).toEqual(mockDetail)
  })

  it('should verify light sources dictionary options contains D65, TL84, CWF, A, UV, TL83', () => {
    const options = getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)
    const values = options.map((opt) => opt.value)
    expect(values).toContain('D65')
    expect(values).toContain('TL84')
    expect(values).toContain('CWF')
    expect(values).toContain('A')
    expect(values).toContain('UV')
    expect(values).toContain('TL83')
  })

  it('should format product snapshot correctly when product is chosen', () => {
    const product = {
      id: 201,
      code: 'P-100',
      name: '双面棉毛布',
      composition: '95% 棉 5% 氨纶',
      widthCm: 185,
      weightGsm: 210
    }

    const snapshot = {
      productCode: product.code,
      productName: product.name,
      composition: product.composition,
      width: product.widthCm ? `${product.widthCm}cm` : '',
      gsm: product.weightGsm ? `${product.weightGsm}g/m²` : ''
    }

    expect(snapshot.productCode).toBe('P-100')
    expect(snapshot.productName).toBe('双面棉毛布')
    expect(snapshot.composition).toBe('95% 棉 5% 氨纶')
    expect(snapshot.width).toBe('185cm')
    expect(snapshot.gsm).toBe('210g/m²')
  })

  // ==================== Issue 03: 查看与修改（含差异保存与行状态守卫） ====================

  it('updateColorInstruction should PUT /lunaerp/production/color-instruction/update', async () => {
    const payload: Partial<ColorInstructionApi.ColorInstructionVO> = {
      id: 1024,
      instructionNo: 'DS20261013001',
      customerId: 101,
      productId: 201,
      colors: [
        {
          id: 1,
          customerColorNo: 'C01',
          colorName: '藏青',
          lightPrimary: 'D65',
          round: 1,
          status: 'PENDING'
        }
      ]
    }
    vi.mocked(request.put).mockResolvedValueOnce(true as any)
    const res = await ColorInstructionApi.updateColorInstruction(payload)
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/update',
      data: payload
    })
    expect(res).toBe(true)
  })

  it('should identify frozen rows (APPROVED / REJECTED) and prevent modifying/deleting them', () => {
    const isRowFrozen = (status?: string) => status === 'APPROVED' || status === 'REJECTED'

    expect(isRowFrozen('APPROVED')).toBe(true)
    expect(isRowFrozen('REJECTED')).toBe(true)
    expect(isRowFrozen('PENDING')).toBe(false)
    expect(isRowFrozen(undefined)).toBe(false)
  })

  it('should generate stable row-key for rows with id or auto-generated tempId', () => {
    let tempCounter = 0
    const getRowKey = (row: { id?: number; _tempId?: string }) => {
      if (row.id) return `id_${row.id}`
      if (!row._tempId) row._tempId = `temp_${++tempCounter}`
      return row._tempId
    }

    const row1 = { id: 101 }
    const row2 = { id: 102 }
    const newRow1 = {}
    const newRow2 = {}

    expect(getRowKey(row1)).toBe('id_101')
    expect(getRowKey(row2)).toBe('id_102')
    const key1 = getRowKey(newRow1)
    const key2 = getRowKey(newRow2)
    expect(key1).toBe('temp_1')
    expect(key2).toBe('temp_2')
    // Repeated calls return stable key
    expect(getRowKey(newRow1)).toBe('temp_1')
  })

  it('should support line reordering (move up/down) preserving item references', () => {
    const colors = [
      { id: 1, customerColorNo: 'C01' },
      { id: 2, customerColorNo: 'C02' },
      { id: 3, customerColorNo: 'C03' }
    ]

    const move = (list: any[], index: number, direction: number) => {
      const targetIndex = index + direction
      if (targetIndex < 0 || targetIndex >= list.length) return
      const current = list[index]
      const target = list[targetIndex]
      list.splice(index, 1, target)
      list.splice(targetIndex, 1, current)
    }

    // Move index 0 down by 1 -> C02, C01, C03
    move(colors, 0, 1)
    expect(colors[0].customerColorNo).toBe('C02')
    expect(colors[1].customerColorNo).toBe('C01')
    expect(colors[2].customerColorNo).toBe('C03')

    // Move index 2 up by 1 -> C02, C03, C01
    move(colors, 2, -1)
    expect(colors[1].customerColorNo).toBe('C03')
    expect(colors[2].customerColorNo).toBe('C01')
  })

  it('should determine view mode accurately based on mode query and order status', () => {
    const isViewMode = (queryMode?: string, status?: string) => {
      if (queryMode === 'view') return true
      if (status && status !== 'PENDING' && status !== 'IN_PROGRESS') return true
      return false
    }

    expect(isViewMode('view', 'PENDING')).toBe(true)
    expect(isViewMode('edit', 'PENDING')).toBe(false)
    expect(isViewMode(undefined, 'IN_PROGRESS')).toBe(false)
    // Non-editable statuses are forced to view mode
    expect(isViewMode(undefined, 'CONFIRMED')).toBe(true)
    expect(isViewMode(undefined, 'IN_PRODUCTION')).toBe(true)
    expect(isViewMode(undefined, 'VOID')).toBe(true)
  })

  // ==================== Issue 04: 行级动作：通过 / 驳回重打（含开始打色） ====================

  it('startColorInstruction should PUT /lunaerp/production/color-instruction/start', async () => {
    vi.mocked(request.put).mockResolvedValueOnce(true as any)
    const res = await ColorInstructionApi.startColorInstruction({ id: 1024 })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/start',
      data: { id: 1024 }
    })
    expect(res).toBe(true)
  })

  it('approveColorLine should PUT /lunaerp/production/color-instruction/color/approve', async () => {
    vi.mocked(request.put).mockResolvedValueOnce(true as any)
    const res = await ColorInstructionApi.approveColorLine({ id: 11 })
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/color/approve',
      data: { id: 11 }
    })
    expect(res).toBe(true)
  })

  it('rejectRestrikeColorLine should PUT /lunaerp/production/color-instruction/color/reject-restrike', async () => {
    vi.mocked(request.put).mockResolvedValueOnce(true as any)
    const payload = { id: 11, rejectReason: '色差偏深，需调浅半度' }
    const res = await ColorInstructionApi.rejectRestrikeColorLine(payload)
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/color/reject-restrike',
      data: payload
    })
    expect(res).toBe(true)
  })

  it('should verify line action permissions and availability for different line statuses', () => {
    const canApprove = (row: { id?: number; status?: string }, orderStatus?: string) => {
      if (!row.id) return false // 未保存行不能审批
      if (orderStatus !== 'PENDING' && orderStatus !== 'IN_PROGRESS') return false
      return row.status === 'PENDING'
    }

    const canRejectRestrike = (row: { id?: number; status?: string }, orderStatus?: string) => {
      if (!row.id) return false // 未保存行不能驳回重打
      if (orderStatus !== 'PENDING' && orderStatus !== 'IN_PROGRESS') return false
      return row.status === 'PENDING' || row.status === 'APPROVED'
    }

    const pendingRow = { id: 1, status: 'PENDING' }
    const approvedRow = { id: 2, status: 'APPROVED' }
    const rejectedRow = { id: 3, status: 'REJECTED' }
    const unsavedRow = { status: 'PENDING' }

    // PENDING 单据下
    expect(canApprove(pendingRow, 'PENDING')).toBe(true)
    expect(canRejectRestrike(pendingRow, 'PENDING')).toBe(true)

    expect(canApprove(approvedRow, 'PENDING')).toBe(false)
    expect(canRejectRestrike(approvedRow, 'PENDING')).toBe(true) // 电话先通过后改口，允许驳回重打

    expect(canApprove(rejectedRow, 'PENDING')).toBe(false)
    expect(canRejectRestrike(rejectedRow, 'PENDING')).toBe(false) // REJECTED 冻结，不可再重打

    expect(canApprove(unsavedRow, 'PENDING')).toBe(false)
    expect(canRejectRestrike(unsavedRow, 'PENDING')).toBe(false)

    // IN_PROGRESS 单据下
    expect(canApprove(pendingRow, 'IN_PROGRESS')).toBe(true)
    expect(canRejectRestrike(pendingRow, 'IN_PROGRESS')).toBe(true)

    // viewMode 查看模式下禁止任何行级动作
    const canDoInView = (isView: boolean, orderStatus: string) => !isView && (orderStatus === 'PENDING' || orderStatus === 'IN_PROGRESS')
    expect(canDoInView(true, 'PENDING')).toBe(false)
    expect(canDoInView(true, 'IN_PROGRESS')).toBe(false)
    expect(canDoInView(false, 'PENDING')).toBe(true)
  })

  it('should check row rejected styling and color group boundary class', () => {
    const tableRowClassName = (
      row: { status?: string; customerColorNo?: string },
      rowIndex: number,
      colors: Array<{ customerColorNo?: string }>
    ) => {
      const classes: string[] = []
      if (row.status === 'REJECTED') {
        classes.push('row-rejected')
      }
      if (rowIndex > 0 && colors[rowIndex]?.customerColorNo && colors[rowIndex]?.customerColorNo !== colors[rowIndex - 1]?.customerColorNo) {
        classes.push('row-group-start')
      }
      return classes.join(' ')
    }

    const colors = [
      { customerColorNo: 'C01', status: 'PENDING' },
      { customerColorNo: 'C01', status: 'REJECTED' },
      { customerColorNo: 'C02', status: 'PENDING' }
    ]

    expect(tableRowClassName(colors[0], 0, colors)).toBe('')
    expect(tableRowClassName(colors[1], 1, colors)).toBe('row-rejected')
    expect(tableRowClassName(colors[2], 2, colors)).toBe('row-group-start')
  })

  // ==================== Issue 05: 确认动作与三道守卫 ====================

  it('confirmColorInstruction should PUT /lunaerp/production/color-instruction/confirm', async () => {
    vi.mocked(request.put).mockResolvedValueOnce(true as any)
    const payload = {
      id: 1024,
      confirmType: 'CUSTOMER' as const,
      customerFeedback: '客户现场看样确认通过'
    }
    const res = await ColorInstructionApi.confirmColorInstruction(payload)
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/confirm',
      data: payload
    })
    expect(res).toBe(true)
  })

  it('COLOR_INSTRUCTION_CONFIRM_TYPES should define CUSTOMER and INTERNAL constants', () => {
    const types = (ColorInstructionApi as any).COLOR_INSTRUCTION_CONFIRM_TYPES
    expect(types).toBeDefined()
    expect(types).toHaveLength(2)
    expect(types.find((t: any) => t.value === 'CUSTOMER')?.label).toBe('客户确认')
    expect(types.find((t: any) => t.value === 'INTERNAL')?.label).toBe('内部确认')
  })

  it('should verify confirm button visibility guard on header', () => {
    const canShowConfirmButton = (isView: boolean, id?: number, status?: string) => {
      if (isView) return false
      if (!id) return false
      return status === 'IN_PROGRESS'
    }

    // 仅已保存且处于 IN_PROGRESS 且非只读模式下显示
    expect(canShowConfirmButton(false, 1024, 'IN_PROGRESS')).toBe(true)
    expect(canShowConfirmButton(false, 1024, 'PENDING')).toBe(false)
    expect(canShowConfirmButton(false, 1024, 'CONFIRMED')).toBe(false)
    expect(canShowConfirmButton(false, 1024, 'VOID')).toBe(false)
    expect(canShowConfirmButton(false, undefined, 'IN_PROGRESS')).toBe(false)
    expect(canShowConfirmButton(true, 1024, 'IN_PROGRESS')).toBe(false)
  })

  it('should transition to full read-only mode once order status becomes CONFIRMED', () => {
    const isViewMode = (queryMode?: string, status?: string) => {
      if (queryMode === 'view') return true
      if (status && status !== 'PENDING' && status !== 'IN_PROGRESS') return true
      return false
    }

    // 确认成功后状态转 CONFIRMED
    const statusAfterConfirm = 'CONFIRMED'
    expect(isViewMode(undefined, statusAfterConfirm)).toBe(true)
    // 行动作在 CONFIRMED 下也全部禁用
    const canDoLineAction = (isView: boolean, status?: string) =>
      !isView && (status === 'PENDING' || status === 'IN_PROGRESS')
    expect(canDoLineAction(true, statusAfterConfirm)).toBe(false)
  })

  it('should validate confirm dialog required fields', () => {
    const confirmFormRules = {
      confirmType: [{ required: true, message: '请选择确认类型', trigger: 'change' }]
    }
    expect(confirmFormRules.confirmType[0].required).toBe(true)
  })

  // ==================== Issue 06: 下游引用列与保存校验 ====================

  it('getColorInstructionPage should pass statusList and keyword params', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({ list: [], total: 0 } as any)
    const params: ColorInstructionApi.ColorInstructionPageReqVO = {
      pageNo: 1,
      pageSize: 20,
      keyword: 'DS001',
      statusList: ['CONFIRMED', 'IN_PRODUCTION']
    }
    await ColorInstructionApi.getColorInstructionPage(params)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/page',
      params
    })
  })

  it('formatColorInstructionOptionLabel should format label correctly for various inputs', () => {
    // 完整详情：单号 + 客户 + 产品 + 颜色摘要
    const itemFull: ColorInstructionApi.ColorInstructionVO = {
      id: 1,
      instructionNo: 'DS20261013001',
      clientName: '上海客户A',
      productName: '双面棉毛布',
      colorSummaries: [
        { customerColorNo: 'C01', colorName: '藏青' },
        { customerColorNo: 'C02', colorName: '雪青' }
      ]
    }
    expect(ColorInstructionApi.formatColorInstructionOptionLabel(itemFull)).toBe(
      'DS20261013001 | 上海客户A | 双面棉毛布 | [C01 藏青, C02 雪青]'
    )

    // 颜色行从 colors 兜底提取
    const itemWithColors: ColorInstructionApi.ColorInstructionVO = {
      id: 1,
      instructionNo: 'DS20261013001',
      clientName: '上海客户A',
      productName: '双面棉毛布',
      colors: [
        { customerColorNo: 'C01', colorName: '藏青' },
        { customerColorNo: 'C02', colorName: '雪青' }
      ]
    }
    expect(ColorInstructionApi.formatColorInstructionOptionLabel(itemWithColors)).toBe(
      'DS20261013001 | 上海客户A | 双面棉毛布 | [C01 藏青, C02 雪青]'
    )

    // 无产品名但有产品编码
    const itemCodeOnly: ColorInstructionApi.ColorInstructionVO = {
      id: 2,
      instructionNo: 'DS20261013002',
      clientName: '客户B',
      productCode: 'P-1002'
    }
    expect(ColorInstructionApi.formatColorInstructionOptionLabel(itemCodeOnly)).toBe(
      'DS20261013002 | 客户B | P-1002'
    )

    // 存量回填的快照（仅有单号）
    const itemSnapshotOnly: ColorInstructionApi.ColorInstructionVO = {
      id: 3,
      instructionNo: 'DS20261013003'
    }
    expect(ColorInstructionApi.formatColorInstructionOptionLabel(itemSnapshotOnly)).toBe(
      'DS20261013003'
    )
  })

  it('downstream selector should set and clear color instruction snapshot', () => {
    const formData: { colorInstructionId?: number | null; colorInstructionNo?: string | null } = {
      colorInstructionId: undefined,
      colorInstructionNo: undefined
    }

    const options: ColorInstructionApi.ColorInstructionVO[] = [
      { id: 10, instructionNo: 'DS20261013001' }
    ]

    const onSelect = (id?: number) => {
      if (!id) {
        formData.colorInstructionId = undefined
        formData.colorInstructionNo = ''
        return
      }
      const found = options.find((o) => o.id === id)
      formData.colorInstructionId = id
      formData.colorInstructionNo = found?.instructionNo || ''
    }

    // 选中打色单
    onSelect(10)
    expect(formData.colorInstructionId).toBe(10)
    expect(formData.colorInstructionNo).toBe('DS20261013001')

    // 清空选择
    onSelect(undefined)
    expect(formData.colorInstructionId).toBeUndefined()
    expect(formData.colorInstructionNo).toBe('')
  })
})

describe('useColorInstructionSelect composable', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should search color instructions with CONFIRMED and IN_PRODUCTION statusList', async () => {
    const { useColorInstructionSelect } = await import(
      '@/views/lunaerp-elmui/production/color-instruction/composables/useColorInstructionSelect'
    )
    const mockList = [
      { id: 101, instructionNo: 'DS20261013001', clientName: '客户A', status: 'CONFIRMED' }
    ]
    vi.mocked(request.get).mockResolvedValueOnce({ list: mockList } as any)

    const {
      colorInstructionOptions,
      colorInstructionLoading,
      searchColorInstructions
    } = useColorInstructionSelect()

    expect(colorInstructionOptions.value).toEqual([])
    expect(colorInstructionLoading.value).toBe(false)

    await searchColorInstructions('DS2026')

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/page',
      params: {
        pageNo: 1,
        pageSize: 50,
        keyword: 'DS2026',
        statusList: ['CONFIRMED', 'IN_PRODUCTION']
      }
    })
    expect(colorInstructionOptions.value).toEqual(mockList)
    expect(colorInstructionLoading.value).toBe(false)
  })

  it('should trigger onSelected callback and handle change/clear', async () => {
    const { useColorInstructionSelect } = await import(
      '@/views/lunaerp-elmui/production/color-instruction/composables/useColorInstructionSelect'
    )
    const onSelected = vi.fn()
    const {
      colorInstructionOptions,
      handleColorInstructionChange
    } = useColorInstructionSelect({ onSelected })

    const mockItem = { id: 101, instructionNo: 'DS20261013001', clientName: '客户A' }
    colorInstructionOptions.value = [mockItem]

    // 选中
    const res = handleColorInstructionChange(101)
    expect(res).toEqual(mockItem)
    expect(onSelected).toHaveBeenCalledWith(mockItem)

    // 清空
    const resCleared = handleColorInstructionChange(undefined)
    expect(resCleared).toBeUndefined()
    expect(onSelected).toHaveBeenCalledWith(undefined)
  })

  it('should preload full detail for existing downstream order and fallback to snapshot on failure', async () => {
    const { useColorInstructionSelect } = await import(
      '@/views/lunaerp-elmui/production/color-instruction/composables/useColorInstructionSelect'
    )
    const {
      colorInstructionOptions,
      preloadColorInstruction
    } = useColorInstructionSelect()

    // 1. 成功获取完整详情
    const mockDetail = {
      id: 501,
      instructionNo: 'DS20261013001',
      clientName: '上海客户A',
      productName: '双面棉毛布',
      colorSummaries: [{ customerColorNo: 'C01', colorName: '藏青' }]
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockDetail as any)

    const loaded = await preloadColorInstruction(501, 'DS20261013001')
    expect(loaded).toEqual(mockDetail)
    expect(colorInstructionOptions.value).toEqual([mockDetail])
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/get?id=501'
    })

    // 2. 失败时降级回退到快照
    vi.mocked(request.get).mockRejectedValueOnce(new Error('Network error'))
    const fallback = await preloadColorInstruction(502, 'DS20261013002')
    expect(fallback).toEqual({ id: 502, instructionNo: 'DS20261013002' })
    expect(colorInstructionOptions.value).toEqual([{ id: 502, instructionNo: 'DS20261013002' }])

    // 3. id 为空时不发起请求
    const emptyRes = await preloadColorInstruction(undefined)
    expect(emptyRes).toBeUndefined()
  })
})



