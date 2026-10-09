import PrintDispatcher from './src/PrintDispatcher.vue'
import PrintPaper from './src/PrintPaper.vue'
import QrSlot from './src/QrSlot.vue'
import { usePrintHtml } from './composables/usePrintHtml'
import {
  printTemplateRegistry,
  registerPrintTemplate,
  getTemplatesByBizType,
  getTemplate
} from './registry'
import type { PrintTemplateReg } from './registry'

export {
  PrintDispatcher,
  PrintPaper,
  QrSlot,
  usePrintHtml,
  printTemplateRegistry,
  registerPrintTemplate,
  getTemplatesByBizType,
  getTemplate
}

export type { PrintTemplateReg }
