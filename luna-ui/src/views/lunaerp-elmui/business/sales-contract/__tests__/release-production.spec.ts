import { describe, expect, it, vi } from 'vitest'
import * as ReleaseProductionApi from '@/api/lunaerp-elmui/production/release-production'
import request from '@/config/axios'
import {
  initReleaseWorkbenchState,
  validateReleaseForm,
  calculateReleaseSummary,
  buildReleaseProductionPayload
} from '../workspace/tabs/releaseProductionHelper'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Release Production API', () => {
  it('getReleaseFromContract should call GET /lunaerp/production/release-production/from-contract', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({
      contractId: 1024,
      contractNo: 'CH20260001',
      clientName: '某某纺织',
      lines: [],
      existingColorInstructions: []
    } as any)

    await ReleaseProductionApi.getReleaseFromContract(1024)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/production/release-production/from-contract',
      params: { contractId: 1024 }
    })
  })

  it('releaseProduction should call POST /lunaerp/production/release-production/release', async () => {
    const mockPayload: ReleaseProductionApi.ReleaseProductionReqVO = {
      contractId: 1024,
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups: []
    }

    vi.mocked(request.post).mockResolvedValueOnce({
      greigeOrders: [{ id: 1, orderNo: 'PB001' }],
      dyeingOrders: [{ id: 2 }],
      inspectionOrders: [{ id: 3, code: 'INS001', type: 'GREIGE' }],
      colorInstructions: []
    } as any)

    await ReleaseProductionApi.releaseProduction(mockPayload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/production/release-production/release',
      data: mockPayload
    })
  })
})

