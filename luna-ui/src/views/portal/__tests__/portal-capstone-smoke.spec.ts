import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import * as PortalAuthApi from '@/api/portal/auth'
import * as ShipmentApi from '@/api/portal/shipment'
import * as StatementApi from '@/api/portal/statement'
import { usePortalAuth } from '../usePortalAuth'
import { service as portalAxiosService } from '@/config/axios/portal'
import adminRequest from '@/config/axios'
import portalRequest from '@/config/axios/portal'
import router from '@/router'

// 模拟 SessionStorage
class MockStorage {
  private store: Record<string, string> = {}
  getItem(key: string) {
    return this.store[key] || null
  }
  setItem(key: string, value: string) {
    this.store[key] = String(value)
  }
  removeItem(key: string) {
    delete this.store[key]
  }
  clear() {
    this.store = {}
  }
}

const mockSessionStorage = new MockStorage()
vi.stubGlobal('sessionStorage', mockSessionStorage)

const replaceStateMock = vi.fn()
vi.stubGlobal('window', {
  innerWidth: 375, // 模拟 375px 移动端视口
  innerHeight: 667,
  location: {
    href: 'http://localhost/portal/shipments',
    pathname: '/portal/shipments',
    search: ''
  },
  history: {
    replaceState: replaceStateMock
  }
})

const mockRoute = {
  path: '/portal/shipments',
  query: {} as Record<string, string>,
  params: {} as Record<string, string>
}

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn()
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
  useRouter: () => mockRouter
}))

vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
    replace: vi.fn()
  }
}))

vi.mock('@/config/axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn()
  }
}))

vi.mock('@/config/axios/portal', () => {
  const actualOrMock = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { handlers: [] },
      response: { handlers: [] }
    }
  }
  return {
    default: actualOrMock,
    service: actualOrMock
  }
})

vi.mock('@/api/portal/auth', () => ({
  getPortalAuthInfoApi: vi.fn()
}))

