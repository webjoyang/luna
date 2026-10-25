/**
 * 发货单与装箱清单标准 A4 打印 CSS 规则
 * 保证隐藏 iframe 打印时边框、网格、字号、边距与屏幕预览完全一致
 */
export const shipmentPrintCss = `
  @page {
    size: portrait;
    margin: 10mm;
  }
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
    font-size: 11px;
    line-height: 1.4;
  }
  .print-paper {
    width: 100%;
    margin: 0 auto;
  }
  .print-header {
    text-align: center;
    margin-bottom: 12px;
    position: relative;
  }
  .header-qr {
    position: absolute;
    right: 0;
    top: 0;
  }
  .print-header .font-bold {
    font-size: 20px;
    font-weight: bold;
    letter-spacing: 2px;
    margin-bottom: 2px;
  }
  .print-header .uppercase {
    font-size: 10px;
    color: #666;
    letter-spacing: 1px;
  }
  .meta-box {
    border: 1px solid #000;
    padding: 6px 10px;
    margin-bottom: 10px;
    background: #fafafa;
    font-size: 11px;
  }
  .grid {
    display: grid;
  }
  .grid-cols-3 {
    grid-template-columns: repeat(3, 1fr);
  }
  .grid-cols-4 {
    grid-template-columns: repeat(4, 1fr);
  }
  .col-span-2 { grid-column: span 2; }
  .col-span-3 { grid-column: span 3; }
  .gap-y-6px { row-gap: 5px; }
  .gap-x-12px { column-gap: 10px; }
  .gap-12px { gap: 10px; }
  .text-gray-600 { color: #555; }
  .font-mono { font-family: monospace, Courier, sans-serif; }
  .font-bold { font-weight: bold; }
  .font-medium { font-weight: 500; }
  .print-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 10.5px;
    margin-bottom: 10px;
    page-break-inside: auto;
  }
  .print-table tr {
    page-break-inside: avoid;
    page-break-after: auto;
  }
  .print-table th, .print-table td {
    border: 1px solid #000;
    padding: 4px 6px;
    line-height: 1.3;
  }
  .print-table th {
    background-color: #f0f0f0 !important;
    font-weight: bold;
  }
  .print-table tfoot td {
    font-weight: bold;
    background-color: #fafafa !important;
  }
  .text-center { text-align: center; }
  .text-left { text-align: left; }
  .text-right { text-align: right; }
  .summary-box {
    border: 1px solid #000;
    padding: 6px 10px;
    margin-bottom: 12px;
    background: #fafafa;
    font-size: 11px;
  }
  .flex { display: flex; }
  .justify-between { justify-content: space-between; }
  .items-center { align-items: center; }
  .sign-block {
    margin-top: 18px;
    page-break-inside: avoid;
    font-size: 11px;
  }
  .border-b { border-bottom: 1px solid #000; }
  .inline-block { display: inline-block; }
  .w-100px { width: 100px; }
  .border-t { border-top: 1px solid #000; }
  .border-dashed { border-style: dashed; }
  .border-gray-300 { border-color: #aaa; }
  .text-10px { font-size: 9.5px; }
  .text-gray-500 { color: #666; }
  .pt-6px { padding-top: 5px; }
  .mt-20px { margin-top: 16px; }
`
