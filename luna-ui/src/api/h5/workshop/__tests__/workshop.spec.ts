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
import { getMyTasks } from '../index'

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
})
