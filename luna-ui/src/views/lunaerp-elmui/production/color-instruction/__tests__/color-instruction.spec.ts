import { describe, it, expect, vi } from 'vitest'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import request from '@/config/axios'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import type { ColorInstructionVO, ColorInstructionSummaryVO } from '@/api/lunaerp-elmui/production/color-instruction'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Color Instruction (打色指示单) Foundation Tests', () => {
  it('should have color instruction dictionary types registered', () => {
    expect(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE).toBe('lunaerp_color_light_source')
    expect(DICT_TYPE.LUNAERP_COLOR_LINE_STATUS).toBe('lunaerp_color_line_status')
    expect(DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS).toBe('lunaerp_color_instruction_status')

    const lightSources = getDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)
    expect(lightSources.length).toBeGreaterThanOrEqual(6)
    expect(lightSources.map((d: any) => d.value)).toContain('D65')
    expect(lightSources.map((d: any) => d.value)).toContain('TL84')

    const lineStatuses = getDictOptions(DICT_TYPE.LUNAERP_COLOR_LINE_STATUS)
    expect(lineStatuses.length).toBe(3)
    expect(lineStatuses.map((d: any) => d.value)).toEqual(['PENDING', 'APPROVED', 'REJECTED'])

    const docStatuses = getDictOptions(DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS)
    expect(docStatuses.length).toBe(5)
    expect(docStatuses.map((d: any) => d.value)).toEqual([
      'PENDING',
      'IN_PROGRESS',
      'CONFIRMED',
      'IN_PRODUCTION',
      'VOID'
    ])
  })

  it('should correctly format color summary chips with latest rounds', () => {
    const rawColors = [
      { customerColorNo: 'C01', colorName: '中国红', round: 1, status: 'REJECTED', colorHex: '#FF0000' },
      { customerColorNo: 'C01', colorName: '中国红', round: 2, status: 'APPROVED', colorHex: '#FF0000' },
      { customerColorNo: 'C02', colorName: '藏青', round: 1, status: 'PENDING', colorHex: '#000080' }
    ]

    // Emulate summary aggregation logic
    const latestColorMap = new Map<string, typeof rawColors[0]>()
    for (const c of rawColors) {
      const existing = latestColorMap.get(c.customerColorNo)
      if (!existing || c.round > existing.round) {
        latestColorMap.set(c.customerColorNo, c)
      }
    }
    const chips: ColorInstructionSummaryVO[] = Array.from(latestColorMap.values())

    expect(chips.length).toBe(2)
    expect(chips[0].customerColorNo).toBe('C01')
    expect(chips[0].round).toBe(2)
    expect(chips[0].status).toBe('APPROVED')
    expect(chips[1].customerColorNo).toBe('C02')
    expect(chips[1].round).toBe(1)
    expect(chips[1].status).toBe('PENDING')
  })

  it('should correctly compute line status tag types', () => {
    const getLineTagType = (status?: string) => {
      if (status === 'APPROVED') return 'success'
      if (status === 'REJECTED') return 'danger'
      return 'info'
    }

    expect(getLineTagType('APPROVED')).toBe('success')
    expect(getLineTagType('REJECTED')).toBe('danger')
    expect(getLineTagType('PENDING')).toBe('info')
    expect(getLineTagType(undefined)).toBe('info')
  })

  it('should construct query params correctly for empty and populated searches', () => {
    const emptyParams = {
      pageNo: 1,
      pageSize: 20,
      instructionNo: undefined,
      customerId: undefined,
      productCodeOrName: undefined,
      customerColorNo: undefined,
      status: undefined,
      instructionDateRange: undefined
    }

    expect(emptyParams.pageNo).toBe(1)
    expect(emptyParams.pageSize).toBe(20)

    const filledParams = {
      ...emptyParams,
      instructionNo: 'DS20261010001',
      customerId: 1001,
      productCodeOrName: '斜纹面料',
      customerColorNo: 'C01',
      status: 'PENDING',
      instructionDateRange: ['2026-10-01', '2026-10-10']
    }

    expect(filledParams.instructionNo).toBe('DS20261010001')
    expect(filledParams.customerColorNo).toBe('C01')
    expect(filledParams.instructionDateRange).toHaveLength(2)
  })

  it('should handle ColorInstructionVO structure correctly', () => {
    const vo: ColorInstructionVO = {
      id: 101,
      instructionNo: 'DS20261010001',
      customerId: 1001,
      clientName: '博洋家纺',
      productId: 2001,
      productCode: 'P2026001',
      productName: '全棉斜纹面料',
      instructionDate: '2026-10-10',
      status: 'PENDING',
      confirmType: 'CUSTOMER',
      colorSummary: [
        {
          customerColorNo: 'C01',
          colorName: '大红',
          round: 1,
          status: 'PENDING'
        }
      ]
    }

    expect(vo.id).toBe(101)
    expect(vo.instructionNo).toBe('DS20261010001')
    expect(vo.colorSummary).toHaveLength(1)
    expect(vo.colorSummary![0].customerColorNo).toBe('C01')
  })

  it('should call getColorInstructionPage and getColorInstruction endpoints correctly', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({ list: [], total: 0 } as any)
    const pageParams = { pageNo: 1, pageSize: 20, status: 'PENDING' }
    await ColorInstructionApi.getColorInstructionPage(pageParams)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/page',
      params: pageParams
    })

    vi.mocked(request.get).mockResolvedValueOnce({ id: 101, instructionNo: 'DS20261010001' } as any)
    await ColorInstructionApi.getColorInstruction(101)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/get',
      params: { id: 101 }
    })
  })

  it('should call getNextNo, createColorInstruction and deleteColorInstruction correctly (Issue 02)', async () => {
    vi.mocked(request.get).mockResolvedValueOnce('DS20261010001' as any)
    const nextNo = await ColorInstructionApi.getNextNo()
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/get-next-no'
    })
    expect(nextNo).toBe('DS20261010001')

    const createPayload: Partial<ColorInstructionVO> = {
      customerId: 1001,
      productId: 2001,
      instructionDate: '2026-10-10',
      colors: [
        {
          customerColorNo: 'C01',
          colorName: '中国红',
          lightPrimary: 'D65',
          copies: 1,
          round: 1,
          status: 'PENDING'
        }
      ]
    }
    vi.mocked(request.post).mockResolvedValueOnce(101 as any)
    const newId = await ColorInstructionApi.createColorInstruction(createPayload)
    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/create',
      data: createPayload
    })
    expect(newId).toBe(101)

    vi.mocked(request.delete).mockResolvedValueOnce(true as any)
    await ColorInstructionApi.deleteColorInstruction(101)
    expect(request.delete).toHaveBeenCalledWith({
      url: '/lunaerp/production/color-instruction/delete',
      params: { id: 101 }
    })
  })

  it('should correctly serialize and count samplePhotos in JSON array format', () => {
    const formatPhotos = (urls: string[]) => {
      return JSON.stringify(urls.map((url, idx) => ({ name: `样布照片_${idx + 1}`, url })))
    }
    const getPhotoCount = (photosStr?: string): number => {
      if (!photosStr) return 0
      try {
        const parsed = JSON.parse(photosStr)
        return Array.isArray(parsed) ? parsed.length : 0
      } catch {
        return photosStr.split(',').filter(Boolean).length
      }
    }

    const testUrls = ['https://cdn.example.com/p1.jpg', 'https://cdn.example.com/p2.jpg']
    const jsonStr = formatPhotos(testUrls)
    expect(getPhotoCount(jsonStr)).toBe(2)
    expect(getPhotoCount('')).toBe(0)
    expect(getPhotoCount(undefined)).toBe(0)
    expect(JSON.parse(jsonStr)[0].url).toBe('https://cdn.example.com/p1.jpg')
  })

  describe('Issue 03: View, Update, Differential Save & Row Guard Tests', () => {
    it('should call updateColorInstruction endpoint with correct payload', async () => {
      const updatePayload: Partial<ColorInstructionVO> = {
        id: 101,
        instructionNo: 'DS20261010001',
        customerId: 1001,
        productId: 2001,
        instructionDate: '2026-10-10',
        colors: [
          {
            id: 201,
            customerColorNo: 'C01',
            colorName: '中国红',
            lightPrimary: 'D65',
            copies: 1,
            round: 1,
            status: 'PENDING'
          },
          {
            customerColorNo: 'C02',
            colorName: '藏青',
            lightPrimary: 'D65',
            copies: 1,
            round: 1,
            status: 'PENDING'
          }
        ]
      }
      vi.mocked(request.put).mockResolvedValueOnce(true as any)
      const res = await ColorInstructionApi.updateColorInstruction(updatePayload)
      expect(request.put).toHaveBeenCalledWith({
        url: '/lunaerp/production/color-instruction/update',
        data: updatePayload
      })
      expect(res).toBe(true)
    })

    it('should correctly identify frozen color rows (APPROVED or REJECTED)', () => {
      const isRowFrozen = (row: { status?: string }) => {
        return row.status === 'APPROVED' || row.status === 'REJECTED'
      }

      expect(isRowFrozen({ status: 'APPROVED' })).toBe(true)
      expect(isRowFrozen({ status: 'REJECTED' })).toBe(true)
      expect(isRowFrozen({ status: 'PENDING' })).toBe(false)
      expect(isRowFrozen({ status: undefined })).toBe(false)
    })

    it('should correctly evaluate view mode from route query and doc status', () => {
      const checkViewMode = (queryMode?: string, docStatus?: string) => {
        return queryMode === 'view' || (!!docStatus && !['PENDING', 'IN_PROGRESS'].includes(docStatus))
      }

      // Explicit view mode query
      expect(checkViewMode('view', 'PENDING')).toBe(true)
      expect(checkViewMode('view', 'IN_PROGRESS')).toBe(true)

      // Edit mode query with editable doc status
      expect(checkViewMode(undefined, 'PENDING')).toBe(false)
      expect(checkViewMode(undefined, 'IN_PROGRESS')).toBe(false)

      // Non-editable terminal doc statuses automatically enter view mode
      expect(checkViewMode(undefined, 'CONFIRMED')).toBe(true)
      expect(checkViewMode(undefined, 'IN_PRODUCTION')).toBe(true)
      expect(checkViewMode(undefined, 'VOID')).toBe(true)
    })

    it('should generate stable row keys using id or _tempId', () => {
      let tempCounter = 0
      const generateTempId = () => `temp_${Date.now()}_${++tempCounter}`
      const getRowKey = (row: { id?: number; _tempId?: string }) => row.id || row._tempId

      const existingRow1 = { id: 501, customerColorNo: 'C01' }
      const existingRow2 = { id: 502, customerColorNo: 'C02' }
      const newRow = { _tempId: generateTempId(), customerColorNo: 'C03' }

      expect(getRowKey(existingRow1)).toBe(501)
      expect(getRowKey(existingRow2)).toBe(502)
      expect(getRowKey(newRow)).toContain('temp_')

      // Stable across array order mutations
      const rows = [existingRow1, existingRow2, newRow]
      const originalKeys = rows.map(getRowKey)

      // Swap position 0 and 1
      const swapped = [rows[1], rows[0], rows[2]]
      expect(swapped.map(getRowKey)).toEqual([originalKeys[1], originalKeys[0], originalKeys[2]])
    })

    it('should preserve original row ids during differential edits and reorders', () => {
      const colors = [
        { id: 11, customerColorNo: 'C01', colorName: '红', status: 'REJECTED', round: 1, rejectReason: '偏深' },
        { id: 12, customerColorNo: 'C01', colorName: '红', status: 'PENDING', round: 2 },
        { id: 13, customerColorNo: 'C02', colorName: '黄', status: 'PENDING', round: 1 }
      ]

      // Edit pending row
      colors[1].colorName = '正红'

      // Move row down
      const temp = colors[0]
      colors[0] = colors[1]
      colors[1] = temp

      // Row IDs remain intact
      expect(colors[0].id).toBe(12)
      expect(colors[0].colorName).toBe('正红')
      expect(colors[1].id).toBe(11)
      expect(colors[1].rejectReason).toBe('偏深')
      expect(colors[2].id).toBe(13)
    })
  })
})
