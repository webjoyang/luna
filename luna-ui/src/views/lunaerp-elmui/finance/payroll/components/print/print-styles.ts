/**
 * 员工工资表 A4 横向/纵向套打标准 CSS 规则
 * 对齐 04 号通用打印内核
 */
export const payrollPrintCss = `
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
    font-size: 12px;
    line-height: 1.5;
  }
  .payroll-sheet-wrapper {
    width: 100%;
    max-width: 180mm;
    margin: 0 auto;
  }
  .print-header {
    text-align: center;
    margin-bottom: 16px;
    position: relative;
  }
  .print-header .main-title {
    font-size: 22px;
    font-weight: 700;
    letter-spacing: 4px;
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
    border: 1px solid #333;
    padding: 10px 14px;
    margin-bottom: 14px;
    background: #fafafa;
    font-size: 12px;
  }
  .meta-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    row-gap: 8px;
    column-gap: 16px;
  }
  .meta-item {
    display: flex;
    align-items: center;
  }
  .meta-item .label {
    color: #555;
    white-space: nowrap;
  }
  .meta-item .value {
    color: #111;
  }
  .table-box {
    margin-bottom: 14px;
  }
  .data-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 11px;
    table-layout: fixed;
  }
  .data-table th,
  .data-table td {
    border: 1px solid #333;
    padding: 6px 8px;
    word-break: break-all;
  }
  .data-table th {
    background-color: #f0f0f0;
    font-weight: 600;
    text-align: center;
  }
  .data-table .total-row td {
    background-color: #f9f9f9;
    font-weight: 700;
  }
  .sign-box {
    border: 1px solid #333;
    padding: 12px 16px;
    margin-top: 16px;
    background: #fff;
  }
  .sign-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
  }
  .sign-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .sign-item .sign-label {
    font-size: 12px;
    font-weight: 600;
    color: #333;
  }
  .sign-item .sign-line {
    border-bottom: 1px solid #666;
    height: 24px;
  }
  .page-break-after {
    page-break-after: always;
  }
`
