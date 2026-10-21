import { describe, expect, it } from 'vitest'
import {
  H5_PERMISSIONS,
  ALL_PERMISSION,
  hasSuperPermission,
  hasWorkshopPermission,
  hasAnyWorkshopPermission,
  getVisibleTabs,
  getDefaultH5Path,
  canAccessH5Path
} from '../h5Auth'

describe('车间移动 H5 权限与路由辅助工具（06-h5-layout-route-guard-login-403）', () => {
  const permReport = H5_PERMISSIONS.REPORT
  const permInspection = H5_PERMISSIONS.INSPECTION
  const permDispatch = H5_PERMISSIONS.DISPATCH

  it('权限码常量与 spec §5 保持完全一致', () => {
    expect(H5_PERMISSIONS.REPORT).toBe('lunaerp:workshop:h5:report')
    expect(H5_PERMISSIONS.INSPECTION).toBe('lunaerp:workshop:h5:inspection')
    expect(H5_PERMISSIONS.DISPATCH).toBe('lunaerp:workshop:h5:dispatch')
  })

  describe('hasSuperPermission 与 hasWorkshopPermission', () => {
    it('超级管理员 (*:*:*) 拥有所有车间操作权限', () => {
      const superPerms = new Set([ALL_PERMISSION])
      expect(hasSuperPermission(superPerms)).toBe(true)
      expect(hasWorkshopPermission(superPerms, permReport)).toBe(true)
      expect(hasWorkshopPermission(superPerms, permInspection)).toBe(true)
      expect(hasWorkshopPermission(superPerms, permDispatch)).toBe(true)
    })

    it('普通角色仅对其拥有的权限返回 true', () => {
      const workerPerms = new Set([permReport, permDispatch])
      expect(hasSuperPermission(workerPerms)).toBe(false)
      expect(hasWorkshopPermission(workerPerms, permReport)).toBe(true)
      expect(hasWorkshopPermission(workerPerms, permDispatch)).toBe(true)
      expect(hasWorkshopPermission(workerPerms, permInspection)).toBe(false)
    })

    it('空权限与 null/undefined 均安全返回 false', () => {
      expect(hasWorkshopPermission(null, permReport)).toBe(false)
      expect(hasWorkshopPermission(undefined, permReport)).toBe(false)
      expect(hasWorkshopPermission(new Set(), permReport)).toBe(false)
    })
  })

  describe('hasAnyWorkshopPermission', () => {
    it('仅有 PC 权限的用户返回 false', () => {
      const pcPerms = new Set(['system:user:query', 'system:role:query'])
      expect(hasAnyWorkshopPermission(pcPerms)).toBe(false)
    })

    it('包含报工、质检或领纱任一权限的用户返回 true', () => {
      expect(hasAnyWorkshopPermission(new Set([permReport]))).toBe(true)
      expect(hasAnyWorkshopPermission(new Set([permInspection]))).toBe(true)
      expect(hasAnyWorkshopPermission(new Set([permDispatch]))).toBe(true)
    })

    it('超级管理员返回 true', () => {
      expect(hasAnyWorkshopPermission(new Set([ALL_PERMISSION]))).toBe(true)
    })

    it('空权限或未登录安全返回 false', () => {
      expect(hasAnyWorkshopPermission(null)).toBe(false)
      expect(hasAnyWorkshopPermission(undefined)).toBe(false)
      expect(hasAnyWorkshopPermission(new Set())).toBe(false)
    })
  })

  describe('getVisibleTabs 与 getDefaultH5Path', () => {
    it('无车间角色时，visibleTabs 为空数组，默认路径为 /h5/403', () => {
      const noPerms = new Set(['infra:config:query'])
      expect(getVisibleTabs(noPerms)).toEqual([])
      expect(getDefaultH5Path(noPerms)).toBe('/h5/403')
    })

    it('车间工人角色（报工 + 发纱）：渲染 报工、领纱、我的 三个 Tab，默认落点为 /h5/report', () => {
      const workerPerms = new Set([permReport, permDispatch])
      const tabs = getVisibleTabs(workerPerms)
      expect(tabs.map((t) => t.path)).toEqual(['/h5/report', '/h5/requisition', '/h5/me'])
      expect(getDefaultH5Path(workerPerms)).toBe('/h5/report')
    })

    it('车间检验员角色（仅质检）：渲染 质检、我的 两个 Tab，默认落点为 /h5/inspection', () => {
      const inspectorPerms = new Set([permInspection])
      const tabs = getVisibleTabs(inspectorPerms)
      expect(tabs.map((t) => t.path)).toEqual(['/h5/inspection', '/h5/me'])
      expect(getDefaultH5Path(inspectorPerms)).toBe('/h5/inspection')
    })

    it('仅拥有发纱权限的用户：渲染 领纱、我的 两个 Tab，默认落点为 /h5/requisition', () => {
      const dispatchPerms = new Set([permDispatch])
      const tabs = getVisibleTabs(dispatchPerms)
      expect(tabs.map((t) => t.path)).toEqual(['/h5/requisition', '/h5/me'])
      expect(getDefaultH5Path(dispatchPerms)).toBe('/h5/requisition')
    })

    it('超级管理员拥有全部四个 Tab，默认落点为 /h5/report', () => {
      const superPerms = new Set([ALL_PERMISSION])
      const tabs = getVisibleTabs(superPerms)
      expect(tabs.map((t) => t.path)).toEqual([
        '/h5/report',
        '/h5/inspection',
        '/h5/requisition',
        '/h5/me'
      ])
      expect(getDefaultH5Path(superPerms)).toBe('/h5/report')
    })
  })

  describe('canAccessH5Path（路由直接输入守卫）', () => {
    it('/h5/login 与 /h5/403 始终允许任何人访问', () => {
      expect(canAccessH5Path('/h5/login', new Set())).toBe(true)
      expect(canAccessH5Path('/h5/403', new Set())).toBe(true)
      expect(canAccessH5Path('/h5/login', null)).toBe(true)
      expect(canAccessH5Path('/h5/403', null)).toBe(true)
    })

    it('无车间角色的用户禁止访问任何 /h5 业务路径', () => {
      const pcPerms = new Set(['system:user:query'])
      expect(canAccessH5Path('/h5', pcPerms)).toBe(false)
      expect(canAccessH5Path('/h5/report', pcPerms)).toBe(false)
      expect(canAccessH5Path('/h5/inspection', pcPerms)).toBe(false)
      expect(canAccessH5Path('/h5/requisition', pcPerms)).toBe(false)
      expect(canAccessH5Path('/h5/me', pcPerms)).toBe(false)
    })

    it('工人账号访问质检路径被拒，访问报工、发纱与我的放行', () => {
      const workerPerms = new Set([permReport, permDispatch])
      expect(canAccessH5Path('/h5/report', workerPerms)).toBe(true)
      expect(canAccessH5Path('/h5/ticket', workerPerms)).toBe(true)
      expect(canAccessH5Path('/h5/requisition', workerPerms)).toBe(true)
      expect(canAccessH5Path('/h5/me', workerPerms)).toBe(true)
      expect(canAccessH5Path('/h5/inspection', workerPerms)).toBe(false)
    })

    it('检验员账号访问报工路径被拒，访问质检与我的放行', () => {
      const inspectorPerms = new Set([permInspection])
      expect(canAccessH5Path('/h5/inspection', inspectorPerms)).toBe(true)
      expect(canAccessH5Path('/h5/me', inspectorPerms)).toBe(true)
      expect(canAccessH5Path('/h5/report', inspectorPerms)).toBe(false)
      expect(canAccessH5Path('/h5/ticket', inspectorPerms)).toBe(false)
      expect(canAccessH5Path('/h5/requisition', inspectorPerms)).toBe(false)
    })

    it('扫码落点 /h5/scan/weaving-ticket 报工与质检均可进入分流', () => {
      const workerPerms = new Set([permReport])
      const inspectorPerms = new Set([permInspection])
      const dispatchPerms = new Set([permDispatch])

      expect(canAccessH5Path('/h5/scan/weaving-ticket?code=BT001', workerPerms)).toBe(true)
      expect(canAccessH5Path('/h5/scan/weaving-ticket?code=BT001', inspectorPerms)).toBe(true)
      expect(canAccessH5Path('/h5/scan/weaving-ticket?code=BT001', dispatchPerms)).toBe(false)
    })
  })
})
