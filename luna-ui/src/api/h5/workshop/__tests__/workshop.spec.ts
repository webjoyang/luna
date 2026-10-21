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
import { getMyTasks, getTicketByCode, startTicket, completeTicket, exceptionTicket } from '../index'

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
})
