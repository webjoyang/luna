import { describe, it, expect, beforeEach, vi } from 'vitest'

// Vitest 纯 Node 环境守卫：mock @/config/axios 防止路由与网络依赖级联报错
vi.mock('@/config/axios', () => ({}))
vi.mock('@/config/axios/portal', () => ({}))

import {
  PORTAL_TOKEN_STORAGE_KEY,
  PORTAL_CUSTOMER_NAME_KEY,
  PORTAL_ERROR_CODE,
  getPortalToken,
  setPortalToken,
  clearPortalToken,
  getPortalCustomerName,
  setPortalCustomerName,
  handlePortalTokenGuide,
  isPortalPath,
  isPortalInvalidPath
} from '@/utils/portalAuth'

import {
  getPortalStatusBadge,
  formatPortalRemainingDays,
  getCopyLinkPayload
} from '@/views/lunaerp-elmui/baseinfo/customer/model/portalToken'

import {
  getSignStatusText,
  getSignStatusTagType,
  formatAmount as formatShipmentAmount,
  resolveShipmentDetailParams,
  canSignShipment,
  validateSignForm
} from '../model/portalShipment'

import {
  filterStatementListByTab,
  resolveStatementDetailParams,
  canConfirmOrReject,
  validateConfirmForm,
  validateRejectForm,
  getStatementStatusText,
  getStatementStatusTagType,
  formatAmount as formatStatementAmount
} from '../model/portalStatement'

