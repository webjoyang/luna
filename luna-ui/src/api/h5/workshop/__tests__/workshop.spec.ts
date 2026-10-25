import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/config/axios', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn()
    }
  }
})

import request from '@/config/axios'
import {
  getMyTasks,
  getTicketByCode,
  startTicket,
  completeTicket,
  exceptionTicket,
  getInspectionPage,
  getInspection,
  ticketScan,
  saveInspectionItem,
  getRequisitionPage,
  getRequisition,
  getWarehouseSimpleList,
  getYarnBalance,
  dispatchRequisitionItem,
  type WorkshopRequisitionDispatchReqVO,
  H5_YARN_REQUISITION_NOT_OWNED
} from '../index'

describe('Workshop H5 API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getMyTasks should call request.get with correct url and params', async () => {
    const mockResponse = {
      list: [
        {
          id: 1,
          code: 'MT-001',
          weavingOrderId: 10,
          weavingOrderCode: 'ZZ-001',
          productName: '棉布',
          plannedQty: 20,
          actualQty: 5,
          status: 'IN_PROGRESS',
          ticketCounts: {
            total: 3,
            generated: 1,
            started: 1,
            completed: 1,
            inspected: 0,
            stocked: 0,
            exception: 0,
            voided: 0
          }
        }
      ],
      total: 1
    }

    vi.mocked(request.get).mockResolvedValueOnce(mockResponse)

    const params = { pageNo: 1, pageSize: 10, status: 'IN_PROGRESS' }
    const result = await getMyTasks(params)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/my-tasks',
      params
    })
    expect(result).toEqual(mockResponse)
    expect(result.list[0].ticketCounts?.total).toBe(3)
  })

  it('getMyTasks should support call without params', async () => {
    vi.mocked(request.get).mockResolvedValueOnce({ list: [], total: 0 })

    const result = await getMyTasks()

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/my-tasks',
      params: undefined
    })
    expect(result).toEqual({ list: [], total: 0 })
  })

  it('getTicketByCode should call request.get with correct url and code param', async () => {
    const mockTicket = {
      id: 101,
      code: 'BT-001',
      weavingOrderId: 201,
      weavingOrderCode: 'ZZ-001',
      machineTaskId: 301,
      machineTaskCode: 'MT-001',
      loomNo: 'LM01',
      productName: '40S 纯棉单面汗布',
      plannedWeight: 25.0,
      status: 'GENERATED',
      claimable: true
    }

    vi.mocked(request.get).mockResolvedValueOnce(mockTicket)

    const result = await getTicketByCode('BT-001')

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/ticket/get-by-code',
      params: { code: 'BT-001' }
    })
    expect(result).toEqual(mockTicket)
    expect(result.claimable).toBe(true)
  })

  it('startTicket should call request.post with /ticket/start and data', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(true)

    const payload = { code: 'BT-001', actualMachine: 'LM02' }
    const result = await startTicket(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/ticket/start',
      data: payload
    })
    expect(result).toBe(true)
  })

  it('completeTicket should call request.post with /ticket/complete and return resp', async () => {
    const mockResp = { alreadyCompleted: false }
    vi.mocked(request.post).mockResolvedValueOnce(mockResp)

    const payload = { code: 'BT-001' }
    const result = await completeTicket(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/ticket/complete',
      data: payload
    })
    expect(result).toEqual(mockResp)
  })

  it('exceptionTicket should call request.post with /ticket/exception and data', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(true)

    const payload = {
      code: 'BT-001',
      exceptionType: 'YARN_BREAK',
      exceptionNote: '断纱停机',
      exceptionPhotos: ['https://oss.example.com/p1.jpg']
    }
    const result = await exceptionTicket(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/ticket/exception',
      data: payload
    })
    expect(result).toBe(true)
  })

  it('getInspectionPage should call request.get with correct url and params', async () => {
    const mockPageResp = {
      list: [
        {
          id: 101,
          code: 'INS-20261021-001',
          type: 'GREIGE',
          status: 'ACTIVE',
          productName: '全棉汗布',
          productCode: 'P20261001',
          packageMode: 'PIECE',
          inspectionFactoryName: '一号质检车间',
          rollCount: 5
        }
      ],
      total: 1
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockPageResp)

    const params = { pageNo: 1, pageSize: 20, status: 'DRAFT,ACTIVE', type: 'GREIGE' }
    const result = await getInspectionPage(params)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/page',
      params
    })
    expect(result).toEqual(mockPageResp)
    expect(result.list[0].rollCount).toBe(5)
  })

  it('getInspection should call request.get with id param and return details with items and photos', async () => {
    const mockDetailResp = {
      id: 101,
      code: 'INS-20261021-001',
      type: 'GREIGE',
      status: 'ACTIVE',
      productName: '全棉汗布',
      readonly: false,
      items: [
        {
          id: 501,
          inspectionOrderId: 101,
          pieceNo: 'BT-001',
          grade: 'A',
          measuredWeight: 25.5,
          photos: ['https://oss.example.com/p1.jpg']
        }
      ]
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockDetailResp)

    const result = await getInspection(101)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/get',
      params: { id: 101 }
    })
    expect(result).toEqual(mockDetailResp)
    expect(result.readonly).toBe(false)
    expect(result.items[0].photos).toHaveLength(1)
  })

  it('ticketScan should call request.get with correct url and code param (first scan, item null)', async () => {
    const mockScanResp = {
      inspectionOrderId: 101,
      weavingTicketId: 201,
      suggestedPieceNo: 'BT-MTZZPB09190010101-0001',
      item: null
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockScanResp)

    const result = await ticketScan('BT-MTZZPB09190010101-0001')

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/ticket-scan',
      params: { code: 'BT-MTZZPB09190010101-0001' }
    })
    expect(result).toEqual(mockScanResp)
    expect(result.inspectionOrderId).toBe(101)
    expect(result.weavingTicketId).toBe(201)
    expect(result.suggestedPieceNo).toBe('BT-MTZZPB09190010101-0001')
    expect(result.item).toBeNull()
  })

  it('ticketScan should call request.get with correct url and return existing item for re-scan', async () => {
    const mockScanResp = {
      inspectionOrderId: 101,
      weavingTicketId: 201,
      suggestedPieceNo: 'BT-MTZZPB09190010101-0001',
      item: {
        id: 501,
        inspectionOrderId: 101,
        pieceNo: 'BT-MTZZPB09190010101-0001',
        grade: 'A',
        measuredWeight: 26.5,
        photos: ['https://oss.example.com/p1.jpg']
      }
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockScanResp)

    const result = await ticketScan('BT-MTZZPB09190010101-0001')

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/ticket-scan',
      params: { code: 'BT-MTZZPB09190010101-0001' }
    })
    expect(result.inspectionOrderId).toBe(101)
    expect(result.item?.id).toBe(501)
    expect(result.item?.grade).toBe('A')
  })

  it('saveInspectionItem should call request.post with /lunaerp/h5/workshop/inspection/item/save and return id', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(501)

    const payload = {
      inspectionOrderId: 101,
      weavingTicketId: 201,
      pieceNo: 'BT-MTZZPB09190010101-0001',
      grade: 'A',
      measuredWeight: 25.5,
      lengthM: 100,
      remark: '布面平整',
      photos: ['https://oss.example.com/p1.jpg']
    }

    const result = await saveInspectionItem(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/item/save',
      data: payload
    })
    expect(result).toBe(501)
  })

  it('saveInspectionItem should support updating existing item with id', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(501)

    const payload = {
      id: 501,
      inspectionOrderId: 101,
      weavingTicketId: 201,
      pieceNo: 'BT-MTZZPB09190010101-0001',
      grade: 'B',
      measuredWeight: 26.0,
      lengthM: 102
    }

    const result = await saveInspectionItem(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/item/save',
      data: payload
    })
    expect(result).toBe(501)
  })

  it('saveInspectionItem should support creating finished ROLL item with vatNo and rollNo', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(601)

    const payload = {
      inspectionOrderId: 102,
      vatNo: 'GH-20261021-01',
      rollNo: 'R001',
      grade: 'A',
      measuredWeight: 28.5,
      lengthM: 110,
      remark: '成品首卷合格',
      photos: ['https://oss.example.com/roll1.jpg']
    }

    const result = await saveInspectionItem(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/item/save',
      data: payload
    })
    expect(result).toBe(601)
  })

  it('saveInspectionItem should support updating finished ROLL item with id', async () => {
    vi.mocked(request.post).mockResolvedValueOnce(601)

    const payload = {
      id: 601,
      inspectionOrderId: 102,
      vatNo: 'GH-20261021-01',
      rollNo: 'R001',
      grade: 'B',
      measuredWeight: 28.0,
      lengthM: 108
    }

    const result = await saveInspectionItem(payload)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/inspection/item/save',
      data: payload
    })
    expect(result).toBe(601)
  })

  it('getRequisitionPage should call request.get with correct url and params', async () => {
    const mockResponse = {
      list: [
        {
          id: 101,
          code: 'YS-001',
          weavingOrderId: 201,
          weavingOrderCode: 'ZZ-001',
          status: 'ACTIVE',
          createTime: '2026-10-21 10:00:00'
        }
      ],
      total: 1
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockResponse)

    const params = { pageNo: 1, pageSize: 10, status: 'ACTIVE,PARTIAL' }
    const result = await getRequisitionPage(params)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/requisition/page',
      params
    })
    expect(result).toEqual(mockResponse)
  })

  it('getRequisition should call request.get with correct url and id', async () => {
    const mockResponse = {
      id: 101,
      code: 'YS-001',
      weavingOrderId: 201,
      weavingOrderCode: 'ZZ-001',
      status: 'ACTIVE',
      defaultWarehouseId: 1,
      items: [
        {
          id: 1001,
          requisitionId: 101,
          yarnSpecId: 10,
          yarnSpecName: '40S全棉',
          requiredQty: 100,
          dispatchedQty: 30,
          unit: 'KG'
        }
      ]
    }
    vi.mocked(request.get).mockResolvedValueOnce(mockResponse)

    const result = await getRequisition(101)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/requisition/get',
      params: { id: 101 }
    })
    expect(result).toEqual(mockResponse)
  })

  it('getWarehouseSimpleList should call request.get with correct url', async () => {
    const mockResponse = [
      { id: 1, name: '原料仓' },
      { id: 2, name: '备用仓' }
    ]
    vi.mocked(request.get).mockResolvedValueOnce(mockResponse)

    const result = await getWarehouseSimpleList()

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/warehouse/simple-list'
    })
    expect(result).toEqual(mockResponse)
  })

  it('getYarnBalance should call request.get with warehouseId and yarnSpecId', async () => {
    vi.mocked(request.get).mockResolvedValueOnce(250.5)

    const result = await getYarnBalance(1, 10)

    expect(request.get).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/yarn-balance',
      params: { warehouseId: 1, yarnSpecId: 10 }
    })
    expect(result).toBe(250.5)
  })

  it('H5_YARN_REQUISITION_NOT_OWNED constant should match error code 1030039006', () => {
    expect(H5_YARN_REQUISITION_NOT_OWNED).toBe(1030039006)
  })

  it('dispatchRequisitionItem should call request.post with correct url and data', async () => {
    const data: WorkshopRequisitionDispatchReqVO = {
      itemId: 1001,
      warehouseId: 1,
      locationId: 10,
      dispatchQty: 50.0,
      remark: '移动端发纱',
      requestId: '550e8400-e29b-41d4-a716-446655440000'
    }
    vi.mocked(request.post).mockResolvedValueOnce(true)

    const result = await dispatchRequisitionItem(data)

    expect(request.post).toHaveBeenCalledWith({
      url: '/lunaerp/h5/workshop/requisition-item/dispatch',
      data
    })
    expect(result).toBe(true)
  })
})
