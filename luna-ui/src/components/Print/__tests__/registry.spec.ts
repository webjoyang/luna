import { describe, it, expect, vi } from 'vitest'

vi.mock('@/api/lunaerp-elmui/production/weaving-ticket', () => ({
  getWeavingTicketPrintData: vi.fn()
}))

vi.mock('@/views/lunaerp-elmui/production/weaving-order/components/print/WeavingTicketLabel.vue', () => ({
  default: { name: 'WeavingTicketLabel' }
}))

import {
  printTemplateRegistry,
  getTemplatesByBizType,
  getTemplate,
  registerPrintTemplate,
  PrintTemplateReg
} from '../registry'

describe('Print Template Registry', () => {
  it('should have WEAVING_TICKET_LABEL statically registered', () => {
    const weavingTemplates = getTemplatesByBizType('WEAVING_TICKET')
    expect(weavingTemplates.length).toBeGreaterThanOrEqual(1)

    const labelTemplate = weavingTemplates.find((t) => t.templateCode === 'WEAVING_TICKET_LABEL')
    expect(labelTemplate).toBeDefined()
    expect(labelTemplate?.paper).toBe('LABEL-100x60')
    expect(labelTemplate?.permission).toBe('lunaerp:production:weaving-ticket:print')
    expect(typeof labelTemplate?.fetchApi).toBe('function')
  })

  it('getTemplate should return default or specific template', () => {
    const defaultTpl = getTemplate('WEAVING_TICKET')
    expect(defaultTpl).toBeDefined()
    expect(defaultTpl?.templateCode).toBe('WEAVING_TICKET_LABEL')

    const specificTpl = getTemplate('WEAVING_TICKET', 'WEAVING_TICKET_LABEL')
    expect(specificTpl?.templateCode).toBe('WEAVING_TICKET_LABEL')

    const nonExistent = getTemplate('NON_EXISTENT_BIZ')
    expect(nonExistent).toBeUndefined()
  })

  it('registerPrintTemplate should allow registering new templates dynamically', () => {
    const dummyComponent = { name: 'DummyDoc' } as any
    const dummyTemplate: PrintTemplateReg = {
      bizType: 'DUMMY_BIZ',
      templateCode: 'DUMMY_DOC',
      name: '测试文档',
      paper: 'A4',
      permission: 'lunaerp:dummy:print',
      component: dummyComponent,
      fetchApi: async () => ({})
    }

    registerPrintTemplate(dummyTemplate)
    const found = getTemplate('DUMMY_BIZ', 'DUMMY_DOC')
    expect(found).toBeDefined()
    expect(found?.name).toBe('测试文档')
    expect(found?.paper).toBe('A4')
  })
})