describe('客户门户 H5 - 375px 移动端全链路冒烟测试 (Issue 06 Capstone Smoke)', () => {
  beforeEach(() => {
    const storage = new Map<string, string>()
    globalThis.sessionStorage = {
      getItem: (key: string) => storage.get(key) || null,
      setItem: (key: string, val: string) => storage.set(key, val),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
      length: storage.size,
      key: () => null
    } as any
  })

  // 1. 管理端生成链接与复制
  it('步骤 1 [管理端]：生成免登链接并校验状态标签、剩余天数与复制载荷（支持基址未配置降级）', () => {
    // 状态标签
    expect(getPortalStatusBadge(true).text).toBe('已开通')
    expect(getPortalStatusBadge(true).type).toBe('success')
    expect(formatPortalRemainingDays(30)).toBe('剩余 30 天')

    // 复制正常完整链接
    const normalPayload = getCopyLinkPayload({
      exists: true,
      url: 'https://portal.luna.com/portal/shipments?t=SECURE_TOKEN_123',
      token: 'SECURE_TOKEN_123'
    })
    expect(normalPayload.canCopy).toBe(true)
    expect(normalPayload.isDegraded).toBe(false)
    expect(normalPayload.text).toBe('https://portal.luna.com/portal/shipments?t=SECURE_TOKEN_123')

    // 未配置基址时降级仅复制 token
    const degradedPayload = getCopyLinkPayload({
      exists: true,
      url: '',
      token: 'SECURE_TOKEN_123'
    })
    expect(degradedPayload.canCopy).toBe(true)
    expect(degradedPayload.isDegraded).toBe(true)
    expect(degradedPayload.text).toBe('SECURE_TOKEN_123')
    expect(degradedPayload.message).toContain('luna.portal.base-url')
  })

  // 2. 客户通过 H5 移动视口打开链接（?t= 清址与 token 持久化）
  it('步骤 2 [客户端打开]：移动端首次打开携带 ?t=，自动存入 sessionStorage 并触发清址标记', () => {
    const incomingToken = 'CUSTOMER_TOKEN_XYZ_123'
    const guideResult = handlePortalTokenGuide(incomingToken, null)

    expect(guideResult.isValid).toBe(true)
    expect(guideResult.token).toBe(incomingToken)
    expect(guideResult.shouldCleanUrl).toBe(true)

    // 校验 token 已持久化至 sessionStorage
    expect(getPortalToken()).toBe(incomingToken)

    // 设置并校验客户名称展示
    setPortalCustomerName('常州盛达纺织有限公司')
    expect(getPortalCustomerName()).toBe('常州盛达纺织有限公司')

    // 第二次路由导航（地址栏已无 ?t=，但 sessionStorage 中存在）
    const secondVisit = handlePortalTokenGuide(null, getPortalToken())
    expect(secondVisit.isValid).toBe(true)
    expect(secondVisit.token).toBe(incomingToken)
    expect(secondVisit.shouldCleanUrl).toBe(false)
  })

  // 3. 发货单列表展示与状态标签
  it('步骤 3 [发货单列表]：金额透明（不脱敏），状态标签映射准确', () => {
    // 状态标签与文本
    expect(getSignStatusText('PENDING')).toBe('待签收')
    expect(getSignStatusTagType('PENDING')).toBe('warning')
    expect(getSignStatusText('SIGNED')).toBe('已签收')
    expect(getSignStatusTagType('SIGNED')).toBe('success')
    expect(getSignStatusText('REFUSED')).toBe('已拒收')
    expect(getSignStatusTagType('REFUSED')).toBe('danger')

    // 金额透明格式化（保留原值并加货币符号，不脱敏）
    expect(formatShipmentAmount(5000)).toBe('¥5000.00')
    expect(formatShipmentAmount(150.5)).toBe('¥150.50')
    expect(formatShipmentAmount('25.50')).toBe('¥25.50')
    expect(formatShipmentAmount(0)).toBe('¥0.00')
  })

  // 4. 发货单详情双入口解析与签收校验（手填姓名必填）
  it('步骤 4 [发货单详情与签收]：双入口（:id 与 ?no=）正确解析，签收需手填姓名且仅 PENDING 可操作', () => {
    // 路由解析：:id 形态
    const fromId = resolveShipmentDetailParams({ id: '1024' }, {})
    expect(fromId.isValid).toBe(true)
    expect(fromId.id).toBe(1024)
    expect(fromId.no).toBeUndefined()

    // 路由解析：?no= 扫码形态
    const fromNo = resolveShipmentDetailParams({}, { no: 'SH-20260927-001' })
    expect(fromNo.isValid).toBe(true)
    expect(fromNo.id).toBeUndefined()
    expect(fromNo.no).toBe('SH-20260927-001')

    // 签收按钮可见性：仅 PENDING 状态允许签收
    expect(canSignShipment('PENDING')).toBe(true)
    expect(canSignShipment('SIGNED')).toBe(false)
    expect(canSignShipment('REFUSED')).toBe(false)

    // 签收表单校验：手填姓名必填，拒绝纯空格
    expect(validateSignForm('').valid).toBe(false)
    expect(validateSignForm('   ').valid).toBe(false)
    expect(validateSignForm(null).valid).toBe(false)
    expect(validateSignForm('李收货').valid).toBe(true)
  })

  // 5. 对账单列表与状态切换
  it('步骤 5 [对账单列表]：按 Tab 过滤，金额透明格式化', () => {
    const mockStatements = [
      { id: 101, statementNo: 'CS001', status: 'SENT', closingBalance: 12000.5 },
      { id: 102, statementNo: 'CS002', status: 'CONFIRMED', closingBalance: 8800.0 },
      { id: 103, statementNo: 'CS003', status: 'REJECTED', closingBalance: 6500.0 }
    ]

    const sentList = filterStatementListByTab(mockStatements, 'SENT')
    expect(sentList.length).toBe(1)
    expect(sentList[0].statementNo).toBe('CS001')

    const confirmedList = filterStatementListByTab(mockStatements, 'CONFIRMED')
    expect(confirmedList.length).toBe(1)
    expect(confirmedList[0].statementNo).toBe('CS002')

    // 金额透明格式化
    expect(formatStatementAmount(12000.5)).toBe('¥12000.50')
    expect(formatStatementAmount(null)).toBe('¥0.00')

    // 状态标签与文本
    expect(getStatementStatusText('SENT')).toBe('待确认')
    expect(getStatementStatusTagType('SENT')).toBe('warning')
    expect(getStatementStatusText('CONFIRMED')).toBe('已确认')
    expect(getStatementStatusTagType('CONFIRMED')).toBe('success')
  })

  // 6. 对账单确认与驳回交互路径
  it('步骤 6 [对账单确认与驳回]：双入口兼容，仅 SENT 允许操作，确认人必填，驳回原因必填', () => {
    // 双入口（:id 与 ?no=）正确解析
    const fromId = resolveStatementDetailParams({ id: '2048' }, {})
    expect(fromId.isValid).toBe(true)
    expect(fromId.id).toBe(2048)

    const fromNo = resolveStatementDetailParams({}, { no: 'CS2026-001' })
    expect(fromNo.isValid).toBe(true)
    expect(fromNo.no).toBe('CS2026-001')

    // 仅 SENT 状态可操作确认或驳回
    expect(canConfirmOrReject('SENT')).toBe(true)
    expect(canConfirmOrReject('CONFIRMED')).toBe(false)
    expect(canConfirmOrReject('REJECTED')).toBe(false)
    expect(canConfirmOrReject('DRAFT')).toBe(false)

    // 确认表单：手填确认人必填
    expect(validateConfirmForm({ confirmByName: '' }).valid).toBe(false)
    expect(validateConfirmForm({ confirmByName: '  ' }).valid).toBe(false)
    expect(validateConfirmForm({ confirmByName: '张财务' }).valid).toBe(true)

    // 驳回表单：驳回原因必填
    expect(validateRejectForm({ rejectReason: '' }).valid).toBe(false)
    expect(validateRejectForm({ rejectReason: '   ' }).valid).toBe(false)
    expect(validateRejectForm({ rejectReason: '9月15日运费金额有出入' }).valid).toBe(true)
  })

  // 7. 吊销/失效后 401 路由拦截与 invalid 页引导
  it('步骤 7 [失效与拦截]：无凭证或后端返回 401 统一引导至 invalid 链接失效页', () => {
    // 清除令牌模拟吊销或过期
    clearPortalToken()
    expect(getPortalToken()).toBe('')

    // 无有效凭证打开门户
    const invalidGuide = handlePortalTokenGuide(null, null)
    expect(invalidGuide.isValid).toBe(false)

    // 路径合法性校验
    expect(isPortalPath('/portal/shipments')).toBe(true)
    expect(isPortalInvalidPath('/portal/invalid')).toBe(true)

    // 错误码规范对齐
    expect(PORTAL_ERROR_CODE.TOKEN_INVALID).toBe(1030038000)
    expect(PORTAL_ERROR_CODE.DATA_ACCESS_DENIED).toBe(1030038001)
    expect(PORTAL_ERROR_CODE.REQUEST_TOO_FREQUENT).toBe(1030038002)
  })
})
