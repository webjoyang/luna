import { describe, it, expect } from 'vitest'
import { parseNotifyParams, getNotifyJumpRoute, registerBizTypeRoute } from '../notifyRoute'

describe('notifyRoute 工具测试', () => {
  describe('parseNotifyParams 参数解析', () => {
    it('参数为空或 undefined 时返回空对象', () => {
      expect(parseNotifyParams(null)).toEqual({})
      expect(parseNotifyParams(undefined)).toEqual({})
      expect(parseNotifyParams('')).toEqual({})
    })

    it('标准 JSON 字符串正常解析', () => {
      const json = JSON.stringify({ bizType: 'APPROVAL', bizId: 1001, bizNo: 'SC-001' })
      expect(parseNotifyParams(json)).toEqual({
        bizType: 'APPROVAL',
        bizId: 1001,
        bizNo: 'SC-001'
      })
    })

    it('非法 JSON 字符串容错返回空对象', () => {
      expect(parseNotifyParams('{invalid-json')).toEqual({})
    })

    it('对象类型直接返回', () => {
      const obj = { bizType: 'APPROVAL', bizId: 2001 }
      expect(parseNotifyParams(obj)).toEqual(obj)
    })
  })

  describe('getNotifyJumpRoute 路由分发', () => {
    it('审批类消息 (bizType=APPROVAL) 跳转至 /approval/todo 并携带 instanceId 和 bizNo', () => {
      const params = { bizType: 'APPROVAL', bizId: 5001, bizNo: 'SC-2026-001' }
      const route = getNotifyJumpRoute(params)
      expect(route).not.toBeNull()
      expect(route?.path).toBe('/approval/todo')
      expect(route?.query?.instanceId).toBe('5001')
      expect(route?.query?.id).toBe('5001')
      expect(route?.query?.bizNo).toBe('SC-2026-001')
    })

    it('审批类大小写容错 (bizType=approval)', () => {
      const params = { bizType: 'approval', bizId: 6001 }
      const route = getNotifyJumpRoute(params)
      expect(route).not.toBeNull()
      expect(route?.path).toBe('/approval/todo')
      expect(route?.query?.instanceId).toBe('6001')
    })

    it('无路由约定的业务类型返回 null，以便前端展示详情弹窗', () => {
      const params = { bizType: 'UNKNOWN_BIZ', bizId: 1 }
      expect(getNotifyJumpRoute(params)).toBeNull()
      expect(getNotifyJumpRoute({})).toBeNull()
    })

    it('支持扩展注册自定义 bizType 路由', () => {
      registerBizTypeRoute('CUSTOM_MODULE', (p) => ({
        path: '/custom/view',
        query: { no: String(p.bizNo) }
      }))

      const route = getNotifyJumpRoute({ bizType: 'CUSTOM_MODULE', bizNo: 'CM-01' })
      expect(route).toEqual({
        path: '/custom/view',
        query: { no: 'CM-01' }
      })
    })
  })
})
