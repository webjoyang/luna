// =============================================================================
// LunaERP - 车间移动 H5 权限与路由辅助工具（ADR-0045）
// =============================================================================

export const H5_PERMISSIONS = {
  REPORT: 'lunaerp:workshop:h5:report',
  INSPECTION: 'lunaerp:workshop:h5:inspection',
  DISPATCH: 'lunaerp:workshop:h5:dispatch'
} as const

export const ALL_PERMISSION = '*:*:*'

export interface H5TabItem {
  path: string
  title: string
  icon: string
  permission?: string
}

export const H5_TABS: readonly H5TabItem[] = [
  {
    path: '/h5/report',
    title: '报工',
    icon: 'records-o',
    permission: H5_PERMISSIONS.REPORT
  },
  {
    path: '/h5/inspection',
    title: '质检',
    icon: 'shield-o',
    permission: H5_PERMISSIONS.INSPECTION
  },
  {
    path: '/h5/requisition',
    title: '领纱',
    icon: 'logistics',
    permission: H5_PERMISSIONS.DISPATCH
  },
  {
    path: '/h5/me',
    title: '我的',
    icon: 'user-o'
  }
] as const

/**
 * 判断用户是否拥有超级权限（*:*:*）
 */
export function hasSuperPermission(permissions: Set<string> | undefined | null): boolean {
  return !!permissions && permissions.has(ALL_PERMISSION)
}

/**
 * 校验用户是否拥有指定的车间权限码（或具备超级权限）
 */
export function hasWorkshopPermission(
  permissions: Set<string> | undefined | null,
  permission: string
): boolean {
  if (!permissions) {
    return false
  }
  if (hasSuperPermission(permissions)) {
    return true
  }
  return permissions.has(permission)
}

/**
 * 判断用户是否具备任一车间移动端权限（报工、质检、领纱或超级权限）
 * 若三项全无且非超级管理员，则无权访问车间移动端业务页面，需重定向到 /h5/403
 */
export function hasAnyWorkshopPermission(permissions: Set<string> | undefined | null): boolean {
  if (!permissions) {
    return false
  }
  if (hasSuperPermission(permissions)) {
    return true
  }
  return (
    permissions.has(H5_PERMISSIONS.REPORT) ||
    permissions.has(H5_PERMISSIONS.INSPECTION) ||
    permissions.has(H5_PERMISSIONS.DISPATCH)
  )
}

/**
 * 根据用户权限过滤出可在移动端底部 Tabbar 渲染的 Tab 项
 */
export function getVisibleTabs(permissions: Set<string> | undefined | null): H5TabItem[] {
  if (!permissions || !hasAnyWorkshopPermission(permissions)) {
    return []
  }
  return H5_TABS.filter((tab) => {
    if (!tab.permission) {
      return true
    }
    return hasWorkshopPermission(permissions, tab.permission)
  })
}

/**
 * 获取当前用户登录进入 /h5 时的默认落点 Tab 路径
 * 优先级：报工 -> 质检 -> 领纱 -> 我的；若三权限全无则返回 /h5/403
 */
export function getDefaultH5Path(permissions: Set<string> | undefined | null): string {
  const visible = getVisibleTabs(permissions)
  return visible.length > 0 ? visible[0].path : '/h5/403'
}

/**
 * 检验用户是否有权直接访问指定 H5 页面路由
 * 用于 permission.ts 路由守卫进行页面级与按钮级守卫拦截
 */
export function canAccessH5Path(
  path: string,
  permissions: Set<string> | undefined | null
): boolean {
  // 白名单与 403 页面无需车间权限守卫
  if (path === '/h5/login' || path === '/h5/403') {
    return true
  }

  // 其他任意 /h5/** 业务路径必须首先拥有任一车间权限
  if (!hasAnyWorkshopPermission(permissions)) {
    return false
  }

  // /h5 容器根路径与 /h5/me 个人中心对所有车间移动端已授权用户开放
  if (path === '/h5' || path === '/h5/' || path === '/h5/me' || path.startsWith('/h5/me/')) {
    return true
  }

  // 报工域：Tab 报工、布票报工卡 (/h5/ticket)
  if (
    path === '/h5/report' ||
    path.startsWith('/h5/report/') ||
    path === '/h5/ticket' ||
    path.startsWith('/h5/ticket?') ||
    path.startsWith('/h5/ticket/')
  ) {
    return hasWorkshopPermission(permissions, H5_PERMISSIONS.REPORT)
  }

  // 质检域：Tab 质检、录检详情 (/h5/inspection/order)
  if (path === '/h5/inspection' || path.startsWith('/h5/inspection/')) {
    return hasWorkshopPermission(permissions, H5_PERMISSIONS.INSPECTION)
  }

  // 领纱域：Tab 领纱、发纱详情 (/h5/requisition/detail)
  if (path === '/h5/requisition' || path.startsWith('/h5/requisition/')) {
    return hasWorkshopPermission(permissions, H5_PERMISSIONS.DISPATCH)
  }

  // 布票二维码冻结落点 (/h5/scan/weaving-ticket)：报工或质检任意一角色均可进入分流
  if (path.startsWith('/h5/scan/weaving-ticket')) {
    return (
      hasWorkshopPermission(permissions, H5_PERMISSIONS.REPORT) ||
      hasWorkshopPermission(permissions, H5_PERMISSIONS.INSPECTION)
    )
  }

  return true
}
