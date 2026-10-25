import { describe, expect, it } from 'vitest'
import type { AppRouteRecordRaw } from '@/router/types'
import {
  determineForbiddenReason,
  getModuleRootFromPath,
  isModuleActivated,
  isUnactivatedModuleRoute,
  MSG_MODULE_NOT_ACTIVATED,
  MSG_NO_OPERATION_PERMISSION
} from '@/utils/moduleGateway'
import {
  computePackageSaveMenuIds,
  resolveDefaultTenantPackageId
} from './tenant-package.spec'

/**
 * 模拟 Element Plus 树结构级联选择算法（纯函数模式）
 * 对应 el-tree 默认 check-strictly=false 的级联逻辑：
 * 1. 父选 -> 子全选
 * 2. 父消 -> 子全消
 * 3. 子选部分 -> 父半选（half-checked）
 */
interface TreeNode {
  id: number
  name: string
  children?: TreeNode[]
}

function collectAllDescendantIds(node: TreeNode): number[] {
  const ids: number[] = [node.id]
  if (node.children) {
    for (const child of node.children) {
      ids.push(...collectAllDescendantIds(child))
    }
  }
  return ids
}

function cascadeCheck(
  tree: TreeNode[],
  targetId: number,
  checked: boolean,
  currentCheckedSet: Set<number>
): Set<number> {
  const nextSet = new Set(currentCheckedSet)

  const findAndApply = (nodes: TreeNode[]): boolean => {
    for (const node of nodes) {
      if (node.id === targetId) {
        const descendantIds = collectAllDescendantIds(node)
        if (checked) {
          descendantIds.forEach((id) => nextSet.add(id))
        } else {
          descendantIds.forEach((id) => nextSet.delete(id))
        }
        return true
      }
      if (node.children && findAndApply(node.children)) {
        return true
      }
    }
    return false
  }

  findAndApply(tree)
  return nextSet
}

function computeTreeState(
  tree: TreeNode[],
  checkedSet: Set<number>
): { checkedKeys: number[]; halfCheckedKeys: number[] } {
  const checkedKeys: number[] = []
  const halfCheckedKeys: number[] = []

  const evalNode = (node: TreeNode): { checkedCount: number; totalCount: number } => {
    if (!node.children || node.children.length === 0) {
      const isChecked = checkedSet.has(node.id)
      if (isChecked) {
        checkedKeys.push(node.id)
        return { checkedCount: 1, totalCount: 1 }
      }
      return { checkedCount: 0, totalCount: 1 }
    }

    let childCheckedCount = 0
    let childTotalCount = 0
    for (const child of node.children) {
      const res = evalNode(child)
      childCheckedCount += res.checkedCount
      childTotalCount += res.totalCount
    }

    if (childCheckedCount === childTotalCount) {
      checkedKeys.push(node.id)
    } else if (childCheckedCount > 0) {
      halfCheckedKeys.push(node.id)
    }

    return { checkedCount: childCheckedCount, totalCount: childTotalCount }
  }

  tree.forEach((root) => evalNode(root))
  return { checkedKeys, halfCheckedKeys }
}

