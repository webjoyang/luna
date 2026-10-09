import { describe, it, expect } from 'vitest'

describe('Weaving Ticket QR Code URL Format', () => {
  it('should format QR code URL correctly as {baseUrl}/h5/scan/weaving-ticket?code={code}', () => {
    const baseUrl = 'https://erp.example.com'
    const ticketCode = 'BT-MTZZPB09190010101-0001'

    const cleanBaseUrl = baseUrl.trim().replace(/\/+$/, '')
    const qrUrl = `${cleanBaseUrl}/h5/scan/weaving-ticket?code=${ticketCode}`

    expect(qrUrl).toBe('https://erp.example.com/h5/scan/weaving-ticket?code=BT-MTZZPB09190010101-0001')
    expect(qrUrl).toContain('/h5/scan/weaving-ticket?code=')
  })

  it('should trim trailing slashes from baseUrl', () => {
    const baseUrl = 'https://erp.example.com///'
    const ticketCode = 'BT-MTZZPB09190010101-0002'

    const cleanBaseUrl = baseUrl.trim().replace(/\/+$/, '')
    const qrUrl = `${cleanBaseUrl}/h5/scan/weaving-ticket?code=${ticketCode}`

    expect(qrUrl).toBe('https://erp.example.com/h5/scan/weaving-ticket?code=BT-MTZZPB09190010101-0002')
  })
})
