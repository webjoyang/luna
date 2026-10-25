/**
 * 供应商对账单 A4 纵向套打标准 CSS 规则
 * 保证在打印预览和隐藏 iframe 打印时边框、排版、字号、边距完全一致
 */
export const supplierReconPrintCss = `
  @page {
    size: A4 portrait;
    margin: 12mm 15mm;
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
    color: #111;
    background: #fff;
    font-size: 11px;
    line-height: 1.4;
  }
  .supplier-recon-sheet-wrapper {
    width: 100%;
    max-width: 190mm;
    margin: 0 auto;
  }
  .print-header {
    text-align: center;
    margin-bottom: 12px;
  }
  .print-header .main-title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 3px;
    margin-bottom: 4px;
    color: #000;
  }
  .print-header .sub-title {
    font-size: 11px;
    color: #666;
    letter-spacing: 1px;
    text-transform: uppercase;
  }
  .meta-box {
    border: 1px solid #000;
    padding: 8px 12px;
    margin-bottom: 12px;
    background: #fafafa;
    font-size: 11px;
  }
  .meta-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    row-gap: 6px;
    column-gap: 12px;
  }
  .meta-item {
    display: flex;
    align-items: baseline;
  }
  .meta-item .label {
    color: #555;
    white-space: nowrap;
  }
  .meta-item .value {
    color: #000;
    font-weight: 500;
  }
  .meta-item.span-2 {
    grid-column: span 2;
  }
  .meta-item.span-3 {
    grid-column: span 3;
  }
  .print-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 12px;
    font-size: 10.5px;
  }
  .print-table th, .print-table td {
    border: 1px solid #333;
    padding: 5px 6px;
    vertical-align: middle;
  }
  .print-table th {
    background-color: #f2f2f2;
    font-weight: 600;
    text-align: center;
  }
  .print-table td.text-center { text-align: center; }
  .print-table td.text-left { text-align: left; }
  .print-table td.text-right { text-align: right; }
  .amount-summary-box {
    border: 1px solid #000;
    padding: 8px 12px;
    margin-bottom: 16px;
    background: #fdfdfd;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .amount-subtotals {
    display: flex;
    gap: 16px;
    font-size: 11px;
    color: #444;
  }
  .amount-grand-total {
    font-size: 14px;
    font-weight: bold;
    color: #000;
  }
  .signature-box {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin-top: 24px;
    page-break-inside: avoid;
  }
  .sign-col {
    border: 1px solid #666;
    padding: 12px 16px;
    min-height: 100px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }
  .sign-col .sign-title {
    font-weight: 600;
    font-size: 12px;
    margin-bottom: 8px;
    border-bottom: 1px dashed #ccc;
    padding-bottom: 4px;
  }
  .sign-lines {
    font-size: 11px;
    color: #333;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .sign-row {
    display: flex;
    justify-content: space-between;
  }
`
