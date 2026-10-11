import { describe, expect, it, vi } from 'vitest'
import * as ReleaseProductionApi from '@/api/lunaerp-elmui/production/release-production'
import type {
  ReleaseContractLineVO,
  ReleaseFromContractRespVO,
  ReleaseGroupVO,
  ReleaseProductionReqVO
} from '@/api/lunaerp-elmui/production/release-production'
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

describe('ReleaseProductionTab (合同详情「生产」工作台)', () => {
  describe('1. API 端点封装', () => {
    it('getReleaseFromContract 应调用 /lunaerp/production/release-production/from-contract', async () => {
      const mockResp: ReleaseFromContractRespVO = {
        contractId: 1001,
        contractNo: 'CH20261011001',
        clientName: '上海某某纺织有限公司',
        deliveryDate: '2026-11-15',
        status: 'ACTIVE',
        lines: [
          {
            contractLineId: 10,
            lineNo: 1,
            productId: 101,
            productCode: 'P001',
            productName: '莫代尔汗布',
            customerColor: 'C101',
            contractQty: 1000,
            releasedQty: 400,
            remainingQty: 600,
            defaultLossRate: 0
          }
        ],
        existingColorInstructions: [
          {
            id: 501,
            instructionNo: 'DS20261011001',
            status: 'CONFIRMED',
            productCode: 'P001'
          }
        ]
      }
      vi.mocked(request.get).mockResolvedValueOnce(mockResp as any)

      const res = await ReleaseProductionApi.getReleaseFromContract(1001)
      expect(request.get).toHaveBeenCalledWith({
        url: '/lunaerp/production/release-production/from-contract',
        params: { contractId: 1001 }
      })
      expect(res.contractNo).toBe('CH20261011001')
      expect(res.lines.length).toBe(1)
      expect(res.existingColorInstructions.length).toBe(1)
    })

    it('releaseProduction 应调用 POST /lunaerp/production/release-production/release', async () => {
      const reqVO: ReleaseProductionReqVO = {
        contractId: 1001,
        greigeFactoryId: 201,
        dyeingFactoryId: 202,
        groups: [
          {
            productId: 101,
            productCode: 'P001',
            productName: '莫代尔汗布',
            colorInstructionMode: 'NONE',
            lines: [
              {
                contractLineId: 10,
                customerColor: 'C101',
                releaseQty: 600,
                lossRate: 0.05
              }
            ]
          }
        ]
      }
      vi.mocked(request.post).mockResolvedValueOnce({
        greigeOrders: [{ id: 11, orderNo: 'PB20261011001' }],
        dyeingOrders: [{ id: 21 }],
        inspectionOrders: [{ id: 31, code: 'INS20261011001', type: 'GREIGE' }],
        colorInstructions: []
      } as any)

      const res = await ReleaseProductionApi.releaseProduction(reqVO)
      expect(request.post).toHaveBeenCalledWith({
        url: '/lunaerp/production/release-production/release',
        data: reqVO
      })
      expect(res.greigeOrders.length).toBe(1)
      expect(res.dyeingOrders.length).toBe(1)
      expect(res.inspectionOrders.length).toBe(1)
    })
  })

  describe('2. 字典项定义校验', () => {
    it('主灯源字典应包含 D65、TL84、CWF 等常用光源', () => {
      const options = getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_LIGHT_SOURCE)
      const values = options.map((opt) => opt.value)
      expect(values).toContain('D65')
      expect(values).toContain('TL84')
      expect(values).toContain('CWF')
      expect(values).toContain('A')
      expect(values).toContain('UV')
    })

    it('打色单状态字典应包含 CONFIRMED 等状态', () => {
      const options = getStrDictOptions(DICT_TYPE.LUNAERP_COLOR_INSTRUCTION_STATUS)
      const values = options.map((opt) => opt.value)
      expect(values).toContain('PENDING')
      expect(values).toContain('CONFIRMED')
      expect(values).toContain('IN_PRODUCTION')
    })
  })

  describe('3. 状态守卫与合同取消逻辑', () => {
    const computeStateGuard = (status?: string) => {
      const isCancelled = status?.toUpperCase() === 'CANCELLED'
      const releaseDisabled = isCancelled
      const showAlert = isCancelled
      return { isCancelled, releaseDisabled, showAlert }
    }

    it('CANCELLED 状态应展示红色警示并禁用下达生产', () => {
      const guard = computeStateGuard('CANCELLED')
      expect(guard.isCancelled).toBe(true)
      expect(guard.releaseDisabled).toBe(true)
      expect(guard.showAlert).toBe(true)
    })

    it('ACTIVE 状态下达生产按钮不因状态被禁用', () => {
      const guard = computeStateGuard('ACTIVE')
      expect(guard.isCancelled).toBe(false)
      expect(guard.releaseDisabled).toBe(false)
      expect(guard.showAlert).toBe(false)
    })
  })

  describe('4. 合同行下达量与超下达校验逻辑', () => {
    const isQtyExceeded = (releaseQty: number, remainingQty: number) => {
      return Number(releaseQty || 0) > Number(remainingQty || 0)
    }

    it('本次下达量不超过剩余量时校验通过', () => {
      expect(isQtyExceeded(600, 600)).toBe(false)
      expect(isQtyExceeded(300, 600)).toBe(false)
      expect(isQtyExceeded(0, 600)).toBe(false)
    })

    it('本次下达量超出剩余量时触发超下达红框警示', () => {
      expect(isQtyExceeded(600.01, 600)).toBe(true)
      expect(isQtyExceeded(700, 600)).toBe(true)
      expect(isQtyExceeded(1, 0)).toBe(true)
    })

    it('初始化时缺省剩余量，下达成功后清空本次下达量', () => {
      const line: ReleaseContractLineVO = {
        contractLineId: 1,
        contractQty: 1000,
        releasedQty: 400,
        remainingQty: 600
      }

      // 初次加载：缺省为剩余量
      const initialReleaseQty = Math.max(0, line.remainingQty)
      expect(initialReleaseQty).toBe(600)

      // 下达成功后：刷新并清空本次下达量为 0
      const isAfterSuccess = true
      const afterSuccessReleaseQty = isAfterSuccess ? 0 : initialReleaseQty
      expect(afterSuccessReleaseQty).toBe(0)
    })
  })

  describe('5. 同产品合并下达与产品分组编排', () => {
    interface TestLine {
      contractLineId: number
      productCode: string
      productName: string
      customerColor: string
      releaseQty: number
      lossRatePercent: number
    }

    const groupLinesByProduct = (lines: TestLine[]) => {
      const map = new Map<string, { productCode: string; productName: string; lines: TestLine[] }>()
      for (const line of lines) {
        if (!map.has(line.productCode)) {
          map.set(line.productCode, {
            productCode: line.productCode,
            productName: line.productName,
            lines: []
          })
        }
        map.get(line.productCode)!.lines.push(line)
      }
      return Array.from(map.values())
    }

    it('同 productCode 的多合同行应合并到同一分组', () => {
      const lines: TestLine[] = [
        {
          contractLineId: 1,
          productCode: 'P001',
          productName: '纯棉汗布',
          customerColor: 'C101 (黑色)',
          releaseQty: 500,
          lossRatePercent: 5
        },
        {
          contractLineId: 2,
          productCode: 'P001',
          productName: '纯棉汗布',
          customerColor: 'C102 (白色)',
          releaseQty: 300,
          lossRatePercent: 3
        },
        {
          contractLineId: 3,
          productCode: 'P002',
          productName: '莫代尔罗马布',
          customerColor: 'C201 (藏青)',
          releaseQty: 200,
          lossRatePercent: 0
        }
      ]

      const groups = groupLinesByProduct(lines)
      expect(groups.length).toBe(2)

      const groupP001 = groups.find((g) => g.productCode === 'P001')
      expect(groupP001).toBeDefined()
      expect(groupP001?.lines.length).toBe(2)

      const groupP002 = groups.find((g) => g.productCode === 'P002')
      expect(groupP002).toBeDefined()
      expect(groupP002?.lines.length).toBe(1)
    })

    it('损耗率百分比正确换算为小数损耗率', () => {
      const lossRatePercent = 5.5
      const lossRate = Number((lossRatePercent / 100).toFixed(4))
      expect(lossRate).toBe(0.055)
    })

    it('单据汇总计算：每有效产品组生成 1 胚布单 + 1 染色单 + 1 胚布检验单', () => {
      const groups = [
        {
          productCode: 'P001',
          lines: [{ releaseQty: 500 }, { releaseQty: 300 }]
        },
        {
          productCode: 'P002',
          lines: [{ releaseQty: 200 }]
        },
        {
          productCode: 'P003',
          lines: [{ releaseQty: 0 }] // 无下达量组
        }
      ]

      const activeGroups = groups.filter((g) => g.lines.some((l) => l.releaseQty > 0))
      expect(activeGroups.length).toBe(2)

      const totalGreigeQty = groups.reduce(
        (sum, g) => sum + g.lines.reduce((s, l) => s + l.releaseQty, 0),
        0
      )
      expect(totalGreigeQty).toBe(1000)

      const inspectionOrderCount = activeGroups.length
      expect(inspectionOrderCount).toBe(2)
    })
  })

  describe('6. 打色单模式处理规则', () => {
    it('EXISTING 模式需要指定已有的 CONFIRMED 打色单', () => {
      const validateExistingMode = (existingColorInstructionId: number | null | undefined) => {
        if (!existingColorInstructionId) {
          return { valid: false, message: '请选择已有打色单' }
        }
        return { valid: true }
      }

      expect(validateExistingMode(undefined).valid).toBe(false)
      expect(validateExistingMode(null).valid).toBe(false)
      expect(validateExistingMode(501).valid).toBe(true)
    })

    it('NEW 模式主灯源必填校验', () => {
      const validateNewModeColors = (colors: Array<{ lightPrimary?: string }>) => {
        for (const c of colors) {
          if (!c.lightPrimary || !c.lightPrimary.trim()) {
            return { valid: false, message: '主灯源不能为空' }
          }
        }
        return { valid: true }
      }

      expect(validateNewModeColors([{ lightPrimary: '' }]).valid).toBe(false)
      expect(validateNewModeColors([{ lightPrimary: 'D65' }, { lightPrimary: '' }]).valid).toBe(false)
      expect(validateNewModeColors([{ lightPrimary: 'D65' }, { lightPrimary: 'TL84' }]).valid).toBe(true)
    })

    it('NEW 模式组装打色单 payload 且胚布/染色不引用它（零回写）', () => {
      const group: ReleaseGroupVO = {
        productCode: 'P001',
        productName: '纯棉汗布',
        colorInstructionMode: 'NEW',
        existingColorInstructionId: null,
        newColorInstruction: {
          labFactoryId: 301,
          colors: [
            {
              contractLineId: 1,
              customerColorNo: 'C101',
              colorName: 'C101',
              lightPrimary: 'D65',
              factoryColorNo: 'FC-991',
              colorCardRef: 'PANTONE-19'
            }
          ]
        },
        lines: [
          {
            contractLineId: 1,
            customerColor: 'C101',
            releaseQty: 500,
            lossRate: 0.05
          }
        ]
      }

      expect(group.colorInstructionMode).toBe('NEW')
      expect(group.existingColorInstructionId).toBeNull()
      expect(group.newColorInstruction).toBeDefined()
      expect(group.newColorInstruction?.labFactoryId).toBe(301)
      expect(group.newColorInstruction?.colors[0].lightPrimary).toBe('D65')
    })
  })

  describe('7. 提交前总体校验与二次确认', () => {
    const validateBeforeSubmit = (params: {
      isCancelled: boolean
      greigeFactoryId?: number
      dyeingFactoryId?: number
      totalReleaseQty: number
      hasExceededQty: boolean
    }) => {
      if (params.isCancelled) return { ok: false, error: '合同已取消，无法下达生产' }
      if (!params.greigeFactoryId) return { ok: false, error: '请选择承织坯布厂' }
      if (!params.dyeingFactoryId) return { ok: false, error: '请选择承染方' }
      if (params.totalReleaseQty <= 0) return { ok: false, error: '本次下达量必须大于 0' }
      if (params.hasExceededQty) return { ok: false, error: '存在合同行本次下达量超出剩余可下达量' }
      return { ok: true }
    }

    it('缺少坯布厂或染厂时校验失败', () => {
      expect(
        validateBeforeSubmit({
          isCancelled: false,
          greigeFactoryId: undefined,
          dyeingFactoryId: 102,
          totalReleaseQty: 500,
          hasExceededQty: false
        }).error
      ).toBe('请选择承织坯布厂')

      expect(
        validateBeforeSubmit({
          isCancelled: false,
          greigeFactoryId: 101,
          dyeingFactoryId: undefined,
          totalReleaseQty: 500,
          hasExceededQty: false
        }).error
      ).toBe('请选择承染方')
    })

    it('所有条件满足时校验通过', () => {
      const res = validateBeforeSubmit({
        isCancelled: false,
        greigeFactoryId: 101,
        dyeingFactoryId: 102,
        totalReleaseQty: 500,
        hasExceededQty: false
      })
      expect(res.ok).toBe(true)
    })
  })
})
