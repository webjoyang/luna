import { describe, it, expect } from 'vitest'
import { buildTaskPageQuery } from '../helper'

describe('排产中心跨单任务列表 (Issue 08)', () => {
  it('① 跨单查询参数装配：不限定 weavingOrderId，天然支持多单分页', () => {
    const query = buildTaskPageQuery({
      pageNo: 1,
      pageSize: 20
    })
    expect(query.pageNo).toBe(1)
    expect(query.pageSize).toBe(20)
    // 保证不强制注入 weavingOrderId
    expect((query as any).weavingOrderId).toBeUndefined()
  })

  it('② conflictOnly 只看冲突红标过滤生效', () => {
    const query = buildTaskPageQuery({
      pageNo: 1,
      pageSize: 10,
      conflictOnly: true
    })
    expect(query.conflictOnly).toBe(true)

    const queryFalse = buildTaskPageQuery({
      pageNo: 1,
      pageSize: 10,
      conflictOnly: false
    })
    expect(queryFalse.conflictOnly).toBeUndefined()
  })

  it('③ 织机、状态与关键词筛选正确传递', () => {
    const query = buildTaskPageQuery({
      pageNo: 2,
      pageSize: 15,
      loomId: 3,
      status: 'IN_PROGRESS',
      code: '  MT-01  '
    })
    expect(query.loomId).toBe(3)
    expect(query.status).toBe('IN_PROGRESS')
    expect(query.code).toBe('MT-01')
  })

  it('④ 空筛选值规范化为 undefined', () => {
    const query = buildTaskPageQuery({
      pageNo: 1,
      pageSize: 10,
      loomId: undefined,
      status: '',
      code: '   '
    })
    expect(query.loomId).toBeUndefined()
    expect(query.status).toBeUndefined()
    expect(query.code).toBeUndefined()
  })
})
