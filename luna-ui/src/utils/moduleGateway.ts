import type { AppRouteRecordRaw } from '@/router/types'

/**
 * 业务模块一级根路径全集（对应 Q18 决策与 spec「模块全集与一级菜单 ID 分配」）。
 * 其中 /sales 与 /business 为同义词（一期兼容既有 /business 路径）。
 */
export const KNOWN_MODULE_ROOTS = [
  '/baseinfo',
  '/sales',
  '/business',
  '/production',
  '/stock',
  '/sample',
  '/finance',
  '/approval'
] as const

export const MSG_MODULE_NOT_ACTIVATED = '该模块未开通，请联系管理员'
export const MSG_NO_OPERATION_PERMISSION = '无操作权限'

/**
 * 从路由路径或 API URL 中提取业务模块根路径（例如 '/production'）。
 * 支持：
 * - 页面路由：'/production/greige-order', '/lunaerp-elmui/production/...'
 * - API 路径：'/lunaerp/production/greige-order/page'
 */
export function getModuleRootFromPath(path: string | undefined | null): string | undefined {
  if (!path) return undefined
  // 移除 query 和 hash
  const cleanPath = path.split('?')[0].split('#')[0]

  // 处理 API / 前端静态子页面前缀
  let normalized = cleanPath
  if (normalized.startsWith('/lunaerp/')) {
    normalized = normalized.substring('/lunaerp'.length)
  } else if (normalized.startsWith('/lunaerp-elmui/')) {
    normalized = normalized.substring('/lunaerp-elmui'.length)
  }

  // 匹配已知业务模块根路径
  for (const root of KNOWN_MODULE_ROOTS) {
    if (normalized === root || normalized.startsWith(`${root}/`)) {
      return root
    }
  }

  return undefined
}

/**
 * 检查指定模块根路径是否在用户已开通的路由列表中。
 * 特殊兼容：/sales 与 /business 互为同义模块，命中任一即可。
 */
export function isModuleActivated(
  moduleRoot: string | undefined,
  addRouters: AppRouteRecordRaw[] | undefined | null
): boolean {
  if (!moduleRoot || !addRouters || addRouters.length === 0) return false

  const targetRoots =
    moduleRoot === '/sales' || moduleRoot === '/business'
      ? ['/sales', '/business']
      : [moduleRoot]

  return addRouters.some((route) => {
    if (!route.path || route.path.startsWith('/:')) return false
    const routePath = route.path.startsWith('/') ? route.path : `/${route.path}`
    return targetRoots.some(
      (root) => routePath === root || routePath.startsWith(`${root}/`)
    )
  })
}

/**
 * 判断目标路由是否属于未开通模块。
 * 用于路由守卫：若是未开通模块的 URL，则直接重定向首页，而非显示 404。
 */
export function isUnactivatedModuleRoute(
  targetPath: string | undefined | null,
  addRouters: AppRouteRecordRaw[] | undefined | null
): boolean {
  if (!targetPath) return false
  const moduleRoot = getModuleRootFromPath(targetPath)
  if (!moduleRoot) {
    // 不是已知业务模块，正常走 404 或既有路由
    return false
  }
  return !isModuleActivated(moduleRoot, addRouters)
}

/**
 * 递归检查路由树中是否存在与候选路径或路由名匹配的菜单
 */
function hasMenuMatch(
  candidatePaths: string[],
  routeName: string | symbol | null | undefined,
  routes: AppRouteRecordRaw[],
  parentPath = ''
): boolean {
  for (const route of routes) {
    if (!route.path || route.path.startsWith('/:')) {
      continue // 忽略 404 等动态通配路由
    }

    let fullPath = route.path
    if (!fullPath.startsWith('/') && parentPath) {
      fullPath = `${parentPath.replace(/\/$/, '')}/${fullPath}`
    } else if (!fullPath.startsWith('/')) {
      fullPath = `/${fullPath}`
    }

    // 检查 name 匹配
    if (routeName && route.name && route.name === routeName) {
      return true
    }

    // 检查 path 匹配
    for (const candidate of candidatePaths) {
      if (candidate === fullPath || candidate.startsWith(`${fullPath}/`)) {
        return true
      }
    }

    if (route.children && route.children.length > 0) {
      if (hasMenuMatch(candidatePaths, routeName, route.children, fullPath)) {
        return true
      }
    }
  }

  return false
}

export interface RouteCheckTarget {
  path?: string
  name?: string | symbol | null
  meta?: Record<string, any>
}

/**
 * 检查当前路由对应的菜单是否存在于 addRouters 列表中。
 */
export function isRouteInAddRouters(
  currentRoute: RouteCheckTarget | undefined | null,
  addRouters: AppRouteRecordRaw[] | undefined | null
): boolean {
  if (!currentRoute || !addRouters || addRouters.length === 0) return false

  const candidatePaths: string[] = []
  if (currentRoute.path) {
    candidatePaths.push(currentRoute.path)
  }
  if (currentRoute.meta?.activeMenu) {
    candidatePaths.push(currentRoute.meta.activeMenu)
  }

  // 1. 如果当前路由属于某个已知模块，但该模块根本未开通，直接判定为 false
  for (const candidate of candidatePaths) {
    const moduleRoot = getModuleRootFromPath(candidate)
    if (moduleRoot && !isModuleActivated(moduleRoot, addRouters)) {
      return false
    }
  }

  // 2. 检查 candidatePaths 或 route name 是否在 addRouters 菜单树中
  return hasMenuMatch(candidatePaths, currentRoute.name, addRouters)
}

export interface ForbiddenCheckContext {
  currentRoute?: RouteCheckTarget | null
  requestUrl?: string | null
  addRouters?: AppRouteRecordRaw[] | null
}

export type ForbiddenReason = 'MODULE_NOT_ACTIVATED' | 'BUTTON_PERMISSION_DENIED'

/**
 * 判定 403 触发场景：
 * ① MODULE_NOT_ACTIVATED：当前路由对应的菜单不在 permissionStore.getAddRouters 中（模块未开通）
 * ② BUTTON_PERMISSION_DENIED：菜单存在但操作按钮级权限不足
 */
export function determineForbiddenReason(context: ForbiddenCheckContext): ForbiddenReason {
  const { currentRoute, requestUrl, addRouters = [] } = context

  // 1. 如果请求的 API URL 明确属于未开通模块
  if (requestUrl) {
    const apiModuleRoot = getModuleRootFromPath(requestUrl)
    if (apiModuleRoot && !isModuleActivated(apiModuleRoot, addRouters)) {
      return 'MODULE_NOT_ACTIVATED'
    }
  }

  // 2. 如果当前路由明确属于未开通模块
  if (currentRoute?.path) {
    const routeModuleRoot = getModuleRootFromPath(currentRoute.path)
    if (routeModuleRoot && !isModuleActivated(routeModuleRoot, addRouters)) {
      return 'MODULE_NOT_ACTIVATED'
    }
  }

  // 3. 检查当前路由对应的菜单是否在 addRouters 中
  if (currentRoute && isRouteInAddRouters(currentRoute, addRouters)) {
    return 'BUTTON_PERMISSION_DENIED'
  }

  // 4. 当前路由不在 addRouters 中，属于模块未开通场景
  return 'MODULE_NOT_ACTIVATED'
}
