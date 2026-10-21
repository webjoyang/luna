import { generateUUID } from '@/utils'
import { saveDraft, getDraft, clearDraft, hasDraft } from '@/utils/h5-draft'
import { formatSubmitError, type SubmitErrorInfo } from '@/utils/h5-submit'

/**
 * 车间移动 H5 - 领纱单发纱与弱网持久化辅助模块（Issue 19）
 */

export const DEFAULT_WAREHOUSE_CONFIG_KEY = 'luna.workshop.default-warehouse-id'
export const DEFAULT_WAREHOUSE_WARNING_MESSAGE =
  '未配置默认发纱仓库，请联系管理员在系统参数中配置（luna.workshop.default-warehouse-id），当前请手动选择发纱仓库'
export const DRAFT_NAMESPACE = 'requisition:dispatch'

export interface ResolveWarehouseParams {
  defaultWarehouseId?: number | null
  warehouseList: { id: number; name: string }[]
  currentSelectedWarehouseId?: number | null
}

export interface ResolveWarehouseResult {
  selectedWarehouseId: number | null
  selectedWarehouseName: string
  shouldShowWarningBanner: boolean
  warningBannerText: string
}

/**
 * 默认仓库消费逻辑：
 * 1. 存在且启用才预选；
 * 2. 未配置或未启用则发纱页不预选、强制手选，顶部黄条提示管理员配置；
 * 3. 优先保留用户已手选的仓库；
 * 4. warehouseId 仍必填校验。
 */
export function resolveDefaultWarehouse(params: ResolveWarehouseParams): ResolveWarehouseResult {
  const { defaultWarehouseId, warehouseList = [], currentSelectedWarehouseId } = params

  const isDefaultWarehouseActive =
    defaultWarehouseId != null && warehouseList.some((wh) => wh.id === defaultWarehouseId)

  const shouldShowWarningBanner = !isDefaultWarehouseActive
  const warningBannerText = DEFAULT_WAREHOUSE_WARNING_MESSAGE

  // 1. 若当前已经有手选仓库，优先保留手选
  if (currentSelectedWarehouseId != null) {
    const matched = warehouseList.find((wh) => wh.id === currentSelectedWarehouseId)
    return {
      selectedWarehouseId: currentSelectedWarehouseId,
      selectedWarehouseName: matched ? matched.name : `仓库 #${currentSelectedWarehouseId}`,
      shouldShowWarningBanner,
      warningBannerText
    }
  }

  // 2. 否则若默认仓库存在且在启用列表中，进行预选
  if (isDefaultWarehouseActive) {
    const matched = warehouseList.find((wh) => wh.id === defaultWarehouseId)!
    return {
      selectedWarehouseId: defaultWarehouseId,
      selectedWarehouseName: matched.name,
      shouldShowWarningBanner: false,
      warningBannerText
    }
  }

  // 3. 未配置或未启用：不预选，强制手选
  return {
    selectedWarehouseId: null,
    selectedWarehouseName: '',
    shouldShowWarningBanner: true,
    warningBannerText
  }
}

export interface RequisitionItemDraftState {
  requisitionId: number
  itemId: number
  dispatchQty: string
  requestId: string
  warehouseId?: number | null
  updatedAt?: number
}

export interface RequisitionDraftState {
  requisitionId: number
  warehouseId?: number | null
  items: Record<number, RequisitionItemDraftState>
}

/**
 * 获取指定领纱行项目的本地草稿
 */
export function getRequisitionItemDraft(
  requisitionId: number,
  itemId: number
): RequisitionItemDraftState | null {
  // 1. 优先从 item.id 键读取
  const itemDraft = getDraft<RequisitionItemDraftState>(DRAFT_NAMESPACE, itemId)
  if (itemDraft && itemDraft.requestId) {
    return itemDraft
  }

  // 2. 兜底从 requisitionId 键读取
  const reqDraft = getDraft<RequisitionDraftState>(DRAFT_NAMESPACE, requisitionId)
  if (reqDraft && reqDraft.items && reqDraft.items[itemId]) {
    return reqDraft.items[itemId]
  }

  return null
}

/**
 * 获取整张领纱单的本地草稿
 */
export function getRequisitionDraft(requisitionId: number): RequisitionDraftState | null {
  return getDraft<RequisitionDraftState>(DRAFT_NAMESPACE, requisitionId)
}

/**
 * 保存领纱行项目的本地草稿与 requestId（双写 item.id 与 requisitionId 确保按 id 检索一致）
 */
