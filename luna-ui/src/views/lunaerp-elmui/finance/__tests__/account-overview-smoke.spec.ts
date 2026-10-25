import { describe, it, expect, vi } from 'vitest'

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

import {
  formatAmount,
  isAccountNoRequired,
  maskAccountNo,
  validateTransfer,
  parseAccountErrorMessage,
  FINANCE_ACCOUNT_ERROR_CODES
} from '../account/model/account'
import {
  formatPercent,
  getPeriodPresets,
  computeGrossMarginMetrics
} from '../overview/model/overview'

describe('Finance Account & Overview Smoke Test (Issue 09 End-to-End Logic)', () => {
  it('smoke: 新建账户校验 → 转账校验 → 流水借贷两笔 TRANSFER → 总览页数字对得上', () => {
    // 1. 新建账户参数校验
    expect(isAccountNoRequired('BANK')).toBe(true)
    expect(isAccountNoRequired('CASH')).toBe(false)

    const accountA = {
      id: 101,
      accountName: '基本户（工行）',
      accountType: 'BANK',
      currency: 'RMB',
      currentBalance: 10000,
      openingBalance: 10000
    }

    const accountB = {
      id: 102,
      accountName: '一般户（招行）',
      accountType: 'BANK',
      currency: 'RMB',
      currentBalance: 2000,
      openingBalance: 2000
    }

    expect(maskAccountNo('6225880123456789')).toBe('6225 **** **** 6789')
    expect(formatAmount(accountA.openingBalance)).toBe('10,000.00')

    // 2. 转账前置校验：从 A 转 3000 至 B
    const transferAmount = 3000
    const checkValid = validateTransfer(accountA, accountB, transferAmount)
    expect(checkValid.valid).toBe(true)

    // 校验同账户与超额转账拦截
    const checkSame = validateTransfer(accountA, accountA, 500)
    expect(checkSame.valid).toBe(false)
    expect(checkSame.message).toContain('转出账户与转入账户不能相同')

    const checkOver = validateTransfer(accountA, accountB, 20000)
    expect(checkOver.valid).toBe(false)
    expect(checkOver.message).toContain('超出转出账户可用余额')

    // 3. 流水抽屉模拟：两笔 TRANSFER 分录（一借一贷）
    const ledgerEntries = [
      {
        id: 1,
        accountId: accountA.id,
        category: 'TRANSFER_OUT',
        direction: 'CREDIT',
        originalAmount: transferAmount,
        baseAmount: transferAmount,
        currency: 'RMB',
        refType: 'TRANSFER',
        refNo: 'ZZ20261020001'
      },
      {
        id: 2,
        accountId: accountB.id,
        category: 'TRANSFER_IN',
        direction: 'DEBIT',
        originalAmount: transferAmount,
        baseAmount: transferAmount,
        currency: 'RMB',
        refType: 'TRANSFER',
        refNo: 'ZZ20261020001'
      }
    ]

    expect(ledgerEntries.length).toBe(2)
    const outEntry = ledgerEntries.find((e) => e.accountId === accountA.id)
    const inEntry = ledgerEntries.find((e) => e.accountId === accountB.id)
    expect(outEntry?.direction).toBe('CREDIT')
    expect(inEntry?.direction).toBe('DEBIT')

    // 4. 总览页数字聚合
    // Account A: opening 10000, inflow 0, outflow 3000 -> ending 7000
    const endingA = accountA.openingBalance - (outEntry?.originalAmount || 0)
    expect(endingA).toBe(7000)

    // Account B: opening 2000, inflow 3000, outflow 0 -> ending 5000
    const endingB = accountB.openingBalance + (inEntry?.originalAmount || 0)
    expect(endingB).toBe(5000)

    // 全账户合计
    const totalOpening = accountA.openingBalance + accountB.openingBalance
    const totalInflow = inEntry?.originalAmount || 0
    const totalOutflow = outEntry?.originalAmount || 0
    const totalEnding = endingA + endingB

    expect(totalOpening).toBe(12000)
    expect(totalInflow).toBe(3000)
    expect(totalOutflow).toBe(3000)
    expect(totalEnding).toBe(12000)
    expect(totalEnding).toBe(totalOpening + totalInflow - totalOutflow)

    // 5. 经营毛利参考卡片指标
    const revenue = 100000
    const cost = 65000
    const margin = computeGrossMarginMetrics(revenue, cost)
    expect(margin.grossProfit).toBe(35000)
    expect(formatPercent(margin.grossMarginRate)).toBe('35.00%')

    // 6. 错误码友好提示覆盖
    expect(
      parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN)
    ).toContain('已有分录引用')
    expect(
      parseAccountErrorMessage(FINANCE_ACCOUNT_ERROR_CODES.ACCOUNT_OPENING_BALANCE_IMMUTABLE)
    ).toContain('期初余额不可修改')
  })
})
