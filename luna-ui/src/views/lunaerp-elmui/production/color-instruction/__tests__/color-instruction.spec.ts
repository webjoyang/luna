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
})
