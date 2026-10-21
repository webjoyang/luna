import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import {
  resolveDefaultWarehouse,
  getOrCreateItemRequestId,
  saveRequisitionItemDraft,
  getRequisitionItemDraft,
  clearRequisitionItemDraft,
  executeItemDispatch,
  DEFAULT_WAREHOUSE_WARNING_MESSAGE,
  DRAFT_NAMESPACE
} from '../requisition-detail-helper'
import { getDraft, hasDraft } from '@/utils/h5-draft'
import { NETWORK_ERROR_MESSAGE } from '@/utils/h5-submit'

class MemoryStorage implements Storage {
  private store = new Map<string, string>()

  get length(): number {
    return this.store.size
  }

  clear(): void {
    this.store.clear()
  }

  getItem(key: string): string | null {
    return this.store.get(key) ?? null
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null
  }

  removeItem(key: string): void {
    this.store.delete(key)
  }

  setItem(key: string, value: string): void {
    this.store.set(key, String(value))
  }
}

describe('requisition-detail-helper', () => {
  let memoryStorage: MemoryStorage

  beforeEach(() => {
    memoryStorage = new MemoryStorage()
    ;(globalThis as any).localStorage = memoryStorage
    ;(globalThis as any).window = { localStorage: memoryStorage }
  })

  afterEach(() => {
    delete (globalThis as any).localStorage
    delete (globalThis as any).window
    vi.restoreAllMocks()
  })

  describe('默认仓库消费与顶部黄条提示 (resolveDefaultWarehouse)', () => {
    const warehouses = [
      { id: 1, name: '一号原料仓' },
      { id: 2, name: '二号面料仓' }
    ]

    it('当配置了有效的 defaultWarehouseId 时，发纱页自动预选该仓库，且不展示顶部黄条', () => {
      const result = resolveDefaultWarehouse({
        defaultWarehouseId: 1,
        warehouseList: warehouses,
        currentSelectedWarehouseId: null
      })

      expect(result.selectedWarehouseId).toBe(1)
      expect(result.selectedWarehouseName).toBe('一号原料仓')
      expect(result.shouldShowWarningBanner).toBe(false)
    })

    it('当未配置或配置为 null 时，发纱页不预选、强制手选，且展示顶部黄条提示管理员配置', () => {
      const result = resolveDefaultWarehouse({
        defaultWarehouseId: null,
        warehouseList: warehouses,
        currentSelectedWarehouseId: null
      })

      expect(result.selectedWarehouseId).toBeNull()
      expect(result.selectedWarehouseName).toBe('')
      expect(result.shouldShowWarningBanner).toBe(true)
      expect(result.warningBannerText).toContain('未配置默认发纱仓库')
      expect(result.warningBannerText).toContain('luna.workshop.default-warehouse-id')
    })

    it('当 defaultWarehouseId 在启用仓库列表中不存在（未启用或不存在）时，不预选且展示顶部黄条', () => {
      const result = resolveDefaultWarehouse({
        defaultWarehouseId: 999,
        warehouseList: warehouses,
        currentSelectedWarehouseId: null
      })

      expect(result.selectedWarehouseId).toBeNull()
      expect(result.shouldShowWarningBanner).toBe(true)
    })

    it('当用户此前已手动选择其他仓库时，保留用户手选，不被默认仓库覆盖', () => {
      const result = resolveDefaultWarehouse({
        defaultWarehouseId: 1,
        warehouseList: warehouses,
        currentSelectedWarehouseId: 2
      })

      expect(result.selectedWarehouseId).toBe(2)
      expect(result.selectedWarehouseName).toBe('二号面料仓')
      expect(result.shouldShowWarningBanner).toBe(false)
    })
  })

  describe('requestId 一次生成随表单 localStorage 持久化与重试沿用', () => {
    it('首次生成 requestId 并持久化至草稿，后续读取沿用同一 requestId', () => {
      const requisitionId = 101
      const itemId = 1001

      // 首次获取：生成全新 UUID 并保存
      const reqId1 = getOrCreateItemRequestId(requisitionId, itemId)
      expect(reqId1).toBeDefined()
      expect(typeof reqId1).toBe('string')
      expect(reqId1.length).toBeGreaterThan(10)

      saveRequisitionItemDraft({
        requisitionId,
        itemId,
        dispatchQty: '35.5',
        requestId: reqId1,
        warehouseId: 1
      })

      // 再次获取：沿用同一 requestId
      const reqId2 = getOrCreateItemRequestId(requisitionId, itemId)
      expect(reqId2).toBe(reqId1)

      // 从 localStorage 草稿中验证 requestId 保持一致
      const draft = getRequisitionItemDraft(requisitionId, itemId)
      expect(draft?.requestId).toBe(reqId1)
      expect(draft?.dispatchQty).toBe('35.5')
      expect(draft?.warehouseId).toBe(1)
    })

    it('草稿按 id 分键，且提交成功后清除对应行的草稿与 requestId', () => {
      const requisitionId = 101
      const itemId = 1001
      const requestId = 'req-uuid-test-001'

      saveRequisitionItemDraft({
        requisitionId,
        itemId,
        dispatchQty: '20.0',
        requestId,
        warehouseId: 1
      })

      expect(hasDraft(DRAFT_NAMESPACE, itemId)).toBe(true)

      // 提交成功清除
      clearRequisitionItemDraft(requisitionId, itemId)

      expect(hasDraft(DRAFT_NAMESPACE, itemId)).toBe(false)
      expect(getRequisitionItemDraft(requisitionId, itemId)).toBeNull()

      // 再次生成时将获得新的 requestId（非旧请求）
      const newReqId = getOrCreateItemRequestId(requisitionId, itemId)
      expect(newReqId).not.toBe(requestId)
    })
  })

  describe('弱网双击防连点与只扣一次 (executeItemDispatch)', () => {
    it('提交中禁用防连点：同一行并发双击时，第二次点击被静默忽略', async () => {
      const requisitionId = 101
      const itemId = 1001
      const dispatchApi = vi
        .fn()
        .mockImplementation(() => new Promise((resolve) => setTimeout(() => resolve(true), 100)))
      const submittingMap = { [itemId]: false }

      // 第一次点击
      const p1 = executeItemDispatch({
        requisitionId,
        itemId,
        dispatchQty: '10.0',
        warehouseId: 1,
        dispatchApi,
        submittingMap
      })

      // 连击双击（submittingMap 此时为 true）
      const p2 = executeItemDispatch({
        requisitionId,
        itemId,
        dispatchQty: '10.0',
        warehouseId: 1,
        dispatchApi,
        submittingMap
      })

      const [res1, res2] = await Promise.all([p1, p2])

      expect(res1?.success).toBe(true)
      expect(res2?.ignored).toBe(true)
      // 底层 API 仅被调用 1 次
      expect(dispatchApi).toHaveBeenCalledTimes(1)
    })
  })

  describe('断网保留草稿恢复后一键重试成功 & 错误分色提示', () => {
    it('发生网络超时/断网时，不清除草稿与 requestId，返回 warning 分色提示，重试沿用同一 requestId', async () => {
      const requisitionId = 101
      const itemId = 1001
      const persistentRequestId = 'req-uuid-network-retry'

      // 先存好草稿
      saveRequisitionItemDraft({
        requisitionId,
        itemId,
        dispatchQty: '50.0',
        requestId: persistentRequestId,
        warehouseId: 1
      })

      // 模拟首次提交发生网络错误（如超时）
      const networkError = new Error('timeout of 10000ms exceeded')
      ;(networkError as any).code = 'ECONNABORTED'

      const dispatchApi = vi.fn().mockRejectedValueOnce(networkError)
      const submittingMap = { [itemId]: false }

      const firstAttempt = await executeItemDispatch({
        requisitionId,
        itemId,
        dispatchQty: '50.0',
        warehouseId: 1,
        dispatchApi,
        submittingMap
      })

      // 验证返回分色提示为 warning，文案符合规约
      expect(firstAttempt.success).toBe(false)
      expect(firstAttempt.errorInfo?.type).toBe('warning')
      expect(firstAttempt.errorInfo?.message).toBe(NETWORK_ERROR_MESSAGE)

      // 验证草稿与 requestId 完整保留在 localStorage 中
      const draftAfterFail = getRequisitionItemDraft(requisitionId, itemId)
      expect(draftAfterFail?.requestId).toBe(persistentRequestId)
      expect(draftAfterFail?.dispatchQty).toBe('50.0')

      // 网络恢复，工人点击重试：沿用同一 requestId 提交成功
      dispatchApi.mockResolvedValueOnce(true)
      const retryAttempt = await executeItemDispatch({
        requisitionId,
        itemId,
        dispatchQty: '50.0',
        warehouseId: 1,
        dispatchApi,
        submittingMap
      })

      expect(retryAttempt.success).toBe(true)
      // 验证两次调用均使用了相同的 persistentRequestId
      expect(dispatchApi).toHaveBeenNthCalledWith(
        1,
        expect.objectContaining({ requestId: persistentRequestId })
      )
      expect(dispatchApi).toHaveBeenNthCalledWith(
        2,
        expect.objectContaining({ requestId: persistentRequestId })
      )

      // 成功后草稿清除
      const draftAfterSuccess = getRequisitionItemDraft(requisitionId, itemId)
      expect(draftAfterSuccess).toBeNull()
    })

    it('发生业务异常（如 015-311 库存不足）时，返回 danger 分色提示与具体业务文案', async () => {
      const requisitionId = 101
      const itemId = 1001
      const businessError = {
        code: 1030015311,
        message: '仓库可用库存余额不足，无法完成发纱'
      }

      const dispatchApi = vi.fn().mockRejectedValueOnce(businessError)
      const submittingMap = { [itemId]: false }

      const result = await executeItemDispatch({
        requisitionId,
        itemId,
        dispatchQty: '9999.0',
        warehouseId: 1,
        dispatchApi,
        submittingMap
      })

      expect(result.success).toBe(false)
      expect(result.errorInfo?.type).toBe('danger')
      expect(result.errorInfo?.message).toBe('仓库可用库存余额不足，无法完成发纱')
    })
  })
})
