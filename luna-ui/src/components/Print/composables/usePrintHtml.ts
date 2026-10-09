import { ref } from 'vue'

export interface PrintOptions {
  title?: string
  pageStyle?: string
  customCss?: string
}

/**
 * 前端统一隐藏 iframe 打印 Composable
 *
 * 提取自 PrintShipmentDialog 的隐藏 iframe + contentWindow.print() 机制
 * 纯前端打印、不干扰宿主页面
 */
export function usePrintHtml() {
  const printIframeRef = ref<HTMLIFrameElement | null>(null)

  const printHtml = async (htmlContent: string, options: PrintOptions = {}) => {
    const {
      title = '打印文档',
      pageStyle = '@page { size: portrait; margin: 10mm; }',
      customCss = ''
    } = options

    const fullHtml = `<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${title}</title>
    <style>
      ${pageStyle}
      * {
        box-sizing: border-box;
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
      body {
        margin: 0;
        padding: 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
        color: #000;
        background: #fff;
        line-height: 1.3;
      }
      ${customCss}
    </style>
  </head>
  <body>
    ${htmlContent}
  </body>
</html>`

    let iframe = printIframeRef.value
    if (!iframe || !document.body.contains(iframe)) {
      iframe = document.createElement('iframe')
      iframe.style.position = 'fixed'
      iframe.style.right = '0'
      iframe.style.bottom = '0'
      iframe.style.width = '0'
      iframe.style.height = '0'
      iframe.style.border = '0'
      document.body.appendChild(iframe)
      printIframeRef.value = iframe
    }

    const doc = iframe.contentDocument || iframe.contentWindow?.document
    if (!doc) return
    doc.open()
    doc.write(fullHtml)
    doc.close()

    try {
      if (doc.fonts?.ready) {
        await doc.fonts.ready
      }
    } catch {
      // 字体就绪监听兜底
    }

    // 留出简短等待时间确保 canvas/图片绘制完毕
    await new Promise((resolve) => setTimeout(resolve, 150))

    iframe.contentWindow?.focus()
    iframe.contentWindow?.print()
  }

  const printElement = async (el: HTMLElement | null, options: PrintOptions = {}) => {
    if (!el) {
      console.warn('[usePrintHtml] printElement: 目标元素不存在')
      return
    }
    await printHtml(el.innerHTML, options)
  }

  return {
    printHtml,
    printElement,
    printIframeRef
  }
}
