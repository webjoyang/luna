/**
 * 站内消息点击跳转与路由分发工具
 *
 * 规范（零 DDL 契约）：
 * 1. 消息 templateParams 固定携带 bizType / bizId / bizNo。
 * 2. 铃铛点击按 bizType→路由表跳转：
 *    - 审批类（bizType=APPROVAL）→ `/approval/todo`，携带 instanceId=bizId、id=bizId 与 bizNo。
 *    - 未在表中约定的业务类型返回 null，前端仅弹出消息详情弹窗，不强行跳转。
 */

export interface NotifyJumpContext {
  bizType?: string
  bizId?: string | number
  bizNo?: string
  [key: string]: any
}

export interface NotifyRouteResult {
  path: string
  query?: Record<string, any>
}

export type BizRouteResolver = (params: NotifyJumpContext) => NotifyRouteResult | null

/**
 * 业务类型路由表
 */
const BIZ_TYPE_ROUTE_REGISTRY = new Map<string, BizRouteResolver>()

// 默认注册审批类路由（契约：审批类 → /approval/todo 带实例 id）
BIZ_TYPE_ROUTE_REGISTRY.set('APPROVAL', (params) => {
  const query: Record<string, any> = {}
  if (params.bizId !== undefined && params.bizId !== null) {
    query.instanceId = String(params.bizId)
    query.id = String(params.bizId)
  }
  if (params.bizNo) {
    query.bizNo = String(params.bizNo)
  }
  return {
    path: '/approval/todo',
    query
  }
})

/**
 * 注册自定义 bizType 路由分发器
 */
export function registerBizTypeRoute(bizType: string, resolver: BizRouteResolver) {
  if (bizType && resolver) {
    BIZ_TYPE_ROUTE_REGISTRY.set(bizType.toUpperCase(), resolver)
  }
}

/**
 * 解析站内信 templateParams 参数
 */
export function parseNotifyParams(params: any): NotifyJumpContext {
  if (!params) {
    return {}
  }
  if (typeof params === 'object') {
    return params
  }
  if (typeof params === 'string') {
    try {
      const parsed = JSON.parse(params)
      return typeof parsed === 'object' && parsed !== null ? parsed : {}
    } catch {
      return {}
    }
  }
  return {}
}

/**
 * 根据消息参数获取跳转目标路由
 *
 * @param params 消息 templateParams 解析结果
 * @returns 目标路由与 query，若无路由约定则返回 null
 */
export function getNotifyJumpRoute(params: NotifyJumpContext): NotifyRouteResult | null {
  const bizType = (params?.bizType || '').toUpperCase()
  if (!bizType) {
    return null
  }

  const resolver = BIZ_TYPE_ROUTE_REGISTRY.get(bizType)
  if (resolver) {
    return resolver(params)
  }

  return null
}