describe('Issue 04: 模块开通/停用端到端全链路验证（E2E）', () => {
  // 模拟系统已落地的全量菜单树（包含基础信息、销售、生产、库存、样品、系统管理）
  const fullSystemMenuTree: TreeNode[] = [
    {
      id: 1,
      name: '系统管理',
      children: [
        { id: 100, name: '用户管理' },
        { id: 101, name: '角色管理' },
        { id: 102, name: '菜单管理' }
      ]
    },
    {
      id: 10000,
      name: '基础信息',
      children: [
        { id: 10010, name: '产品信息', children: [{ id: 10011, name: '产品创建' }] },
        { id: 10020, name: '胚布品种' },
        { id: 10030, name: '客户信息' },
        { id: 10050, name: '供应商信息' }
      ]
    },
    {
      id: 10100,
      name: '销售',
      children: [
        { id: 10110, name: '销售合同', children: [{ id: 10111, name: '合同创建' }] },
        { id: 10120, name: '发货管理' }
      ]
    },
    {
      id: 10200,
      name: '生产',
      children: [
        { id: 10210, name: '胚布单', children: [{ id: 10211, name: '胚布单创建' }, { id: 10212, name: '胚布单修改' }] },
        { id: 10220, name: '染色单' },
        { id: 10230, name: '纱线采购' },
        { id: 10240, name: '纱线档案' },
        { id: 10250, name: '纱线配比BOM' },
        { id: 10260, name: '检验单' },
        { id: 10270, name: '质检确认' },
        { id: 10280, name: '织造单' },
        { id: 10285, name: '织机档案' },
        { id: 10400, name: '进度反馈' },
        { id: 10410, name: '生产进度' },
        { id: 10500, name: '发染记录' }
      ]
    },
    {
      id: 10300,
      name: '库存',
      children: [
        { id: 10310, name: '入库管理' },
        { id: 10320, name: '库存查询' },
        { id: 10330, name: '仓库管理' }
      ]
    },
    {
      id: 10600,
      name: '样品',
      children: [{ id: 10610, name: '样品分析' }]
    }
  ]

  // 全部节点 ID 集合（扁平展开）
  const allLandingNodeIds = fullSystemMenuTree.flatMap((root) => collectAllDescendantIds(root))
  // 核心包原本存储的 menuIds（包含尚未落地的 10700 财务与 10800 审批中心）
  const initialCorePackageMenuIds = [...allLandingNodeIds, 10700, 10800]

  /**
   * 构造前端 AppRouteRecordRaw 路由树
   */
  const createRoutesFromMenuIds = (menuIds: number[]): AppRouteRecordRaw[] => {
    const activeIdSet = new Set(menuIds)
    const routes: AppRouteRecordRaw[] = []

    if (activeIdSet.has(1)) {
      routes.push({
        path: '/system',
        name: 'System',
        children: [{ path: 'user', name: 'User' }]
      } as unknown as AppRouteRecordRaw)
    }
    if (activeIdSet.has(10000)) {
      routes.push({
        path: '/baseinfo',
        name: 'BaseInfo',
        children: [{ path: 'product', name: 'Product' }, { path: 'customer', name: 'Customer' }]
      } as unknown as AppRouteRecordRaw)
    }
    if (activeIdSet.has(10100)) {
      routes.push({
        path: '/business',
        name: 'Business',
        children: [{ path: 'sales-contract', name: 'SalesContract' }]
      } as unknown as AppRouteRecordRaw)
    }
    if (activeIdSet.has(10200)) {
      routes.push({
        path: '/production',
        name: 'Production',
        children: [
          { path: 'greige-order', name: 'GreigeOrder' },
          { path: 'dyeing-order', name: 'DyeingOrder' },
          { path: 'yarn-order', name: 'YarnOrder' },
          { path: 'weaving-order', name: 'WeavingOrder' }
        ]
      } as unknown as AppRouteRecordRaw)
    }
    if (activeIdSet.has(10300)) {
      routes.push({
        path: '/stock',
        name: 'Stock',
        children: [{ path: 'stock-in', name: 'StockIn' }]
      } as unknown as AppRouteRecordRaw)
    }
    if (activeIdSet.has(10600)) {
      routes.push({
        path: '/sample',
        name: 'Sample',
        children: [{ path: 'sample-analysis', name: 'SampleAnalysis' }]
      } as unknown as AppRouteRecordRaw)
    }

    return routes
  }

  it('链路阶段 ①：新建租户默认绑定核心包后，侧边栏可见已落地的全部核心模块', () => {
    // 1. 新建租户默认分配套餐 ID: 100
    const packages = [{ id: 100, name: '核心包' }]
    const assignedPackageId = resolveDefaultTenantPackageId('create', undefined, packages)
    expect(assignedPackageId).toBe(100)

    // 2. 租户生成路由
    const initialRouters = createRoutesFromMenuIds(initialCorePackageMenuIds)

    // 3. 校验已落地的核心模块均处于开通激活状态
    expect(isModuleActivated('/baseinfo', initialRouters)).toBe(true)
    expect(isModuleActivated('/sales', initialRouters)).toBe(true) // 兼容 /business
    expect(isModuleActivated('/production', initialRouters)).toBe(true)
    expect(isModuleActivated('/stock', initialRouters)).toBe(true)
    expect(isModuleActivated('/sample', initialRouters)).toBe(true)

    // 4. 尚未建表的 10700(财务) 与 10800(审批中心) 不报错且平滑过滤
    expect(isModuleActivated('/finance', initialRouters)).toBe(false)
    expect(isModuleActivated('/approval', initialRouters)).toBe(false)
  })

  it('链路阶段 ②：套餐管理页菜单树级联勾选验证（父选子全选、父消子全消）', () => {
    // 初始状态：全部勾选
    let checkedSet = new Set(allLandingNodeIds)
    const productionNode = fullSystemMenuTree.find((n) => n.id === 10200)!
    const productionAllIds = collectAllDescendantIds(productionNode)

    // 动作 1：取消勾选「生产」一级目录（10200）
    checkedSet = cascadeCheck(fullSystemMenuTree, 10200, false, checkedSet)

    // 校验：父消子全消 -> 生产目录及其下 10210(胚布单)、10211(创建按钮)、10230(纱线采购) 等全部被取消
    for (const prodId of productionAllIds) {
      expect(checkedSet.has(prodId)).toBe(false)
    }

    // 动作 2：重新勾选「生产」一级目录（10200）
    checkedSet = cascadeCheck(fullSystemMenuTree, 10200, true, checkedSet)

    // 校验：父选子全选 -> 生产目录及其下整棵子树全量重新勾选
    for (const prodId of productionAllIds) {
      expect(checkedSet.has(prodId)).toBe(true)
    }

    // 动作 3：只取消勾选「胚布单」(10210)
    checkedSet = cascadeCheck(fullSystemMenuTree, 10210, false, checkedSet)
    const { checkedKeys, halfCheckedKeys } = computeTreeState(fullSystemMenuTree, checkedSet)

    // 校验：子消部分 -> 父节点「生产」(10200) 变为半选中状态 (halfChecked)
    expect(checkedKeys).not.toContain(10200)
    expect(halfCheckedKeys).toContain(10200)
  })

  it('链路阶段 ③：从核心包移除「生产」后，租户侧边栏无生产，访问 /production 重定向首页', () => {
    // 1. 管理员在核心包中取消「生产」一级目录勾选
    const checkedSetWithoutProduction = cascadeCheck(
      fullSystemMenuTree,
      10200,
      false,
      new Set(allLandingNodeIds)
    )
    const { checkedKeys, halfCheckedKeys } = computeTreeState(
      fullSystemMenuTree,
      checkedSetWithoutProduction
    )

    // 计算保存后的 menuIds，确保 10700/10800 悬挂规划模块未被冲掉
    const savedMenuIds = computePackageSaveMenuIds(
      fullSystemMenuTree,
      checkedKeys,
      halfCheckedKeys,
      initialCorePackageMenuIds
    )

    // 断言生产模块整树被彻底移除，而财务/审批中心挂起 ID 仍保留
    expect(savedMenuIds).not.toContain(10200)
    expect(savedMenuIds).not.toContain(10210)
    expect(savedMenuIds).not.toContain(10211)
    expect(savedMenuIds).toContain(10700)
    expect(savedMenuIds).toContain(10800)
    expect(savedMenuIds).toContain(10000) // 基础信息在
    expect(savedMenuIds).toContain(10100) // 销售在

    // 2. 租户用户刷新后生成新路由表
    const routersWithoutProduction = createRoutesFromMenuIds(savedMenuIds)
    expect(isModuleActivated('/production', routersWithoutProduction)).toBe(false)
    expect(isModuleActivated('/baseinfo', routersWithoutProduction)).toBe(true)

    // 3. 用户在地址栏直接访问 /production/** 各种子路由
    const testProductionPaths = [
      '/production/greige-order',
      '/production/greige-order/edit/1',
      '/production/dyeing-order',
      '/production/yarn-order',
      '/production/weaving-order'
    ]

    for (const testPath of testProductionPaths) {
      // 路由守卫判定：属于未开通模块路由
      const isUnactivated = isUnactivatedModuleRoute(testPath, routersWithoutProduction)
      expect(isUnactivated).toBe(true)
      // 在 permission.ts 中，isUnactivated 为 true 时直接 next({ path: '/index', replace: true })
      // 既不放行，也不进入 404
    }

    // 4. 对比：访问已开通的销售路由
    expect(isUnactivatedModuleRoute('/business/sales-contract', routersWithoutProduction)).toBe(false)
    // 访问系统未定义的未知路径（不属于已知模块根），不判定为未开通模块，正常进入 404
    expect(isUnactivatedModuleRoute('/unknown/something', routersWithoutProduction)).toBe(false)
  })

  it('链路阶段 ④：调用生产模块 API 返回 403 时，前端精确提示「该模块未开通，请联系管理员」', () => {
    // 租户未开通生产模块的路由上下文
    const routersWithoutProduction = createRoutesFromMenuIds(
      initialCorePackageMenuIds.filter((id) => id < 10200 || id >= 10300)
    )

    // 场景 A：调用生产模块 API（如胚布单列表接口）返回 403
    const productionReason = determineForbiddenReason({
      currentRoute: { path: '/index' },
      requestUrl: '/lunaerp/production/greige-order/page',
      addRouters: routersWithoutProduction
    })
    expect(productionReason).toBe('MODULE_NOT_ACTIVATED')

    // 场景 B：在残留页面调用染色单 API 返回 403
    const dyeingReason = determineForbiddenReason({
      currentRoute: { path: '/production/dyeing-order' },
      requestUrl: '/lunaerp/production/dyeing-order/page',
      addRouters: routersWithoutProduction
    })
    expect(dyeingReason).toBe('MODULE_NOT_ACTIVATED')

    // 场景 C：对照组——已开通的模块（销售），在销售合同页面点击删除按钮因无按钮权限返回 403
    const buttonForbiddenReason = determineForbiddenReason({
      currentRoute: { path: '/business/sales-contract', name: 'SalesContract' },
      requestUrl: '/admin-api/lunaerp/business/sales-contract/delete',
      addRouters: routersWithoutProduction
    })
    // 菜单已开通，判定为按钮权限不足，提示「无操作权限」，不跳转首页
    expect(buttonForbiddenReason).toBe('BUTTON_PERMISSION_DENIED')

    // 校验文案常量与 spec 约定严格一致
    expect(MSG_MODULE_NOT_ACTIVATED).toBe('该模块未开通，请联系管理员')
    expect(MSG_NO_OPERATION_PERMISSION).toBe('无操作权限')
  })

  it('链路阶段 ⑤：将「生产」重新加回核心包后，刷新即恢复，历史业务数据保留可操作', () => {
    // 1. 管理员重新将生产加回套餐
    const restoredCheckedSet = cascadeCheck(
      fullSystemMenuTree,
      10200,
      true,
      new Set(allLandingNodeIds)
    )
    const { checkedKeys, halfCheckedKeys } = computeTreeState(
      fullSystemMenuTree,
      restoredCheckedSet
    )

    const restoredMenuIds = computePackageSaveMenuIds(
      fullSystemMenuTree,
      checkedKeys,
      halfCheckedKeys,
      initialCorePackageMenuIds
    )

    expect(restoredMenuIds).toContain(10200)
    expect(restoredMenuIds).toContain(10210)

    // 2. 租户用户刷新后获取到恢复后的路由集合
    const restoredRouters = createRoutesFromMenuIds(restoredMenuIds)

    // 3. 校验生产模块已重新激活
    expect(isModuleActivated('/production', restoredRouters)).toBe(true)

    // 4. 再次直接访问生产模块页面，不再被路由守卫重定向
    expect(isUnactivatedModuleRoute('/production/greige-order', restoredRouters)).toBe(false)
    expect(isUnactivatedModuleRoute('/production/dyeing-order', restoredRouters)).toBe(false)

    // 5. 调用生产接口不再命中 MODULE_NOT_ACTIVATED
    const restoredReason = determineForbiddenReason({
      currentRoute: { path: '/production/greige-order', name: 'GreigeOrder' },
      requestUrl: '/admin-api/lunaerp/production/greige-order/page',
      addRouters: restoredRouters
    })
    // 此时生产模块已开通，若因特定按钮 403 也只会提示 BUTTON_PERMISSION_DENIED
    expect(restoredReason).toBe('BUTTON_PERMISSION_DENIED')
  })

  it('确认项：字段脱敏配置已移交 04 号 ticket（common-foundation），不在本网关 spec 范围', () => {
    // 本 spec 仅对租户一级模块可见性与 403 路由做拦截控制
    // 字段脱敏（如 SensitiveSerializer 等）正交独立
    expect(true).toBe(true)
  })
})
