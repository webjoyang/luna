import { describe, it, expect, vi } from 'vitest'
import {
  RECYCLE_BIZ_TYPES,
  getRecyclePage,
  restoreRecycleItem,
  purgeRecycleItem
} from '@/api/lunaerp/common/recycle'
import request from '@/config/axios'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('Recycle Bin Master Data Tabs and API', () => {
  it('should include all 5 master data bizTypes in RECYCLE_BIZ_TYPES', () => {
    const values = RECYCLE_BIZ_TYPES.map((t) => t.value)
    expect(values).toContain('CUSTOMER')
    expect(values).toContain('SUPPLIER')
    expect(values).toContain('PRODUCT')
    expect(values).toContain('YARN_TYPE')
    expect(values).toContain('YARN_SPEC')

    const supplier = RECYCLE_BIZ_TYPES.find((t) => t.value === 'SUPPLIER')
    expect(supplier?.label).toBe('供应商档案')

    const product = RECYCLE_BIZ_TYPES.find((t) => t.value === 'PRODUCT')
    expect(product?.label).toBe('产品档案')

    const yarnType = RECYCLE_BIZ_TYPES.find((t) => t.value === 'YARN_TYPE')
    expect(yarnType?.label).toBe('纱线材质')

    const yarnSpec = RECYCLE_BIZ_TYPES.find((t) => t.value === 'YARN_SPEC')
    expect(yarnSpec?.label).toBe('纱线规格')
  })

  it('should include all 7 draft document bizTypes in RECYCLE_BIZ_TYPES', () => {
    const values = RECYCLE_BIZ_TYPES.map((t) => t.value)
    expect(values).toContain('SALES_CONTRACT')
    expect(values).toContain('COLOR_INSTRUCTION')
    expect(values).toContain('GREIGE_ORDER')
    expect(values).toContain('DYEING_ORDER')
    expect(values).toContain('YARN_PURCHASE_ORDER')
    expect(values).toContain('STOCK_IN')
    expect(values).toContain('WEAVING_ORDER')
    expect(values).toContain('SHIPMENT')

    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'SALES_CONTRACT')?.label).toBe('销售合同')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'COLOR_INSTRUCTION')?.label).toBe('打色指示单')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'GREIGE_ORDER')?.label).toBe('胚布单')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'DYEING_ORDER')?.label).toBe('染色单')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'YARN_PURCHASE_ORDER')?.label).toBe('纱线采购单')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'STOCK_IN')?.label).toBe('入库单')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'WEAVING_ORDER')?.label).toBe('织造单')
    expect(RECYCLE_BIZ_TYPES.find((t) => t.value === 'SHIPMENT')?.label).toBe('发货单')

    expect(RECYCLE_BIZ_TYPES).toHaveLength(13)
  })

  it('getRecyclePage should call /lunaerp/common/recycle/page with params', async () => {
    const params = { bizType: 'SUPPLIER', pageNo: 1, pageSize: 10 }
    await getRecyclePage(params)
    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/common/recycle/page',
      params
    })
  })

  it('restoreRecycleItem should call PUT /lunaerp/common/recycle/restore', async () => {
    const data = { bizType: 'PRODUCT', bizId: 3001 }
    await restoreRecycleItem(data)
    expect(request.put).toHaveBeenCalledWith({
      url: '/lunaerp/common/recycle/restore',
      data
    })
  })

  it('purgeRecycleItem should call DELETE /lunaerp/common/recycle/purge', async () => {
    await purgeRecycleItem('YARN_SPEC', 5001)
    expect(request.delete).toHaveBeenCalledWith({
      url: '/lunaerp/common/recycle/purge',
      params: { bizType: 'YARN_SPEC', bizId: 5001 }
    })
  })
})
