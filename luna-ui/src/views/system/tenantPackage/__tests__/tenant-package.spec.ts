import { describe, expect, it } from 'vitest'

/**
 * 模拟租户套餐树与未落地模块 ID 保留函数
 */
export function computePackageSaveMenuIds(
  treeMenuOptions: Array<{ id: number; children?: Array<{ id: number; children?: any[] }> }>,
  checkedKeys: number[],
  halfCheckedKeys: number[],
  originalMenuIds: number[]
): number[] {
  const allTreeKeys = new Set<number>()
  const collectTreeKeys = (nodes: any[]) => {
    nodes?.forEach((node) => {
      allTreeKeys.add(node.id)
      if (node.children?.length) {
        collectTreeKeys(node.children)
      }
    })
  }
  collectTreeKeys(treeMenuOptions)

  // 过滤出树中不存在的悬挂 ID（如尚未在 system_menu 落地的规划模块 10700/10800）
  const hangingIds = (originalMenuIds || []).filter((mid: number) => !allTreeKeys.has(mid))

  return Array.from(new Set([...checkedKeys, ...halfCheckedKeys, ...hangingIds]))
}

/**
 * 模拟新租户默认绑定核心包逻辑
 */
export function resolveDefaultTenantPackageId(
  formType: string,
  currentPackageId: number | undefined,
  packages: Array<{ id: number; name: string }>
): number | undefined {
  if (formType === 'create' && !currentPackageId) {
    const corePkg = packages.find((pkg) => pkg.name === '核心包')
    return corePkg ? corePkg.id : 100
  }
  return currentPackageId
}

describe('TenantPackage & Tenant Core Package Binding Specs', () => {
  it('保留尚未在菜单树落地的规划模块 ID（10700 财务 / 10800 审批中心）', () => {
    // 模拟当前已落地的菜单树：仅包含基础信息(10000)与销售(10100)及其子菜单，暂无 10700 与 10800
    const treeMenuOptions = [
      {
        id: 10000,
        children: [{ id: 10010, children: [{ id: 10011 }] }]
      },
      {
        id: 10100,
        children: [{ id: 10110 }]
      }
    ]

    // 核心包原本存储的 menuIds（包含规划中的 10700 与 10800）
    const originalMenuIds = [10000, 10010, 10011, 10100, 10110, 10700, 10800]

    // 管理员在 UI 界面全选或保存当前树上的所有节点
    const checkedKeys = [10011, 10110]
    const halfCheckedKeys = [10000, 10010, 10100]

    const result = computePackageSaveMenuIds(treeMenuOptions, checkedKeys, halfCheckedKeys, originalMenuIds)

    // 断言：10700 和 10800 未被冲掉，仍完整保留在待提交的 menuIds 中
    expect(result).toContain(10700)
    expect(result).toContain(10800)
    expect(result).toContain(10000)
    expect(result).toContain(10110)
    expect(result.length).toBe(7)
  })

  it('新建租户时默认绑定「核心包」套餐（id: 100）', () => {
    const packageList = [
      { id: 111, name: '普通套餐' },
      { id: 100, name: '核心包' }
    ]

    // 新增模式且 packageId 为空
    const defaultId = resolveDefaultTenantPackageId('create', undefined, packageList)
    expect(defaultId).toBe(100)

    // 修改模式下应保留已有 packageId
    const editId = resolveDefaultTenantPackageId('update', 111, packageList)
    expect(editId).toBe(111)
  })

  it('若套餐列表中未显式返回核心包名称，默认保底指向 100', () => {
    const packageList = [{ id: 111, name: '普通套餐' }]
    const defaultId = resolveDefaultTenantPackageId('create', undefined, packageList)
    expect(defaultId).toBe(100)
  })
})