export function saveRequisitionItemDraft(params: {
  requisitionId: number
  itemId: number
  dispatchQty: string
  requestId: string
  warehouseId?: number | null
}): void {
  const { requisitionId, itemId, dispatchQty, requestId, warehouseId } = params

  const payload: RequisitionItemDraftState = {
    requisitionId,
    itemId,
    dispatchQty,
    requestId,
    warehouseId,
    updatedAt: Date.now()
  }

  // 1. 按 itemId 独立存草稿（按 id 分键）
  saveDraft(DRAFT_NAMESPACE, itemId, payload)

  // 2. 同步写入 requisitionId 草稿聚合
  const reqDraft = getDraft<RequisitionDraftState>(DRAFT_NAMESPACE, requisitionId) || {
    requisitionId,
    warehouseId: warehouseId ?? null,
    items: {}
  }
  if (warehouseId != null) {
    reqDraft.warehouseId = warehouseId
  }
  if (!reqDraft.items) {
    reqDraft.items = {}
  }
  reqDraft.items[itemId] = payload
  saveDraft(DRAFT_NAMESPACE, requisitionId, reqDraft)
}

/**
 * 清除指定领纱行项目的本地草稿（发纱成功后调用）
 */
export function clearRequisitionItemDraft(requisitionId: number, itemId: number): void {
  // 1. 清除 itemId 草稿
  clearDraft(DRAFT_NAMESPACE, itemId)

  // 2. 清除 requisitionId 聚合中的对应行
  const reqDraft = getDraft<RequisitionDraftState>(DRAFT_NAMESPACE, requisitionId)
  if (reqDraft && reqDraft.items) {
    delete reqDraft.items[itemId]
    if (Object.keys(reqDraft.items).length === 0) {
      clearDraft(DRAFT_NAMESPACE, requisitionId)
    } else {
      saveDraft(DRAFT_NAMESPACE, requisitionId, reqDraft)
    }
  }
}

/**
 * 获取或一次性生成持久化 requestId（存在草稿则沿用，否则生成全新 UUID 并持久化）
 */
export function getOrCreateItemRequestId(requisitionId: number, itemId: number): string {
  const existing = getRequisitionItemDraft(requisitionId, itemId)
  if (existing && existing.requestId && existing.requestId.trim()) {
    return existing.requestId
  }

  const newRequestId = generateUUID()
  saveRequisitionItemDraft({
    requisitionId,
    itemId,
    dispatchQty: existing?.dispatchQty || '',
    requestId: newRequestId,
    warehouseId: existing?.warehouseId ?? null
  })
  return newRequestId
}

export interface ExecuteItemDispatchParams {
  requisitionId: number
  itemId: number
  dispatchQty: string
  warehouseId: number
  dispatchApi: (req: any) => Promise<any>
  submittingMap: Record<number, boolean>
}

export interface ExecuteItemDispatchResult {
  success: boolean
  ignored?: boolean
  data?: any
  errorInfo?: SubmitErrorInfo
}

/**
 * 执行发纱出库（集成提交中防连点、requestId 沿用、弱网/业务分色提示、成功清理草稿）
 */
export function executeItemDispatch(
  params: ExecuteItemDispatchParams
): Promise<ExecuteItemDispatchResult> {
  const { requisitionId, itemId, dispatchQty, warehouseId, dispatchApi, submittingMap } = params

  // 1. 提交中防连点：同一行并发双击静默忽略
  if (submittingMap[itemId]) {
    return Promise.resolve({ success: false, ignored: true })
  }

  submittingMap[itemId] = true

  return (async () => {
    try {
      // 2. 取出或生成持久化的 requestId（重试沿用同一 id）
      const requestId = getOrCreateItemRequestId(requisitionId, itemId)

      // 实时保存草稿防断电/断网
      saveRequisitionItemDraft({
        requisitionId,
        itemId,
        dispatchQty,
        requestId,
        warehouseId
      })

      // 3. 执行远程发纱出库
      const res = await dispatchApi({
        itemId,
        warehouseId,
        dispatchQty: Number(dispatchQty),
        requestId
      })

      // 4. 提交成功后清除草稿与 requestId
      clearRequisitionItemDraft(requisitionId, itemId)

      return { success: true, data: res }
    } catch (err: unknown) {
      // 5. 错误处理：草稿与 requestId 保持保留供重试
      const errorInfo = formatSubmitError(err)
      return { success: false, errorInfo }
    } finally {
      submittingMap[itemId] = false
    }
  })()
}