describe('Release Production Helper Logic', () => {
  const mockContractLines: ReleaseProductionApi.ReleaseContractLineVO[] = [
    {
      contractLineId: 1,
      lineNo: 1,
      productCode: 'P001',
      productName: '纯棉汗布',
      composition: '100%棉',
      width: '180cm',
      gsm: '200g/m²',
      customerColor: 'C101',
      contractQty: 1000,
      releasedQty: 400,
      remainingQty: 600,
      defaultLossRate: 0
    },
    {
      contractLineId: 2,
      lineNo: 2,
      productCode: 'P001',
      productName: '纯棉汗布',
      composition: '100%棉',
      width: '180cm',
      gsm: '200g/m²',
      customerColor: 'C102',
      contractQty: 500,
      releasedQty: 100,
      remainingQty: 400,
      defaultLossRate: 0
    },
    {
      contractLineId: 3,
      lineNo: 3,
      productCode: 'P002',
      productName: '罗马布',
      composition: '80%棉20%涤',
      width: '160cm',
      gsm: '280g/m²',
      customerColor: 'R01',
      contractQty: 800,
      releasedQty: 0,
      remainingQty: 800,
      defaultLossRate: 0.05
    }
  ]

  it('initReleaseWorkbenchState groups lines by productCode with defaults', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)

    expect(groups).toHaveLength(2)

    // First group P001 has 2 lines
    expect(groups[0].productCode).toBe('P001')
    expect(groups[0].productName).toBe('纯棉汗布')
    expect(groups[0].colorInstructionMode).toBe('NONE')
    expect(groups[0].lines).toHaveLength(2)
    expect(groups[0].lines[0].releaseQty).toBe(600)
    expect(groups[0].lines[0].lossRate).toBe(0)
    expect(groups[0].lines[1].releaseQty).toBe(400)

    // Second group P002 has 1 line
    expect(groups[1].productCode).toBe('P002')
    expect(groups[1].lines).toHaveLength(1)
    expect(groups[1].lines[0].releaseQty).toBe(800)
    expect(groups[1].lines[0].lossRate).toBe(0.05)
  })

  it('calculateReleaseSummary calculates quantities and order counts accurately', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)
    // Make group 0 use EXISTING, group 1 use NEW
    groups[0].colorInstructionMode = 'EXISTING'
    groups[0].existingColorInstructionId = 99
    groups[1].colorInstructionMode = 'NEW'

    const summary = calculateReleaseSummary(groups)

    // Total releaseQty = 600 + 400 + 800 = 1800
    expect(summary.totalGreigeQty).toBe(1800)
    expect(summary.totalDyeingQty).toBe(1800)
    expect(summary.greigeOrderCount).toBe(2)
    expect(summary.dyeingOrderCount).toBe(2)
    expect(summary.inspectionOrderCount).toBe(2)
    expect(summary.colorInstructionCount).toBe(1)
  })

  it('validateReleaseForm detects errors properly', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)

    // Valid case
    let validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(true)
    expect(validation.errors).toHaveLength(0)

    // 1. CANCELLED contract
    validation = validateReleaseForm({
      contractStatus: 'CANCELLED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors).toContain('合同已取消，无法下达生产')

    // 2. Missing factories
    validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: undefined,
      dyeingFactoryId: undefined,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors).toContain('请选择承织坯布厂')
    expect(validation.errors).toContain('请选择承染方')

    // 3. releaseQty > remainingQty
    groups[0].lines[0].releaseQty = 700 // remaining is 600
    validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors.some((e) => e.includes('超出剩余可下达量'))).toBe(true)

    // Reset line 0
    groups[0].lines[0].releaseQty = 600

    // 4. EXISTING mode without existingColorInstructionId
    groups[0].colorInstructionMode = 'EXISTING'
    groups[0].existingColorInstructionId = undefined
    validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors.some((e) => e.includes('请选择已有打色单'))).toBe(true)

    // 5. NEW mode without lightPrimary
    groups[0].colorInstructionMode = 'NONE'
    groups[1].colorInstructionMode = 'NEW'
    groups[1].newColorInstruction = {
      labFactoryId: null,
      colors: [
        {
          contractLineId: 3,
          customerColorNo: 'R01',
          colorName: 'R01',
          lightPrimary: '' // missing
        }
      ]
    }
    validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors.some((e) => e.includes('主灯源不能为空'))).toBe(true)
  })

  it('buildReleaseProductionPayload generates correct ReleaseProductionReqVO', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)
    groups[0].colorInstructionMode = 'EXISTING'
    groups[0].existingColorInstructionId = 88
    groups[1].colorInstructionMode = 'NEW'
    groups[1].newColorInstruction = {
      labFactoryId: 999,
      colors: [
        {
          contractLineId: 3,
          customerColorNo: 'R01',
          colorName: '罗马红',
          lightPrimary: 'D65',
          lightSecondary: 'TL84',
          factoryColorNo: 'F10',
          colorCardRef: 'PANTONE-01'
        }
      ]
    }

    const payload = buildReleaseProductionPayload({
      contractId: 1024,
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })

    expect(payload.contractId).toBe(1024)
    expect(payload.greigeFactoryId).toBe(10)
    expect(payload.dyeingFactoryId).toBe(20)
    expect(payload.groups).toHaveLength(2)

    // Check group 0
    expect(payload.groups[0].productCode).toBe('P001')
    expect(payload.groups[0].colorInstructionMode).toBe('EXISTING')
    expect(payload.groups[0].existingColorInstructionId).toBe(88)
    expect(payload.groups[0].lines).toHaveLength(2)
    expect(payload.groups[0].lines[0].contractLineId).toBe(1)
    expect(payload.groups[0].lines[0].releaseQty).toBe(600)
    expect(payload.groups[0].lines[0].lossRate).toBe(0)

    // Check group 1
    expect(payload.groups[1].productCode).toBe('P002')
    expect(payload.groups[1].colorInstructionMode).toBe('NEW')
    expect(payload.groups[1].newColorInstruction?.labFactoryId).toBe(999)
    expect(payload.groups[1].newColorInstruction?.colors[0].lightPrimary).toBe('D65')
    expect(payload.groups[1].lines[0].contractLineId).toBe(3)
    expect(payload.groups[1].lines[0].releaseQty).toBe(800)
    expect(payload.groups[1].lines[0].lossRate).toBe(0.05)
  })

  it('handles partial release where some lines or groups have 0 releaseQty', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)
    // Set group 0 line 0 to 0 releaseQty, line 1 to 200
    groups[0].lines[0].releaseQty = 0
    groups[0].lines[1].releaseQty = 200
    // Set group 1 line 0 to 0 releaseQty (entire group inactive)
    groups[1].lines[0].releaseQty = 0

    const summary = calculateReleaseSummary(groups)
    expect(summary.totalGreigeQty).toBe(200)
    expect(summary.totalDyeingQty).toBe(200)
    expect(summary.greigeOrderCount).toBe(1)
    expect(summary.dyeingOrderCount).toBe(1)
    expect(summary.inspectionOrderCount).toBe(1)

    const payload = buildReleaseProductionPayload({
      contractId: 1024,
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    // Inactive group is skipped, only active group remains
    expect(payload.groups).toHaveLength(1)
    expect(payload.groups[0].productCode).toBe('P001')
    expect(payload.groups[0].lines).toHaveLength(1)
    expect(payload.groups[0].lines[0].contractLineId).toBe(2)
    expect(payload.groups[0].lines[0].releaseQty).toBe(200)
  })

  it('validates negative releaseQty and zero total releaseQty across all lines', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)
    // 1. Negative releaseQty
    groups[0].lines[0].releaseQty = -10
    let validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors).toContain('行【1】本次下达量不能为负数')

    // 2. All zero releaseQty
    groups[0].lines[0].releaseQty = 0
    groups[0].lines[1].releaseQty = 0
    groups[1].lines[0].releaseQty = 0
    validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(false)
    expect(validation.errors).toContain('至少有一行本次下达量必须大于0')
  })

  it('initReleaseWorkbenchState with clearReleaseQty=true initializes all releaseQty to 0', () => {
    const groups = initReleaseWorkbenchState(mockContractLines, true)
    for (const group of groups) {
      for (const line of group.lines) {
        expect(line.releaseQty).toBe(0)
      }
    }
  })

  it('validateReleaseForm ignores missing lightPrimary on lines with releaseQty = 0', () => {
    const groups = initReleaseWorkbenchState(mockContractLines)
    groups[0].colorInstructionMode = 'NEW'
    // group 0 has two lines: line 0 (releaseQty=600) and line 1 (releaseQty=400)
    // Set line 1 releaseQty to 0, and clear its lightPrimary
    groups[0].lines[1].releaseQty = 0
    groups[0].newColorInstruction = {
      labFactoryId: null,
      colors: [
        {
          contractLineId: 1,
          customerColorNo: 'C101',
          colorName: 'C101',
          lightPrimary: 'D65'
        },
        {
          contractLineId: 2,
          customerColorNo: 'C102',
          colorName: 'C102',
          lightPrimary: '' // missing, but line has releaseQty = 0
        }
      ]
    }
    // group 1 is NONE
    groups[1].colorInstructionMode = 'NONE'

    const validation = validateReleaseForm({
      contractStatus: 'APPROVED',
      greigeFactoryId: 10,
      dyeingFactoryId: 20,
      groups
    })
    expect(validation.valid).toBe(true)
    expect(validation.errors).toHaveLength(0)
  })
})
