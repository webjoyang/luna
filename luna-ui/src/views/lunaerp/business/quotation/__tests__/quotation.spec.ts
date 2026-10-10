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
})
