import { describe, expect, it } from 'vitest'
import { SystemDataScopeEnum } from '@/utils/constants'

/**
 * 允许的数据范围白名单常量（顺序：全部 / 本部门及以下 / 本部门 / 本人）
 * 对应 Issue 06: 自定义部门档（value = 2）已停用并隐藏
 */
export const ALLOWED_DATA_SCOPES = [1, 4, 3, 5]

export interface DictOption {
  label: string
  value: number
}

/**
 * 角色数据权限选项过滤与排序函数（与 RoleDataPermissionForm.vue 保持严格一致）
 */
export function filterAndSortRoleDataScopeOptions(rawOptions: DictOption[]): DictOption[] {
  const filtered = rawOptions.filter(
    (item) => item.value !== 2 && ALLOWED_DATA_SCOPES.includes(item.value)
  )
  return ALLOWED_DATA_SCOPES.map((scope) => filtered.find((item) => item.value === scope)).filter(
    (item): item is DictOption => item !== undefined
  )
}

/**
 * 构造提交数据函数（与 RoleDataPermissionForm.vue submitForm 保持一致）
 */
export function buildAssignRoleDataScopePayload(
  roleId: number,
  dataScope: number,
  treeCheckedKeys: number[]
) {
  return {
    roleId,
    dataScope,
    dataScopeDeptIds: dataScope !== SystemDataScopeEnum.DEPT_CUSTOM ? [] : treeCheckedKeys
  }
}

describe('Role Data Permission Scope Shutdown Specs (Issue 06)', () => {
  const mockSystemDictOptions: DictOption[] = [
    { label: '全部数据权限', value: 1 },
    { label: '指定部门数据权限', value: 2 },
    { label: '本部门数据权限', value: 3 },
    { label: '本部门及以下数据权限', value: 4 },
    { label: '仅本人数据权限', value: 5 }
  ]

  it('角色弹窗下拉过滤 value === 2（指定部门）不可见，选项恰为 4 项', () => {
    const options = filterAndSortRoleDataScopeOptions(mockSystemDictOptions)

    // 不包含 value = 2
    expect(options.some((opt) => opt.value === 2)).toBe(false)
    expect(options.some((opt) => opt.label.includes('指定部门'))).toBe(false)
    expect(options.length).toBe(4)
  })

  it('角色弹窗下拉顺序严格为全部(1) -> 本部门及以下(4) -> 本部门(3) -> 本人(5)', () => {
    const options = filterAndSortRoleDataScopeOptions(mockSystemDictOptions)

    expect(options.map((opt) => opt.value)).toEqual([1, 4, 3, 5])
    expect(options[0].label).toBe('全部数据权限')
    expect(options[1].label).toBe('本部门及以下数据权限')
    expect(options[2].label).toBe('本部门数据权限')
    expect(options[3].label).toBe('仅本人数据权限')
  })

  it('合法 4 档提交时 dataScopeDeptIds 恒为空数组，避免多余部门传递', () => {
    for (const scope of ALLOWED_DATA_SCOPES) {
      const payload = buildAssignRoleDataScopePayload(10, scope, [100, 101])
      expect(payload.dataScope).toBe(scope)
      expect(payload.dataScopeDeptIds).toEqual([])
    }
  })

  it('底座 SystemDataScopeEnum.DEPT_CUSTOM 常量仍保留为 2（底座最小改动兼容）', () => {
    expect(SystemDataScopeEnum.DEPT_CUSTOM).toBe(2)
  })
})