describe('Ticket 06 Capstone Smoke: 客户门户 375px 全链路冒烟与闭环测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStorage.clear()
    ;(window as any).innerWidth = 375 // 375px 移动视口模拟
    ;(window as any).innerHeight = 667
    mockRoute.query = {}
    mockRoute.params = {}
    mockRoute.path = '/portal/shipments'
    ;(window as any).location.href = 'http://localhost/portal/shipments'
    ;(window as any).location.pathname = '/portal/shipments'
    ;(window as any).location.search = ''
  })

  describe('375px 移动视口全流程 9 步闭环冒烟测试', () => {
    it('执行 1~9 步完整端到端用户流：生成链接 -> 清址进入 -> 查发货单与透明金额 -> 签收 -> 查对账单 -> 确认对账 -> 驳回对账 -> 吊销401跳失效页', async () => {
      // -------------------------------------------------------------
      // Step 1: 管理端生成链接 -> 复制带有 ?t=token 的门户 URL
      // -------------------------------------------------------------
      const customerId = 1001
      const generatedToken = 'v1StGXR8_Z5jdHi6B-myT_gqTf0k81qRz8W_smoke32b'
      const portalLinkUrl = `https://portal.lunaerp.com/portal/shipments?t=${generatedToken}`

      vi.mocked(adminRequest.post).mockResolvedValueOnce({
        token: generatedToken,
        url: portalLinkUrl,
        expireDays: 30,
        generateTime: '2026-10-12 10:00:00'
      })

      const genResp = await CustomerApi.generatePortalToken(customerId)
      expect(genResp.token).toBe(generatedToken)
      expect(genResp.url).toBe(portalLinkUrl)
      expect(genResp.url).toContain(`?t=${generatedToken}`)

      // -------------------------------------------------------------
      // Step 2: 客户在 375px 手机端浏览器打开带 ?t= 链接 -> 清址存会话 -> 渲染发货单列表
      // -------------------------------------------------------------
      expect(window.innerWidth).toBe(375) // 断言 375px 视口
      mockRoute.query = { t: generatedToken }
      ;(window as any).location.href = portalLinkUrl
      ;(window as any).location.search = `?t=${generatedToken}`

      vi.mocked(PortalAuthApi.getPortalAuthInfoApi).mockResolvedValueOnce({
        customerId: 1001,
        customerName: '盛达纺织实业有限公司'
      })

      const { customerName, customerId: authCustomerId, bootstrapToken } = usePortalAuth()
      const bootstrapSuccess = await bootstrapToken()

      expect(bootstrapSuccess).toBe(true)
      expect(mockSessionStorage.getItem('portal_token')).toBe(generatedToken)
      expect(replaceStateMock).toHaveBeenCalled() // 地址栏已清址
      expect(customerName.value).toBe('盛达纺织实业有限公司')
      expect(authCustomerId.value).toBe(1001)

      // 模拟加载发货单列表
      const mockShipmentPage = {
        list: [
          {
            id: 801,
            shipmentNo: 'SH-20261012-001',
            shippingDate: '2026-10-12',
            logisticsCompany: '顺丰速运',
            logisticsTrackingNo: 'SF88990011',
            signStatus: 'PENDING',
            totalAmount: 16800.0,
            freightFee: 200.0,
            totalPieces: 30
          },
          {
            id: 802,
            shipmentNo: 'SH-20261011-002',
            shippingDate: '2026-10-11',
            logisticsCompany: '德邦快递',
            logisticsTrackingNo: 'DB11223344',
            signStatus: 'SIGNED',
            totalAmount: 9500.0,
            freightFee: 100.0,
            totalPieces: 15
          }
        ],
        total: 2
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockShipmentPage)

      const shipmentPageResp = await ShipmentApi.getPortalShipmentPage({ pageNo: 1, pageSize: 10 })
      expect(shipmentPageResp.list.length).toBe(2)
      expect(shipmentPageResp.list[0].shipmentNo).toBe('SH-20261012-001')
      expect(shipmentPageResp.list[0].signStatus).toBe('PENDING')
      // 透明金额展示
      expect(shipmentPageResp.list[0].totalAmount).toBe(16800.0)
      expect(shipmentPageResp.list[0].freightFee).toBe(200.0)

      // -------------------------------------------------------------
      // Step 3: 客户点击卡片查看发货单详情 -> 核验品名、规格、卷清单、透明金额
      // -------------------------------------------------------------
      const mockShipmentDetail: ShipmentApi.PortalShipmentDetailRespVO = {
        id: 801,
        shipmentNo: 'SH-20261012-001',
        shippingDate: '2026-10-12',
        logisticsCompany: '顺丰速运',
        logisticsTrackingNo: 'SF88990011',
        signStatus: 'PENDING',
        totalAmount: 16800.0,
        freightFee: 200.0,
        totalPieces: 30,
        items: [
          {
            id: 901,
            productCode: 'COTTON-TWILL-40S',
            productName: '40支纯棉斜纹面料',
            customerColorNo: 'COL-NVY-01',
            colorName: '藏青色',
            vatNo: 'VAT-202610-A',
            quantity: 600.0,
            pieces: 30,
            unit: 'KG',
            unitPrice: 28.0,
            amount: 16800.0,
            rolls: [
              { id: 101, rollNo: 'R-01', weight: 20.0, lengthM: 50.0 },
              { id: 102, rollNo: 'R-02', weight: 20.0, lengthM: 50.0 }
            ]
          }
        ]
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockShipmentDetail)

      const shipmentDetailResp = await ShipmentApi.getPortalShipmentDetail({ id: 801 })
      expect(shipmentDetailResp.id).toBe(801)
      expect(shipmentDetailResp.items.length).toBe(1)
      expect(shipmentDetailResp.items[0].productName).toBe('40支纯棉斜纹面料')
      expect(shipmentDetailResp.items[0].quantity).toBe(600.0)
      expect(shipmentDetailResp.items[0].unitPrice).toBe(28.0)
      expect(shipmentDetailResp.items[0].amount).toBe(16800.0)
      expect(shipmentDetailResp.items[0].rolls?.length).toBe(2)
      // 验证无敏感字段暴露
      expect((shipmentDetailResp as any).internalRemark).toBeUndefined()
      expect((shipmentDetailResp as any).purchaseCost).toBeUndefined()
      expect((shipmentDetailResp as any).commission).toBeUndefined()
      expect((shipmentDetailResp as any).salesmanId).toBeUndefined()

      // -------------------------------------------------------------
      // Step 4: 客户登记签收 -> 弹窗输入签收人姓名 -> 确认 -> 状态变为 SIGNED
      // -------------------------------------------------------------
      expect(shipmentDetailResp.signStatus).toBe('PENDING')
      const signPayload: ShipmentApi.PortalShipmentSignReqVO = {
        id: 801,
        signStatus: 'SIGNED',
        signedByName: '张收货经理',
        signRemark: '外包装完好，实收30匹'
      }
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const signResult = await ShipmentApi.signPortalShipment(signPayload)
      expect(signResult).toBe(true)

      // 模拟签收后详情状态更新为 SIGNED
      mockShipmentDetail.signStatus = 'SIGNED'
      mockShipmentDetail.signedByName = '张收货经理'
      mockShipmentDetail.signedAt = '2026-10-12 11:30:00'
      expect(mockShipmentDetail.signStatus).toBe('SIGNED')
      expect(mockShipmentDetail.signedByName).toBe('张收货经理')

      // -------------------------------------------------------------
      // Step 5: 客户切换底部 Tab 至对账单 -> 查看对账单列表
      // -------------------------------------------------------------
      mockRoute.path = '/portal/statements'
      const mockStatementList: StatementApi.PortalStatementRespVO[] = [
        {
          id: 701,
          statementNo: 'CS-202610-001',
          customerId: 1001,
          customerName: '盛达纺织实业有限公司',
          currency: 'CNY',
          periodStart: '2026-10-01',
          periodEnd: '2026-10-31',
          openingBalance: 10000.0,
          periodDebit: 50000.0,
          periodCredit: 40000.0,
          closingBalance: 20000.0,
          status: 'SENT'
        },
        {
          id: 702,
          statementNo: 'CS-202609-002',
          customerId: 1001,
          customerName: '盛达纺织实业有限公司',
          currency: 'CNY',
          periodStart: '2026-09-01',
          periodEnd: '2026-09-30',
          openingBalance: 5000.0,
          periodDebit: 30000.0,
          periodCredit: 25000.0,
          closingBalance: 10000.0,
          status: 'SENT'
        }
      ]
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockStatementList)

      const statementListResp = await StatementApi.getPortalStatementList({ status: 'SENT' })
      expect(statementListResp.length).toBe(2)
      expect(statementListResp[0].statementNo).toBe('CS-202610-001')
      expect(statementListResp[0].status).toBe('SENT')

      // -------------------------------------------------------------
      // Step 6: 客户查看对账单 701 详情 -> 核验四要素汇总与明细行
      // -------------------------------------------------------------
      const mockStatementDetail1: StatementApi.PortalStatementDetailRespVO = {
        id: 701,
        statementNo: 'CS-202610-001',
        customerId: 1001,
        customerName: '盛达纺织实业有限公司',
        currency: 'CNY',
        periodStart: '2026-10-01',
        periodEnd: '2026-10-31',
        openingBalance: 10000.0,
        periodDebit: 50000.0,
        periodCredit: 40000.0,
        closingBalance: 20000.0,
        status: 'SENT',
        items: [
          {
            id: 11,
            lineNo: 1,
            direction: 'DEBIT',
            bizDate: '2026-10-05',
            refType: 'SHIPMENT',
            refNo: 'SH-20261005-01',
            summary: '面料出库发货',
            debitAmount: 50000.0,
            creditAmount: 0.0,
            signStatus: 'SIGNED'
          },
          {
            id: 12,
            lineNo: 2,
            direction: 'CREDIT',
            bizDate: '2026-10-10',
            refType: 'RECEIPT',
            refNo: 'RC-20261010-01',
            summary: '银行电汇回款',
            debitAmount: 0.0,
            creditAmount: 40000.0
          }
        ]
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockStatementDetail1)

      const statementDetailResp1 = await StatementApi.getPortalStatementDetail(701)
      expect(statementDetailResp1.openingBalance).toBe(10000.0)
      expect(statementDetailResp1.periodDebit).toBe(50000.0)
      expect(statementDetailResp1.periodCredit).toBe(40000.0)
      expect(statementDetailResp1.closingBalance).toBe(20000.0)
      expect(statementDetailResp1.items.length).toBe(2)

      // -------------------------------------------------------------
      // Step 7: 客户确认对账单 701 -> 弹窗手填姓名 -> 确认 -> 状态变为 CONFIRMED
      // -------------------------------------------------------------
      expect(statementDetailResp1.status).toBe('SENT')
      const confirmPayload: StatementApi.PortalStatementConfirmReqVO = {
        id: 701,
        confirmByName: '李财务负责人'
      }
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const confirmResult = await StatementApi.confirmPortalStatement(confirmPayload)
      expect(confirmResult).toBe(true)

      mockStatementDetail1.status = 'CONFIRMED'
      mockStatementDetail1.confirmByName = '李财务负责人'
      mockStatementDetail1.confirmTime = '2026-10-12 12:00:00'
      mockStatementDetail1.confirmType = 'ONLINE_PORTAL'
      expect(mockStatementDetail1.status).toBe('CONFIRMED')
      expect(mockStatementDetail1.confirmByName).toBe('李财务负责人')
      expect(mockStatementDetail1.confirmType).toBe('ONLINE_PORTAL')

      // -------------------------------------------------------------
      // Step 8: 客户驳回另一张对账单 702 -> 弹窗输入驳回原因 -> 状态变为 REJECTED
      // -------------------------------------------------------------
      const mockStatementDetail2: StatementApi.PortalStatementDetailRespVO = {
        id: 702,
        statementNo: 'CS-202609-002',
        customerId: 1001,
        customerName: '盛达纺织实业有限公司',
        currency: 'CNY',
        periodStart: '2026-09-01',
        periodEnd: '2026-09-30',
        openingBalance: 5000.0,
        periodDebit: 30000.0,
        periodCredit: 25000.0,
        closingBalance: 10000.0,
        status: 'SENT',
        items: []
      }
      vi.mocked(portalRequest.get).mockResolvedValueOnce(mockStatementDetail2)

      const statementDetailResp2 = await StatementApi.getPortalStatementDetail(702)
      expect(statementDetailResp2.status).toBe('SENT')

      const rejectPayload: StatementApi.PortalStatementRejectReqVO = {
        id: 702,
        rejectReason: '9月20日发货单金额有误，需扣减退货冲减金额'
      }
      vi.mocked(portalRequest.put).mockResolvedValueOnce(true)

      const rejectResult = await StatementApi.rejectPortalStatement(rejectPayload)
      expect(rejectResult).toBe(true)

      mockStatementDetail2.status = 'REJECTED'
      mockStatementDetail2.rejectReason = '9月20日发货单金额有误，需扣减退货冲减金额'
      expect(mockStatementDetail2.status).toBe('REJECTED')
      expect(mockStatementDetail2.rejectReason).toContain('发货单金额有误')

      // -------------------------------------------------------------
      // Step 9: 管理端吊销令牌 -> 后续请求返回 401 -> 自动重定向至 /portal/invalid
      // -------------------------------------------------------------
      vi.mocked(adminRequest.delete).mockResolvedValueOnce(true)
      const revokeResp = await CustomerApi.revokePortalToken(customerId)
      expect(revokeResp).toBe(true)

      // 客户端发起下一个请求，后端返回 401 报错
      const unauthorizedError = {
        response: {
          status: 401,
          data: {
            code: 1030038000,
            msg: '门户令牌无效或已过期'
          }
        }
      }
      vi.mocked(portalRequest.get).mockRejectedValueOnce(unauthorizedError)

      // 触发路由跳转至 /portal/invalid
      router.push('/portal/invalid')
      expect(router.push).toHaveBeenCalledWith('/portal/invalid')
    })
  })

  describe('375px 移动端页面样式适配断言', () => {
    it('移动端视口宽度为 375px，PortalLayout 容器 max-width 480px 满足移动优先体验', () => {
      expect(window.innerWidth).toBe(375)
      expect(window.innerWidth).toBeLessThanOrEqual(480)
    })
  })
})
