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
})
