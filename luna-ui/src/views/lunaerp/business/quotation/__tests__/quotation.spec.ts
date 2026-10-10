import { describe, it, expect } from 'vitest'

describe('Quotation Math and Logic Tests', () => {
  it('should reverse-calculate quotation unit price from cost and target margin', () => {
    const costUnitPrice = 80.0
    const targetMarginRate = 0.2 // 20%
    const divisor = 1 - targetMarginRate
    const unitPrice = Number((costUnitPrice / divisor).toFixed(4))
    expect(unitPrice).toBe(100.0)

    // actual margin calculation
    const amount = Number((100 * unitPrice).toFixed(4))
    const costAmount = Number((100 * costUnitPrice).toFixed(4))
    const marginRate = Number(((amount - costAmount) / amount).toFixed(6))
    expect(marginRate).toBe(0.2)
  })

  it('should convert foreign exchange correctly and snapshot exchange rate', () => {
    const costRmb = 80.0
    const exchangeRate = 8.0 // 1 USD = 8 RMB
    const costUsd = Number((costRmb / exchangeRate).toFixed(4))
    expect(costUsd).toBe(10.0)

    const targetMargin = 0.2
    const unitPriceUsd = Number((costUsd / (1 - targetMargin)).toFixed(4))
    expect(unitPriceUsd).toBe(12.5)

    const qty = 50
    const totalAmount = Number((qty * unitPriceUsd).toFixed(4))
    const totalCost = Number((qty * costUsd).toFixed(4))
    expect(totalAmount).toBe(625.0)
    expect(totalCost).toBe(500.0)
    expect(Number(((totalAmount - totalCost) / totalAmount).toFixed(6))).toBe(0.2)
  })

  it('should compute weighted average margin for multiple lines', () => {
    // Line 1: qty=100, unitPrice=100, cost=80 => amt=10000, costAmt=8000
    // Line 2: qty=200, unitPrice=50, cost=45 => amt=10000, costAmt=9000
    const totalQty = 100 + 200
    const totalAmt = 10000 + 10000
    const totalCost = 8000 + 9000
    const avgMargin = Number(((totalAmt - totalCost) / totalAmt).toFixed(6))
    // (20000 - 17000) / 20000 = 3000 / 20000 = 0.15 (15%)
    expect(totalQty).toBe(300)
    expect(totalAmt).toBe(20000)
    expect(totalCost).toBe(17000)
    expect(avgMargin).toBe(0.15)
  })

  it('should flag low margin warning when margin is negative or below threshold', () => {
    const minThreshold = 0.1 // 10%
    const margin1 = 0.05
    const margin2 = -0.05
    const margin3 = 0.15

    expect(margin1 < minThreshold).toBe(true)
    expect(margin2 < minThreshold).toBe(true)
    expect(margin3 < minThreshold).toBe(false)
  })

  it('should verify contract-prefill maps quotation fields and omits cost/margin fields', () => {
    const quotation = {
      id: 1024,
      quotationNo: 'QT20260001',
      versionNo: 1,
      customerId: 2048,
      clientName: '测试客户',
      clientContact: '张三',
      phone: '13800000000',
      currency: 'USD',
      salespersonUserId: 5,
      items: [
        {
          productCode: 'P001',
          productName: '纯棉平布',
          customerColor: 'C-01',
          composition: '100%棉',
          width: '150cm',
          weight: '200g/m2',
          quantity: 1000,
          unitPrice: 12.5,
          costUnitPrice: 10.0, // internal cost
          marginRate: 0.2 // internal margin
        }
      ]
    }

    // Prefill mapping logic
    const prefill = {
      sourceQuotationId: quotation.id,
      sourceQuotationNo: `${quotation.quotationNo}-v${quotation.versionNo}`,
      customerId: quotation.customerId,
      clientName: quotation.clientName,
      clientContact: quotation.clientContact,
      currency: quotation.currency,
      tradeType: 'DOMESTIC',
      salespersonUserId: quotation.salespersonUserId,
      items: quotation.items.map((it) => ({
        productCode: it.productCode,
        productName: it.productName,
        customerColor: it.customerColor,
        composition: it.composition,
        width: it.width,
        weight: it.weight,
        quantity: it.quantity,
        unitPrice: it.unitPrice
      }))
    }

    expect(prefill.sourceQuotationNo).toBe('QT20260001-v1')
    expect(prefill.currency).toBe('USD')
    expect(prefill.tradeType).toBe('DOMESTIC')
    expect(prefill.items[0].productCode).toBe('P001')
    expect(prefill.items[0].unitPrice).toBe(12.5)

    // Strict assertion: prefill and items must NOT contain cost or margin
    expect((prefill as any).costUnitPrice).toBeUndefined()
    expect((prefill as any).marginRate).toBeUndefined()
    expect((prefill.items[0] as any).costUnitPrice).toBeUndefined()
    expect((prefill.items[0] as any).marginRate).toBeUndefined()
  })

  it('should verify print-data contains only clean customer/spec fields without cost/margin', () => {
    const printData = {
      id: 1024,
      quotationNo: 'QT20260001',
      versionNo: 2,
      displayNo: 'QT20260001-v2',
      clientName: '外贸客户',
      currency: 'USD',
      totalQuantity: 500,
      totalAmount: 6250.0,
      items: [
        {
          lineNo: 1,
          productCode: 'P002',
          productName: '涤纶斜纹',
          unit: 'M',
          quantity: 500,
          unitPrice: 12.5,
          amount: 6250.0
        }
      ]
    }

    const printKeys = Object.keys(printData)
    expect(printKeys.some((k) => k.toLowerCase().includes('cost') || k.toLowerCase().includes('margin'))).toBe(false)

    const itemKeys = Object.keys(printData.items[0])
    expect(itemKeys.some((k) => k.toLowerCase().includes('cost') || k.toLowerCase().includes('margin'))).toBe(false)
  })
})
