import { describe, expect, it } from 'vitest'
import {
  calculateRollTotalLength,
  calculateRollTotalWeight,
  calculateQcRestockAndScrapQty,
  getLineRollQty,
  getSalesReturnActionFlags,
  isQtyRollMismatch,
  validateQcDecision,
  validateSalesReturnLine
} from '../model/salesReturn'

describe('SalesReturn Domain Model Tests', () => {
  describe('getSalesReturnActionFlags', () => {
    it('草稿态且未提交（DRAFT + PENDING）允许编辑、删除、提交审批，不显示详情', () => {
      const flags = getSalesReturnActionFlags('DRAFT', 'PENDING')
      expect(flags.canDetail).toBe(false)
      expect(flags.canEdit).toBe(true)
      expect(flags.canDelete).toBe(true)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canWithdraw).toBe(false)
      expect(flags.canReceive).toBe(false)
      expect(flags.canPost).toBe(false)
      expect(flags.canCancel).toBe(true)
      expect(flags.canPrint).toBe(true)
    })

    it('草稿被驳回（DRAFT + REJECTED）按规范仅允许编辑与提交，禁止删除', () => {
      const flags = getSalesReturnActionFlags('DRAFT', 'REJECTED')
      expect(flags.canDetail).toBe(false)
      expect(flags.canEdit).toBe(true)
      expect(flags.canDelete).toBe(false)
      expect(flags.canSubmit).toBe(true)
      expect(flags.canWithdraw).toBe(false)
    })

    it('审批中（DRAFT + APPROVING）仅允许撤回，禁止编辑和删除', () => {
      const flags = getSalesReturnActionFlags('DRAFT', 'APPROVING')
      expect(flags.canDetail).toBe(false)
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canSubmit).toBe(false)
      expect(flags.canWithdraw).toBe(true)
    })

    it('已审批（APPROVED）允许到货质检与作废，禁止编辑', () => {
      const flags = getSalesReturnActionFlags('APPROVED', 'APPROVED')
      expect(flags.canDetail).toBe(true)
      expect(flags.canReceive).toBe(true)
      expect(flags.canCancel).toBe(true)
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
    })

    it('已收货（RECEIVED）允许财务确认', () => {
      const flags = getSalesReturnActionFlags('RECEIVED', 'APPROVED')
      expect(flags.canDetail).toBe(true)
      expect(flags.canPost).toBe(true)
      expect(flags.canReceive).toBe(false)
      expect(flags.canEdit).toBe(false)
    })

    it('已过账（POSTED）为终态，仅允许打印与查看详情', () => {
      const flags = getSalesReturnActionFlags('POSTED', 'APPROVED')
      expect(flags.canDetail).toBe(true)
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canCancel).toBe(false)
      expect(flags.canPrint).toBe(true)
    })

    it('已作废（CANCELLED）全部写动作禁止，仅允许查看详情', () => {
      const flags = getSalesReturnActionFlags('CANCELLED', 'CANCELLED')
      expect(flags.canDetail).toBe(true)
      expect(flags.canEdit).toBe(false)
      expect(flags.canDelete).toBe(false)
      expect(flags.canPrint).toBe(false)
    })
  })

  describe('Roll calculations and soft-alignment mismatch', () => {
    it('正确计算卷重量与米数合计', () => {
      const rolls = [
        { rollNo: 'R1', weight: 25.5, lengthM: 50.0 },
        { rollNo: 'R2', weight: 24.5, lengthM: 48.0 }
      ]
      expect(calculateRollTotalWeight(rolls)).toBe(50.0)
      expect(calculateRollTotalLength(rolls)).toBe(98.0)
    })

    it('按单位识别口径汇总卷数量', () => {
      const rolls = [
        { rollNo: 'R1', weight: 20.0, lengthM: 60.0 },
        { rollNo: 'R2', weight: 30.0, lengthM: 40.0 }
      ]
      expect(getLineRollQty('KG', rolls)).toBe(50.0)
      expect(getLineRollQty('M', rolls)).toBe(100.0)
    })

    it('软齐套判定：行数量与卷合计一致时返回 false', () => {
      const rolls = [
        { rollNo: 'R1', weight: 25.0 },
        { rollNo: 'R2', weight: 25.0 }
      ]
      expect(isQtyRollMismatch(50.0, 'KG', rolls)).toBe(false)
    })

    it('软齐套判定：行数量与卷合计不一致时返回 true（黄色警示）', () => {
      const rolls = [
        { rollNo: 'R1', weight: 20.0 },
        { rollNo: 'R2', weight: 20.0 }
      ]
      // 行数量 50，卷合计 40
      expect(isQtyRollMismatch(50.0, 'KG', rolls)).toBe(true)
    })
  })

  describe('validateSalesReturnLine', () => {
    it('合法行返回 valid: true', () => {
      const result = validateSalesReturnLine({
        shipmentItemId: 101,
        unit: 'KG',
        returnQty: 25.0,
        returnReason: 'QUALITY',
        rolls: [{ rollNo: 'R1', weight: 25.0 }]
      })
      expect(result.valid).toBe(true)
    })

    it('缺少退货原因返回 valid: false', () => {
      const result = validateSalesReturnLine({
        shipmentItemId: 101,
        unit: 'KG',
        returnQty: 25.0,
        returnReason: '',
        rolls: [{ rollNo: 'R1', weight: 25.0 }]
      })
      expect(result.valid).toBe(false)
      expect(result.message).toContain('退货原因不能为空')
    })

    it('缺少卷清单返回 valid: false', () => {
      const result = validateSalesReturnLine({
        shipmentItemId: 101,
        unit: 'KG',
        returnQty: 25.0,
        returnReason: 'QUALITY',
        rolls: []
      })
      expect(result.valid).toBe(false)
      expect(result.message).toContain('每行必须至少登记一卷')
    })

    it('卷重量与米数双空返回 valid: false', () => {
      const result = validateSalesReturnLine({
        shipmentItemId: 101,
        unit: 'KG',
        returnQty: 25.0,
        returnReason: 'QUALITY',
        rolls: [{ rollNo: 'R1', weight: undefined, lengthM: undefined }]
      })
      expect(result.valid).toBe(false)
      expect(result.message).toContain('重量与米数至少需填写一项')
    })
  })

  describe('calculateQcRestockAndScrapQty', () => {
    it('KG 单位：正确汇总 RESTOCK 卷重量与 SCRAP 卷重量', () => {
      const rolls = [
        { rollNo: 'R1', weight: 30.5, qcResult: 'RESTOCK' },
        { rollNo: 'R2', weight: 20.0, qcResult: 'SCRAP' },
        { rollNo: 'R3', weight: 15.2, qcResult: 'RESTOCK' }
      ]
      const { restockQty, scrapQty } = calculateQcRestockAndScrapQty('KG', rolls)
      expect(restockQty).toBe(45.7)
      expect(scrapQty).toBe(20.0)
    })

    it('M 单位：正确汇总 RESTOCK 卷米数与 SCRAP 卷米数', () => {
      const rolls = [
        { rollNo: 'R1', lengthM: 100.0, qcResult: 'RESTOCK' },
        { rollNo: 'R2', lengthM: 50.0, qcResult: 'SCRAP' }
      ]
      const { restockQty, scrapQty } = calculateQcRestockAndScrapQty('M', rolls)
      expect(restockQty).toBe(100.0)
      expect(scrapQty).toBe(50.0)
    })

    it('全废分支：restockQty 为 0，scrapQty 等于总数量', () => {
      const rolls = [
        { rollNo: 'R1', weight: 25.0, qcResult: 'SCRAP' },
        { rollNo: 'R2', weight: 35.0, qcResult: 'SCRAP' }
      ]
      const { restockQty, scrapQty } = calculateQcRestockAndScrapQty('KG', rolls)
      expect(restockQty).toBe(0)
      expect(scrapQty).toBe(60.0)
    })
  })

  describe('validateQcDecision', () => {
    it('全部卷已判定且合法时返回 valid: true', () => {
      const rolls = [
        { rollNo: 'R1', qcResult: 'RESTOCK' },
        { rollNo: 'R2', qcResult: 'SCRAP' }
      ]
      expect(validateQcDecision(rolls).valid).toBe(true)
    })

    it('存在未判定或非法判定时返回 valid: false 并提示', () => {
      const rolls = [
        { rollNo: 'R1', qcResult: 'RESTOCK' },
        { rollNo: 'R2', qcResult: '' }
      ]
      const result = validateQcDecision(rolls)
      expect(result.valid).toBe(false)
      expect(result.message).toContain('未完成质检判定')
    })
  })
})
